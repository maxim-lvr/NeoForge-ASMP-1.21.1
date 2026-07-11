package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.network.payload.BankActionPayload;
import net.maximlvr.asmpthings.network.payload.BankSyncPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class BankScreen extends Screen {
    private static final int MAX_ACCOUNTS = 5;
    private static final int BG_WIDTH = 128;
    private static final int BG_HEIGHT = 64;
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "textures/screens/bank_background.png");

    private final List<ClientBankAccount> accounts = new ArrayList<>();
    private final List<ClientSavedIban> savedIbans = new ArrayList<>();
    private Page page = Page.HOME;
    private String selectedAccountId = "";
    private int carriedCoins;
    private String message = "";
    private EditBox accountNameField;
    private EditBox amountField;
    private EditBox targetField;
    private EditBox ibanNameField;
    private EditBox ibanCodeField;
    private EditBox cardNameField;
    private EditBox pinField;
    private String selectedCardColor = "";
    private String cardNameDraft = "";
    private String pinDraft = "";
    private String transferTargetDraft = "";
    private String transferAmountDraft = "";
    private int paperButtonX;
    private int paperButtonY;

    public BankScreen() {
        super(Component.literal("Banque"));
    }

    @Override
    protected void init() {
        switch (page) {
            case HOME -> initHome();
            case CREATE -> initCreate();
            case ACCOUNTS -> initAccounts();
            case ACCOUNT -> initAccount();
            case TRANSFER -> initTransfer();
            case IBAN_BOOK -> initIbanBook();
            case DEPOSIT -> initMoneyAction("deposit");
            case WITHDRAW -> initMoneyAction("withdraw");
            case CARD -> initCard();
        }
    }

    private void initHome() {
        int x = centerX() - 70;
        int y = centerY() - 22;

        addRenderableWidget(Button.builder(Component.literal("Voir mes comptes"), button -> switchPage(Page.ACCOUNTS))
                .bounds(x, y, 140, 20)
                .build());

        Button createButton = Button.builder(Component.literal("Creer un compte"), button -> switchPage(Page.CREATE))
                .bounds(x, y + 28, 140, 20)
                .build();
        createButton.active = accounts.size() < MAX_ACCOUNTS;
        addRenderableWidget(createButton);
    }

    private void initCreate() {
        int x = centerX() - 80;
        int y = centerY() - 34;

        accountNameField = new EditBox(font, x, y + 20, 160, 18, Component.literal("Nom du compte"));
        accountNameField.setMaxLength(24);
        addRenderableWidget(accountNameField);

        Button createButton = Button.builder(Component.literal("Creer"), button -> sendCreate())
                .bounds(x, y + 48, 76, 20)
                .build();
        createButton.active = accounts.size() < MAX_ACCOUNTS;
        addRenderableWidget(createButton);

        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> switchPage(Page.HOME))
                .bounds(x + 84, y + 48, 76, 20)
                .build());
    }

    private void initAccounts() {
        int x = centerX() - 90;
        int y = centerY() - 54;

        for (int i = 0; i < accounts.size(); i++) {
            ClientBankAccount account = accounts.get(i);
            int buttonY = y + i * 24;
            addRenderableWidget(Button.builder(Component.literal(account.name()), button -> {
                selectedAccountId = account.id();
                switchPage(Page.ACCOUNT);
            }).bounds(x, buttonY, 180, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.HOME))
                .bounds(x, y + 126, 84, 20)
                .build());
    }

    private void initAccount() {
        int buttonWidth = 110;
        int gap = 8;
        int x = centerX() - buttonWidth - gap / 2;
        int y = centerY() - 12;

        addRenderableWidget(Button.builder(Component.literal("Faire un transfert"), button -> switchPage(Page.TRANSFER))
                .bounds(x, y, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Deposer de l'argent"), button -> switchPage(Page.DEPOSIT))
                .bounds(x + buttonWidth + gap, y, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Retirer de l'argent"), button -> switchPage(Page.WITHDRAW))
                .bounds(x, y + 26, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Demander une carte"), button -> switchPage(Page.CARD))
                .bounds(x + buttonWidth + gap, y + 26, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.ACCOUNTS))
                .bounds(x, y + 54, buttonWidth, 20)
                .build());
    }

    private void initTransfer() {
        int x = centerX() - 80;
        int y = centerY() - 38;

        targetField = new EditBox(font, x, y + 28, 108, 18, Component.literal("IBAN cible"));
        targetField.setMaxLength(4);
        targetField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,4}"));
        targetField.setValue(transferTargetDraft);
        addRenderableWidget(targetField);

        paperButtonX = x + 116;
        paperButtonY = y + 28;
        addRenderableWidget(Button.builder(Component.empty(), button -> {
            transferTargetDraft = targetField.getValue();
            transferAmountDraft = amountField == null ? transferAmountDraft : amountField.getValue();
            switchPage(Page.IBAN_BOOK);
        }).bounds(paperButtonX, paperButtonY, 22, 18).build());

        amountField = new EditBox(font, x, y + 58, 160, 18, Component.literal("Montant"));
        amountField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        amountField.setValue(transferAmountDraft);
        addRenderableWidget(amountField);

        addRenderableWidget(Button.builder(Component.literal("Transferer"), button -> sendMoney("transfer"))
                .bounds(x, y + 88, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> switchPage(Page.ACCOUNT))
                .bounds(x + 84, y + 88, 76, 20)
                .build());
    }

    private void initIbanBook() {
        int x = centerX() - 100;
        int y = centerY() - 64;

        for (int i = 0; i < Math.min(savedIbans.size(), 4); i++) {
            ClientSavedIban savedIban = savedIbans.get(i);
            addRenderableWidget(Button.builder(Component.literal(savedIban.name() + " - " + savedIban.iban()), button -> {
                transferTargetDraft = savedIban.iban();
                switchPage(Page.TRANSFER);
            }).bounds(x, y + i * 22, 200, 18).build());
        }

        ibanNameField = new EditBox(font, x, y + 92, 96, 18, Component.literal("Pseudo"));
        ibanNameField.setMaxLength(24);
        addRenderableWidget(ibanNameField);

        ibanCodeField = new EditBox(font, x + 104, y + 92, 48, 18, Component.literal("IBAN"));
        ibanCodeField.setMaxLength(4);
        ibanCodeField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,4}"));
        addRenderableWidget(ibanCodeField);

        addRenderableWidget(Button.builder(Component.literal("Ajouter"), button -> sendSaveIban())
                .bounds(x, y + 120, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.TRANSFER))
                .bounds(x + 84, y + 120, 76, 20)
                .build());
    }

    private void initMoneyAction(String action) {
        int x = centerX() - 80;
        int y = centerY() - 26;

        amountField = new EditBox(font, x, y + 34, 160, 18, Component.literal("Montant"));
        amountField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        addRenderableWidget(amountField);

        String label = action.equals("deposit") ? "Deposer" : "Retirer";
        addRenderableWidget(Button.builder(Component.literal(label), button -> sendMoney(action))
                .bounds(x, y + 66, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> switchPage(Page.ACCOUNT))
                .bounds(x + 84, y + 66, 76, 20)
                .build());
    }

    private void initCard() {
        int x = centerX() - 100;
        int y = centerY() - 34;

        cardNameField = new EditBox(font, x, y + 18, 200, 18, Component.literal("Nom carte"));
        cardNameField.setMaxLength(32);
        cardNameField.setValue(cardNameDraft);
        addRenderableWidget(cardNameField);

        pinField = new EditBox(font, x, y + 48, 200, 18, Component.literal("Code"));
        pinField.setMaxLength(6);
        pinField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,6}"));
        pinField.setValue(pinDraft);
        addRenderableWidget(pinField);

        int colorX = centerX() - 170;
        addColorButton("blue", "Bleue", colorX, y + 78, 64);
        addColorButton("red", "Rouge", colorX + 69, y + 78, 64);
        addColorButton("green", "Verte", colorX + 138, y + 78, 64);
        addColorButton("black", "Noire", colorX + 207, y + 78, 64);
        addColorButton("grey", "Grise", colorX + 276, y + 78, 64);

        addRenderableWidget(Button.builder(Component.literal("Creer carte"), button -> sendCard())
                .bounds(centerX() - 80, y + 110, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> switchPage(Page.ACCOUNT))
                .bounds(centerX() + 4, y + 110, 76, 20)
                .build());
    }

    private void addColorButton(String color, String label, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.literal((selectedCardColor.equals(color) ? "[x] " : "[ ] ") + label), button -> {
            cardNameDraft = cardNameField.getValue();
            pinDraft = pinField.getValue();
            selectedCardColor = color;
            switchPage(Page.CARD);
        }).bounds(x, y, width, 20).build());
    }

    private void switchPage(Page page) {
        this.page = page;
        clearWidgets();
        init();
    }

    private void sendCreate() {
        PacketDistributor.sendToServer(new BankActionPayload(
                "create",
                "",
                "",
                accountNameField.getValue(),
                0,
                ""
        ));
    }

    private void sendMoney(String action) {
        PacketDistributor.sendToServer(new BankActionPayload(
                action,
                selectedAccountId,
                targetField == null ? "" : targetField.getValue(),
                "",
                parseAmount(),
                ""
        ));
    }

    private void sendSaveIban() {
        PacketDistributor.sendToServer(new BankActionPayload(
                "save_iban",
                selectedAccountId,
                ibanCodeField.getValue(),
                ibanNameField.getValue(),
                0,
                ""
        ));
    }

    private void sendCard() {
        if (selectedCardColor.isEmpty()) {
            message = "Choisis une couleur.";
            return;
        }

        PacketDistributor.sendToServer(new BankActionPayload(
                "card",
                selectedAccountId,
                selectedCardColor,
                cardNameField.getValue(),
                0,
                pinField.getValue()
        ));
    }

    private int parseAmount() {
        if (amountField == null || amountField.getValue().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(amountField.getValue());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public void handleSync(BankSyncPayload payload) {
        Page previousPage = page;
        accounts.clear();
        savedIbans.clear();
        selectedAccountId = payload.selectedAccountId();
        carriedCoins = payload.carriedCoins();
        message = payload.message();

        if (!payload.accounts().isEmpty()) {
            for (String line : payload.accounts().split("\n")) {
                String[] parts = line.split("\\|", 3);

                if (parts.length == 3) {
                    accounts.add(new ClientBankAccount(parts[0], parts[1], parseInt(parts[2])));
                }
            }
        }

        if (!payload.savedIbans().isEmpty()) {
            for (String line : payload.savedIbans().split("\n")) {
                String[] parts = line.split("\\|", 2);

                if (parts.length == 2) {
                    savedIbans.add(new ClientSavedIban(parts[0], parts[1]));
                }
            }
        }

        if (previousPage == Page.CREATE && !selectedAccountId.isEmpty()) {
            page = Page.ACCOUNTS;
        } else if (previousPage == Page.IBAN_BOOK) {
            page = Page.IBAN_BOOK;
        } else if (previousPage == Page.DEPOSIT || previousPage == Page.WITHDRAW || previousPage == Page.TRANSFER || previousPage == Page.CARD) {
            page = Page.ACCOUNT;
        }

        if (minecraft != null) {
            clearWidgets();
            init();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderBankBackground(guiGraphics);
        renderPageText(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderPaperIcon(guiGraphics);
    }

    private void renderPaperIcon(GuiGraphics guiGraphics) {
        if (page == Page.TRANSFER) {
            guiGraphics.renderItem(new ItemStack(Items.PAPER), paperButtonX + 3, paperButtonY + 1);
        }
    }

    private void renderBankBackground(GuiGraphics guiGraphics) {
        int scale = Math.max(2, Math.min(width / BG_WIDTH, height / BG_HEIGHT));
        int drawWidth = BG_WIDTH * scale;
        int drawHeight = BG_HEIGHT * scale;
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;

        guiGraphics.blit(BACKGROUND, x, y, drawWidth, drawHeight, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    private void renderPageText(GuiGraphics guiGraphics) {
        int titleX = centerX() - 90;
        int titleY = centerY() - 82;
        ClientBankAccount selected = selectedAccount();

        guiGraphics.drawString(font, title(), titleX, titleY, 0xFFFFFF);

        if (page == Page.ACCOUNTS && accounts.isEmpty()) {
            guiGraphics.drawString(font, "Aucun compte pour le moment.", centerX() - 76, centerY() - 12, 0xFFD36E);
        }

        if (page == Page.CREATE) {
            guiGraphics.drawString(font, "Nom du compte", centerX() - 80, centerY() - 30, 0xFFFFFF);
            guiGraphics.drawString(font, accounts.size() + "/" + MAX_ACCOUNTS + " comptes", centerX() - 80, centerY() + 44, 0xA7D8FF);
        }

        if (selected != null && page != Page.ACCOUNTS && page != Page.HOME && page != Page.CREATE && page != Page.CARD && page != Page.IBAN_BOOK) {
            guiGraphics.drawString(font, selected.name(), centerX() - 80, centerY() - 62, 0xFFFFFF);
            guiGraphics.drawString(font, "IBAN: " + selected.id(), centerX() - 80, centerY() - 50, 0xA7D8FF);
            guiGraphics.drawString(font, "Solde: " + selected.balance() + " crazycoin(s)", centerX() - 80, centerY() - 38, 0xA7D8FF);
        }

        if (selected != null && page == Page.CARD) {
            guiGraphics.drawString(font, selected.name() + " | IBAN: " + selected.id() + " | " + selected.balance() + " crazycoin(s)", centerX() - 100, centerY() - 54, 0xA7D8FF);
        }

        if (page == Page.DEPOSIT) {
            guiGraphics.drawString(font, "Sur toi: " + carriedCoins + " crazycoins", centerX() - 80, centerY() - 18, 0xFFFFFF);
        }

        if (page == Page.TRANSFER) {
            guiGraphics.drawString(font, "IBAN cible", centerX() - 80, centerY() - 22, 0xFFFFFF);
            guiGraphics.drawString(font, "Montant", centerX() - 80, centerY() + 8, 0xFFFFFF);
        }

        if (page == Page.IBAN_BOOK) {
            if (savedIbans.isEmpty()) {
                guiGraphics.drawString(font, "Aucun IBAN sauvegarde.", centerX() - 86, centerY() - 34, 0xFFD36E);
            }

            guiGraphics.drawString(font, "Pseudo", centerX() - 100, centerY() + 20, 0xFFFFFF);
            guiGraphics.drawString(font, "IBAN", centerX() + 4, centerY() + 20, 0xFFFFFF);
        }

        if (page == Page.DEPOSIT || page == Page.WITHDRAW) {
            guiGraphics.drawString(font, "Montant", centerX() - 80, centerY() - 6, 0xFFFFFF);
        }

        if (page == Page.CARD) {
            guiGraphics.drawString(font, "Nom de la carte", centerX() - 100, centerY() - 24, 0xFFFFFF);
            guiGraphics.drawString(font, "Code", centerX() - 100, centerY() + 6, 0xFFFFFF);
            guiGraphics.drawString(font, "Couleur de carte", centerX() - 100, centerY() + 36, 0xFFFFFF);
        }

        if (!message.isEmpty()) {
            int messageY = page == Page.CARD ? centerY() + 102 : centerY() + 76;
            guiGraphics.drawString(font, message, centerX() - 90, messageY, 0xFFD36E);
        }
    }

    private String title() {
        return switch (page) {
            case HOME -> "Banque";
            case CREATE -> "Creer un compte";
            case ACCOUNTS -> "Mes comptes";
            case ACCOUNT -> "Compte";
            case TRANSFER -> "Faire un transfert";
            case IBAN_BOOK -> "Carnet IBAN";
            case DEPOSIT -> "Deposer de l'argent";
            case WITHDRAW -> "Retirer de l'argent";
            case CARD -> "Demander une carte";
        };
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Disable the vanilla blur behind this interface.
    }

    private ClientBankAccount selectedAccount() {
        for (ClientBankAccount account : accounts) {
            if (account.id().equals(selectedAccountId)) {
                return account;
            }
        }

        return null;
    }

    private int centerX() {
        return width / 2;
    }

    private int centerY() {
        return height / 2;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Page {
        HOME,
        CREATE,
        ACCOUNTS,
        ACCOUNT,
        TRANSFER,
        IBAN_BOOK,
        DEPOSIT,
        WITHDRAW,
        CARD
    }

    private record ClientBankAccount(String id, String name, int balance) {
    }

    private record ClientSavedIban(String name, String iban) {
    }
}
