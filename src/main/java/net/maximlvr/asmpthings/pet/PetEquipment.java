package net.maximlvr.asmpthings.pet;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class PetEquipment extends ItemStackHandler {
    public PetEquipment() { super(1); }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return stack.getItem() instanceof PetItem; }
}
