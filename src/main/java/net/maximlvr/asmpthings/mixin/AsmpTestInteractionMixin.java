package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.command.AsmpTestCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AsmpTestInteractionMixin {
    @Shadow public ServerPlayer player;
    // Movement and sprint/crouch packets now come from the owning client's vanilla controller.
    @Inject(method = {"handlePlayerAction", "handleUseItem", "handleUseItemOn", "handleInteract",
            "handleSetCarriedItem"}, at = @At("HEAD"), cancellable = true)
    private void asmpthings$ignoreLocalInteraction(CallbackInfo ci) {
        if (player.server.isSameThread() && AsmpTestCommand.controlsOther(player)) ci.cancel();
    }
}
