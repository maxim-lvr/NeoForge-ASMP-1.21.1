package net.maximlvr.asmpthings.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class BlueCardItem extends Item {
    public static final String ACCOUNT_ID_TAG = "bank_account_id";
    public static final String PIN_TAG = "bank_pin";

    public BlueCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String accountId = getAccountId(stack);

        if (!accountId.isEmpty()) {
            tooltip.add(Component.literal("Compte: " + accountId));
        }
    }

    public static String getAccountId(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getString(ACCOUNT_ID_TAG);
    }

    public static String getPin(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getString(PIN_TAG);
    }

    public static void setup(ItemStack stack, String accountId, String pin) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(ACCOUNT_ID_TAG, accountId);
        tag.putString(PIN_TAG, pin);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
