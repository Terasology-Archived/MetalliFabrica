package org.terasology.MetalliFabrica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.registry.In;
import org.terasology.rendering.nui.BaseInteractionScreen;
import org.terasology.rendering.nui.databinding.ReadOnlyBinding;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;

public class SmelteryWindow extends BaseInteractionScreen {

    private static final Logger logger = LoggerFactory.getLogger(SmelterySystem.class);

    InventoryGrid smelteryInput;
    InventoryGrid smelteryFuelInput;

    @In
    private LocalPlayer localPlayer;

    private boolean regionCheckBool;

    @Override
    public void initialise() {
        smelteryInput = find("SmelteryInput", InventoryGrid.class);
        smelteryFuelInput = find("Fuel", InventoryGrid.class);
    }

    @Override
    public boolean isModal() {
        return false;
    }

    public void initializeWithInteractionTarget(final EntityRef interactionTarget) {

        final SmelteryComponent smelteryInfo = interactionTarget.getComponent(SmelteryComponent.class);
        if (smelteryInput != null && smelteryInfo != null) {
            smelteryInput.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
                @Override
                public EntityRef get() {
                    return interactionTarget;
                }
            });
            smelteryInput.setMaxHorizontalCells(5);
            smelteryInput.setMaxCellCount(20);

            if (smelteryFuelInput != null) {
                smelteryFuelInput.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
                    @Override
                    public EntityRef get() {
                        return interactionTarget;
                    }
                });
                smelteryFuelInput.setMaxHorizontalCells(1);
                smelteryFuelInput.setMaxCellCount(1);
            }
        }
    }
}