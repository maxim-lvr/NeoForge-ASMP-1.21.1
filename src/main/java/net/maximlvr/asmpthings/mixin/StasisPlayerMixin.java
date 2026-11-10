package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StasisPlayerMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = {"handleMovePlayer", "handleMoveVehicle", "handlePlayerAction", "handleUseItem",
            "handleUseItemOn", "handleInteract", "handleSetCarriedItem", "handlePlayerCommand"},
            at = @At("HEAD"), cancellable = true)
    private void asmpthings$preventStasisActions(CallbackInfo ci) {
        if (player.server.isSameThread() && ModEffects.isFrozen(player)) ci.cancel();
    }
}
