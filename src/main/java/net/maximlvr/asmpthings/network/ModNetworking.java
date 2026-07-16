package net.maximlvr.asmpthings.network;

import de.maxhenkel.camera.ImageData;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.bank.BankAccount;
import net.maximlvr.asmpthings.bank.BankCitizen;
import net.maximlvr.asmpthings.bank.BankMember;
import net.maximlvr.asmpthings.bank.BankPlayerRegistry;
import net.maximlvr.asmpthings.bank.BankSavedData;
import net.maximlvr.asmpthings.bank.SavedIban;
import net.maximlvr.asmpthings.block.custom.CardReaderBlock;
import net.maximlvr.asmpthings.block.entity.CardReaderBlockEntity;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.maximlvr.asmpthings.item.custom.BlueCardItem;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhoneContactByNumberPayload;
import net.maximlvr.asmpthings.network.payload.BankActionPayload;
import net.maximlvr.asmpthings.network.payload.BankSyncPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneContactResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneCameraAlbumActionPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneCameraPhotoActionPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneMessageResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhonePhotoResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneSetupResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneSyncPayload;
import net.maximlvr.asmpthings.network.payload.DisableCrazyPhoneCameraPayload;
import net.maximlvr.asmpthings.network.payload.OpenBankPayload;
import net.maximlvr.asmpthings.network.payload.OpenCardReaderConfigPayload;
import net.maximlvr.asmpthings.network.payload.OpenCrazyPhonePayload;
import net.maximlvr.asmpthings.network.payload.OpenScratchTicketPayload;
import net.maximlvr.asmpthings.network.payload.SaveCardReaderConfigPayload;
import net.maximlvr.asmpthings.network.payload.ScratchTicketScratchPayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhoneMessagePayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.SetCrazyPhoneLockedPayload;
import net.maximlvr.asmpthings.network.payload.SetupCrazyPhonePayload;
import net.maximlvr.asmpthings.network.payload.OpenCardReaderPinPayload;
import net.maximlvr.asmpthings.network.payload.SubmitCardReaderPinPayload;
import net.maximlvr.asmpthings.network.payload.TakeCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.stats.ModStats;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;


public class ModNetworking {
    private static final int GRID_COLS = 64;
    private static final int GRID_ROWS = 64;
    private static final int TOTAL_CELLS = GRID_COLS * GRID_ROWS;
    private static final int CARD_TEXTURE_WIDTH = 760;
    private static final int CARD_TEXTURE_HEIGHT = 1000;
    private static final ScratchArea[] SCRATCH_AREAS = {
            new ScratchArea(150, 348, 450, 430),
            new ScratchArea(30, 889, 700, 88)
    };
    private static final int SCRATCHABLE_CELLS = countScratchableCells();
    private static final int SCRATCH_STAT_THRESHOLD = Math.max(1, (int) Math.ceil(SCRATCHABLE_CELLS * 0.05D));
    private static final int MAX_BANK_ACCOUNTS = 5;

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
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

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
                        awardScratchTicketStatIfNeeded(player, stack, chars);
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

                        if (!ModItems.isCrazyPhone(stack)) {
                            return;
                        }

                        net.maximlvr.asmpthings.client.ClientHooks.openCrazyPhoneScreen(stack, payload.mainHand());
                    });
                }
        );

        registrar.playToClient(
                OpenBankPayload.TYPE,
                OpenBankPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.maximlvr.asmpthings.client.ClientHooks.openBankScreen();
                    });
                }
        );

        registrar.playToServer(
                BankActionPayload.TYPE,
                BankActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        handleBankAction(player, payload);
                    });
                }
        );

        registrar.playToClient(
                BankSyncPayload.TYPE,
                BankSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.maximlvr.asmpthings.client.screen.BankScreen screen) {
                            screen.handleSync(payload);
                        }
                    });
                }
        );

        registrar.playToClient(
                OpenCardReaderConfigPayload.TYPE,
                OpenCardReaderConfigPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> net.maximlvr.asmpthings.client.ClientHooks.openCardReaderConfigScreen(
                            payload.pos(),
                            payload.targetAccountId(),
                            payload.amount()
                    ));
                }
        );

        registrar.playToServer(
                SaveCardReaderConfigPayload.TYPE,
                SaveCardReaderConfigPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        if (player.level().getBlockEntity(payload.pos()) instanceof CardReaderBlockEntity blockEntity
                                && player.getUUID().equals(blockEntity.getOwner())) {
                            String target = sanitizeDigits(payload.targetAccountId(), 4);
                            int amount = Math.max(1, payload.amount());

                            if (BankSavedData.get(player.server).getAccount(target) != null) {
                                blockEntity.configure(target, amount);
                                player.displayClientMessage(Component.literal("Lecteur configure."), true);
                            } else {
                                player.displayClientMessage(Component.literal("Compte receveur introuvable."), true);
                            }
                        }
                    });
                }
        );

        registrar.playToClient(
                OpenCardReaderPinPayload.TYPE,
                OpenCardReaderPinPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> net.maximlvr.asmpthings.client.ClientHooks.openCardReaderPinScreen(payload.pos()));
                }
        );

        registrar.playToServer(
                SubmitCardReaderPinPayload.TYPE,
                SubmitCardReaderPinPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        handleCardReaderPayment(player, payload);
                    });
                }
        );


        registrar.playToServer(
                SetupCrazyPhonePayload.TYPE,
                SetupCrazyPhonePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!ModItems.isCrazyPhone(stack)) {
                            return;
                        }

                        String name = sanitize(payload.name(), 24);
                        String number = sanitizePhoneNumber(payload.number());
                        String password = sanitize(payload.password(), 16);

                        if (name.isEmpty() || number.isEmpty() || password.isEmpty()) {
                            sendSetupResult(player, false, "Champs obligatoires");
                            return;
                        }

                        if (isPhoneNumberUsedByAnotherPhone(player, stack, number)) {
                            sendSetupResult(player, false, "Numero deja utilise");
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        tag.putString("name", name);
                        tag.putString("number", number);
                        tag.putString("password", password);
                        tag.putBoolean("locked", false);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        syncPhone(player, payload.mainHand(), stack);
                        sendSetupResult(player, true, "");
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

                        if (!ModItems.isCrazyPhone(stack)) {
                            return;
                        }

                        String name = sanitize(payload.name(), 24);
                        String number = sanitizePhoneNumber(payload.number());

                        if (name.isEmpty() || number.isEmpty()) {
                            sendContactResult(player, false, "", "", "", "Numero introuvable");
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        String ownNumber = sanitizePhoneNumber(tag.getString("number"));

                        if (number.equals(ownNumber)) {
                            sendContactResult(player, false, "", "", number, "Ce numero nous appartient");
                            return;
                        }

                        PhoneContact found = findPhoneByNumber(player, number);

                        if (found == null) {
                            sendContactResult(player, false, "", "", number, "Numero introuvable");
                            return;
                        }

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

                        if (!ModItems.isCrazyPhone(stack)) {
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

                        if (!ModItems.isCrazyPhone(stack)) {
                            return;
                        }

                        String title = sanitize(payload.title(), 32);
                        String texture = sanitize(payload.texture(), 512);
                        String type = sanitize(payload.photoType(), 16);

                        if (title.isEmpty() || texture.isEmpty()) {
                            return;
                        }

                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        ListTag photos = tag.getList("photos", 10);

                        CompoundTag photo = new CompoundTag();
                        photo.putString("title", title);
                        photo.putString("texture", texture);
                        photo.putString("type", type.isEmpty() ? "custom" : type);
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

                        if (!ModItems.isCrazyPhone(stack)) {
                            return;
                        }

                        CrazyPhoneCameraHelper.takeOrStartPhoto(player.level(), player, stack);
                    });
                }
        );

        registrar.playToServer(
                DisableCrazyPhoneCameraPayload.TYPE,
                DisableCrazyPhoneCameraPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        for (InteractionHand hand : InteractionHand.values()) {
                            ItemStack stack = player.getItemInHand(hand);

                            if (!ModItems.isCrazyPhone(stack) || !CrazyPhoneCameraHelper.isActive(stack)) {
                                continue;
                            }

                            de.maxhenkel.camera.Main.CAMERA.get().setActive(stack, false);
                        }
                    });
                }
        );

        registrar.playToServer(
                CrazyPhoneCameraPhotoActionPayload.TYPE,
                CrazyPhoneCameraPhotoActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack senderStack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!ModItems.isCrazyPhone(senderStack)) {
                            return;
                        }

                        int albumIndex = payload.albumIndex();
                        List<CameraPhotoSelection> imageSelections = parseSelectedPhotoSelections(payload.imageIndexes(), payload.albumIndex(), 5);
                        String action = sanitize(payload.action(), 16);

                        if (imageSelections.isEmpty()) {
                            sendPhotoResult(player, false, "Photo introuvable");
                            return;
                        }

                        if ("take".equals(action)) {
                            int taken = 0;
                            imageSelections.sort(Comparator
                                    .comparingInt(CameraPhotoSelection::albumIndex)
                                    .thenComparingInt(CameraPhotoSelection::imageIndex)
                                    .reversed());

                            for (CameraPhotoSelection selection : imageSelections) {
                                ItemStack image = CrazyPhoneCameraHelper.removeCameraImageAt(player, senderStack, selection.albumIndex(), selection.imageIndex());

                                if (image.isEmpty()) {
                                    continue;
                                }

                                CrazyPhoneCameraHelper.giveImageFallback(player, image);
                                taken++;
                            }

                            if (taken <= 0) {
                                sendPhotoResult(player, false, "Photo introuvable");
                                return;
                            }

                            syncPhone(player, payload.mainHand(), senderStack);
                            sendPhotoResult(player, true, taken == 1 ? "Photo prise" : taken + " photos prises");
                            return;
                        }

                        if ("delete".equals(action)) {
                            int deleted = 0;
                            imageSelections.sort(Comparator
                                    .comparingInt(CameraPhotoSelection::albumIndex)
                                    .thenComparingInt(CameraPhotoSelection::imageIndex)
                                    .reversed());

                            for (CameraPhotoSelection selection : imageSelections) {
                                ItemStack image = CrazyPhoneCameraHelper.removeCameraImageAt(player, senderStack, selection.albumIndex(), selection.imageIndex());

                                if (!image.isEmpty()) {
                                    deleted++;
                                }
                            }

                            syncPhone(player, payload.mainHand(), senderStack);
                            sendPhotoResult(player, deleted > 0, deleted <= 0 ? "Photo introuvable" : deleted == 1 ? "Photo effacee" : deleted + " photos effacees");
                            return;
                        }

                        if ("send".equals(action)) {
                            String contactNumber = sanitizePhoneNumber(payload.contactNumber());

                            if (contactNumber.isEmpty()) {
                                sendPhotoResult(player, false, "Numero introuvable");
                                return;
                            }

                            int sent = 0;
                            CompoundTag senderTag = senderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                            String senderNumber = sanitizePhoneNumber(senderTag.getString("number"));

                            if (senderNumber.isEmpty()) {
                                sendPhotoResult(player, false, "Telephone non configure");
                                return;
                            }

                            PhoneStackRef targetPhone = findPhoneRefByNumber(player, contactNumber);

                            if (targetPhone == null) {
                                sendPhotoResult(player, false, "Numero introuvable");
                                return;
                            }

                            for (CameraPhotoSelection selection : imageSelections) {
                                ItemStack image = CrazyPhoneCameraHelper.getCameraImageAt(player.level().registryAccess(), senderStack, selection.albumIndex(), selection.imageIndex());

                                if (image.isEmpty()) {
                                    continue;
                                }

                                long time = player.serverLevel().getGameTime();
                                addPhonePhotoMessage(player.level().registryAccess(), senderStack, contactNumber, senderNumber, contactNumber, image, true, time);
                                addPhonePhotoMessage(player.level().registryAccess(), targetPhone.stack(), senderNumber, senderNumber, contactNumber, image, false, time);
                                sent++;
                            }

                            if (sent <= 0) {
                                sendPhotoResult(player, false, "Photo introuvable");
                                return;
                            }

                            player.awardStat(ModStats.CRAZY_PHONE_MESSAGES_SENT.get(), sent);
                            syncPhone(player, payload.mainHand(), senderStack);
                            syncHeldPhone(targetPhone);
                            sendPhotoResult(player, true, sent == 1 ? "Photo envoyee" : sent + " photos envoyees");
                        }
                    });
                }
        );

        registrar.playToServer(
                CrazyPhoneCameraAlbumActionPayload.TYPE,
                CrazyPhoneCameraAlbumActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        if (!ModItems.isCrazyPhone(stack)) {
                            return;
                        }

                        String action = sanitize(payload.action(), 16);

                        if ("create".equals(action)) {
                            CrazyPhoneCameraHelper.createAlbum(player, stack);
                        } else if ("rename".equals(action)) {
                            CrazyPhoneCameraHelper.renameAlbum(player, stack, payload.albumIndex(), sanitize(payload.name(), 24));
                        } else if ("upload_target".equals(action)) {
                            CrazyPhoneCameraHelper.setUploadTarget(player, payload.albumIndex(), payload.uploadCount());
                        } else if ("assign".equals(action)) {
                            List<CameraPhotoSelection> selections = parseSelectedPhotoSelections(payload.name(), -1, 5);

                            for (CameraPhotoSelection selection : selections) {
                                ItemStack image = CrazyPhoneCameraHelper.getCameraImageAt(player.level().registryAccess(), stack, selection.albumIndex(), selection.imageIndex());
                                ImageData data = ImageData.fromStack(image);

                                if (data != null) {
                                    CrazyPhoneCameraHelper.assignImageToGroup(player, stack, data.getId().toString(), payload.albumIndex());
                                }
                            }
                        }
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

                        if (!ModItems.isCrazyPhone(senderStack)) {
                            return;
                        }

                        String contactNumber = sanitizePhoneNumber(payload.contactNumber());
                        String title = sanitize(payload.title(), 32);
                        String texture = sanitize(payload.texture(), 128);
                        String type = sanitize(payload.photoType(), 16);

                        if (contactNumber.isEmpty() || title.isEmpty() || texture.isEmpty()) {
                            sendPhotoResult(player, false, "Photo introuvable");
                            return;
                        }

                        PhoneStackRef targetPhone = findPhoneRefByNumber(player, contactNumber);

                        if (targetPhone == null) {
                            sendPhotoResult(player, false, "Numero introuvable");
                            return;
                        }

                        CompoundTag senderTag = senderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        String senderNumber = sanitizePhoneNumber(senderTag.getString("number"));

                        if (senderNumber.isEmpty()) {
                            sendPhotoResult(player, false, "Telephone non configure");
                            return;
                        }

                        long time = player.serverLevel().getGameTime();
                        addPhoneUploadedPhotoMessage(senderStack, contactNumber, senderNumber, contactNumber, title, texture, type, true, time);
                        addPhoneUploadedPhotoMessage(targetPhone.stack(), senderNumber, senderNumber, contactNumber, title, texture, type, false, time);
                        player.awardStat(ModStats.CRAZY_PHONE_MESSAGES_SENT.get());
                        syncPhone(player, payload.mainHand(), senderStack);
                        syncHeldPhone(targetPhone);

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

                        if (!ModItems.isCrazyPhone(senderStack)) {
                            return;
                        }

                        String contactNumber = sanitizePhoneNumber(payload.contactNumber());
                        String message = sanitize(payload.message(), 800);

                        if (contactNumber.isEmpty() || message.isEmpty()) {
                            sendMessageResult(player, false, contactNumber, "", "Message vide");
                            return;
                        }

                        CompoundTag senderTag = senderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        String senderNumber = sanitizePhoneNumber(senderTag.getString("number"));

                        if (senderNumber.isEmpty()) {
                            sendMessageResult(player, false, contactNumber, "", "Telephone non configure");
                            return;
                        }

                        PhoneStackRef targetPhone = findPhoneRefByNumber(player, contactNumber);

                        if (targetPhone == null) {
                            sendMessageResult(player, false, contactNumber, "", "Numero introuvable");
                            return;
                        }

                        long time = player.serverLevel().getGameTime();
                        addPhoneMessage(senderStack, contactNumber, senderNumber, contactNumber, message, true, time);
                        addPhoneMessage(targetPhone.stack(), senderNumber, senderNumber, contactNumber, message, false, time);
                        player.awardStat(ModStats.CRAZY_PHONE_MESSAGES_SENT.get());
                        syncHeldPhone(targetPhone);
                        sendMessageResult(player, true, contactNumber, message, "Message envoye");
                    });
                }
        );

        registrar.playToClient(
                CrazyPhoneSetupResultPayload.TYPE,
                CrazyPhoneSetupResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen screen) {
                            screen.handleSetupResult(payload);
                        }
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

        registrar.playToClient(
                CrazyPhoneSyncPayload.TYPE,
                CrazyPhoneSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen screen) {
                            screen.handleSync(payload);
                        }
                    });
                }
        );

    }

    private static void awardScratchTicketStatIfNeeded(ServerPlayer player, ItemStack stack, char[] scratchData) {
        if (stack.getOrDefault(ModDataComponents.SCRATCH_STAT_COUNTED, false)) {
            return;
        }

        if (countScratchedScratchableCells(scratchData) < SCRATCH_STAT_THRESHOLD) {
            return;
        }

        player.awardStat(ModStats.SCRATCH_TICKETS_SCRATCHED.get());
        stack.set(ModDataComponents.SCRATCH_STAT_COUNTED, true);
    }

    private static int countScratchedScratchableCells(char[] scratchData) {
        int count = 0;

        for (int index = 0; index < Math.min(scratchData.length, TOTAL_CELLS); index++) {
            int col = index % GRID_COLS;
            int row = index / GRID_COLS;

            if (scratchData[index] == '1' && isScratchableCell(col, row)) {
                count++;
            }
        }

        return count;
    }

    private static int countScratchableCells() {
        int count = 0;

        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                if (isScratchableCell(col, row)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static boolean isScratchableCell(int col, int row) {
        float textureX = (col + 0.5F) * CARD_TEXTURE_WIDTH / GRID_COLS;
        float textureY = (row + 0.5F) * CARD_TEXTURE_HEIGHT / GRID_ROWS;
        return isScratchableTexturePoint(textureX, textureY);
    }

    private static boolean isScratchableTexturePoint(float textureX, float textureY) {
        for (ScratchArea area : SCRATCH_AREAS) {
            if (textureX >= area.x()
                    && textureX < area.x() + area.width()
                    && textureY >= area.y()
                    && textureY < area.y() + area.height()) {
                return true;
            }
        }

        return false;
    }

    private static String sanitize(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();

        if (trimmed.length() <= maxLength) {
            return trimmed;
        }

        return trimmed.substring(0, maxLength);
    }

    private static String sanitizeDigits(String value, int maxLength) {
        String sanitized = sanitize(value, maxLength).replaceAll("[^0-9]", "");

        if (sanitized.length() <= maxLength) {
            return sanitized;
        }

        return sanitized.substring(0, maxLength);
    }

    private static String sanitizePhoneNumber(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.matches("\\d{1,6}") ? trimmed : "";
    }

    private static List<CameraPhotoSelection> parseSelectedPhotoSelections(String value, int defaultAlbumIndex, int maxCount) {
        List<CameraPhotoSelection> indexes = new ArrayList<>();

        for (String part : sanitize(value, 128).split(",")) {
            if (indexes.size() >= maxCount) {
                break;
            }

            try {
                String trimmed = part.trim();
                int albumIndex = defaultAlbumIndex;
                int imageIndex;

                if (trimmed.contains(":")) {
                    String[] parts = trimmed.split(":", 2);
                    albumIndex = Integer.parseInt(parts[0].trim());
                    imageIndex = Integer.parseInt(parts[1].trim());
                } else {
                    imageIndex = Integer.parseInt(trimmed);
                }

                CameraPhotoSelection selection = new CameraPhotoSelection(albumIndex, imageIndex);

                if (albumIndex >= 0 && imageIndex >= 0 && !indexes.contains(selection)) {
                    indexes.add(selection);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return indexes;
    }

    private static void syncPhone(ServerPlayer player, boolean mainHand, ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new CrazyPhoneSyncPayload(mainHand, tag));
    }

    private static void syncHeldPhone(PhoneStackRef phone) {
        if (phone.held()) {
            syncPhone(phone.player(), phone.mainHand(), phone.stack());
        }
    }

    private static void handleBankAction(ServerPlayer player, BankActionPayload payload) {
        BankSavedData bank = BankSavedData.get(player.server);
        String selectedId = sanitizeDigits(payload.accountId(), 4);
        String message = "";

        switch (sanitize(payload.action(), 16)) {
            case "create" -> {
                String name = sanitize(payload.text(), 24);

                if (name.isEmpty()) {
                    message = "Nom de compte requis.";
                } else if (bank.getOwnedAccounts(player.getUUID()).size() >= MAX_BANK_ACCOUNTS) {
                    message = "Maximum 5 comptes.";
                } else {
                    BankAccount account = bank.createAccount(player.getUUID(), name, player.getRandom());
                    selectedId = account.id();
                    message = "Compte " + account.id() + " cree.";
                }
            }
            case "deposit" -> {
                int amount = Math.max(0, payload.amount());
                BankAccount account = bank.getAccount(selectedId);

                if (!canUseAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
                } else if (account != null && account.adminAccount()) {
                    message = "Ce compte ne peut pas deposer.";
                } else if (amount <= 0 || countCrazyCoins(player) < amount) {
                    message = "Pas assez de crazycoins sur toi.";
                } else {
                    removeCrazyCoins(player, amount);
                    bank.deposit(selectedId, amount);
                    message = "Depot effectue.";
                }
            }
            case "withdraw" -> {
                int amount = Math.max(0, payload.amount());
                BankAccount account = bank.getAccount(selectedId);

                if (!canUseAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
                } else if (account != null && account.adminAccount()) {
                    message = "Ce compte ne peut pas retirer.";
                } else if (!bank.withdraw(selectedId, amount)) {
                    message = "Solde insuffisant.";
                } else {
                    giveCrazyCoins(player, amount);
                    message = "Retrait effectue.";
                }
            }
            case "transfer" -> {
                String targetId = sanitizeDigits(payload.targetAccountId(), 4);
                int amount = Math.max(0, payload.amount());

                if (!canUseAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
                } else if (!bank.transfer(selectedId, targetId, amount)) {
                    message = "Transfert impossible.";
                } else {
                    message = "Transfert envoye.";
                }
            }
            case "card" -> {
                BankAccount account = bank.getAccount(selectedId);

                if (!canUseAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
                } else if (account != null && account.adminAccount()) {
                    message = "Ce compte ne peut pas creer de carte.";
                } else {
                    String pin = sanitizeDigits(payload.pin(), 6);
                    String color = sanitize(payload.targetAccountId(), 16);

                    if (pin.length() < 4) {
                        message = "Code de carte: 4 chiffres minimum.";
                    } else if (cardItemForColor(color) == null) {
                        message = "Choisis une couleur.";
                    } else {
                        String cardName = sanitize(payload.text(), 32);
                        giveBankCard(player, selectedId, cardName.isEmpty() ? "Carte bancaire" : cardName, pin, color);
                        message = "Carte generee.";
                    }
                }
            }
            case "save_iban" -> {
                String targetId = sanitizeDigits(payload.targetAccountId(), 4);
                String name = sanitize(payload.text(), 24);

                if (name.isEmpty() || targetId.length() != 4) {
                    message = "Pseudo et IBAN requis.";
                } else if (bank.getAccount(targetId) == null) {
                    message = "IBAN introuvable.";
                } else {
                    bank.saveIban(player.getUUID(), name, targetId);
                    message = "IBAN sauvegarde.";
                }
            }
            case "add_member" -> {
                String targetName = sanitize(payload.targetAccountId(), 32);
                ServerPlayer targetPlayer = targetName.isEmpty() ? null : player.server.getPlayerList().getPlayerByName(targetName);

                if (!ownsAccount(player, bank, selectedId)) {
                    message = "Seul le createur peut gerer ce compte.";
                } else if (targetPlayer == null) {
                    message = "Joueur introuvable ou hors ligne.";
                } else if (bank.addMember(selectedId, player.getUUID(), targetPlayer.getUUID(), targetPlayer.getGameProfile().getName())) {
                    message = "Personne ajoutee.";
                } else {
                    message = "Ajout impossible.";
                }
            }
            case "remove_member" -> {
                UUID memberId = parseUuid(payload.targetAccountId());

                if (!ownsAccount(player, bank, selectedId)) {
                    message = "Seul le createur peut gerer ce compte.";
                } else if (memberId == null || !bank.removeMember(selectedId, player.getUUID(), memberId)) {
                    message = "Retrait impossible.";
                } else {
                    message = "Personne retiree.";
                }
            }
            case "save_citizen" -> {
                String targetId = sanitizeDigits(payload.targetAccountId(), 4);
                UUID citizenId = parseUuid(payload.text());
                int salary = Math.max(0, payload.amount());
                BankPlayerRegistry.Entry playerEntry = citizenId == null ? null : BankPlayerRegistry.get(player.server).get(citizenId);

                if (!canManageAdminAccount(player, bank, selectedId)) {
                    message = "Compte admin requis.";
                } else if (playerEntry == null) {
                    message = "Joueur introuvable.";
                } else if (!targetId.isEmpty() && targetId.length() != 4) {
                    message = "IBAN invalide.";
                } else if (!targetId.isEmpty() && bank.getAccount(targetId) == null) {
                    message = "IBAN inexistant.";
                } else if (bank.saveCitizen(selectedId, citizenId, playerEntry.name(), targetId, salary)) {
                    message = "Citoyen sauvegarde.";
                } else {
                    message = "Citoyen impossible a sauvegarder.";
                }
            }
            default -> message = "";
        }

        if (!message.isEmpty()) {
            player.displayClientMessage(Component.literal(message), false);
        }

        sendBankSync(player, selectedId, "");
    }

    private static void handleCardReaderPayment(ServerPlayer player, SubmitCardReaderPinPayload payload) {
        ItemStack stack = payload.mainHand() ? player.getMainHandItem() : player.getOffhandItem();

        if (!ModItems.isBankCard(stack)) {
            player.displayClientMessage(Component.literal("Carte bancaire introuvable."), true);
            return;
        }

        if (!(player.level().getBlockEntity(payload.pos()) instanceof CardReaderBlockEntity blockEntity)) {
            return;
        }

        BankSavedData bank = BankSavedData.get(player.server);
        String cardAccountId = BlueCardItem.getAccountId(stack);
        String cardPin = BlueCardItem.getPin(stack);

        if (!sanitizeDigits(payload.pin(), 6).equals(cardPin)) {
            player.displayClientMessage(Component.literal("Code incorrect."), true);
            return;
        }

        if (blockEntity.getTargetAccountId().isEmpty() || bank.getAccount(blockEntity.getTargetAccountId()) == null) {
            player.displayClientMessage(Component.literal("Lecteur non configure."), true);
            return;
        }

        long gameTime = player.level().getGameTime();

        if (!blockEntity.canAcceptPayment(gameTime, CardReaderBlock.PAYMENT_COOLDOWN_TICKS)) {
            player.displayClientMessage(Component.literal("Lecteur en attente."), true);
            return;
        }

        if (!bank.transfer(cardAccountId, blockEntity.getTargetAccountId(), blockEntity.getAmount())) {
            player.displayClientMessage(Component.literal("Paiement refuse."), true);
            return;
        }

        blockEntity.markPayment(gameTime);
        CardReaderBlock.triggerPaymentSignal(player.level(), payload.pos());

        player.displayClientMessage(Component.literal("Paiement accepte."), true);
    }

    private static boolean ownsAccount(ServerPlayer player, BankSavedData bank, String accountId) {
        BankAccount account = bank.getAccount(accountId);
        return account != null && account.owner().equals(player.getUUID());
    }

    private static boolean canUseAccount(ServerPlayer player, BankSavedData bank, String accountId) {
        BankAccount account = bank.getAccount(accountId);
        return account != null && account.hasAccess(player.getUUID());
    }

    private static boolean canManageAdminAccount(ServerPlayer player, BankSavedData bank, String accountId) {
        BankAccount account = bank.getAccount(accountId);
        return account != null && account.adminAccount() && account.hasAccess(player.getUUID());
    }

    public static void sendBankSync(ServerPlayer player, String selectedAccountId, String message) {
        BankSavedData bank = BankSavedData.get(player.server);
        BankPlayerRegistry playerRegistry = BankPlayerRegistry.get(player.server);
        playerRegistry.syncPlayerBankInfo(player, bank);
        List<BankAccount> accounts = bank.getAccounts(player.getUUID());
        StringBuilder builder = new StringBuilder();
        StringBuilder savedIbanBuilder = new StringBuilder();

        for (BankAccount account : accounts) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }

            builder.append(account.id())
                    .append('|')
                    .append(syncField(account.name()))
                    .append('|')
                    .append(account.balance())
                    .append('|')
                    .append(account.owner().equals(player.getUUID()) ? "1" : "0")
                    .append('|')
                    .append(account.members().isEmpty() ? "0" : "1")
                    .append('|')
                    .append(syncMembers(account.members()))
                    .append('|')
                    .append(account.adminAccount() ? "1" : "0")
                    .append('|')
                    .append(syncCitizens(account.citizens(), playerRegistry.displayedPlayers()));
        }

        boolean selectedAccountVisible = false;

        for (BankAccount account : accounts) {
            if (account.id().equals(selectedAccountId)) {
                selectedAccountVisible = true;
                break;
            }
        }

        if (!accounts.isEmpty() && (selectedAccountId == null || !selectedAccountVisible)) {
            selectedAccountId = accounts.getFirst().id();
        } else if (accounts.isEmpty()) {
            selectedAccountId = "";
        }

        for (SavedIban savedIban : bank.getSavedIbans(player.getUUID())) {
            if (!savedIbanBuilder.isEmpty()) {
                savedIbanBuilder.append('\n');
            }

            savedIbanBuilder.append(savedIban.name().replace("|", " "))
                    .append('|')
                    .append(savedIban.iban());
        }

        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new BankSyncPayload(builder.toString(), savedIbanBuilder.toString(), selectedAccountId == null ? "" : selectedAccountId, countCrazyCoins(player), message)
        );
    }

    private static int countCrazyCoins(ServerPlayer player) {
        int count = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CRAZY_COIN.get())) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private static void removeCrazyCoins(ServerPlayer player, int amount) {
        int remaining = amount;

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModItems.CRAZY_COIN.get())) {
                continue;
            }

            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;

            if (remaining <= 0) {
                break;
            }
        }
    }

    private static void giveCrazyCoins(ServerPlayer player, int amount) {
        int remaining = amount;

        while (remaining > 0) {
            int count = Math.min(64, remaining);
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CRAZY_COIN.get(), count));
            remaining -= count;
        }
    }

    private static void giveBankCard(ServerPlayer player, String accountId, String cardName, String pin, String color) {
        Item item = cardItemForColor(color);

        if (item == null) {
            return;
        }

        ItemStack stack = new ItemStack(item);
        BlueCardItem.setup(stack, accountId, pin);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(cardName));
        player.getInventory().placeItemBackInInventory(stack);
    }

    private static Item cardItemForColor(String color) {
        return switch (color) {
            case "blue" -> ModItems.BLUE_CARD.get();
            case "red" -> ModItems.RED_CARD.get();
            case "green" -> ModItems.GREEN_CARD.get();
            case "black" -> ModItems.BLACK_CARD.get();
            case "grey" -> ModItems.GREY_CARD.get();
            default -> null;
        };
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String syncMembers(List<BankMember> members) {
        StringBuilder builder = new StringBuilder();

        for (BankMember member : members) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }

            builder.append(member.id())
                    .append(',')
                    .append(syncField(member.name()));
        }

        return builder.toString();
    }

    private static String syncCitizens(List<BankCitizen> citizens, List<BankPlayerRegistry.Entry> displayedPlayers) {
        StringBuilder builder = new StringBuilder();

        for (BankPlayerRegistry.Entry player : displayedPlayers) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }

            BankCitizen citizen = null;

            for (BankCitizen candidate : citizens) {
                if (candidate.playerId().equals(player.id())) {
                    citizen = candidate;
                    break;
                }
            }

            builder.append(player.id())
                    .append(',')
                    .append(syncField(player.name()))
                    .append(',')
                    .append(citizen == null ? "" : citizen.iban())
                    .append(',')
                    .append(citizen == null ? 0 : citizen.salary());
        }

        return builder.toString();
    }

    private static String syncField(String value) {
        return value.replace('|', ' ')
                .replace(';', ' ')
                .replace(',', ' ')
                .replace('\n', ' ');
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

    private static void addPhonePhotoMessage(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack, String conversationNumber, String fromNumber, String toNumber, ItemStack image, boolean outgoing, long time) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag conversations = tag.getList("conversations", 10);
        CompoundTag conversation = getOrCreateConversation(conversations, conversationNumber);
        ListTag messages = conversation.getList("messages", 10);
        CompoundTag message = createBaseMessage(fromNumber, toNumber, "[Photo]", outgoing, time);
        message.putString("kind", "camera_photo");
        message.put("image", image.save(registries));
        messages.add(message);
        conversation.put("messages", messages);
        tag.put("conversations", conversations);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void addPhoneUploadedPhotoMessage(ItemStack stack, String conversationNumber, String fromNumber, String toNumber, String title, String texture, String type, boolean outgoing, long time) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag conversations = tag.getList("conversations", 10);
        CompoundTag conversation = getOrCreateConversation(conversations, conversationNumber);
        ListTag messages = conversation.getList("messages", 10);
        CompoundTag message = createBaseMessage(fromNumber, toNumber, "[Photo]", outgoing, time);
        message.putString("kind", "uploaded_photo");
        message.putString("photoTitle", title);
        message.putString("photoTexture", texture);
        message.putString("photoType", type.isEmpty() ? "recu" : type);
        messages.add(message);
        conversation.put("messages", messages);
        tag.put("conversations", conversations);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getOrCreateConversation(ListTag conversations, String conversationNumber) {
        for (int i = 0; i < conversations.size(); i++) {
            CompoundTag current = conversations.getCompound(i);

            if (conversationNumber.equals(current.getString("number"))) {
                return current;
            }
        }

        CompoundTag conversation = new CompoundTag();
        conversation.putString("number", conversationNumber);
        conversation.put("messages", new ListTag());
        conversations.add(conversation);
        return conversation;
    }

    private static CompoundTag createBaseMessage(String fromNumber, String toNumber, String text, boolean outgoing, long time) {
        CompoundTag message = new CompoundTag();
        message.putString("from", fromNumber);
        message.putString("to", toNumber);
        message.putString("text", text);
        message.putBoolean("outgoing", outgoing);
        message.putLong("time", time);
        return message;
    }

    private static boolean isPhoneNumberUsedByAnotherPhone(ServerPlayer requester, ItemStack currentStack, String number) {
        for (ServerPlayer player : requester.server.getPlayerList().getPlayers()) {
            if (isPhoneNumberOnAnotherStack(currentStack, player.getMainHandItem(), number)
                    || isPhoneNumberOnAnotherStack(currentStack, player.getOffhandItem(), number)
                    || isPhoneNumberInStacks(currentStack, player.getInventory().items, number)
                    || isPhoneNumberInStacks(currentStack, player.getInventory().armor, number)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPhoneNumberInStacks(ItemStack currentStack, Iterable<ItemStack> stacks, String number) {
        for (ItemStack stack : stacks) {
            if (isPhoneNumberOnAnotherStack(currentStack, stack, number)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPhoneNumberOnAnotherStack(ItemStack currentStack, ItemStack stack, String number) {
        return stack != currentStack && isPhoneNumber(stack, number);
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

    private static PhoneStackRef findPhoneRefByNumber(ServerPlayer requester, String number) {
        for (ServerPlayer player : requester.server.getPlayerList().getPlayers()) {
            PhoneStackRef ref = findPhoneRefInInventory(player, number);

            if (ref != null) {
                return ref;
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

    private static PhoneStackRef findPhoneRefInInventory(ServerPlayer player, String number) {
        ItemStack mainHand = player.getMainHandItem();

        if (isPhoneNumber(mainHand, number)) {
            return new PhoneStackRef(player, mainHand, true, true);
        }

        ItemStack offhand = player.getOffhandItem();

        if (isPhoneNumber(offhand, number)) {
            return new PhoneStackRef(player, offhand, false, true);
        }

        ItemStack inventoryStack = findPhoneStackInStacks(number, player.getInventory().items);

        if (!inventoryStack.isEmpty()) {
            return new PhoneStackRef(player, inventoryStack, false, false);
        }

        inventoryStack = findPhoneStackInStacks(number, player.getInventory().armor);

        if (!inventoryStack.isEmpty()) {
            return new PhoneStackRef(player, inventoryStack, false, false);
        }

        return null;
    }

    private static PhoneContact findPhoneInStacks(ServerPlayer player, String number, Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!ModItems.isCrazyPhone(stack)) {
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
            if (isPhoneNumber(stack, number)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isPhoneNumber(ItemStack stack, String number) {
        if (!ModItems.isCrazyPhone(stack)) {
            return false;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return number.equals(tag.getString("number"));
    }

    private static void sendContactResult(ServerPlayer player, boolean success, String uuid, String name, String number, String message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new CrazyPhoneContactResultPayload(success, uuid, name, number, message)
        );
    }

    private static void sendSetupResult(ServerPlayer player, boolean success, String message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new CrazyPhoneSetupResultPayload(success, message)
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

    private record PhoneStackRef(ServerPlayer player, ItemStack stack, boolean mainHand, boolean held) {
    }

    private record CameraPhotoSelection(int albumIndex, int imageIndex) {
    }

    private record ScratchArea(int x, int y, int width, int height) {
    }
}
