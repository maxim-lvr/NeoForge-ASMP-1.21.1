package net.maximlvr.asmpthings.pet;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class PetManager {
    private static final ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pet_armor");
    private static final ResourceLocation HEALTH = ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pet_health");
    private static final Map<UUID, PetCompanionEntity> ACTIVE = new HashMap<>();

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) return;
        PetKind kind = player.isAlive() && !player.isSpectator()
                ? PetKind.fromStack(player.getData(ModPets.EQUIPMENT).getStackInSlot(0)) : null;
        PetCompanionEntity pet = ACTIVE.get(player.getUUID());
        if (pet != null && (kind == null || pet.isRemoved() || pet.level() != player.level() || pet.kind() != kind)) {
            pet.discard();
            ACTIVE.remove(player.getUUID());
            pet = null;
        }
        if (kind != null && pet == null) {
            pet = ModEntities.PET_COMPANION.get().create(player.serverLevel());
            if (pet != null) {
                pet.configure(player, kind);
                if (player.serverLevel().addFreshEntity(pet)) ACTIVE.put(player.getUUID(), pet);
            }
        }
        double armor = kind == null ? 0 : PetConfig.bonus(kind).armor().get();
        double health = kind == null ? 0 : PetConfig.bonus(kind).health().get();
        update(player, Attributes.ARMOR, ARMOR, armor, AttributeModifier.Operation.ADD_VALUE);
        update(player, Attributes.MAX_HEALTH, HEALTH, health, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void update(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                               ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        var existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount && existing.operation() == operation) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PetCompanionEntity pet = ACTIVE.remove(event.getEntity().getUUID());
        if (pet != null) pet.discard();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        ACTIVE.values().forEach(PetCompanionEntity::discard);
        ACTIVE.clear();
    }
    private PetManager() {}
}
