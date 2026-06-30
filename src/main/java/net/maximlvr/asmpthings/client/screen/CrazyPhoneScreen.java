package net.maximlvr.asmpthings.client.screen;

import de.maxhenkel.camera.gui.ImageScreen;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhoneContactByNumberPayload;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneContactResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneMessageResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhonePhotoResultPayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhoneMessagePayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.SetCrazyPhoneLockedPayload;
import net.maximlvr.asmpthings.network.payload.SetupCrazyPhonePayload;
import net.maximlvr.asmpthings.network.payload.TakeCrazyPhonePhotoPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class CrazyPhoneScreen extends Screen {
    private static final ResourceLocation PHONE_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/phone-background.png");
    private static final ResourceLocation LOCK_BUTTON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-lock.png");
    private static final ResourceLocation HOME_BUTTON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-home.png");
    private static final ResourceLocation BACK_BUTTON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-back.png");
    private static final ResourceLocation CONTACTS_ICON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-contacts-icon.png");
    private static final ResourceLocation ALBUM_ICON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-album-icon.png");
    private static final ResourceLocation SMS_ICON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-sms-icon.png");
    private static final ResourceLocation ADD_ICON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-photo-icon.png");
    private static final ResourceLocation SEND_MESSAGE_BUTTON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-send-message.png");
    private static final ResourceLocation CONTACT_ROW_BACKGROUND_1 =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/phone-background-contact-1.png");
    private static final ResourceLocation CONTACT_ROW_BACKGROUND_2 =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/phone-background-contact-2.png");

    private static final int PHONE_TEXTURE_WIDTH = 122;
    private static final int PHONE_TEXTURE_HEIGHT = 195;
    private static final int PHONE_SCALE_NUMERATOR = 3;
    private static final int PHONE_SCALE_DENOMINATOR = 2;
    private static final int PHONE_WIDTH = PHONE_TEXTURE_WIDTH * PHONE_SCALE_NUMERATOR / PHONE_SCALE_DENOMINATOR;
    private static final int PHONE_HEIGHT = PHONE_TEXTURE_HEIGHT * PHONE_SCALE_NUMERATOR / PHONE_SCALE_DENOMINATOR;
    private static final int NAV_BUTTON_WIDTH = scale(27);
    private static final int NAV_BUTTON_HEIGHT = scale(12);
    private static final int APP_BUTTON_WIDTH = scale(24);
    private static final int APP_BUTTON_HEIGHT = scale(28);
    private static final int CONTACT_ROW_HEIGHT = scale(24);
    private static final int PHOTO_ROW_HEIGHT = scale(31);
    private static final int CAMERA_GRID_COLUMNS = 5;
    private static final int CAMERA_GRID_CELL = scale(20);
    private static final int LIST_TOP = scale(49);
    private static final int LIST_BOTTOM = scale(164);
    private static final int TEXT_PRIMARY = 0xFFFFFF;
    private static final int TEXT_SECONDARY = 0xD8ECFF;
    private static final int TEXT_ERROR = 0xFF7777;
    private static final int TEXT_SUCCESS = 0x96FF9B;

    private final ItemStack stack;
    private final boolean mainHand;
    private Page page = Page.HOME;

    private EditBox nameField;
    private EditBox numberField;
    private EditBox passwordField;
    private EditBox contactNameField;
    private EditBox contactNumberField;
    private EditBox photoTitleField;
    private EditBox photoTextureField;
    private EditBox unlockPasswordField;
    private EditBox messageField;
    private String contactStatus = "";
    private int contactStatusColor = 0xFFBFD7FF;
    private String unlockStatus = "";
    private String photoStatus = "";
    private int photoStatusColor = TEXT_ERROR;
    private String messageStatus = "";
    private int messageStatusColor = TEXT_ERROR;
    private ContactEntry selectedContact;
    private Page conversationBackPage = Page.SMS;
    private PhotoEntry selectedPhoto;
    private int selectedCameraAlbumIndex = 0;
    private int contactScrollOffset = 0;
    private int albumScrollOffset = 0;
    private int smsScrollOffset = 0;
    private int messageScrollOffset = 0;

    public CrazyPhoneScreen(ItemStack stack, boolean mainHand) {
        super(Component.literal("Crazy Phone"));
        this.stack = stack;
        this.mainHand = mainHand;
        if (isSetup() && getTag().getBoolean("locked")) {
            this.page = Page.LOCKED;
        }
    }

    @Override
    protected void init() {
        if (!isSetup()) {
            initSetup();
            return;
        }

        if (page == Page.LOCKED) {
            initLocked();
            return;
        }

        switch (page) {
            case CONTACTS -> initContacts();
            case SMS -> initSms();
            case ADD_CONTACT -> initAddContact();
            case CONVERSATION -> initConversation();
            case ALBUMS -> initAlbums();
            case CAMERA_ALBUM -> initCameraAlbum();
            case ADD_PHOTO -> initAddPhoto();
            case SEND_PHOTO -> initSendPhoto();
            default -> initHome();
        }
    }

    private void initSetup() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int inputX = phoneX + scale(19);
        int inputWidth = scale(84);

        nameField = new EditBox(this.font, inputX, phoneY + scale(58), inputWidth, scale(16), Component.literal("Nom"));
        nameField.setMaxLength(24);
        nameField.setTextColor(TEXT_PRIMARY);
        nameField.setTextColorUneditable(TEXT_PRIMARY);
        nameField.setValue(getTag().getString("name"));
        addRenderableWidget(nameField);

        numberField = new EditBox(this.font, inputX, phoneY + scale(86), inputWidth, scale(16), Component.literal("Numero"));
        numberField.setMaxLength(12);
        numberField.setTextColor(TEXT_PRIMARY);
        numberField.setTextColorUneditable(TEXT_PRIMARY);
        numberField.setValue(getTag().getString("number"));
        addRenderableWidget(numberField);

        passwordField = new EditBox(this.font, inputX, phoneY + scale(114), inputWidth, scale(16), Component.literal("Mot de passe"));
        passwordField.setMaxLength(16);
        passwordField.setTextColor(TEXT_PRIMARY);
        passwordField.setTextColorUneditable(TEXT_PRIMARY);
        passwordField.setValue(getTag().getString("password"));
        addRenderableWidget(passwordField);

        Button validateButton = Button.builder(Component.literal("Valider"), button -> setupPhone())
                .bounds(inputX, phoneY + scale(143), inputWidth, scale(18))
                .build();
        validateButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(validateButton);

        addSystemButtons(phoneX, phoneY);
    }

    private void initLocked() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int inputX = phoneX + scale(18);
        int inputWidth = scale(86);

        unlockPasswordField = new EditBox(this.font, inputX, phoneY + scale(85), inputWidth, scale(16), Component.literal("Mot de passe"));
        unlockPasswordField.setMaxLength(16);
        unlockPasswordField.setTextColor(TEXT_PRIMARY);
        unlockPasswordField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(unlockPasswordField);

        Button unlockButton = Button.builder(Component.literal("Deverrouiller"), button -> unlockPhone())
                .bounds(inputX, phoneY + scale(116), inputWidth, scale(18))
                .build();
        unlockButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(unlockButton);
    }

    private void initHome() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;

        addAppGrid(phoneX, phoneY);
        addSystemButtons(phoneX, phoneY);
    }

    private void initContacts() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int addRowY = phoneY + LIST_TOP - contactScrollOffset;

        if (addRowY + scale(21) >= phoneY + LIST_TOP && addRowY <= phoneY + LIST_BOTTOM) {
            addRenderableWidget(new InvisibleButton(
                    phoneX + scale(17), addRowY, scale(88), scale(21),
                    Component.literal("Ajouter un contact"), button -> {
                page = Page.ADD_CONTACT;
                contactStatus = "";
                rebuildWidgets();
            }));
        }

        int rowY = phoneY + LIST_TOP + scale(28) - contactScrollOffset;

        for (ContactEntry contact : getContacts()) {
            if (rowY + CONTACT_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        phoneX + scale(17), rowY, scale(88), CONTACT_ROW_HEIGHT,
                        Component.literal("Discussion " + contact.name()), button -> openConversation(contact)
                ));
            }

            rowY += CONTACT_ROW_HEIGHT;
        }

        addSystemButtons(phoneX, phoneY);
    }

    private void initSms() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int rowY = phoneY + LIST_TOP - smsScrollOffset;

        for (ConversationEntry conversation : getConversations()) {
            if (rowY + CONTACT_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        phoneX + scale(17), rowY, scale(88), CONTACT_ROW_HEIGHT,
                        Component.literal("Discussion " + conversation.displayName()), button -> openConversation(conversation)
                ));
            }

            rowY += CONTACT_ROW_HEIGHT;
        }

        addSystemButtons(phoneX, phoneY);
    }


    private void initAddContact() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;

        addSystemButtons(phoneX, phoneY);

        int inputX = phoneX + scale(18);
        int inputWidth = scale(86);

        contactNameField = new EditBox(this.font, inputX, phoneY + scale(66), inputWidth, scale(16), Component.literal("Nom"));
        contactNameField.setMaxLength(24);
        contactNameField.setTextColor(TEXT_PRIMARY);
        contactNameField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(contactNameField);

        contactNumberField = new EditBox(this.font, inputX, phoneY + scale(101), inputWidth, scale(16), Component.literal("Numero"));
        contactNumberField.setMaxLength(12);
        contactNumberField.setTextColor(TEXT_PRIMARY);
        contactNumberField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(contactNumberField);

        Button addButton = Button.builder(Component.literal("Ajouter"), button -> requestAddContactByNumber())
                .bounds(inputX, phoneY + scale(132), inputWidth, scale(18))
                .build();
        addButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(addButton);
    }

    private void initConversation() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int inputX = phoneX + scale(17);
        int inputY = phoneY + scale(158);

        messageField = new EditBox(this.font, inputX, inputY, scale(68), scale(16), Component.literal("Message"));
        messageField.setMaxLength(160);
        messageField.setTextColor(TEXT_PRIMARY);
        messageField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(messageField);

        addRenderableWidget(new TextureButton(
                phoneX + scale(88), inputY + scale(1), scale(16), scale(16),
                SEND_MESSAGE_BUTTON, 14, 14,
                Component.literal("Envoyer"), button -> sendMessage()
        ));

        addSystemButtons(phoneX, phoneY);
    }

    private void initAlbums() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int addRowY = phoneY + LIST_TOP - albumScrollOffset;

        if (addRowY + scale(21) >= phoneY + LIST_TOP && addRowY <= phoneY + LIST_BOTTOM) {
            addRenderableWidget(new InvisibleButton(
                    phoneX + scale(17), addRowY, scale(88), scale(21),
                    Component.literal("Ajouter une photo"), button -> {
                page = Page.ADD_PHOTO;
                photoStatus = "";
                rebuildWidgets();
            }));
        }

        int rowY = phoneY + LIST_TOP + scale(28) - albumScrollOffset;

        List<ItemStack> cameraAlbums = getCameraAlbums();

        for (int i = 0; i < cameraAlbums.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int albumX = phoneX + scale(18) + column * CAMERA_GRID_CELL;
            int albumY = rowY + row * CAMERA_GRID_CELL;
            int albumIndex = i;

            if (albumY + CAMERA_GRID_CELL >= phoneY + LIST_TOP && albumY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        albumX, albumY, scale(18), scale(18),
                        Component.literal("Photos camera"), button -> {
                    selectedCameraAlbumIndex = albumIndex;
                    page = Page.CAMERA_ALBUM;
                    albumScrollOffset = 0;
                    rebuildWidgets();
                }
                ));
            }
        }

        rowY += getCameraAlbumGridHeight(cameraAlbums.size());

        for (PhotoEntry photo : getPhotos()) {
            if (rowY + PHOTO_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        phoneX + scale(17), rowY, scale(88), PHOTO_ROW_HEIGHT,
                        Component.literal("Envoyer"), button -> {
                    selectedPhoto = photo;
                    contactScrollOffset = 0;
                    photoStatus = "";
                    page = Page.SEND_PHOTO;
                    rebuildWidgets();
                }));
            }

            rowY += PHOTO_ROW_HEIGHT;
        }

        addSystemButtons(phoneX, phoneY);
    }

    private void initCameraAlbum() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int gridTop = phoneY + LIST_TOP - albumScrollOffset;
        List<ItemStack> images = getSelectedCameraAlbumImages();

        for (int i = 0; i < images.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int imageX = phoneX + scale(18) + column * CAMERA_GRID_CELL;
            int imageY = gridTop + row * CAMERA_GRID_CELL;
            ItemStack image = images.get(i);

            if (imageY + CAMERA_GRID_CELL >= phoneY + LIST_TOP && imageY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        imageX, imageY, scale(18), scale(18),
                        Component.literal("Voir photo"), button -> openCameraImage(image)
                ));
            }
        }

        addSystemButtons(phoneX, phoneY);
    }

    private void initAddPhoto() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int inputX = phoneX + scale(18);
        int inputWidth = scale(86);

        photoTitleField = new EditBox(this.font, inputX, phoneY + scale(66), inputWidth, scale(16), Component.literal("Nom"));
        photoTitleField.setMaxLength(32);
        photoTitleField.setTextColor(TEXT_PRIMARY);
        photoTitleField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(photoTitleField);

        photoTextureField = new EditBox(this.font, inputX, phoneY + scale(101), inputWidth, scale(16), Component.literal("Texture"));
        photoTextureField.setMaxLength(128);
        photoTextureField.setTextColor(TEXT_PRIMARY);
        photoTextureField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(photoTextureField);

        Button addButton = Button.builder(Component.literal("Ajouter"), button -> addCustomPhoto())
                .bounds(inputX, phoneY + scale(132), inputWidth, scale(18))
                .build();
        addButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(addButton);

        addSystemButtons(phoneX, phoneY);
    }

    private void initSendPhoto() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int rowY = phoneY + LIST_TOP - contactScrollOffset;

        for (ContactEntry contact : getContacts()) {
            if (rowY + CONTACT_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        phoneX + scale(17), rowY, scale(88), CONTACT_ROW_HEIGHT,
                        Component.literal("Envoyer a " + contact.name()), button -> sendSelectedPhoto(contact.number())
                ));
            }

            rowY += CONTACT_ROW_HEIGHT;
        }

        addSystemButtons(phoneX, phoneY);
    }

    private void addAppGrid(int phoneX, int phoneY) {
        int startX = phoneX + scale(14);
        int startY = phoneY + scale(43);
        int colGap = scale(11);
        int rowGap = scale(3);
        int firstRowY = startY + 0 * (APP_BUTTON_HEIGHT + rowGap);

        addAppButton(startX, firstRowY, ADD_ICON, 14, 15, Component.literal("Photo"), button -> {
            PacketDistributor.sendToServer(new TakeCrazyPhonePhotoPayload(mainHand));
            onClose();
        });

        addAppButton(startX + APP_BUTTON_WIDTH + colGap, firstRowY, CONTACTS_ICON, 53, 66, Component.literal("Contacts"), button -> {
            page = Page.CONTACTS;
            contactScrollOffset = 0;
            rebuildWidgets();
        });

        addAppButton(startX + (APP_BUTTON_WIDTH + colGap) * 2, firstRowY, SMS_ICON, 46, 62, Component.literal("SMS"), button -> {
            page = Page.SMS;
            smsScrollOffset = 0;
            rebuildWidgets();
        });

        addAppButton(startX, startY + APP_BUTTON_HEIGHT + rowGap, ALBUM_ICON, 52, 62, Component.literal("Albums"), button -> {
            page = Page.ALBUMS;
            albumScrollOffset = 0;
            rebuildWidgets();
        });

        // The grid reserves room for 3 columns x 4 rows:
        // startY + 3 * (APP_BUTTON_HEIGHT + rowGap) + APP_BUTTON_HEIGHT <= nav row.
    }

    private void addAppButton(int x, int y, ResourceLocation texture, int textureWidth, int textureHeight, Component label, Button.OnPress onPress) {
        addRenderableWidget(new TextureButton(
                x, y, APP_BUTTON_WIDTH, APP_BUTTON_HEIGHT,
                texture, textureWidth, textureHeight,
                label, onPress
        ));
    }

    private void addSystemButtons(int phoneX, int phoneY) {
        int navY = phoneY + PHONE_HEIGHT - scale(14);

        addRenderableWidget(new TextureButton(
                phoneX + scale(13), navY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT,
                BACK_BUTTON, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT,
                Component.literal("Retour"), button -> goBack()
        ));

        addRenderableWidget(new TextureButton(
                phoneX + scale(48), navY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT,
                HOME_BUTTON, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT,
                Component.literal("Accueil"), button -> {
            page = Page.HOME;
            rebuildWidgets();
        }));

        addRenderableWidget(new TextureButton(
                phoneX + scale(83), navY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT,
                LOCK_BUTTON, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT,
                Component.literal("Verrouiller"), button -> lockPhone()
        ));
    }

    private void goBack() {
        if (page == Page.LOCKED) {
            return;
        }

        if (page == Page.ADD_CONTACT) {
            page = Page.CONTACTS;
        } else if (page == Page.CONVERSATION) {
            page = conversationBackPage;
        } else if (page == Page.ADD_PHOTO) {
            page = Page.ALBUMS;
        } else if (page == Page.SEND_PHOTO) {
            page = Page.ALBUMS;
        } else if (page == Page.CAMERA_ALBUM) {
            page = Page.ALBUMS;
        } else {
            page = Page.HOME;
        }

        rebuildWidgets();
    }

    private void openConversation(ContactEntry contact) {
        selectedContact = contact;
        conversationBackPage = Page.CONTACTS;
        messageStatus = "";
        messageScrollOffset = 0;
        page = Page.CONVERSATION;
        rebuildWidgets();
    }

    private void openConversation(ConversationEntry conversation) {
        selectedContact = new ContactEntry("", conversation.displayName(), conversation.number());
        conversationBackPage = Page.SMS;
        messageStatus = "";
        messageScrollOffset = getMaxMessageScroll(conversation.number());
        page = Page.CONVERSATION;
        rebuildWidgets();
    }

    private void setupPhone() {
        String name = nameField.getValue().trim();
        String number = numberField.getValue().trim();
        String password = passwordField.getValue().trim();

        if (name.isEmpty()) {
            name = "Steve";
        }

        if (number.isEmpty()) {
            number = generateNumber();
        }

        if (password.isEmpty()) {
            password = "1234";
        }

        PacketDistributor.sendToServer(new SetupCrazyPhonePayload(mainHand, name, number, password));

        CompoundTag tag = getTag();
        tag.putString("name", name);
        tag.putString("number", number);
        tag.putString("password", password);
        tag.putBoolean("locked", false);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        rebuildWidgets();
    }

    private void lockPhone() {
        CompoundTag tag = getTag();
        tag.putBoolean("locked", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PacketDistributor.sendToServer(new SetCrazyPhoneLockedPayload(mainHand, true));
        onClose();
    }

    private void unlockPhone() {
        String enteredPassword = unlockPasswordField == null ? "" : unlockPasswordField.getValue();
        String password = getTag().getString("password");

        if (!enteredPassword.equals(password)) {
            unlockStatus = "Mot de passe incorrect";
            return;
        }

        CompoundTag tag = getTag();
        tag.putBoolean("locked", false);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PacketDistributor.sendToServer(new SetCrazyPhoneLockedPayload(mainHand, false));
        page = Page.HOME;
        unlockStatus = "";
        rebuildWidgets();
    }

    private String generateNumber() {
        if (minecraft == null || minecraft.player == null) {
            return "3630";
        }

        int value = Math.abs(minecraft.player.getUUID().hashCode() % 9000) + 1000;
        return Integer.toString(value);
    }

    private boolean isSetup() {
        return !getTag().getString("name").isEmpty();
    }

    private List<ContactEntry> getContacts() {
        List<ContactEntry> contacts = new ArrayList<>();
        ListTag contactTags = getTag().getList("contacts", 10);

        for (int i = 0; i < contactTags.size(); i++) {
            CompoundTag contactTag = contactTags.getCompound(i);
            String uuid = contactTag.getString("uuid");
            String name = contactTag.getString("name");
            String number = contactTag.getString("number");

            if (!uuid.isEmpty() && !name.isEmpty()) {
                contacts.add(new ContactEntry(uuid, name, number));
            }
        }

        return contacts;
    }

    private List<PhotoEntry> getPhotos() {
        List<PhotoEntry> photos = new ArrayList<>();
        ListTag photoTags = getTag().getList("photos", 10);

        for (int i = 0; i < photoTags.size(); i++) {
            CompoundTag photoTag = photoTags.getCompound(i);
            String title = photoTag.getString("title");
            String texture = photoTag.getString("texture");
            String type = photoTag.getString("type");

            if (title.isEmpty() || texture.isEmpty()) {
                continue;
            }

            if (type.isEmpty()) {
                type = "custom";
            }

            photos.add(new PhotoEntry(title, texture, type));
        }

        return photos;
    }

    private List<MessageEntry> getMessages(String contactNumber) {
        List<MessageEntry> messages = new ArrayList<>();
        ListTag conversations = getTag().getList("conversations", 10);

        for (int i = 0; i < conversations.size(); i++) {
            CompoundTag conversation = conversations.getCompound(i);

            if (!contactNumber.equals(conversation.getString("number"))) {
                continue;
            }

            ListTag messageTags = conversation.getList("messages", 10);

            for (int j = 0; j < messageTags.size(); j++) {
                CompoundTag messageTag = messageTags.getCompound(j);
                String text = messageTag.getString("text");

                if (!text.isEmpty()) {
                    messages.add(new MessageEntry(text, messageTag.getBoolean("outgoing")));
                }
            }

            break;
        }

        return messages;
    }

    private List<ConversationEntry> getConversations() {
        List<ConversationEntry> conversations = new ArrayList<>();
        ListTag conversationTags = getTag().getList("conversations", 10);

        for (int i = 0; i < conversationTags.size(); i++) {
            CompoundTag conversationTag = conversationTags.getCompound(i);
            String number = conversationTag.getString("number");

            if (number.isEmpty()) {
                continue;
            }

            List<MessageEntry> messages = getMessages(number);

            if (messages.isEmpty()) {
                continue;
            }

            conversations.add(new ConversationEntry(number, getContactDisplayName(number), messages.get(messages.size() - 1).text()));
        }

        return conversations;
    }

    private String getContactDisplayName(String number) {
        for (ContactEntry contact : getContacts()) {
            if (contact.number().equals(number)) {
                return contact.name();
            }
        }

        return number;
    }

    private boolean hasContactNumber(String number) {
        for (ContactEntry contact : getContacts()) {
            if (contact.number().equals(number)) {
                return true;
            }
        }

        return false;
    }

    private void requestAddContactByNumber() {
        String name = contactNameField == null ? "" : contactNameField.getValue().trim();
        String number = contactNumberField == null ? "" : contactNumberField.getValue().trim();

        if (name.isEmpty() || number.isEmpty()) {
            contactStatus = "Numero introuvable";
            contactStatusColor = TEXT_ERROR;
            return;
        }

        PacketDistributor.sendToServer(new AddCrazyPhoneContactByNumberPayload(mainHand, name, number));
    }

    private void addCustomPhoto() {
        String title = photoTitleField == null ? "" : photoTitleField.getValue().trim();
        String texture = photoTextureField == null ? "" : photoTextureField.getValue().trim();

        if (title.isEmpty() || texture.isEmpty()) {
            photoStatus = "Image introuvable";
            photoStatusColor = TEXT_ERROR;
            return;
        }

        String normalizedTexture = normalizePhotoTexture(texture);

        if (ResourceLocation.tryParse(normalizedTexture) == null) {
            photoStatus = "Image introuvable";
            photoStatusColor = TEXT_ERROR;
            return;
        }

        addPhotoLocally(title, normalizedTexture);
        PacketDistributor.sendToServer(new AddCrazyPhonePhotoPayload(mainHand, title, normalizedTexture));
        page = Page.ALBUMS;
        albumScrollOffset = 0;
        rebuildWidgets();
    }

    private String normalizePhotoTexture(String texture) {
        if (texture.contains(":")) {
            return texture;
        }

        String path = texture;

        if (!path.startsWith("textures/")) {
            path = "textures/screens/" + path;
        }

        if (!path.endsWith(".png")) {
            path = path + ".png";
        }

        return "asmpthingsmod:" + path;
    }

    public void handleContactResult(CrazyPhoneContactResultPayload payload) {
        contactStatus = payload.message();
        contactStatusColor = payload.success() ? TEXT_SUCCESS : TEXT_ERROR;

        if (!payload.success()) {
            return;
        }

        addContactLocally(payload.uuid(), payload.name(), payload.number());
        page = Page.CONTACTS;
        rebuildWidgets();
    }

    public void handlePhotoResult(CrazyPhonePhotoResultPayload payload) {
        photoStatus = payload.message();
        photoStatusColor = payload.success() ? TEXT_SUCCESS : TEXT_ERROR;

        if (payload.success()) {
            page = Page.ALBUMS;
            selectedPhoto = null;
        }

        rebuildWidgets();
    }

    public void handleMessageResult(CrazyPhoneMessageResultPayload payload) {
        messageStatus = payload.status();
        messageStatusColor = payload.success() ? TEXT_SUCCESS : TEXT_ERROR;

        if (payload.success()) {
            addMessageLocally(payload.contactNumber(), payload.messageText(), true);

            if (messageField != null) {
                messageField.setValue("");
            }

            messageScrollOffset = getMaxMessageScroll();
        }

        rebuildWidgets();
    }

    private void sendMessage() {
        if (selectedContact == null || messageField == null) {
            return;
        }

        String message = messageField.getValue().trim();

        if (message.isEmpty()) {
            messageStatus = "Message vide";
            messageStatusColor = TEXT_ERROR;
            return;
        }

        PacketDistributor.sendToServer(new SendCrazyPhoneMessagePayload(mainHand, selectedContact.number(), message));
    }

    private void sendSelectedPhoto(String contactNumber) {
        if (selectedPhoto == null) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        PacketDistributor.sendToServer(new SendCrazyPhonePhotoPayload(
                mainHand,
                contactNumber,
                selectedPhoto.title(),
                selectedPhoto.texture(),
                "recu"
        ));
    }

    private void addContactLocally(String uuid, String name, String number) {
        if (hasContactNumber(number)) {
            return;
        }

        CompoundTag tag = getTag();
        ListTag contacts = tag.getList("contacts", 10);
        CompoundTag contact = new CompoundTag();
        contact.putString("uuid", uuid);
        contact.putString("name", name);
        contact.putString("number", number);
        contacts.add(contact);
        tag.put("contacts", contacts);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void addPhotoLocally(String title, String texture) {
        CompoundTag tag = getTag();
        ListTag photos = tag.getList("photos", 10);
        CompoundTag photo = new CompoundTag();
        photo.putString("title", title);
        photo.putString("texture", texture);
        photo.putString("type", "custom");
        photos.add(photo);
        tag.put("photos", photos);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void addMessageLocally(String contactNumber, String text, boolean outgoing) {
        CompoundTag tag = getTag();
        ListTag conversations = tag.getList("conversations", 10);
        CompoundTag conversation = null;

        for (int i = 0; i < conversations.size(); i++) {
            CompoundTag current = conversations.getCompound(i);

            if (contactNumber.equals(current.getString("number"))) {
                conversation = current;
                break;
            }
        }

        if (conversation == null) {
            conversation = new CompoundTag();
            conversation.putString("number", contactNumber);
            conversation.put("messages", new ListTag());
            conversations.add(conversation);
        }

        ListTag messages = conversation.getList("messages", 10);
        CompoundTag message = new CompoundTag();
        message.putString("text", text);
        message.putBoolean("outgoing", outgoing);
        messages.add(message);
        conversation.put("messages", messages);
        tag.put("conversations", conversations);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private CompoundTag getTag() {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page != Page.CONTACTS && page != Page.SMS && page != Page.CONVERSATION && page != Page.ALBUMS && page != Page.CAMERA_ALBUM && page != Page.SEND_PHOTO) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;

        if (mouseX < phoneX || mouseX > phoneX + PHONE_WIDTH || mouseY < phoneY + LIST_TOP || mouseY > phoneY + LIST_BOTTOM) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maxScroll = page == Page.ALBUMS || page == Page.CAMERA_ALBUM ? getMaxAlbumScroll() : page == Page.CONVERSATION ? getMaxMessageScroll() : page == Page.SMS ? getMaxSmsScroll() : getMaxContactScroll();

        if (maxScroll <= 0) {
            return true;
        }

        if (page == Page.CONTACTS || page == Page.SEND_PHOTO) {
            contactScrollOffset = clamp(contactScrollOffset - (int) Math.round(scrollY * scale(12)), 0, maxScroll);
        } else if (page == Page.SMS) {
            smsScrollOffset = clamp(smsScrollOffset - (int) Math.round(scrollY * scale(12)), 0, maxScroll);
        } else if (page == Page.CONVERSATION) {
            messageScrollOffset = clamp(messageScrollOffset - (int) Math.round(scrollY * scale(12)), 0, maxScroll);
        } else {
            albumScrollOffset = clamp(albumScrollOffset - (int) Math.round(scrollY * scale(12)), 0, maxScroll);
        }

        rebuildWidgets();
        return true;
    }

    private int getMaxContactScroll() {
        int contentHeight = scale(28) + getContacts().size() * CONTACT_ROW_HEIGHT;
        int viewportHeight = LIST_BOTTOM - LIST_TOP;
        return Math.max(0, contentHeight - viewportHeight);
    }

    private int getMaxSmsScroll() {
        int contentHeight = getConversations().size() * CONTACT_ROW_HEIGHT;
        int viewportHeight = LIST_BOTTOM - LIST_TOP;
        return Math.max(0, contentHeight - viewportHeight);
    }

    private int getMaxAlbumScroll() {
        if (page == Page.CAMERA_ALBUM) {
            int contentHeight = getCameraAlbumGridHeight(getSelectedCameraAlbumImages().size());
            int viewportHeight = LIST_BOTTOM - LIST_TOP;
            return Math.max(0, contentHeight - viewportHeight);
        }

        int contentHeight = scale(28) + getCameraAlbumGridHeight(getCameraAlbums().size()) + getPhotos().size() * PHOTO_ROW_HEIGHT;
        int viewportHeight = LIST_BOTTOM - LIST_TOP;
        return Math.max(0, contentHeight - viewportHeight);
    }

    private int getMaxMessageScroll() {
        if (selectedContact == null) {
            return 0;
        }

        return getMaxMessageScroll(selectedContact.number());
    }

    private int getMaxMessageScroll(String number) {
        int contentHeight = getMessages(number).size() * scale(28);
        int viewportHeight = scale(94);
        return Math.max(0, contentHeight - viewportHeight);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;

        guiGraphics.blit(
                PHONE_BACKGROUND,
                phoneX,
                phoneY,
                PHONE_WIDTH,
                PHONE_HEIGHT,
                0,
                0,
                PHONE_TEXTURE_WIDTH,
                PHONE_TEXTURE_HEIGHT,
                PHONE_TEXTURE_WIDTH,
                PHONE_TEXTURE_HEIGHT
        );

        CompoundTag tag = getTag();
        String title = getTitle(tag);
        String subtitle = getSubtitle(tag);

        if (title.isEmpty()) {
            drawCenteredText(guiGraphics, subtitle, phoneX + PHONE_WIDTH / 2, phoneY + scale(22), TEXT_SECONDARY);
        } else {
            drawCenteredText(guiGraphics, title, phoneX + PHONE_WIDTH / 2, phoneY + scale(20), TEXT_PRIMARY);
            drawCenteredText(guiGraphics, subtitle, phoneX + PHONE_WIDTH / 2, phoneY + scale(32), TEXT_SECONDARY);
        }

        if (page == Page.LOCKED) {
            renderLocked(guiGraphics, phoneX, phoneY);
        } else if (!isSetup()) {
            drawText(guiGraphics, "Nom", phoneX + scale(19), phoneY + scale(48), TEXT_PRIMARY);
            drawText(guiGraphics, "Numero", phoneX + scale(19), phoneY + scale(76), TEXT_PRIMARY);
            drawText(guiGraphics, "Code", phoneX + scale(19), phoneY + scale(104), TEXT_PRIMARY);
        } else if (page == Page.CONTACTS) {
            renderContactList(guiGraphics, phoneX, phoneY);
        } else if (page == Page.SMS) {
            renderSmsList(guiGraphics, phoneX, phoneY);
        } else if (page == Page.ADD_CONTACT) {
            renderAddContact(guiGraphics, phoneX, phoneY);
        } else if (page == Page.CONVERSATION) {
            renderConversation(guiGraphics, phoneX, phoneY);
        } else if (page == Page.ALBUMS) {
            renderAlbumList(guiGraphics, phoneX, phoneY);
        } else if (page == Page.CAMERA_ALBUM) {
            renderCameraAlbum(guiGraphics, phoneX, phoneY);
        } else if (page == Page.ADD_PHOTO) {
            renderAddPhoto(guiGraphics, phoneX, phoneY);
        } else if (page == Page.SEND_PHOTO) {
            renderSendPhoto(guiGraphics, phoneX, phoneY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private String getTitle(CompoundTag tag) {
        if (!isSetup()) {
            return "Configuration";
        }

        return switch (page) {
            case CONTACTS -> "Contacts";
            case SMS -> "SMS";
            case ADD_CONTACT -> "Ajouter";
            case CONVERSATION -> "Discussion";
            case ADD_PHOTO -> "Ajouter";
            case SEND_PHOTO -> "Envoyer";
            case CAMERA_ALBUM -> "Camera";
            case ALBUMS -> "Albums";
            default -> "";
        };
    }

    private String getSubtitle(CompoundTag tag) {
        if (!isSetup()) {
            return "CrazyPhone";
        }

        return switch (page) {
            case CONTACTS -> getContacts().size() + " contact(s)";
            case SMS -> getConversations().size() + " discussion(s)";
            case ADD_CONTACT -> "Numero";
            case CONVERSATION -> selectedContact == null ? "" : trimToWidth(selectedContact.name(), scale(86));
            case ADD_PHOTO -> "Photo custom";
            case SEND_PHOTO -> selectedPhoto == null ? "Choisir contact" : trimToWidth(selectedPhoto.title(), scale(86));
            case CAMERA_ALBUM -> getSelectedCameraAlbumImages().size() + " photo(s)";
            case LOCKED -> "Verrouille";
            case ALBUMS -> getTotalPhotoCount() + " photo(s)";
            default -> tag.getString("number");
        };
    }

    private void renderContactList(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<ContactEntry> contacts = getContacts();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int rowY = listTop - contactScrollOffset;

        if (rowY + scale(24) >= listTop && rowY <= listBottom) {
            drawText(guiGraphics, "Ajouter un contact", phoneX + scale(18), rowY + scale(6), TEXT_PRIMARY);
            drawSeparator(guiGraphics, phoneX, rowY + scale(22));
        }
        rowY += scale(28);

        if (contacts.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucun contact", phoneX + PHONE_WIDTH / 2, rowY + scale(16), TEXT_SECONDARY);
            return;
        }

        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry contact = contacts.get(i);

            if (rowY + CONTACT_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                renderListRowBackground(guiGraphics, phoneX, rowY, i);
                renderContactRow(guiGraphics, contact.name(), contact.number(), phoneX + scale(20), rowY);
                drawSeparator(guiGraphics, phoneX, rowY + CONTACT_ROW_HEIGHT - scale(3));
            }
            rowY += CONTACT_ROW_HEIGHT;

            if (rowY > listBottom + CONTACT_ROW_HEIGHT) {
                break;
            }
        }
    }

    private void renderSmsList(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<ConversationEntry> conversations = getConversations();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int rowY = listTop - smsScrollOffset;

        if (conversations.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucun SMS", phoneX + PHONE_WIDTH / 2, rowY + scale(16), TEXT_SECONDARY);
            return;
        }

        for (int i = 0; i < conversations.size(); i++) {
            ConversationEntry conversation = conversations.get(i);

            if (rowY + CONTACT_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                renderListRowBackground(guiGraphics, phoneX, rowY, i);
                renderContactRow(guiGraphics, conversation.displayName(), conversation.preview(), phoneX + scale(20), rowY);
                drawSeparator(guiGraphics, phoneX, rowY + CONTACT_ROW_HEIGHT - scale(3));
            }

            rowY += CONTACT_ROW_HEIGHT;

            if (rowY > listBottom + CONTACT_ROW_HEIGHT) {
                break;
            }
        }
    }

    private void renderLocked(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        drawText(guiGraphics, "Mot de passe", phoneX + scale(18), phoneY + scale(74), TEXT_PRIMARY);

        if (!unlockStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(unlockStatus, scale(92)), phoneX + PHONE_WIDTH / 2, phoneY + scale(146), TEXT_ERROR);
        }
    }

    private void renderAddContact(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        drawText(guiGraphics, "Nom", phoneX + scale(18), phoneY + scale(55), TEXT_PRIMARY);
        drawText(guiGraphics, "Numero", phoneX + scale(18), phoneY + scale(90), TEXT_PRIMARY);

        if (!contactStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(contactStatus, scale(86)), phoneX + PHONE_WIDTH / 2, phoneY + scale(158), contactStatusColor);
        }
    }

    private void renderConversation(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        if (selectedContact == null) {
            drawCenteredText(guiGraphics, "Aucun contact", phoneX + PHONE_WIDTH / 2, phoneY + scale(86), TEXT_SECONDARY);
            return;
        }

        drawCenteredText(guiGraphics, selectedContact.number(), phoneX + PHONE_WIDTH / 2, phoneY + scale(43), TEXT_SECONDARY);

        List<MessageEntry> messages = getMessages(selectedContact.number());
        int listTop = phoneY + scale(52);
        int listBottom = phoneY + scale(151);
        int rowY = listTop - messageScrollOffset;

        if (messages.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucun message", phoneX + PHONE_WIDTH / 2, listTop + scale(28), TEXT_SECONDARY);
        }

        for (MessageEntry message : messages) {
            if (rowY + scale(24) >= listTop && rowY <= listBottom) {
                renderMessageBubble(guiGraphics, message, phoneX, rowY);
            }

            rowY += scale(28);

            if (rowY > listBottom + scale(28)) {
                break;
            }
        }

        drawSeparator(guiGraphics, phoneX, phoneY + scale(153));
        drawText(guiGraphics, "Message", phoneX + scale(17), phoneY + scale(146), TEXT_SECONDARY);

        if (!messageStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(messageStatus, scale(86)), phoneX + PHONE_WIDTH / 2, phoneY + scale(141), messageStatusColor);
        }
    }

    private void renderAlbumList(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<PhotoEntry> photos = getPhotos();
        List<ItemStack> cameraAlbums = getCameraAlbums();
        int cameraPhotoCount = getCameraPhotoCount();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int rowY = listTop - albumScrollOffset;

        if (rowY + scale(24) >= listTop && rowY <= listBottom) {
            drawText(guiGraphics, "Ajouter une photo", phoneX + scale(18), rowY + scale(6), TEXT_PRIMARY);
            drawSeparator(guiGraphics, phoneX, rowY + scale(22));
        }
        rowY += scale(28);

        if (!cameraAlbums.isEmpty()) {
            renderCameraAlbumGrid(guiGraphics, cameraAlbums, phoneX, rowY, listTop, listBottom);
            rowY += getCameraAlbumGridHeight(cameraAlbums.size());
        }

        if (photos.isEmpty() && cameraPhotoCount <= 0) {
            drawCenteredText(guiGraphics, "Aucune photo", phoneX + PHONE_WIDTH / 2, rowY + scale(16), TEXT_SECONDARY);
            return;
        }

        for (PhotoEntry photo : photos) {
            if (rowY + PHOTO_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                renderPhotoRow(guiGraphics, photo, phoneX + scale(18), rowY);
                drawSeparator(guiGraphics, phoneX, rowY + PHOTO_ROW_HEIGHT - scale(3));
            }
            rowY += PHOTO_ROW_HEIGHT;

            if (rowY > listBottom + PHOTO_ROW_HEIGHT) {
                break;
            }
        }
    }

    private int getCameraPhotoCount() {
        if (minecraft == null || minecraft.level == null) {
            return 0;
        }

        return CrazyPhoneCameraHelper.getCameraPhotoCount(minecraft.level.registryAccess(), stack);
    }

    private int getTotalPhotoCount() {
        return getPhotos().size() + getCameraPhotoCount();
    }

    private List<ItemStack> getCameraAlbums() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }

        return CrazyPhoneCameraHelper.getCameraAlbums(minecraft.level.registryAccess(), stack);
    }

    private List<ItemStack> getSelectedCameraAlbumImages() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }

        List<ItemStack> albums = getCameraAlbums();

        if (selectedCameraAlbumIndex < 0 || selectedCameraAlbumIndex >= albums.size()) {
            return List.of();
        }

        return CrazyPhoneCameraHelper.getCameraImagesFromAlbum(minecraft.level.registryAccess(), albums.get(selectedCameraAlbumIndex));
    }

    private void openCameraImage(ItemStack image) {
        if (minecraft == null) {
            return;
        }

        minecraft.setScreen(new ImageScreen(image.copy()));
    }

    private void renderAddPhoto(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        drawText(guiGraphics, "Nom", phoneX + scale(18), phoneY + scale(55), TEXT_PRIMARY);
        drawText(guiGraphics, "Texture", phoneX + scale(18), phoneY + scale(90), TEXT_PRIMARY);

        if (!photoStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(photoStatus, scale(86)), phoneX + PHONE_WIDTH / 2, phoneY + scale(158), photoStatusColor);
        }
    }

    private void renderSendPhoto(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<ContactEntry> contacts = getContacts();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int rowY = listTop - contactScrollOffset;

        if (contacts.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucun contact", phoneX + PHONE_WIDTH / 2, rowY + scale(16), TEXT_SECONDARY);
        }

        for (ContactEntry contact : contacts) {
            if (rowY + CONTACT_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                renderContactRow(guiGraphics, contact.name(), contact.number(), phoneX + scale(20), rowY);
                drawSeparator(guiGraphics, phoneX, rowY + CONTACT_ROW_HEIGHT - scale(3));
            }

            rowY += CONTACT_ROW_HEIGHT;

            if (rowY > listBottom + CONTACT_ROW_HEIGHT) {
                break;
            }
        }

        if (!photoStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(photoStatus, scale(86)), phoneX + PHONE_WIDTH / 2, phoneY + scale(158), photoStatusColor);
        }
    }

    private void renderCameraAlbum(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<ItemStack> images = getSelectedCameraAlbumImages();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int gridTop = listTop - albumScrollOffset;

        if (images.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucune photo", phoneX + PHONE_WIDTH / 2, gridTop + scale(16), TEXT_SECONDARY);
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int imageX = phoneX + scale(18) + column * CAMERA_GRID_CELL;
            int imageY = gridTop + row * CAMERA_GRID_CELL;

            if (imageY + CAMERA_GRID_CELL >= listTop && imageY <= listBottom) {
                renderCameraPhotoItem(guiGraphics, images.get(i), imageX, imageY);
            }
        }
    }

    private int getCameraAlbumGridHeight(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }

        int rows = (itemCount + CAMERA_GRID_COLUMNS - 1) / CAMERA_GRID_COLUMNS;
        return rows * CAMERA_GRID_CELL + scale(4);
    }

    private void renderCameraAlbumGrid(GuiGraphics guiGraphics, List<ItemStack> albums, int phoneX, int y, int listTop, int listBottom) {
        for (int i = 0; i < albums.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int albumX = phoneX + scale(18) + column * CAMERA_GRID_CELL;
            int albumY = y + row * CAMERA_GRID_CELL;

            if (albumY + CAMERA_GRID_CELL >= listTop && albumY <= listBottom) {
                renderCameraPhotoItem(guiGraphics, albums.get(i), albumX, albumY);
            }
        }
    }

    private void renderPhotoRow(GuiGraphics guiGraphics, PhotoEntry photo, int x, int y) {
        ResourceLocation texture = ResourceLocation.tryParse(photo.texture());

        if (texture != null) {
            guiGraphics.blit(texture, x, y + scale(2), scale(24), scale(18), 0, 0, 64, 64, 64, 64);
        } else {
            guiGraphics.fill(x, y + scale(2), x + scale(24), y + scale(20), 0xFF324154);
        }

        drawText(guiGraphics, trimToWidth(photo.title(), scale(54)), x + scale(30), y + scale(4), TEXT_PRIMARY);
        drawText(guiGraphics, trimToWidth(photo.type(), scale(54)), x + scale(30), y + scale(15), TEXT_SECONDARY);
    }

    private void renderCameraPhotoItem(GuiGraphics guiGraphics, ItemStack image, int x, int y) {
        guiGraphics.renderItem(image, x + 1, y + 1);
    }

    private void drawSeparator(GuiGraphics guiGraphics, int phoneX, int y) {
        guiGraphics.fill(phoneX + scale(17), y, phoneX + PHONE_WIDTH - scale(17), y + 1, 0x66FFFFFF);
    }

    private void renderListRowBackground(GuiGraphics guiGraphics, int phoneX, int y, int index) {
        ResourceLocation texture = index % 2 == 0 ? CONTACT_ROW_BACKGROUND_1 : CONTACT_ROW_BACKGROUND_2;
        guiGraphics.blit(
                texture,
                phoneX + scale(17),
                y,
                PHONE_WIDTH - scale(34),
                scale(22),
                0,
                0,
                114,
                22,
                114,
                22
        );
    }

    private void renderContactRow(GuiGraphics guiGraphics, String name, String number, int x, int y) {
        drawText(guiGraphics, trimToWidth(name, scale(82)), x, y + scale(2), TEXT_PRIMARY);
        drawText(guiGraphics, trimToWidth(number, scale(82)), x, y + scale(13), TEXT_SECONDARY);
    }

    private void renderMessageBubble(GuiGraphics guiGraphics, MessageEntry message, int phoneX, int y) {
        int bubbleWidth = scale(68);
        int bubbleX = message.outgoing()
                ? phoneX + PHONE_WIDTH - scale(17) - bubbleWidth
                : phoneX + scale(17);
        int color = message.outgoing() ? 0xAA2F7D56 : 0xAA2E4F72;

        guiGraphics.fill(bubbleX, y, bubbleX + bubbleWidth, y + scale(22), color);
        drawText(guiGraphics, trimToWidth(message.text(), bubbleWidth - scale(8)), bubbleX + scale(4), y + scale(7), TEXT_PRIMARY);
    }

    private void drawText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x, y, color, true);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x - this.font.width(text) / 2, y, color, true);
    }

    private String trimToWidth(String value, int maxWidth) {
        String text = value;

        while (this.font.width(text) > maxWidth && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
        }

        if (!text.equals(value) && text.length() > 1) {
            return text.substring(0, text.length() - 1) + ".";
        }

        return text;
    }

    private enum Page {
        HOME,
        CONTACTS,
        SMS,
        ADD_CONTACT,
        CONVERSATION,
        ALBUMS,
        CAMERA_ALBUM,
        ADD_PHOTO,
        SEND_PHOTO,
        LOCKED
    }

    private record ContactEntry(String uuid, String name, String number) {
    }

    private record PhotoEntry(String title, String texture, String type) {
    }

    private record ConversationEntry(String number, String displayName, String preview) {
    }

    private record MessageEntry(String text, boolean outgoing) {
    }

    private static int scale(int value) {
        return value * PHONE_SCALE_NUMERATOR / PHONE_SCALE_DENOMINATOR;
    }

    private static class TextureButton extends Button {
        private final ResourceLocation texture;
        private final int textureWidth;
        private final int textureHeight;

        TextureButton(
                int x,
                int y,
                int width,
                int height,
                ResourceLocation texture,
                int textureWidth,
                int textureHeight,
                Component message,
                OnPress onPress
        ) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.texture != null) {
                guiGraphics.blit(
                        this.texture,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight(),
                        0,
                        0,
                        this.textureWidth,
                        this.textureHeight,
                        this.textureWidth,
                        this.textureHeight
                );
                return;
            }

            int color = this.isHoveredOrFocused() ? 0xFFE6F2FF : 0xFFFFFFFF;
            int centerX = this.getX() + this.getWidth() / 2;
            int centerY = this.getY() + this.getHeight() / 2;
            guiGraphics.fill(this.getX() + 4, centerY - 2, this.getX() + this.getWidth() - 4, centerY + 2, color);
            guiGraphics.fill(centerX - 2, this.getY() + 4, centerX + 2, this.getY() + this.getHeight() - 4, color);
        }
    }

    private static class InvisibleButton extends Button {
        InvisibleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }
    }
}
