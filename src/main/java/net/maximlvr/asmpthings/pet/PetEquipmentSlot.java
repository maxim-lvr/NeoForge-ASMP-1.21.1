package net.maximlvr.asmpthings.pet;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PetEquipmentSlot extends Slot {
    public static final int X = 26, Y = 26;
    private final Player owner;
    public PetEquipmentSlot(Player owner, int x, int y) {
        super(new EquipmentContainer(owner), 0, x, y);
        this.owner = owner;
    }
    @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof PetItem; }
    @Override public boolean mayPickup(Player player) { return player == owner; }
    @Override public int getMaxStackSize() { return 1; }

    /** Resolve the attachment on every access: player loading/respawning can replace it after menu construction. */
    private record EquipmentContainer(Player owner) implements Container {
        private PetEquipment equipment() { return owner.getData(ModPets.EQUIPMENT); }
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
        @Override public ItemStack getItem(int slot) { return equipment().getStackInSlot(0); }
        @Override public ItemStack removeItem(int slot, int count) { return equipment().extractItem(0, count, false); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, 1); }
        @Override public void setItem(int slot, ItemStack stack) {
            if (stack.isEmpty() || stack.getItem() instanceof PetItem)
                equipment().setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return player == owner; }
        @Override public void clearContent() { equipment().setStackInSlot(0, ItemStack.EMPTY); }
    }
}
