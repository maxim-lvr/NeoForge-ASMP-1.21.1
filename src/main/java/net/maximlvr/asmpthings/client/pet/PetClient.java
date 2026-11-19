package net.maximlvr.asmpthings.client.pet;

import com.mojang.blaze3d.platform.InputConstants;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.pet.ModPets;
import net.maximlvr.asmpthings.pet.PetNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class PetClient {
    public static final KeyMapping OPEN = new KeyMapping("key.asmpthingsmod.pet_menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P, "key.categories.asmpthingsmod");
    public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    public static void screens(RegisterMenuScreensEvent event) { event.register(ModPets.MENU.get(), PetScreen::new); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        while (OPEN.consumeClick()) if (minecraft.player != null && minecraft.screen == null) requestMenu();
    }
    public static void requestMenu() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        if (!minecraft.player.containerMenu.getCarried().isEmpty()) {
            minecraft.player.displayClientMessage(Component.translatable("menu.asmpthingsmod.pet_put_away"), true);
            return;
        }
        PacketDistributor.sendToServer(new PetNetworking.OpenMenu());
    }
    public static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
        graphics.fill(x, y + 16, x + 17, y + 17, 0xffffffff);
        graphics.fill(x + 16, y, x + 17, y + 17, 0xffffffff);
    }
    private PetClient() {}
}
