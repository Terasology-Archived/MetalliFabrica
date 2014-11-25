package org.terasology.MetalliFabrica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.delay.DelayedActionTriggeredEvent;
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
import org.terasology.engine.Time;

import java.util.HashMap;
import java.util.Iterator;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SmelterySystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final long UPDATE_INTERVAL = 250;
    private static final Logger logger = LoggerFactory.getLogger(SmelterySystem.class);

    public int height = 1;
    public int lastHeight;
    public int totalAvailableSlotsInSmeltery;
    public int currentSmeltingProcesses;

    public Region3i insideRegion;

    public boolean outsideRegionCheckBool;

    public EntityRef curSmelteryEntity;

    public InventoryComponent smelteryInventoryCom;
    public SmelteryComponent smelteryCom;

    @In
    public WorldProvider worldProvider;
    @In
    public InventoryManager inventoryManager;
    @In
    public BlockManager blockManager;
    @In
    Time time;
    @In
    DelayedActionTriggeredEvent delayedActionTriggeredEvent;

    long nextUpdateTime;

    public HashMap<String, Integer> liquidValues;

    @ReceiveEvent(components = {SmelteryComponent.class}, priority = EventPriority.PRIORITY_CRITICAL)
    public void onActivateSmeltery(ActivateEvent event, EntityRef smelteryEntity, SmelteryComponent smelteryComponent, InventoryComponent inventoryComponent) {
        curSmelteryEntity = smelteryEntity;
        smelteryInventoryCom = inventoryComponent;
        smelteryCom = smelteryComponent;
        BlockComponent smelteryBlock = smelteryEntity.getComponent(BlockComponent.class);
        Vector3i targetBlockPos = smelteryBlock.getPosition();
        lastHeight = height;

        //gets direction in which region should be built

        // calculate how many layers are currently above the smeltery.
        calculateSmelteryHeight(targetBlockPos);

        // if the height has changed,
        //create the outer and inner Regions and check the build
        if (lastHeight != height) {
            createArea(targetBlockPos, new Vector3i(5, height, 5));
            checkInsideRegion(insideRegion);
        }
    }

    @Override
    public void update(float delta) {
        long currentTime = time.getGameTimeInMs();
        if (currentTime > nextUpdateTime) {
            nextUpdateTime = currentTime + UPDATE_INTERVAL;
            if (outsideRegionCheckBool) {
                //get number and reference of items in each slot of the inventory and send them to "Smelt"
                getItemsInInventory(curSmelteryEntity, smelteryInventoryCom);
            }
        }
    }

    public void calculateSmelteryHeight(Vector3i smelteryBlockPos) {

        Region3i heightCheckRegion = Region3i.createFromMinMax(smelteryBlockPos, new Vector3i(smelteryBlockPos.x, smelteryBlockPos.y + 19, smelteryBlockPos.z));
        Iterator<Vector3i> heightCheck = heightCheckRegion.iterator();
        height = 0;

        while (heightCheck.hasNext()) {
            Vector3i heightVectorPos = heightCheck.next();
            Block currentHeightCheckBlock = worldProvider.getBlock(heightVectorPos);
            EntityRef currentHeightBlockEnt = currentHeightCheckBlock.getEntity();
            if (currentHeightBlockEnt.hasComponent(SmelteryCountComponent.class)) {
                height = height + 1;
            }
        }
    }

    public void createArea(Vector3i smelteryBlockPos, Vector3i size) {

        Region3i outsideRegion = Region3i.createFromMinAndSize(smelteryBlockPos, size);
        Vector3i outsideMax = outsideRegion.max();
        Vector3i outsideMin = outsideRegion.min();
        insideRegion = Region3i.createBounded(new Vector3i(outsideMin.x + 1, outsideMin.y + 1, outsideMin.z + 1), new Vector3i(outsideMax.x - 1, outsideMax.y - 1, outsideMax.z - 1));

        checkOutsideRegion(outsideRegion, insideRegion);
    }

    public void checkOutsideRegion(Region3i outsideRegion, Region3i innerRegion) {
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
    }

    public void checkInsideRegion(Region3i newInsideRegion) {
        int insideBlockCount = 0;
        Iterator<Vector3i> insideRegionBlockCount = newInsideRegion.iterator();

        while (insideRegionBlockCount.hasNext()) {
            Vector3i vectorPos = insideRegionBlockCount.next();
            Block curInsideBlock = worldProvider.getBlock(vectorPos);
            if (curInsideBlock.getURI().toString().equals("engine:air")) {
                insideBlockCount = insideBlockCount + 1;
            }
        }
        totalAvailableSlotsInSmeltery = insideBlockCount;
    }

    public void getItemsInInventory(EntityRef smelteryEntity, InventoryComponent smelteryInvCom) {
        int slotCount = inventoryManager.getNumSlots(smelteryEntity);
        logger.info(Integer.toString(slotCount));
        for (int i = 0; i < slotCount; i++) {
            EntityRef stackedItemsInSlots = smelteryInvCom.itemSlots.get(i);
            EntityRef curBlockEnt = inventoryManager.getItemInSlot(smelteryEntity, i);
            BlockItemComponent curBlockItemCom = curBlockEnt.getComponent(BlockItemComponent.class);
            if (totalAvailableSlotsInSmeltery > 0) {
                if (curBlockItemCom != null) {
                    String blockItemName = curBlockItemCom.blockFamily.getURI().toString();
                    startSmelt(blockItemName);
                    inventoryManager.removeItem(smelteryEntity, curBlockEnt, stackedItemsInSlots, true, 1);
                }
            }
        }
    }

    public void startSmelt(String liquidName) {
        //remove 1 from the available slot count
        totalAvailableSlotsInSmeltery = totalAvailableSlotsInSmeltery - 1;
        //tracks a separate int that dictates how many current smelting processes are going
        currentSmeltingProcesses = currentSmeltingProcesses + 1;
        // add current "smelt Action" to delayed action queue that triggers the endSmelt method;

        endSmelt(liquidName);

        /* initiates the fuel consumption.
        just so i dont forget
            boolean isSmelting = true;
        */
    }

    public void endSmelt(String liquidName) {
        //remove 1 from processesCount and add 1 to the available slot count
        currentSmeltingProcesses = currentSmeltingProcesses - 1;
        totalAvailableSlotsInSmeltery = totalAvailableSlotsInSmeltery + 1;
        //remove the "smelt Action" from the delayed action queue
        // place the liquid inside the "inner Region"
        //track the liquids and their amounts
        SmelteryComponent curSmelteryCom = smelteryCom;

        if (!curSmelteryCom.liquidsAndValues.containsKey(liquidName)) {
            curSmelteryCom.liquidsAndValues.put(liquidName, 100);
        } else {
            Integer currentLiquidValue = curSmelteryCom.liquidsAndValues.get(liquidName);
            currentLiquidValue = currentLiquidValue + 100;
            curSmelteryCom.liquidsAndValues.put(liquidName, currentLiquidValue);
        }

            /* stops the boolean isSmelting
            if(!delayedActionManager.hasDelayedAction){
                boolean isSmelting = false;
            }
            */
    }

    public void getItemsInFuelSlot(EntityRef smelteryEntity, InventoryComponent fuelInvCom) {
        //gets the items in the fuel slot, checks them for burning capability, and adds to the fuel.

    }
}
