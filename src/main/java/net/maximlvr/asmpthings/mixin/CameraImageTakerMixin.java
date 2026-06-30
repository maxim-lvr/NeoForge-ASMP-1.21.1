package net.maximlvr.asmpthings.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import de.maxhenkel.camera.ImageProcessor;
import de.maxhenkel.camera.ImageTaker;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCaptureState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ImageTaker.class)
public class CameraImageTakerMixin {
    @Shadow(remap = false)
    private static boolean takeScreenshot;

    @Shadow(remap = false)
    private static UUID uuid;

    @Shadow(remap = false)
    private static boolean hide;

    @Inject(method = "takeScreenshot", at = @At("HEAD"), cancellable = true, remap = false)
    private static void asmpthings$prepareCleanScreenshot(UUID imageId, CallbackInfo callbackInfo) {
        if (takeScreenshot && imageId.equals(uuid)) {
            callbackInfo.cancel();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        hide = minecraft.options.hideGui;
        minecraft.options.hideGui = true;
        uuid = imageId;
        takeScreenshot = true;
        CrazyPhoneCaptureState.setTakingScreenshot(true);
        minecraft.setScreen(null);
        callbackInfo.cancel();
    }

    @Inject(method = "onRenderTickEnd", at = @At("HEAD"), cancellable = true, remap = false)
    private static void asmpthings$takeCleanScreenshot(RenderFrameEvent.Post event, CallbackInfo callbackInfo) {
        if (!takeScreenshot) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null || !minecraft.options.hideGui) {
            callbackInfo.cancel();
            return;
        }

        NativeImage screenshot = Screenshot.takeScreenshot(minecraft.getMainRenderTarget());
        minecraft.options.hideGui = hide;
        takeScreenshot = false;
        CrazyPhoneCaptureState.setTakingScreenshot(false);
        ImageProcessor.sendScreenshotThreaded(uuid, screenshot);
        callbackInfo.cancel();
    }
}
