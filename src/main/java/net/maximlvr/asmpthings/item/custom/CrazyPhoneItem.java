package net.maximlvr.asmpthings.item.custom;

import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.maximlvr.asmpthings.network.payload.OpenCrazyPhonePayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class CrazyPhoneItem extends Item {
    public CrazyPhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (CrazyPhoneCameraHelper.isActive(stack)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                CrazyPhoneCameraHelper.takeOrStartPhoto(level, serverPlayer, stack);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new OpenCrazyPhonePayload(usedHand == InteractionHand.MAIN_HAND)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String number = tag.getString("number");
        String name = tag.getString("name");

        if (!number.isEmpty()) {
            tooltip.add(Component.literal("Numero: " + number));
        }

        if (!name.isEmpty()) {
            tooltip.add(Component.literal("Nom: " + name));
        }
    }
}
