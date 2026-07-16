package net.maximlvr.asmpthings.integration.camera;

import de.maxhenkel.camera.Main;
import de.maxhenkel.camera.ImageData;
import de.maxhenkel.camera.ModSounds;
import de.maxhenkel.camera.inventory.AlbumInventory;
import de.maxhenkel.camera.items.CameraItem;
import de.maxhenkel.camera.items.ImageItem;
import de.maxhenkel.camera.net.MessageTakeImage;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.item.custom.CrazyPhoneItem;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneSyncPayload;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CrazyPhoneCameraHelper {
    private static final String TAG_CAMERA_ALBUM = "cameraAlbum";
    private static final String TAG_CAMERA_ALBUMS = "cameraAlbums";
    private static final String TAG_CAMERA_ALBUM_GROUPS = "cameraAlbumGroups";
    private static final String TAG_CAMERA_PHOTO_GROUPS = "cameraPhotoGroups";
    private static final String TAG_NEXT_CAMERA_ALBUM_GROUP_ID = "nextCameraAlbumGroupId";
    private static final Map<UUID, UploadTarget> UPLOAD_TARGETS = new HashMap<>();

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
        UploadTarget uploadTarget = getUploadTarget(player);

        for (ItemStack album : albums) {
            if (tryAddImageToAlbum(player, album, image)) {
                assignUploadedImageToTarget(player, tag, image);
                saveCameraAlbums(registries, tag, albums);
                phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                syncHeldPhone(player, phone, tag);
                return true;
            }
        }

        ItemStack newAlbum = new ItemStack(Main.ALBUM.get());

        if (!tryAddImageToAlbum(player, newAlbum, image)) {
            return false;
        }

        albums.add(newAlbum);
        assignUploadedImageToTarget(player, tag, image);
        saveCameraAlbums(registries, tag, albums);
        phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        syncHeldPhone(player, phone, tag);
        return true;
    }

    public static boolean createAlbum(ServerPlayer player, ItemStack phone) {
        if (phone.isEmpty() || !ModItems.isCrazyPhone(phone)) {
            return false;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag groups = tag.getList(TAG_CAMERA_ALBUM_GROUPS, 10);
        int id = tag.getInt(TAG_NEXT_CAMERA_ALBUM_GROUP_ID);

        if (id <= 0) {
            id = getNextGroupId(groups);
        }

        CompoundTag group = new CompoundTag();
        group.putInt("id", id);
        group.putString("name", "Album " + (groups.size() + 1));
        groups.add(group);
        tag.put(TAG_CAMERA_ALBUM_GROUPS, groups);
        tag.putInt(TAG_NEXT_CAMERA_ALBUM_GROUP_ID, id + 1);
        phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        syncHeldPhone(player, phone, tag);
        return true;
    }

    public static boolean renameAlbum(ServerPlayer player, ItemStack phone, int albumIndex, String name) {
        if (phone.isEmpty() || !ModItems.isCrazyPhone(phone) || albumIndex < 0 || name.isBlank()) {
            return false;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag groups = tag.getList(TAG_CAMERA_ALBUM_GROUPS, 10);

        for (int i = 0; i < groups.size(); i++) {
            CompoundTag group = groups.getCompound(i);

            if (group.getInt("id") != albumIndex) {
                continue;
            }

            group.putString("name", name.trim());
            groups.set(i, group);
            tag.put(TAG_CAMERA_ALBUM_GROUPS, groups);
            phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            syncHeldPhone(player, phone, tag);
            return true;
        }

        return false;
    }

    public static void setUploadTarget(ServerPlayer player, int groupId, int uploadCount) {
        if (uploadCount <= 0) {
            UPLOAD_TARGETS.remove(player.getUUID());
            return;
        }

        UPLOAD_TARGETS.put(player.getUUID(), new UploadTarget(groupId, Math.min(uploadCount, 50), System.currentTimeMillis() + 30_000L));
    }

    public static boolean assignImageToGroup(ServerPlayer player, ItemStack phone, String imageId, int groupId) {
        if (phone.isEmpty() || !ModItems.isCrazyPhone(phone) || imageId == null || imageId.isBlank()) {
            return false;
        }

        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        setPhotoGroup(tag, imageId, groupId);
        phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        syncHeldPhone(player, phone, tag);
        return true;
    }

    public static boolean insertImageIntoPhone(ServerPlayer player, ItemStack phone, ItemStack image) {
        if (phone.isEmpty() || !ModItems.isCrazyPhone(phone) || image.isEmpty() || !(image.getItem() instanceof ImageItem)) {
            return false;
        }

        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<ItemStack> albums = getOrCreateCameraAlbums(registries, tag);

        for (ItemStack album : albums) {
            if (tryAddImageToAlbum(player, album, image)) {
                saveCameraAlbums(registries, tag, albums);
                phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                syncHeldPhone(player, phone, tag);
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
        syncHeldPhone(player, phone, tag);
        return true;
    }

    public static ItemStack getCameraImageAt(HolderLookup.Provider registries, ItemStack phone, int albumIndex, int imageIndex) {
        List<ItemStack> albums = getCameraAlbums(registries, phone);

        if (albumIndex < 0 || albumIndex >= albums.size() || imageIndex < 0) {
            return ItemStack.EMPTY;
        }

        AlbumInventory inventory = new AlbumInventory(registries, albums.get(albumIndex));
        int visibleIndex = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack image = inventory.getItem(slot);

            if (image.isEmpty() || !(image.getItem() instanceof ImageItem)) {
                continue;
            }

            if (visibleIndex == imageIndex) {
                return image.copy();
            }

            visibleIndex++;
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack removeCameraImageAt(ServerPlayer player, ItemStack phone, int albumIndex, int imageIndex) {
        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<ItemStack> albums = readCameraAlbums(registries, tag);

        if (albumIndex < 0 || albumIndex >= albums.size() || imageIndex < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack album = albums.get(albumIndex);
        AlbumInventory inventory = new AlbumInventory(registries, album);
        int visibleIndex = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack image = inventory.getItem(slot);

            if (image.isEmpty() || !(image.getItem() instanceof ImageItem)) {
                continue;
            }

            if (visibleIndex == imageIndex) {
                ItemStack removed = image.copy();
                inventory.setItem(slot, ItemStack.EMPTY);
                saveCameraAlbums(registries, tag, albums);
                phone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                syncHeldPhone(player, phone, tag);
                return removed;
            }

            visibleIndex++;
        }

        return ItemStack.EMPTY;
    }

    public static int getCameraPhotoCount(HolderLookup.Provider registries, ItemStack phone) {
        int count = 0;

        for (ItemStack album : getCameraAlbums(registries, phone)) {
            count += Main.ALBUM.get().getImages(registries, album).size();
        }

        return count;
    }

    public static int getAlbumPhotoCount(HolderLookup.Provider registries, ItemStack album) {
        return getCameraImagesFromAlbum(registries, album).size();
    }

    public static int getAlbumCapacity() {
        return AlbumInventory.SIZE;
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

        if (ModItems.isCrazyPhone(mainHand)) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();

        if (ModItems.isCrazyPhone(offhand)) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getCameraAlbum(HolderLookup.Provider registries, ItemStack phone) {
        CompoundTag tag = phone.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (!tag.contains(TAG_CAMERA_ALBUM)) {
            return ItemStack.EMPTY;
        }

        return parseSavedItem(registries, tag.getCompound(TAG_CAMERA_ALBUM));
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
                ItemStack album = parseSavedItem(registries, albumTags.getCompound(i));

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

        return parseSavedItem(registries, tag.getCompound(TAG_CAMERA_ALBUM));
    }

    private static ItemStack parseSavedItem(HolderLookup.Provider registries, CompoundTag tag) {
        if (!tag.contains("id")) {
            return ItemStack.EMPTY;
        }

        return ItemStack.parseOptional(registries, tag);
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

    private static void assignUploadedImageToTarget(ServerPlayer player, CompoundTag tag, ItemStack image) {
        UploadTarget target = getUploadTarget(player);

        if (target == null) {
            return;
        }

        consumeUploadTarget(player);

        if (target.groupId() < 0) {
            return;
        }

        ImageData data = ImageData.fromStack(image);

        if (data != null) {
            setPhotoGroup(tag, data.getId().toString(), target.groupId());
        }
    }

    private static UploadTarget getUploadTarget(ServerPlayer player) {
        UploadTarget target = UPLOAD_TARGETS.get(player.getUUID());

        if (target == null) {
            return null;
        }

        if (target.expiresAt() < System.currentTimeMillis() || target.remaining() <= 0) {
            UPLOAD_TARGETS.remove(player.getUUID());
            return null;
        }

        return target;
    }

    private static void consumeUploadTarget(ServerPlayer player) {
        UploadTarget target = getUploadTarget(player);

        if (target == null) {
            return;
        }

        if (target.remaining() <= 1) {
            UPLOAD_TARGETS.remove(player.getUUID());
        } else {
            UPLOAD_TARGETS.put(player.getUUID(), new UploadTarget(target.groupId(), target.remaining() - 1, target.expiresAt()));
        }
    }

    private static void setPhotoGroup(CompoundTag tag, String imageId, int groupId) {
        ListTag photoGroups = tag.getList(TAG_CAMERA_PHOTO_GROUPS, 10);

        for (int i = 0; i < photoGroups.size(); i++) {
            CompoundTag current = photoGroups.getCompound(i);

            if (!imageId.equals(current.getString("imageId"))) {
                continue;
            }

            if (groupId < 0) {
                photoGroups.remove(i);
            } else {
                current.putInt("groupId", groupId);
                photoGroups.set(i, current);
            }

            tag.put(TAG_CAMERA_PHOTO_GROUPS, photoGroups);
            return;
        }

        if (groupId >= 0) {
            CompoundTag assignment = new CompoundTag();
            assignment.putString("imageId", imageId);
            assignment.putInt("groupId", groupId);
            photoGroups.add(assignment);
            tag.put(TAG_CAMERA_PHOTO_GROUPS, photoGroups);
        }
    }

    private static int getNextGroupId(ListTag groups) {
        int nextId = 1;

        for (int i = 0; i < groups.size(); i++) {
            nextId = Math.max(nextId, groups.getCompound(i).getInt("id") + 1);
        }

        return nextId;
    }

    public static void giveImageFallback(ServerPlayer player, ItemStack image) {
        if (player.addItem(image)) {
            return;
        }

        Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), image);
    }

    private static void syncHeldPhone(ServerPlayer player, ItemStack phone, CompoundTag tag) {
        if (phone == player.getMainHandItem()) {
            PacketDistributor.sendToPlayer(player, new CrazyPhoneSyncPayload(true, tag.copy()));
        } else if (phone == player.getOffhandItem()) {
            PacketDistributor.sendToPlayer(player, new CrazyPhoneSyncPayload(false, tag.copy()));
        }
    }

    public static boolean hasCameraPhotos(HolderLookup.Provider registries, ItemStack phone) {
        return getCameraPhotoCount(registries, phone) > 0;
    }

    private record UploadTarget(int groupId, int remaining, long expiresAt) {
    }

}
