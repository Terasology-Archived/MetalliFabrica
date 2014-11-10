package org.terasology.MetalliFabrica;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

@RegisterSystem(RegisterMode.AUTHORITY)

public class ChiselSystem extends BaseComponentSystem {
	
	@In
	private BlockManager blockManager;
	
	@In
	private WorldProvider worldProvider;
	
	//private static final Logger logger = LoggerFactory.getLogger(ChiselSystem.class);
	
@ReceiveEvent(components = {ChiselComponent.class, ItemComponent.class})

public void OnActivate (ActivateEvent event, EntityRef entity){
	
	int rotateInt = 0;
	
	EntityRef target = event.getTarget();

    BlockComponent blockComponent = target.getComponent(BlockComponent.class);
    Block targetBlock = blockComponent.getBlock();
    
    if(blockComponent == null){
    	//GUI
    	
    } else {
    	
    switch (targetBlock.toString()){ 
    	case "core:stone": 
    		rotateInt = 1;
    		Block engravedBlockStone = blockManager.getBlock("MetalliFabrica:EngravedStone" + rotateInt );
    		worldProvider.setBlock(blockComponent.getPosition(), engravedBlockStone);
    break;
    
    	case "MetalliFabrica:EngravedStone1":
    		rotateInt = 2;
    		Block engravedBlockStone2 = blockManager.getBlock("MetalliFabrica:EngravedStone" + rotateInt);
    		worldProvider.setBlock(blockComponent.getPosition(), engravedBlockStone2);
    break;
    
    	case "MetalliFabrica:EngravedStone2":
    		rotateInt = 3;
    		Block EngravedblockStone3 = blockManager.getBlock("MetalliFabrica:EngravedStoneOhm");
    		worldProvider.setBlock(blockComponent.getPosition(), EngravedblockStone3);
    break;
    
    	case "MetalliFabrica:EngravedStoneOhm":
    		rotateInt = 0;
    		Block blockStone = blockManager.getBlock("core:stone");
    		worldProvider.setBlock(blockComponent.getPosition(), blockStone);
    
    	default:;
    break;
    }
    
    }
    
}


}