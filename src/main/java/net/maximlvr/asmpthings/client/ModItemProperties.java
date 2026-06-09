package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class ModItemProperties {

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModItems.ROLE_CARD.get(),
                    ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "role_view"),
                    (stack, level, entity, seed) -> {
                        Minecraft mc = Minecraft.getInstance();

                        if (mc.player == null) return 0.0f;

                        // Si la carte est rendue chez un autre joueur, on masque le rôle
                        if (entity != null && entity != mc.player) return 0.0f;

                        int roleType = stack.getOrDefault(ModDataComponents.ROLE_CARD_TYPE.get(), 0);

                        return switch (roleType) {
                            case 1 -> 0.1f; // loup
                            case 2 -> 0.2f; // cupidon
                            default -> 0.0f; // base
                        };
                    }
            );
        });
    }
}