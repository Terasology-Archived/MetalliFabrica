package org.terasology.MetalliFabrica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.interactions.InteractionEndEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
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

public class SmelterySystem extends BaseComponentSystem{

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

    @ReceiveEvent(priority = EventPriority.PRIORITY_CRITICAL)
    public void OnActivateSmeltery (ActivateEvent event, EntityRef smelteryEntity,  SmelteryComponent smelteryComponent, BlockComponent blockComponent, InventoryComponent inventoryComponent) {

        BlockComponent smelteryBlock = smelteryEntity.getComponent(BlockComponent.class);
        Vector3i targetBlockPos = smelteryBlock.getPosition();
        lastHeight = height;

        // calculate how many layers are currently above the smeltery.
        CalculateSmelteryHeight (targetBlockPos);

        //create the outer and inner Regions and check the build
        CreateAndCheckArea(targetBlockPos, new Vector3i(5, height, 5));

        //get number and reference of items in each slot of the inventory
        GetItemsInInventory(smelteryEntity);

    }

    public void CalculateSmelteryHeight (Vector3i smelteryBlockPos){

        Region3i heightCheckRegion = Region3i.createFromMinMax(smelteryBlockPos, new Vector3i(smelteryBlockPos.x, smelteryBlockPos.y + 19, smelteryBlockPos.z));
        Iterator<Vector3i> heightCheck = heightCheckRegion.iterator();

        while (heightCheck.hasNext()){
            Vector3i heightVectorPos = heightCheck.next();
            Block currentHeightCheckBlock = worldProvider.getBlock(heightVectorPos);
            BlockUri currentHeightCheckBlockURI = currentHeightCheckBlock.getURI();
            height = 0;

            if(!currentHeightCheckBlockURI.toString().equals("engine:air")){
                EntityRef currentHeightBlockEnt = currentHeightCheckBlock.getEntity();
                logger.info("check");
                BlockComponent curHeightBlockCom = currentHeightBlockEnt.getComponent(BlockComponent.class);
                if(currentHeightBlockEnt.hasComponent(SmelteryCountComponent.class)){
                    height = height + 1;
                    logger.info(Integer.toString(height));
                }
            }
        }
    }

    public void CreateAndCheckArea (Vector3i smelteryBlockPos, Vector3i size){

        Region3i outsideRegion = Region3i.createFromMinAndSize(smelteryBlockPos, size);
        Vector3i OutsideMax = outsideRegion.max();
        Vector3i OutsideMin = outsideRegion.min();
        insideRegion = Region3i.createBounded(new Vector3i(OutsideMin.x+1, OutsideMin.y+1, OutsideMin.z+1), new Vector3i(OutsideMax.x-1, OutsideMax.y-1, OutsideMax.z-1));

        CheckRegions(outsideRegion, insideRegion);
    }

    public void CheckRegions (Region3i outsideRegion, Region3i insideRegion){

        logger.info("starting check!");

        Iterator<Vector3i> outsideRegionCheck = outsideRegion.subtract(insideRegion);

        while (outsideRegionCheck.hasNext()){
            Vector3i curOutsideBlockPos = outsideRegionCheck.next();
            logger.info(curOutsideBlockPos.toString());
            Block curBlock = worldProvider.getBlock(curOutsideBlockPos);
            EntityRef curOutsideBlockEnt = curBlock.getEntity();

            if(curOutsideBlockEnt.hasComponent(SmelteryCountComponent.class) || curOutsideBlockEnt.hasComponent(SmelteryComponent.class)){
                outsideRegionCheckBool = true;
                logger.info("block Passed!");
            } else {
                outsideRegionCheckBool = false;
                logger.info("Block Failed! :(");
            }
        }

        Iterator<Vector3i> insideRegionBlockCount = insideRegion.iterator();

        while (insideRegionBlockCount.hasNext()){
            Vector3i VectorPos = insideRegionBlockCount.next();
            Block curInsideBlock = worldProvider.getBlock(VectorPos);
            if(curInsideBlock.getURI().equals("engine:air")) {
                insideBlockCount = insideBlockCount + 1;
            }
        }

        availableSlotsInSmeltery = insideBlockCount;
    }

    public void GetItemsInInventory (EntityRef smelteryEntity){
        InventoryComponent smelteryInventoryCom = smelteryEntity.getComponent(InventoryComponent.class);
        int slotCount = inventoryManager.getNumSlots(smelteryEntity);
        logger.info("slotCount : " + Integer.toString(slotCount));
            for (int i = 0; i < slotCount; i++) {
                EntityRef itemsInSlots = smelteryInventoryCom.itemSlots.get(i);
                int curStackSize = inventoryManager.getStackSize(itemsInSlots);
                logger.info(Integer.toString(curStackSize));
                BlockItemComponent blockItemCom = inventoryManager.getItemInSlot(smelteryEntity, i).getComponent(BlockItemComponent.class);
                if(blockItemCom != null) {
                    logger.info(blockItemCom.blockFamily.getURI().toString());
                    String blockItemName = blockItemCom.blockFamily.getURI().toString();
                    SmeltToLiquid(blockItemName);
                }
            }
    }

    public void SmeltToLiquid (String liquidName) {
        Iterator<Vector3i> insideRegionSpaceCheck = insideRegion.iterator();
        while(insideRegionSpaceCheck.hasNext()){
            Vector3i curCheckPos = insideRegionSpaceCheck.next();
            Block curCheckBlock = worldProvider.getBlock(curCheckPos);
            Block curPlaceBlock = blockManager.getBlock(liquidName);
            if(liquidName != null) {
                if (curCheckBlock.getURI().equals("engine:air")) {
                    worldProvider.setBlock(curCheckPos, curPlaceBlock);
                }
            }
        }

    }

}
