package net.maximlvr.asmpthings.pet;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModPets {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, AsmpThingsMod.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AsmpThingsMod.MOD_ID);
    public static final Supplier<AttachmentType<PetEquipment>> EQUIPMENT = ATTACHMENTS.register("pet_equipment",
            () -> AttachmentType.serializable(PetEquipment::new).copyOnDeath().build());
    public static final Supplier<MenuType<PetMenu>> MENU = MENUS.register("pet",
            () -> new MenuType<>(PetMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); MENUS.register(bus); PetNetworking.register(bus); }
    private ModPets() {}
}
