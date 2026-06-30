package net.maximlvr.asmpthings.mixin;

import de.maxhenkel.camera.ClientEvents;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCaptureState;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientEvents.class)
public class CameraClientEventsMixin {
    @Inject(method = "getActiveCamera", at = @At("RETURN"), cancellable = true, remap = false)
    private void asmpthings$getActiveCrazyPhone(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack current = cir.getReturnValue();

        if (current != null && !current.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);

            if (!CrazyPhoneCameraHelper.isSupportedCamera(stack) || !CrazyPhoneCameraHelper.isActive(stack)) {
                continue;
            }

            cir.setReturnValue(stack);
            return;
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private void asmpthings$hideOverlayDuringScreenshot(RenderGuiLayerEvent.Pre event, CallbackInfo callbackInfo) {
        if (!CrazyPhoneCaptureState.isTakingScreenshot()) {
            return;
        }

        event.setCanceled(true);
        callbackInfo.cancel();
    }
}
