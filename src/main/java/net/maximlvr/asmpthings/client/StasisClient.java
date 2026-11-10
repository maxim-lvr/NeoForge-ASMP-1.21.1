package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class StasisClient {
    private StasisClient() {}

    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent event) {
        if (!ModEffects.isFrozen(event.getEntity())) return;
        var input = event.getInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (ModEffects.isFrozen(Minecraft.getInstance().player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
