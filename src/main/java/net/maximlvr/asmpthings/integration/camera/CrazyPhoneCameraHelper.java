package net.maximlvr.asmpthings.integration.camera;

import de.maxhenkel.camera.Main;
import de.maxhenkel.camera.ModSounds;
import de.maxhenkel.camera.inventory.AlbumInventory;
import de.maxhenkel.camera.items.CameraItem;
import de.maxhenkel.camera.items.ImageItem;
import de.maxhenkel.camera.net.MessageTakeImage;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.item.custom.CrazyPhoneItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CrazyPhoneCameraHelper {
    private static final String TAG_CAMERA_ALBUM = "cameraAlbum";
    private static final String TAG_CAMERA_ALBUMS = "cameraAlbums";

    private CrazyPhoneCameraHelper() {
    }

    public static boolean isSupportedCamera(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getItem() instanceof CameraItem || stack.getItem() instanceof CrazyPhoneItem;
    }

    public static boolean isActive(ItemStack stack) {
        return Main.CAMERA.get().isActive(stack);
    }

    public static void takeOrStartPhoto(Level level, ServerPlayer player, ItemStack phone) {
        if (!isActive(phone)) {
            player.closeContainer();
            Main.CAMERA.get().setActive(phone, true);
            return;
        }

        if (!Main.PACKET_MANAGER.canTakeImage(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.image_cooldown"), true);
            return;
        }

        SoundEvent sound = ModSounds.TAKE_IMAGE.get();
        level.playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, 1F, 1F);

        UUID imageId = UUID.randomUUID();
        PacketDistributor.sendToPlayer(player, new MessageTakeImage(imageId));
        Main.CAMERA.get().setActive(phone, false);
    }

    public static boolean tryInsertImageIntoCrazyPhone(ServerPlayer player, ItemStack image) {
        if (image.isEmpty() || !(image.getItem() instanceof ImageItem)) {
            return false;
        }

        ItemStack phone = findTargetPhone(player);

        if (phone.isEmpty()) {
            return false;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<ItemStack> albums = getOrCreateCameraAlbums(registries, tag);

        for (ItemStack album : albums) {
            if (tryAddImageToAlbum(player, album, image)) {
                saveCameraAlbums(registries, tag, albums);
                phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                return true;
            }
        }

        ItemStack newAlbum = new ItemStack(Main.ALBUM.get());

        if (!tryAddImageToAlbum(player, newAlbum, image)) {
            return false;
        }

        albums.add(newAlbum);
        saveCameraAlbums(registries, tag, albums);
        phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static int getCameraPhotoCount(HolderLookup.Provider registries, ItemStack phone) {
        int count = 0;

        for (ItemStack album : getCameraAlbums(registries, phone)) {
            count += Main.ALBUM.get().getImages(registries, album).size();
        }

        return count;
    }

    public static List<ItemStack> getCameraAlbums(HolderLookup.Provider registries, ItemStack phone) {
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return readCameraAlbums(registries, tag);
    }

    public static List<ItemStack> getCameraImages(HolderLookup.Provider registries, ItemStack phone) {
        List<ItemStack> images = new ArrayList<>();

        for (ItemStack album : getCameraAlbums(registries, phone)) {
            images.addAll(getCameraImagesFromAlbum(registries, album));
        }

        return images;
    }

    public static List<ItemStack> getCameraImagesFromAlbum(HolderLookup.Provider registries, ItemStack album) {
        List<ItemStack> images = new ArrayList<>();

        if (album.isEmpty()) {
            return images;
        }

        AlbumInventory inventory = new AlbumInventory(registries, album);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack image = inventory.getItem(slot);

            if (image.isEmpty() || !(image.getItem() instanceof ImageItem)) {
                continue;
            }

            images.add(image.copy());
        }

        return images;
    }

    private static ItemStack findTargetPhone(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.is(ModItems.CRAZY_PHONE.get())) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();

        if (offhand.is(ModItems.CRAZY_PHONE.get())) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getCameraAlbum(HolderLookup.Provider registries, ItemStack phone) {
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (!tag.contains(TAG_CAMERA_ALBUM)) {
            return ItemStack.EMPTY;
        }

        return ItemStack.parseOptional(registries, tag.getCompound(TAG_CAMERA_ALBUM));
    }

    private static List<ItemStack> getOrCreateCameraAlbums(HolderLookup.Provider registries, CompoundTag tag) {
        List<ItemStack> albums = readCameraAlbums(registries, tag);

        if (albums.isEmpty()) {
            albums.add(new ItemStack(Main.ALBUM.get()));
        }

        return albums;
    }

    private static List<ItemStack> readCameraAlbums(HolderLookup.Provider registries, CompoundTag tag) {
        List<ItemStack> albums = new ArrayList<>();

        if (tag.contains(TAG_CAMERA_ALBUMS)) {
            ListTag albumTags = tag.getList(TAG_CAMERA_ALBUMS, 10);

            for (int i = 0; i < albumTags.size(); i++) {
                ItemStack album = ItemStack.parseOptional(registries, albumTags.getCompound(i));

                if (!album.isEmpty()) {
                    albums.add(album);
                }
            }

            return albums;
        }

        ItemStack legacyAlbum = getCameraAlbumFromTag(registries, tag);

        if (!legacyAlbum.isEmpty()) {
            albums.add(legacyAlbum);
        }

        return albums;
    }

    private static ItemStack getCameraAlbumFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        if (!tag.contains(TAG_CAMERA_ALBUM)) {
            return ItemStack.EMPTY;
        }

        return ItemStack.parseOptional(registries, tag.getCompound(TAG_CAMERA_ALBUM));
    }

    private static void saveCameraAlbums(HolderLookup.Provider registries, CompoundTag tag, List<ItemStack> albums) {
        ListTag albumTags = new ListTag();

        for (ItemStack album : albums) {
            if (!album.isEmpty()) {
                albumTags.add(album.save(registries));
            }
        }

        tag.put(TAG_CAMERA_ALBUMS, albumTags);
        tag.remove(TAG_CAMERA_ALBUM);
    }

    private static boolean tryAddImageToAlbum(ServerPlayer player, ItemStack album, ItemStack image) {
        AlbumInventory inventory = new AlbumInventory(player.level().registryAccess(), album);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                continue;
            }

            inventory.setItem(slot, image.copy());
            return true;
        }

        return false;
    }

    public static void giveImageFallback(ServerPlayer player, ItemStack image) {
        if (player.addItem(image)) {
            return;
        }

        Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), image);
    }

    public static boolean hasCameraPhotos(HolderLookup.Provider registries, ItemStack phone) {
        return getCameraPhotoCount(registries, phone) > 0;
    }

}
