package net.maximlvr.asmpthings.command;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

final class AsmpTestInventory {
    private AsmpTestInventory() {}

    static void open(ServerPlayer controller, ServerPlayer body) {
        Container inventory = new Container() {
            public int getContainerSize() { return 45; }
            public boolean isEmpty() { return body.getInventory().isEmpty(); }
            public ItemStack getItem(int slot) { return slot < 41 ? body.getInventory().getItem(slot) : ItemStack.EMPTY; }
            public ItemStack removeItem(int slot, int count) { return slot < 41 ? body.getInventory().removeItem(slot, count) : ItemStack.EMPTY; }
            public ItemStack removeItemNoUpdate(int slot) { return slot < 41 ? body.getInventory().removeItemNoUpdate(slot) : ItemStack.EMPTY; }
            public void setItem(int slot, ItemStack stack) { if (slot < 41) body.getInventory().setItem(slot, stack); }
            public void setChanged() { body.getInventory().setChanged(); }
            public boolean stillValid(Player player) { return body.isAlive() && AsmpTestCommand.paired(controller, body); }
            public void clearContent() { /* This view never owns or clears the player's inventory. */ }
            public boolean canPlaceItem(int slot, ItemStack stack) {
                if (slot >= 41) return false;
                if (slot >= 36 && slot <= 39) {
                    if (stack.getCount() > 1) return false;
                    var equipment = body.getEquipmentSlotForItem(stack);
                    return equipment.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR
                            && equipment.getIndex() == slot - 36;
                }
                return true;
            }
        };
        controller.openMenu(new SimpleMenuProvider((id, playerInventory, player) ->
                new ChestMenu(MenuType.GENERIC_9x5, id, playerInventory, inventory, 5) {
                    @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                    @Override public void clicked(int slot, int button, ClickType type, Player p) {
                        if (!getCarried().isEmpty() && !inventory.canPlaceItem(slot, getCarried())) return;
                        if (slot >= 0 && slot < 41 && type == ClickType.PICKUP
                                && inventory.stillValid(p)) super.clicked(slot, button, type, p);
                    }
                    @Override public void removed(Player p) {
                        ItemStack carried = getCarried();
                        setCarried(ItemStack.EMPTY);
                        if (!carried.isEmpty() && (!body.isAlive() || !body.getInventory().add(carried))) body.drop(carried, false);
                        super.removed(p);
                    }
                }, Component.literal("POC - " + body.getGameProfile().getName())));
    }
}
