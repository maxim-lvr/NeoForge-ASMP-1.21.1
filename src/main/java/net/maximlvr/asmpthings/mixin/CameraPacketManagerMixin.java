package net.maximlvr.asmpthings.mixin;

import de.maxhenkel.camera.ImageData;
import de.maxhenkel.camera.ImageTools;
import de.maxhenkel.camera.Main;
import de.maxhenkel.camera.net.PacketManager;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Mixin(PacketManager.class)
public abstract class CameraPacketManagerMixin {
    @Shadow(remap = false)
    private Map<UUID, byte[]> clientDataMap;

    @Shadow(remap = false)
    private Map<UUID, BufferedImage> imageCache;

    @Shadow(remap = false)
    public abstract BufferedImage completeImage(UUID uuid);

    /**
     * Stores finished Camera Mod images in the active CrazyPhone album before falling back to the player inventory.
     */
    @Overwrite(remap = false)
    public void addBytes(ServerPlayer player, UUID uuid, int offset, int size, byte[] data) {
        byte[] imageBytes = clientDataMap.getOrDefault(uuid, new byte[size]);
        System.arraycopy(data, 0, imageBytes, offset, data.length);
        clientDataMap.put(uuid, imageBytes);

        if (offset + data.length < imageBytes.length) {
            return;
        }

        BufferedImage image = completeImage(uuid);

        if (image == null) {
            return;
        }

        imageCache.put(uuid, image);

        new Thread(() -> saveImageAndGiveItem(player, uuid, image), "SaveImageThread").start();
    }

    private void saveImageAndGiveItem(ServerPlayer player, UUID uuid, BufferedImage image) {
        try {
            ImageTools.saveImage(player, uuid, image);
            player.getServer().submitAsync(() -> createImageItem(player, uuid));
        } catch (IOException exception) {
            AsmpThingsMod.LOGGER.error("Impossible de sauvegarder la photo CrazyPhone {}", uuid, exception);
        }
    }

    private static void createImageItem(ServerPlayer player, UUID uuid) {
        ItemStack image = new ItemStack(Main.IMAGE.get());
        ImageData.create(player, uuid).addToImage(image);

        if (CrazyPhoneCameraHelper.tryInsertImageIntoCrazyPhone(player, image)) {
            return;
        }

        CrazyPhoneCameraHelper.giveImageFallback(player, image);
    }
}
