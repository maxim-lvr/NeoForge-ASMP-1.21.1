package net.maximlvr.asmpthings.pet;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class PetMenu extends AbstractContainerMenu {
    private static final int PET_SLOT = 0, INVENTORY_START = 1, HOTBAR_START = 28, SLOT_END = 37;
    public PetMenu(int id, Inventory inventory) {
        super(ModPets.MENU.get(), id);
        addSlot(new PetEquipmentSlot(inventory.player, PetEquipmentSlot.X, PetEquipmentSlot.Y));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
    @Override public boolean stillValid(Player player) { return true; }
    public ItemStack equipped() { return slots.get(PET_SLOT).getItem(); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index == PET_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, SLOT_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PetItem && !slots.get(PET_SLOT).hasItem()) {
            if (!moveItemStackTo(stack, PET_SLOT, INVENTORY_START, false)) return ItemStack.EMPTY;
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, SLOT_END, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, original);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
