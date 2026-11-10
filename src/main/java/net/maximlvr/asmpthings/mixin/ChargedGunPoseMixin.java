package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.item.custom.ChargedGunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class ChargedGunPoseMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void asmpthings$aimChargedGun(LivingEntity entity, float swing, float amount, float age,
                                        float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof Player) || !entity.isUsingItem()
                || !(entity.getUseItem().getItem() instanceof ChargedGunItem)) return;
        HumanoidArm side = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm() : entity.getMainArm().getOpposite();
        ModelPart arm = side == HumanoidArm.RIGHT ? rightArm : leftArm;
        arm.xRot = head.xRot - (float) Math.PI / 2;
        arm.yRot = head.yRot;
        arm.zRot = (float) Math.sin(age * 2.7) * 0.008F;
    }
}
