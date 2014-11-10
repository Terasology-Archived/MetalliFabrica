package org.terasology.MetalliFabrica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Region3i;
import org.terasology.math.Vector3i;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.items.BlockItemComponent;

import java.util.Iterator;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SmelterySystem extends BaseComponentSystem {

    @In
    public WorldProvider worldProvider;

    @In
    public InventoryManager inventoryManager;

    @In
    public BlockManager blockManager;

    public int height = 1;
    public int lastHeight;
    public int insideBlockCount;
    public int availableSlotsInSmeltery;

    public Region3i insideRegion;

    public boolean outsideRegionCheckBool;

    private static final Logger logger = LoggerFactory.getLogger(SmelterySystem.class);

    @ReceiveEvent(components = {SmelteryComponent.class}, priority = EventPriority.PRIORITY_CRITICAL)
    public void onActivateSmeltery(ActivateEvent event, EntityRef smelteryEntity, SmelteryComponent smelteryComponent, BlockComponent blockComponent, InventoryComponent inventoryComponent) {

        BlockComponent smelteryBlock = smelteryEntity.getComponent(BlockComponent.class);
        Vector3i targetBlockPos = smelteryBlock.getPosition();
        lastHeight = height;

        // calculate how many layers are currently above the smeltery.
        calculateSmelteryHeight(targetBlockPos);

        if (lastHeight != height) {
            //create the outer and inner Regions and check the build
            createArea(targetBlockPos, new Vector3i(5, height, 5));
            setregionBool(outsideRegionCheckBool, smelteryComponent);
        }

        if (outsideRegionCheckBool) {
            //get number and reference of items in each slot of the inventory
            getItemsInInventory(smelteryEntity, inventoryComponent);
        }
    }

    @Override
    public void update (float delta){
        super.update(delta);
    }

    public void calculateSmelteryHeight(Vector3i smelteryBlockPos) {

        Region3i heightCheckRegion = Region3i.createFromMinMax(smelteryBlockPos, new Vector3i(smelteryBlockPos.x, smelteryBlockPos.y + 19, smelteryBlockPos.z));
        Iterator<Vector3i> heightCheck = heightCheckRegion.iterator();
        height = 0;

        while (heightCheck.hasNext()) {
            Vector3i heightVectorPos = heightCheck.next();
            Block currentHeightCheckBlock = worldProvider.getBlock(heightVectorPos);
            BlockUri currentHeightCheckBlockURI = currentHeightCheckBlock.getURI();

            if (!currentHeightCheckBlockURI.toString().equals("engine:air")) {
                EntityRef currentHeightBlockEnt = currentHeightCheckBlock.getEntity();
                logger.info("check");
                logger.info(currentHeightCheckBlockURI.toString());
                BlockComponent curHeightBlockCom = currentHeightBlockEnt.getComponent(BlockComponent.class);
                if (currentHeightBlockEnt.hasComponent(SmelteryCountComponent.class)) {
                    height = height + 1;
                    logger.info(Integer.toString(height));
                }
            }
        }
    }

    public void createArea(Vector3i smelteryBlockPos, Vector3i size) {

        Region3i outsideRegion = Region3i.createFromMinAndSize(smelteryBlockPos, size);
        Vector3i outsideMax = outsideRegion.max();
        Vector3i outsideMin = outsideRegion.min();
        insideRegion = Region3i.createBounded(new Vector3i(outsideMin.x + 1, outsideMin.y + 1, outsideMin.z + 1), new Vector3i(outsideMax.x - 1, outsideMax.y - 1, outsideMax.z - 1));

        checkRegions(outsideRegion, insideRegion);
    }

    public void checkRegions(Region3i outsideRegion, Region3i innerRegion) {

        logger.info("starting check!");

        Iterator<Vector3i> outsideRegionCheck = outsideRegion.subtract(innerRegion);

        while (outsideRegionCheck.hasNext()) {
            Vector3i curOutsideBlockPos = outsideRegionCheck.next();
            Block curBlock = worldProvider.getBlock(curOutsideBlockPos);
            EntityRef curOutsideBlockEnt = curBlock.getEntity();

            if (curOutsideBlockEnt.hasComponent(SmelteryCountComponent.class) || curOutsideBlockEnt.hasComponent(SmelteryComponent.class)) {
                outsideRegionCheckBool = true;
            } else {
                outsideRegionCheckBool = false;
                logger.info("Block Failed! :(");
            }
        }

        Iterator<Vector3i> insideRegionBlockCount = insideRegion.iterator();

        while (insideRegionBlockCount.hasNext()) {
            Vector3i vectorPos = insideRegionBlockCount.next();
            Block curInsideBlock = worldProvider.getBlock(vectorPos);
            if (curInsideBlock.getURI().toString().equals("engine:air")) {
                insideBlockCount = insideBlockCount + 1;
            }
        }
        availableSlotsInSmeltery = insideBlockCount;
    }

    public void setregionBool(boolean checkBool, SmelteryComponent smeltCom) {
        smeltCom.regionCheckBool = checkBool;
    }

    public void getItemsInInventory(EntityRef smelteryEntity, InventoryComponent smelteryInventoryCom) {
        int slotCount = inventoryManager.getNumSlots(smelteryEntity);
        for (int i = 0; i < slotCount; i++) {
            EntityRef itemsInSlots = smelteryInventoryCom.itemSlots.get(i);
            //int curStackSize = inventoryManager.getStackSize(itemsInSlots);
            EntityRef curBlockEnt = inventoryManager.getItemInSlot(smelteryEntity, i);
            BlockItemComponent curBlockItemCom = curBlockEnt.getComponent(BlockItemComponent.class);
            logger.info(curBlockEnt.getPrefabURI().toString());
            if (curBlockEnt != null) {
                if (curBlockItemCom != null) {
                    logger.info(curBlockItemCom.blockFamily.getURI().toString());
                    String blockItemName = curBlockItemCom.blockFamily.getURI().toString();
                    testSmelt(blockItemName);
                    inventoryManager.removeItem(smelteryEntity, curBlockEnt, itemsInSlots, true, 1);
                }
            }
        }
    }

    public void testSmelt(String liquidName) {
        Iterator<Vector3i> insideRegionSpaceCheck = insideRegion.iterator();
        while (insideRegionSpaceCheck.hasNext()) {
            Vector3i curCheckPos = insideRegionSpaceCheck.next();
            Block curCheckBlock = worldProvider.getBlock(curCheckPos);
            Block curPlaceBlock = blockManager.getBlock(liquidName);
            if (liquidName != null) {
                if (curCheckBlock.getURI().toString().equals("engine:air")) {
                    worldProvider.setBlock(curCheckPos, curPlaceBlock);
                    //logger.info(blockManager.listAvailableBlockUris().toString());
                    break;
                }
            }
        }
    }
}
