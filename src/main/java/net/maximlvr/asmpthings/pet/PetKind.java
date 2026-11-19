package net.maximlvr.asmpthings.pet;

import net.minecraft.world.item.ItemStack;

/** Add future companions here; equipment, following and menus are shared. */
public enum PetKind {
    PET_1("pet_1", 0xffffffff), PET_2("pet_2", 0xffaaccff);

    public final String id;
    public final int tint;
    PetKind(String id, int tint) { this.id = id; this.tint = tint; }
    public String nameKey() { return "item.asmpthingsmod." + id; }
    public static PetKind fromStack(ItemStack stack) {
        return stack.getItem() instanceof PetItem item ? item.kind : null;
    }
    public static PetKind fromId(String id) {
        for (PetKind kind : values()) if (kind.id.equals(id)) return kind;
        return null;
    }
}
