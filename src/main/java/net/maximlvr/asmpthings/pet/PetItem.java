package net.maximlvr.asmpthings.pet;

import net.minecraft.world.item.Item;

public final class PetItem extends Item {
    public final PetKind kind;
    public PetItem(PetKind kind, Properties properties) { super(properties); this.kind = kind; }
}
