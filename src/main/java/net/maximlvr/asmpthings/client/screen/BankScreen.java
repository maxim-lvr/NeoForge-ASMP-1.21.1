package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.network.payload.BankActionPayload;
import net.maximlvr.asmpthings.network.payload.BankSyncPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class BankScreen extends Screen {
    private static final int MAX_ACCOUNTS = 5;
    private static final int BG_WIDTH = 128;
    private static final int BG_HEIGHT = 64;
    private static final int VISIBLE_CITIZENS = 5;
    private static final int CITIZEN_ROW_HEIGHT = 18;
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "textures/screens/bank_background.png");

    private final List<ClientBankAccount> accounts = new ArrayList<>();
    private final List<ClientSavedIban> savedIbans = new ArrayList<>();
    private Page page = Page.HOME;
    private Page accountListPage = Page.ACCOUNTS;
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
    private EditBox memberNameField;
    private EditBox citizenNameField;
    private EditBox citizenIbanField;
    private EditBox citizenSalaryField;
    private EditBox citizenSearchField;
    private String selectedCardColor = "";
    private String cardNameDraft = "";
    private String pinDraft = "";
    private String transferTargetDraft = "";
    private String transferAmountDraft = "";
    private String citizenOriginalName = "";
    private String citizenPlayerId = "";
    private String citizenNameDraft = "";
    private String citizenIbanDraft = "";
    private String citizenSalaryDraft = "";
    private String citizenSearchDraft = "";
    private int paperButtonX;
    private int paperButtonY;
    private int citizenScrollOffset;

    public BankScreen() {
        super(Component.literal("Banque"));
    }

    @Override
    protected void init() {
        switch (page) {
            case HOME -> initHome();
            case CREATE -> initCreate();
            case ACCOUNTS -> initAccounts();
            case COMMON_ACCOUNTS -> initCommonAccounts();
            case ACCOUNT -> initAccount();
            case TRANSFER -> initTransfer();
            case IBAN_BOOK -> initIbanBook();
            case MEMBERS -> initMembers();
            case CITIZENS -> initCitizens();
            case CITIZEN_FORM -> initCitizenForm();
            case DEPOSIT -> initMoneyAction("deposit");
            case WITHDRAW -> initMoneyAction("withdraw");
            case CARD -> initCard();
        }
    }

    private void initHome() {
        int x = centerX() - 70;
        int y = hasCommonAccounts() ? centerY() - 36 : centerY() - 22;

        addRenderableWidget(Button.builder(Component.literal("Voir mes comptes"), button -> switchPage(Page.ACCOUNTS))
                .bounds(x, y, 140, 20)
                .build());

        int createY = y + 28;

        if (hasCommonAccounts()) {
            addRenderableWidget(Button.builder(Component.literal("Voir mes comptes communs"), button -> switchPage(Page.COMMON_ACCOUNTS))
                    .bounds(x, y + 28, 140, 20)
                    .build());
            createY = y + 56;
        }

        Button createButton = Button.builder(Component.literal("Creer un compte"), button -> switchPage(Page.CREATE))
                .bounds(x, createY, 140, 20)
                .build();
        createButton.active = ownedAccountCount() < MAX_ACCOUNTS;
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
        createButton.active = ownedAccountCount() < MAX_ACCOUNTS;
        addRenderableWidget(createButton);

        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> switchPage(Page.HOME))
                .bounds(x + 84, y + 48, 76, 20)
                .build());
    }

    private void initAccounts() {
        int x = centerX() - 90;
        int y = centerY() - 54;
        int buttonIndex = 0;

        for (ClientBankAccount account : accounts) {
            if (!account.owner()) {
                continue;
            }

            int buttonY = y + buttonIndex * 24;
            addRenderableWidget(Button.builder(accountListLabel(account), button -> {
                selectedAccountId = account.id();
                accountListPage = Page.ACCOUNTS;
                switchPage(Page.ACCOUNT);
            }).bounds(x, buttonY, 180, 20).build());
            buttonIndex++;
        }

        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.HOME))
                .bounds(x, y + 126, 84, 20)
                .build());
    }

    private void initCommonAccounts() {
        int x = centerX() - 90;
        int y = centerY() - 54;
        int buttonIndex = 0;

        for (ClientBankAccount account : accounts) {
            if (account.owner()) {
                continue;
            }

            int buttonY = y + buttonIndex * 24;
            addRenderableWidget(Button.builder(accountListLabel(account), button -> {
                selectedAccountId = account.id();
                accountListPage = Page.COMMON_ACCOUNTS;
                switchPage(Page.ACCOUNT);
            }).bounds(x, buttonY, 180, 20).build());
            buttonIndex++;
        }

        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.HOME))
                .bounds(x, y + 126, 84, 20)
                .build());
    }

    private void initAccount() {
        ClientBankAccount account = selectedAccount();
        int buttonWidth = 110;
        int gap = 8;
        int x = centerX() - buttonWidth - gap / 2;
        int y = centerY() - 12;

        addRenderableWidget(Button.builder(Component.literal("Faire un transfert"), button -> switchPage(Page.TRANSFER))
                .bounds(x, y, buttonWidth, 20)
                .build());

        if (account != null && account.adminAccount()) {
            addRenderableWidget(Button.builder(Component.literal("Citoyens"), button -> switchPage(Page.CITIZENS))
                    .bounds(x + buttonWidth + gap, y, buttonWidth, 20)
                    .build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Deposer de l'argent"), button -> switchPage(Page.DEPOSIT))
                    .bounds(x + buttonWidth + gap, y, buttonWidth, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Retirer de l'argent"), button -> switchPage(Page.WITHDRAW))
                    .bounds(x, y + 26, buttonWidth, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Demander une carte"), button -> switchPage(Page.CARD))
                    .bounds(x + buttonWidth + gap, y + 26, buttonWidth, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Membres"), button -> switchPage(Page.MEMBERS))
                    .bounds(x, y + 54, buttonWidth, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(accountListPage))
                .bounds(x + buttonWidth + gap, y + 54, buttonWidth, 20)
                .build());
    }

    private void initCitizens() {
        int x = centerX() - 112;
        int y = centerY() - 56;
        int citizenCount = filteredCitizens().size();
        citizenScrollOffset = Math.max(0, Math.min(citizenScrollOffset, Math.max(0, citizenCount - VISIBLE_CITIZENS)));

        citizenSearchField = new EditBox(font, x, y, 180, 18, Component.literal("Rechercher"));
        citizenSearchField.setMaxLength(32);
        citizenSearchField.setValue(citizenSearchDraft);
        citizenSearchField.setResponder(value -> citizenSearchDraft = value);
        addRenderableWidget(citizenSearchField);

        for (int i = 0; i < VISIBLE_CITIZENS; i++) {
            int rowIndex = i;
            int rowY = citizenListY() + i * CITIZEN_ROW_HEIGHT - 4;
            Button editButton = Button.builder(Component.literal("Edit"), button -> openCitizenForm(rowIndex))
                    .bounds(x + 184, rowY, 48, 16)
                    .build();
            editButton.visible = false;
            editButton.active = false;
            addRenderableWidget(editButton);
        }

        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.ACCOUNT))
                .bounds(x, y + 122, 76, 20)
                .build());
    }

    private void openCitizenForm(int rowIndex) {
        List<ClientBankCitizen> citizens = filteredCitizens();
        int index = citizenScrollOffset + rowIndex;

        if (index < 0 || index >= citizens.size()) {
            return;
        }

        ClientBankCitizen citizen = citizens.get(index);
        citizenOriginalName = citizen.name();
        citizenPlayerId = citizen.id();
        citizenNameDraft = citizen.name();
        citizenIbanDraft = citizen.iban();
        citizenSalaryDraft = Integer.toString(citizen.salary());
        switchPage(Page.CITIZEN_FORM);
    }

    private void initCitizenForm() {
        int x = centerX() - 80;
        int y = centerY() - 40;

        citizenNameField = new EditBox(font, x, y + 18, 160, 18, Component.literal("Pseudo"));
        citizenNameField.setMaxLength(24);
        citizenNameField.setValue(citizenNameDraft);
        citizenNameField.setEditable(false);
        addRenderableWidget(citizenNameField);

        citizenIbanField = new EditBox(font, x, y + 48, 160, 18, Component.literal("IBAN"));
        citizenIbanField.setMaxLength(4);
        citizenIbanField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,4}"));
        citizenIbanField.setValue(citizenIbanDraft);
        addRenderableWidget(citizenIbanField);

        citizenSalaryField = new EditBox(font, x, y + 78, 160, 18, Component.literal("Salaire"));
        citizenSalaryField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        citizenSalaryField.setValue(citizenSalaryDraft);
        addRenderableWidget(citizenSalaryField);

        addRenderableWidget(Button.builder(Component.literal("Valider"), button -> sendCitizen())
                .bounds(x, y + 108, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Annuler"), button -> switchPage(Page.CITIZENS))
                .bounds(x + 84, y + 108, 76, 20)
                .build());
    }

    private void initMembers() {
        ClientBankAccount account = selectedAccount();
        int x = centerX() - 100;
        int y = centerY() - 20;

        if (account == null) {
            addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.ACCOUNT))
                    .bounds(x, y + 98, 84, 20)
                    .build());
            return;
        }

        int visibleMembers = account.owner() ? 3 : 4;

        for (int i = 0; i < Math.min(account.members().size(), visibleMembers); i++) {
            ClientBankMember member = account.members().get(i);
            Button button = Button.builder(Component.literal(account.owner() ? "Retirer " + member.name() : member.name()), ignored -> sendRemoveMember(member.id()))
                    .bounds(x, y + i * 22, 200, 18)
                    .build();
            button.active = account.owner();
            addRenderableWidget(button);
        }

        if (account.owner()) {
            memberNameField = new EditBox(font, x, y + 78, 200, 18, Component.literal("Pseudo"));
            memberNameField.setMaxLength(32);
            addRenderableWidget(memberNameField);

            addRenderableWidget(Button.builder(Component.literal("Ajouter"), button -> sendAddMember())
                    .bounds(x, y + 106, 76, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.ACCOUNT))
                    .bounds(x + 84, y + 106, 76, 20)
                    .build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Retour"), button -> switchPage(Page.ACCOUNT))
                    .bounds(x, y + 120, 76, 20)
                    .build());
        }
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
        if (page == Page.CITIZENS && this.page != Page.CITIZEN_FORM) {
            citizenScrollOffset = 0;
        }

        this.page = page;
        clearWidgets();
        init();
    }

    private Component accountListLabel(ClientBankAccount account) {
        MutableComponent label = Component.literal(account.name());

        if (account.adminAccount()) {
            return label.append(Component.literal(" - "))
                    .append(Component.literal("Banque").withStyle(ChatFormatting.GREEN));
        }

        if (account.common()) {
            return label.append(Component.literal(" - "))
                    .append(Component.literal("commun").withStyle(ChatFormatting.BLUE));
        }

        return label;
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
        PacketDistributor.sendToServer(new BankActionPayload(
                "card",
                selectedAccountId,
                selectedCardColor,
                cardNameField.getValue(),
                0,
                pinField.getValue()
        ));
    }

    private void sendAddMember() {
        PacketDistributor.sendToServer(new BankActionPayload(
                "add_member",
                selectedAccountId,
                memberNameField.getValue(),
                "",
                0,
                ""
        ));
    }

    private void sendRemoveMember(String memberId) {
        PacketDistributor.sendToServer(new BankActionPayload(
                "remove_member",
                selectedAccountId,
                memberId,
                "",
                0,
                ""
        ));
    }

    private void sendCitizen() {
        PacketDistributor.sendToServer(new BankActionPayload(
                "save_citizen",
                selectedAccountId,
                citizenIbanField.getValue(),
                citizenPlayerId,
                parseCitizenSalary(),
                ""
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

    private int parseCitizenSalary() {
        if (citizenSalaryField == null || citizenSalaryField.getValue().isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(citizenSalaryField.getValue());
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
        message = "";

        if (!payload.accounts().isEmpty()) {
            for (String line : payload.accounts().split("\n")) {
                String[] parts = line.split("\\|", 8);

                if (parts.length >= 3) {
                    boolean owner = parts.length > 3 && parts[3].equals("1");
                    boolean common = parts.length > 4 && parts[4].equals("1");
                    List<ClientBankMember> members = parts.length > 5 ? parseMembers(parts[5]) : List.of();
                    boolean adminAccount = parts.length > 6 && parts[6].equals("1");
                    List<ClientBankCitizen> citizens = parts.length > 7 ? parseCitizens(parts[7]) : List.of();
                    accounts.add(new ClientBankAccount(parts[0], parts[1], parseInt(parts[2]), owner, common || !members.isEmpty(), members, adminAccount, citizens));
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
        } else if (previousPage == Page.IBAN_BOOK || previousPage == Page.MEMBERS || previousPage == Page.CITIZENS) {
            page = previousPage;
        } else if (previousPage == Page.CITIZEN_FORM) {
            page = Page.CITIZENS;
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
        updateCitizenEditButtons(mouseX, mouseY);
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

        if (page == Page.ACCOUNTS && ownedAccountCount() == 0) {
            guiGraphics.drawString(font, "Aucun compte pour le moment.", centerX() - 76, centerY() - 12, 0xFFD36E);
        }

        if (page == Page.COMMON_ACCOUNTS && !hasCommonAccounts()) {
            guiGraphics.drawString(font, "Aucun compte commun.", centerX() - 62, centerY() - 12, 0xFFD36E);
        }

        if (page == Page.CREATE) {
            guiGraphics.drawString(font, "Nom du compte", centerX() - 80, centerY() - 30, 0xFFFFFF);
            guiGraphics.drawString(font, ownedAccountCount() + "/" + MAX_ACCOUNTS + " comptes", centerX() - 80, centerY() + 44, 0xA7D8FF);
        }

        if (selected != null && page != Page.ACCOUNTS && page != Page.COMMON_ACCOUNTS && page != Page.HOME && page != Page.CREATE && page != Page.CARD && page != Page.IBAN_BOOK && page != Page.CITIZENS && page != Page.CITIZEN_FORM) {
            guiGraphics.drawString(font, selected.name(), centerX() - 80, centerY() - 62, 0xFFFFFF);
            if (selected.adminAccount()) {
                guiGraphics.drawString(font, "Compte admin", centerX() + 18, centerY() - 62, 0x7CFFB2);
            } else if (selected.common()) {
                guiGraphics.drawString(font, "Compte commun", centerX() + 18, centerY() - 62, 0x7CFFB2);
            }
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

        if (page == Page.MEMBERS) {
            if (selected != null && selected.members().isEmpty()) {
                guiGraphics.drawString(font, "Aucun membre ajoute.", centerX() - 80, centerY() - 6, 0xFFD36E);
            }

            if (selected != null && selected.owner()) {
                guiGraphics.drawString(font, "Pseudo du joueur", centerX() - 100, centerY() + 46, 0xFFFFFF);
            } else if (selected != null) {
                guiGraphics.drawString(font, "Seul le createur peut modifier.", centerX() - 96, centerY() + 42, 0xFFD36E);
            }
        }

        if (page == Page.CITIZENS) {
            ClientBankAccount account = selectedAccount();

            if (account != null && account.citizens().isEmpty()) {
                guiGraphics.drawString(font, "Aucun citoyen.", centerX() - 58, centerY() - 4, 0xFFD36E);
            } else if (account != null) {
                List<ClientBankCitizen> citizens = filteredCitizens();
                clampCitizenScroll(citizens.size());
                int x = centerX() - 112;
                int y = citizenListY();
                int visible = Math.min(citizens.size() - citizenScrollOffset, VISIBLE_CITIZENS);

                for (int i = 0; i < visible; i++) {
                    ClientBankCitizen citizen = citizens.get(citizenScrollOffset + i);
                    guiGraphics.drawString(font, citizen.name() + " - " + citizen.iban() + " - " + citizen.salary(), x, y + i * CITIZEN_ROW_HEIGHT, 0xFFFFFF);
                }

                if (citizens.size() > VISIBLE_CITIZENS) {
                    guiGraphics.drawString(font, (citizenScrollOffset + 1) + "-" + (citizenScrollOffset + visible) + "/" + citizens.size(), centerX() + 62, centerY() + 58, 0xA7D8FF);
                }
            }
        }

        if (page == Page.CITIZEN_FORM) {
            guiGraphics.drawString(font, "Pseudo", centerX() - 80, centerY() - 34, 0xFFFFFF);
            guiGraphics.drawString(font, "IBAN", centerX() - 80, centerY() - 4, 0xFFFFFF);
            guiGraphics.drawString(font, "Salaire", centerX() - 80, centerY() + 26, 0xFFFFFF);
        }

        if (page == Page.DEPOSIT || page == Page.WITHDRAW) {
            guiGraphics.drawString(font, "Montant", centerX() - 80, centerY() - 6, 0xFFFFFF);
        }

        if (page == Page.CARD) {
            guiGraphics.drawString(font, "Nom de la carte", centerX() - 100, centerY() - 24, 0xFFFFFF);
            guiGraphics.drawString(font, "Code", centerX() - 100, centerY() + 6, 0xFFFFFF);
            guiGraphics.drawString(font, "Couleur de carte", centerX() - 100, centerY() + 36, 0xFFFFFF);
        }

    }

    private String title() {
        return switch (page) {
            case HOME -> "Banque";
            case CREATE -> "Creer un compte";
            case ACCOUNTS -> "Mes comptes";
            case COMMON_ACCOUNTS -> "Mes comptes communs";
            case ACCOUNT -> "Compte";
            case TRANSFER -> "Faire un transfert";
            case IBAN_BOOK -> "Carnet IBAN";
            case MEMBERS -> "Membres du compte";
            case CITIZENS -> "Citoyens";
            case CITIZEN_FORM -> citizenOriginalName.isEmpty() ? "Ajouter un citoyen" : "Editer un citoyen";
            case DEPOSIT -> "Deposer de l'argent";
            case WITHDRAW -> "Retirer de l'argent";
            case CARD -> "Demander une carte";
        };
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Disable the vanilla blur behind this interface.
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == Page.CITIZENS) {
            ClientBankAccount account = selectedAccount();
            List<ClientBankCitizen> citizens = filteredCitizens();

            if (account != null && citizens.size() > VISIBLE_CITIZENS) {
                int maxOffset = citizens.size() - VISIBLE_CITIZENS;
                int nextOffset = citizenScrollOffset + (scrollY < 0 ? 1 : -1);
                citizenScrollOffset = Math.max(0, Math.min(maxOffset, nextOffset));
                clearWidgets();
                init();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    private int citizenListY() {
        return centerY() - 26;
    }

    private List<ClientBankCitizen> filteredCitizens() {
        ClientBankAccount account = selectedAccount();
        List<ClientBankCitizen> filtered = new ArrayList<>();

        if (account == null) {
            return filtered;
        }

        String query = citizenSearchField == null ? citizenSearchDraft : citizenSearchField.getValue();
        query = query == null ? "" : query.trim().toLowerCase();

        for (ClientBankCitizen citizen : account.citizens()) {
            if (query.isEmpty() || citizen.name().toLowerCase().contains(query) || citizen.iban().contains(query)) {
                filtered.add(citizen);
            }
        }

        return filtered;
    }

    private void clampCitizenScroll(int count) {
        citizenScrollOffset = Math.max(0, Math.min(citizenScrollOffset, Math.max(0, count - VISIBLE_CITIZENS)));
    }

    private void updateCitizenEditButtons(int mouseX, int mouseY) {
        if (page != Page.CITIZENS) {
            return;
        }

        int rowX = centerX() - 112;
        int rowY = citizenListY() - 5;
        int editIndex = 0;
        int visible = Math.min(filteredCitizens().size() - citizenScrollOffset, VISIBLE_CITIZENS);

        for (var widget : renderables) {
            if (!(widget instanceof Button button) || !button.getMessage().getString().equals("Edit")) {
                continue;
            }

            int currentRowY = rowY + editIndex * CITIZEN_ROW_HEIGHT;
            boolean hoveredRow = editIndex < visible && mouseX >= rowX && mouseX <= rowX + 232 && mouseY >= currentRowY && mouseY < currentRowY + CITIZEN_ROW_HEIGHT;
            button.visible = hoveredRow;
            button.active = hoveredRow;
            editIndex++;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private int ownedAccountCount() {
        int count = 0;

        for (ClientBankAccount account : accounts) {
            if (account.owner()) {
                count++;
            }
        }

        return count;
    }

    private boolean hasCommonAccounts() {
        for (ClientBankAccount account : accounts) {
            if (!account.owner()) {
                return true;
            }
        }

        return false;
    }

    private List<ClientBankMember> parseMembers(String value) {
        List<ClientBankMember> members = new ArrayList<>();

        if (value.isEmpty()) {
            return members;
        }

        for (String entry : value.split(";")) {
            String[] parts = entry.split(",", 2);

            if (parts.length == 2) {
                members.add(new ClientBankMember(parts[0], parts[1]));
            }
        }

        return members;
    }

    private List<ClientBankCitizen> parseCitizens(String value) {
        List<ClientBankCitizen> citizens = new ArrayList<>();

        if (value.isEmpty()) {
            return citizens;
        }

        for (String entry : value.split(";")) {
            String[] parts = entry.split(",", 4);

            if (parts.length == 4) {
                citizens.add(new ClientBankCitizen(parts[0], parts[1], parts[2], parseInt(parts[3])));
            }
        }

        return citizens;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Page {
        HOME,
        CREATE,
        ACCOUNTS,
        COMMON_ACCOUNTS,
        ACCOUNT,
        TRANSFER,
        IBAN_BOOK,
        MEMBERS,
        CITIZENS,
        CITIZEN_FORM,
        DEPOSIT,
        WITHDRAW,
        CARD
    }

    private record ClientBankAccount(String id, String name, int balance, boolean owner, boolean common, List<ClientBankMember> members, boolean adminAccount, List<ClientBankCitizen> citizens) {
    }

    private record ClientSavedIban(String name, String iban) {
    }

    private record ClientBankMember(String id, String name) {
    }

    private record ClientBankCitizen(String id, String name, String iban, int salary) {
    }
}
