package org.terasology.MetalliFabrica;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.registry.In;
import org.terasology.rendering.nui.BaseInteractionScreen;
import org.terasology.rendering.nui.databinding.ReadOnlyBinding;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmelteryWindow extends BaseInteractionScreen {

    @In
    private LocalPlayer localPlayer;
    InventoryGrid SmelteryInput;

    private static final Logger logger = LoggerFactory.getLogger(SmelterySystem.class);

    @Override
    public void initialise() {

        SmelteryInput = find("SmelteryInput", InventoryGrid.class);

    }

    @Override
    public boolean isModal() {
        return false;
    }

    public void initializeWithInteractionTarget(final EntityRef interactionTarget) {

        final SmelteryComponent SmelteryInfo = interactionTarget.getComponent(SmelteryComponent.class);

        if (SmelteryInput != null && SmelteryInfo != null) {

            SmelteryInput.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
                @Override
                public EntityRef get() {
                    return interactionTarget;
                }
            });
            SmelteryInput.setMaxHorizontalCells(5);
            SmelteryInput.setMaxCellCount(20);

        }
    }

}