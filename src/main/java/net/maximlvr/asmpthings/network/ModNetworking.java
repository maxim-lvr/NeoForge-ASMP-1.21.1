package net.maximlvr.asmpthings.network;

import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhoneContactByNumberPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneContactResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneMessageResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhonePhotoResultPayload;
import net.maximlvr.asmpthings.network.payload.OpenCrazyPhonePayload;
import net.maximlvr.asmpthings.network.payload.OpenScratchTicketPayload;
import net.maximlvr.asmpthings.network.payload.ScratchTicketScratchPayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhoneMessagePayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.SetCrazyPhoneLockedPayload;
import net.maximlvr.asmpthings.network.payload.SetupCrazyPhonePayload;
import net.maximlvr.asmpthings.network.payload.TakeCrazyPhonePhotoPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;


public class ModNetworking {
    private static final int GRID_COLS = 64;
    private static final int GRID_ROWS = 64;
    private static final int TOTAL_CELLS = GRID_COLS * GRID_ROWS;

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(
                ScratchTicketScratchPayload.TYPE,
                ScratchTicketScratchPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();

                        ItemStack stack = player.getMainHandItem();

                        if (!stack.is(ModItems.GOAL_SMALL_TICKET.get())) {
                            stack = player.getOffhandItem();
                        }

                        if (!stack.is(ModItems.GOAL_SMALL_TICKET.get())) {
                            return;
                        }

                        int index = payload.index();

                        if (index < 0 || index >= TOTAL_CELLS) {
                            return;
                        }

                        String data = stack.getOrDefault(ModDataComponents.SCRATCH_DATA, "");

                        if (data.length() != TOTAL_CELLS) {
                            data = "0".repeat(TOTAL_CELLS);
                        }

                        char[] chars = data.toCharArray();
                        chars[index] = '1';

                        stack.set(ModDataComponents.SCRATCH_DATA, new String(chars));
                    });
                }
        );

        registrar.playToClient(
                OpenScratchTicketPayload.TYPE,
                OpenScratchTicketPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = net.minecraft.client.Minecraft.getInstance().player;

                        if (player == null) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        stack.set(ModDataComponents.SCRATCH_PRIZE, payload.prize());

                        net.maximlvr.asmpthings.client.ClientHooks.openScratchTicketScreen(stack);
                    });
                }
        );

        registrar.playToClient(
                OpenCrazyPhonePayload.TYPE,
                OpenCrazyPhonePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = net.minecraft.client.Minecraft.getInstance().player;

                        if (player == null) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        net.maximlvr.asmpthings.client.ClientHooks.openCrazyPhoneScreen(stack, payload.mainHand());
                    });
                }
        );


        registrar.playToServer(
                SetupCrazyPhonePayload.TYPE,
                SetupCrazyPhonePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        tag.putString("name", sanitize(payload.name(), 24));
                        tag.putString("number", sanitize(payload.number(), 12));
                        tag.putString("password", sanitize(payload.password(), 16));
                        tag.putBoolean("locked", false);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    });
                }
        );

        registrar.playToServer(
                AddCrazyPhoneContactByNumberPayload.TYPE,
                AddCrazyPhoneContactByNumberPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        String name = sanitize(payload.name(), 24);
                        String number = sanitize(payload.number(), 12);

                        if (name.isEmpty() || number.isEmpty()) {
                            sendContactResult(player, false, "", "", "", "Numero introuvable");
                            return;
                        }

                        PhoneContact found = findPhoneByNumber(player, number);

                        if (found == null) {
                            sendContactResult(player, false, "", "", number, "Numero introuvable");
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        ListTag contacts = tag.getList("contacts", 10);

                        for (int i = 0; i < contacts.size(); i++) {
                            if (found.number().equals(contacts.getCompound(i).getString("number"))) {
                                sendContactResult(player, true, found.number(), name, found.number(), "Contact deja ajoute");
                                return;
                            }
                        }

                        CompoundTag contact = new CompoundTag();
                        contact.putString("uuid", found.number());
                        contact.putString("name", name);
                        contact.putString("number", found.number());
                        contacts.add(contact);
                        tag.put("contacts", contacts);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                        sendContactResult(player, true, found.number(), name, found.number(), "Contact ajoute");
                    });
                }
        );

        registrar.playToServer(
                SetCrazyPhoneLockedPayload.TYPE,
                SetCrazyPhoneLockedPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        tag.putBoolean("locked", payload.locked());
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    });
                }
        );

        registrar.playToServer(
                AddCrazyPhonePhotoPayload.TYPE,
                AddCrazyPhonePhotoPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        String title = sanitize(payload.title(), 32);
                        String texture = sanitize(payload.texture(), 128);

                        if (title.isEmpty() || texture.isEmpty()) {
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        ListTag photos = tag.getList("photos", 10);

                        CompoundTag photo = new CompoundTag();
                        photo.putString("title", title);
                        photo.putString("texture", texture);
                        photo.putString("type", "custom");
                        photos.add(photo);
                        tag.put("photos", photos);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    });
                }
        );

        registrar.playToServer(
                TakeCrazyPhonePhotoPayload.TYPE,
                TakeCrazyPhonePhotoPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        CrazyPhoneCameraHelper.takeOrStartPhoto(player.level(), player, stack);
                    });
                }
        );

        registrar.playToServer(
                SendCrazyPhonePhotoPayload.TYPE,
                SendCrazyPhonePhotoPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack senderStack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!senderStack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        String contactNumber = sanitize(payload.contactNumber(), 12);
                        String title = sanitize(payload.title(), 32);
                        String texture = sanitize(payload.texture(), 128);
                        String type = sanitize(payload.photoType(), 16);

                        if (contactNumber.isEmpty() || title.isEmpty() || texture.isEmpty()) {
                            sendPhotoResult(player, false, "Photo introuvable");
                            return;
                        }

                        ItemStack targetStack = findPhoneStackByNumber(player, contactNumber);

                        if (targetStack == null) {
                            sendPhotoResult(player, false, "Numero introuvable");
                            return;
                        }

                        CompoundTag tag = targetStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        ListTag photos = tag.getList("photos", 10);

                        CompoundTag photo = new CompoundTag();
                        photo.putString("title", title);
                        photo.putString("texture", texture);
                        photo.putString("type", type.isEmpty() ? "recu" : type);
                        photos.add(photo);
                        tag.put("photos", photos);
                        targetStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                        sendPhotoResult(player, true, "Photo envoyee");
                    });
                }
        );

        registrar.playToServer(
                SendCrazyPhoneMessagePayload.TYPE,
                SendCrazyPhoneMessagePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack senderStack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!senderStack.is(ModItems.CRAZY_PHONE.get())) {
                            return;
                        }

                        String contactNumber = sanitize(payload.contactNumber(), 12);
                        String message = sanitize(payload.message(), 160);

                        if (contactNumber.isEmpty() || message.isEmpty()) {
                            sendMessageResult(player, false, contactNumber, "", "Message vide");
                            return;
                        }

                        CompoundTag senderTag = senderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        String senderNumber = sanitize(senderTag.getString("number"), 12);

                        if (senderNumber.isEmpty()) {
                            sendMessageResult(player, false, contactNumber, "", "Telephone non configure");
                            return;
                        }

                        ItemStack targetStack = findPhoneStackByNumber(player, contactNumber);

                        if (targetStack == null) {
                            sendMessageResult(player, false, contactNumber, "", "Numero introuvable");
                            return;
                        }

                        long time = player.serverLevel().getGameTime();
                        addPhoneMessage(senderStack, contactNumber, senderNumber, contactNumber, message, true, time);
                        addPhoneMessage(targetStack, senderNumber, senderNumber, contactNumber, message, false, time);
                        sendMessageResult(player, true, contactNumber, message, "Message envoye");
                    });
                }
        );

        registrar.playToClient(
                CrazyPhoneContactResultPayload.TYPE,
                CrazyPhoneContactResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen screen) {
                            screen.handleContactResult(payload);
                        }
                    });
                }
        );

        registrar.playToClient(
                CrazyPhonePhotoResultPayload.TYPE,
                CrazyPhonePhotoResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen screen) {
                            screen.handlePhotoResult(payload);
                        }
                    });
                }
        );

        registrar.playToClient(
                CrazyPhoneMessageResultPayload.TYPE,
                CrazyPhoneMessageResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen screen) {
                            screen.handleMessageResult(payload);
                        }
                    });
                }
        );


    }

    private static String sanitize(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();

        if (trimmed.length() <= maxLength) {
            return trimmed;
        }

        return trimmed.substring(0, maxLength);
    }

    private static void addPhoneMessage(ItemStack stack, String conversationNumber, String fromNumber, String toNumber, String text, boolean outgoing, long time) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag conversations = tag.getList("conversations", 10);
        CompoundTag conversation = null;

        for (int i = 0; i < conversations.size(); i++) {
            CompoundTag current = conversations.getCompound(i);

            if (conversationNumber.equals(current.getString("number"))) {
                conversation = current;
                break;
            }
        }

        if (conversation == null) {
            conversation = new CompoundTag();
            conversation.putString("number", conversationNumber);
            conversation.put("messages", new ListTag());
            conversations.add(conversation);
        }

        ListTag messages = conversation.getList("messages", 10);
        CompoundTag message = new CompoundTag();
        message.putString("from", fromNumber);
        message.putString("to", toNumber);
        message.putString("text", text);
        message.putBoolean("outgoing", outgoing);
        message.putLong("time", time);
        messages.add(message);
        conversation.put("messages", messages);
        tag.put("conversations", conversations);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static PhoneContact findPhoneByNumber(ServerPlayer requester, String number) {
        for (ServerPlayer player : requester.server.getPlayerList().getPlayers()) {
            PhoneContact contact = findPhoneInInventory(player, number);

            if (contact != null) {
                return contact;
            }
        }

        return null;
    }

    private static ItemStack findPhoneStackByNumber(ServerPlayer requester, String number) {
        for (ServerPlayer player : requester.server.getPlayerList().getPlayers()) {
            ItemStack stack = findPhoneStackInInventory(player, number);

            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return null;
    }

    private static PhoneContact findPhoneInInventory(ServerPlayer player, String number) {
        Inventory inventory = player.getInventory();

        PhoneContact contact = findPhoneInStacks(player, number, inventory.items);

        if (contact != null) {
            return contact;
        }

        contact = findPhoneInStacks(player, number, inventory.offhand);

        if (contact != null) {
            return contact;
        }

        return findPhoneInStacks(player, number, inventory.armor);
    }

    private static ItemStack findPhoneStackInInventory(ServerPlayer player, String number) {
        Inventory inventory = player.getInventory();

        ItemStack stack = findPhoneStackInStacks(number, inventory.items);

        if (!stack.isEmpty()) {
            return stack;
        }

        stack = findPhoneStackInStacks(number, inventory.offhand);

        if (!stack.isEmpty()) {
            return stack;
        }

        return findPhoneStackInStacks(number, inventory.armor);
    }

    private static PhoneContact findPhoneInStacks(ServerPlayer player, String number, Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                continue;
            }

            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            if (!number.equals(tag.getString("number"))) {
                continue;
            }

            String name = tag.getString("name");

            if (name.isEmpty()) {
                name = player.getGameProfile().getName();
            }

            return new PhoneContact(player.getUUID().toString(), name, number);
        }

        return null;
    }

    private static ItemStack findPhoneStackInStacks(String number, Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.is(ModItems.CRAZY_PHONE.get())) {
                continue;
            }

            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            if (number.equals(tag.getString("number"))) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static void sendContactResult(ServerPlayer player, boolean success, String uuid, String name, String number, String message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new CrazyPhoneContactResultPayload(success, uuid, name, number, message)
        );
    }

    private static void sendPhotoResult(ServerPlayer player, boolean success, String message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new CrazyPhonePhotoResultPayload(success, message)
        );
    }

    private static void sendMessageResult(ServerPlayer player, boolean success, String contactNumber, String messageText, String status) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new CrazyPhoneMessageResultPayload(success, contactNumber, messageText, status)
        );
    }


    private record PhoneContact(String uuid, String name, String number) {
    }
}
