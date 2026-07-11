package net.maximlvr.asmpthings.network;

import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.bank.BankAccount;
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
import net.maximlvr.asmpthings.network.payload.CrazyPhoneMessageResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhonePhotoResultPayload;
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
import java.util.UUID;


public class ModNetworking {
    private static final int GRID_COLS = 64;
    private static final int GRID_ROWS = 64;
    private static final int TOTAL_CELLS = GRID_COLS * GRID_ROWS;
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

    private static String sanitizeDigits(String value, int maxLength) {
        String sanitized = sanitize(value, maxLength).replaceAll("[^0-9]", "");

        if (sanitized.length() <= maxLength) {
            return sanitized;
        }

        return sanitized.substring(0, maxLength);
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
                } else if (bank.getAccounts(player.getUUID()).size() >= MAX_BANK_ACCOUNTS) {
                    message = "Maximum 5 comptes.";
                } else {
                    BankAccount account = bank.createAccount(player.getUUID(), name, player.getRandom());
                    selectedId = account.id();
                    player.displayClientMessage(Component.literal("Compte " + account.id() + " cree."), false);
                }
            }
            case "deposit" -> {
                int amount = Math.max(0, payload.amount());

                if (!ownsAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
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

                if (!ownsAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
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

                if (!ownsAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
                } else if (!bank.transfer(selectedId, targetId, amount)) {
                    message = "Transfert impossible.";
                } else {
                    message = "Transfert envoye.";
                }
            }
            case "card" -> {
                if (!ownsAccount(player, bank, selectedId)) {
                    message = "Selectionne un de tes comptes.";
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
            default -> message = "";
        }

        sendBankSync(player, selectedId, message);
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

        if (!bank.transfer(cardAccountId, blockEntity.getTargetAccountId(), blockEntity.getAmount())) {
            player.displayClientMessage(Component.literal("Paiement refuse."), true);
            return;
        }

        BlockState state = player.level().getBlockState(payload.pos());

        if (state.getBlock() instanceof CardReaderBlock && !state.getValue(CardReaderBlock.POWERED)) {
            player.level().setBlock(payload.pos(), state.setValue(CardReaderBlock.POWERED, true), 3);
            player.level().scheduleTick(payload.pos(), state.getBlock(), 20);
            player.level().updateNeighborsAt(payload.pos(), state.getBlock());
        }

        player.displayClientMessage(Component.literal("Paiement accepte."), true);
    }

    private static boolean ownsAccount(ServerPlayer player, BankSavedData bank, String accountId) {
        BankAccount account = bank.getAccount(accountId);
        return account != null && account.owner().equals(player.getUUID());
    }

    public static void sendBankSync(ServerPlayer player, String selectedAccountId, String message) {
        BankSavedData bank = BankSavedData.get(player.server);
        List<BankAccount> accounts = bank.getAccounts(player.getUUID());
        StringBuilder builder = new StringBuilder();
        StringBuilder savedIbanBuilder = new StringBuilder();

        for (BankAccount account : accounts) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }

            builder.append(account.id())
                    .append('|')
                    .append(account.name().replace("|", " "))
                    .append('|')
                    .append(account.balance());
        }

        if ((selectedAccountId == null || selectedAccountId.isEmpty()) && !accounts.isEmpty()) {
            selectedAccountId = accounts.getFirst().id();
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
