package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.client.AsmpTestClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class AsmpTestSprintMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean asmpthings$remoteSprint(KeyMapping key) {
        return AsmpTestClient.controlling() && key == Minecraft.getInstance().options.keySprint
                ? AsmpTestClient.sprintDown() : key.isDown();
    }
}
