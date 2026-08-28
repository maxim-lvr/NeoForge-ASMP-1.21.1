package net.maximlvr.asmpthings.integration.sophisticated;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.AccessLogRecord;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class BackpackInspectionInventory {
    public static final String HANDLER_NAME = "asmp_backpack_inspect";

    private static final String SEPARATOR = "|";
    private static final Map<UUID, ItemStack> VIRTUAL_BACKPACKS = new ConcurrentHashMap<>();
    private static boolean registered;

    private BackpackInspectionInventory() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        PlayerInventoryProvider.get().addPlayerInventoryHandler(
                HANDLER_NAME,
                player -> Set.of(),
                (player, identifier) -> 1,
                BackpackInspectionInventory::getStackInSlot,
                false,
                false,
                false,
                false
        );
    }

    public static String encode(AccessLogRecord record) {
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(record.getBackpackName().getBytes(StandardCharsets.UTF_8));

        return record.getBackpackUuid()
                + SEPARATOR + record.getBackpackItemRegistryName()
                + SEPARATOR + record.getClothColor()
                + SEPARATOR + record.getTrimColor()
                + SEPARATOR + record.getColumnsTaken()
                + SEPARATOR + encodedName;
    }

    private static ItemStack getStackInSlot(Player player, String identifier, int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }

        Optional<BackpackReference> reference = decode(identifier);
        if (reference.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return findOnlineBackpackStack(player, reference.get().uuid())
                .orElseGet(() -> getVirtualBackpackStack(reference.get()));
    }

    private static Optional<BackpackReference> decode(String identifier) {
        String[] parts = identifier.split("\\|", 6);
        if (parts.length != 6) {
            return Optional.empty();
        }

        try {
            String name = new String(Base64.getUrlDecoder().decode(parts[5]), StandardCharsets.UTF_8);
            return Optional.of(new BackpackReference(
                    UUID.fromString(parts[0]),
                    ResourceLocation.parse(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    name
            ));
        } catch (IllegalArgumentException exception) {
            AsmpThingsMod.LOGGER.warn("Identifiant de backpack invalide: {}", identifier, exception);
            return Optional.empty();
        }
    }

    private static Optional<ItemStack> findOnlineBackpackStack(Player requester, UUID backpackUuid) {
        MinecraftServer server = requester.getServer();
        if (server == null) {
            return Optional.empty();
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AtomicReference<ItemStack> foundStack = new AtomicReference<>(ItemStack.EMPTY);

            PlayerInventoryProvider.get().runOnBackpacks(player, (stack, handlerName, identifier, slot) -> {
                if (!(stack.getItem() instanceof BackpackItem)) {
                    return false;
                }

                boolean matches = BackpackWrapper.fromStack(stack).getContentsUuid()
                        .map(backpackUuid::equals)
                        .orElse(false);

                if (matches) {
                    foundStack.set(stack);
                }

                return matches;
            });

            if (!foundStack.get().isEmpty()) {
                return Optional.of(foundStack.get());
            }
        }

        return Optional.empty();
    }

    private static ItemStack getVirtualBackpackStack(BackpackReference reference) {
        Item item = BuiltInRegistries.ITEM.get(reference.itemId());
        if (!(item instanceof BackpackItem)) {
            return ItemStack.EMPTY;
        }

        ItemStack backpack = VIRTUAL_BACKPACKS.compute(reference.uuid(), (uuid, existingStack) -> {
            if (existingStack == null || existingStack.getItem() != item) {
                return new ItemStack(item);
            }
            return existingStack;
        });

        if (!reference.name().isBlank()) {
            backpack.set(DataComponents.CUSTOM_NAME, Component.literal(reference.name()));
        }

        var wrapper = BackpackWrapper.fromStack(backpack);
        wrapper.setColors(reference.clothColor(), reference.trimColor());
        wrapper.setColumnsTaken(reference.columnsTaken(), false);
        wrapper.setContentsUuid(reference.uuid());

        return backpack;
    }

    private record BackpackReference(
            UUID uuid,
            ResourceLocation itemId,
            int clothColor,
            int trimColor,
            int columnsTaken,
            String name
    ) {
    }
}
