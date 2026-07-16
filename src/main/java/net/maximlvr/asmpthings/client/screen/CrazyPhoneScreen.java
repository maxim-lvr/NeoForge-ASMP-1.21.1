package net.maximlvr.asmpthings.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import de.maxhenkel.camera.ImageData;
import de.maxhenkel.camera.ImageProcessor;
import de.maxhenkel.camera.TextureCache;
import de.maxhenkel.camera.gui.ImageScreen;
import net.maximlvr.asmpthings.integration.camera.CrazyPhoneCameraHelper;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhoneContactByNumberPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneCameraAlbumActionPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneCameraPhotoActionPayload;
import net.maximlvr.asmpthings.network.payload.AddCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneContactResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneMessageResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhonePhotoResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneSetupResultPayload;
import net.maximlvr.asmpthings.network.payload.CrazyPhoneSyncPayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhoneMessagePayload;
import net.maximlvr.asmpthings.network.payload.SendCrazyPhonePhotoPayload;
import net.maximlvr.asmpthings.network.payload.SetCrazyPhoneLockedPayload;
import net.maximlvr.asmpthings.network.payload.SetupCrazyPhonePayload;
import net.maximlvr.asmpthings.network.payload.TakeCrazyPhonePhotoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

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
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/asmpphone-sms-icon.png");
    private static final ResourceLocation ADD_ICON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/asmpphone-photo-icon.png");
    private static final ResourceLocation SEND_MESSAGE_BUTTON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-send-message.png");
    private static final ResourceLocation SEND_MESSAGE_BUTTON_HOVER =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-send-message_hover.png");
    private static final ResourceLocation ADD_IMAGE_BUTTON =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-add-image.png");
    private static final ResourceLocation ADD_IMAGE_BUTTON_HOVER =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone-add-image_hover.png");
    private static final ResourceLocation SLOT_SELECTED =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/slot_selected.png");
    private static final ResourceLocation CAMERA_UPLOAD_PHOTO =
            ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "textures/screens/crazyphone_upload_photo.png");
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
    private static final int CONTACT_ROW_HEIGHT = scale(16);
    private static final int ALBUM_ROW_HEIGHT = scale(22);
    private static final int PHOTO_ROW_HEIGHT = scale(31);
    private static final int CAMERA_GRID_COLUMNS = 6;
    private static final int CAMERA_GRID_CELL = scale(14);
    private static final int PHOTO_THUMBNAIL_SIZE = scale(11);
    private static final int PHOTO_SELECTION_SIZE = 18;
    private static final int UPLOAD_ALBUM_CAPACITY = 50;
    private static final int LIST_TOP = scale(49);
    private static final int LIST_BOTTOM = scale(164);
    private static final int PHOTO_GRID_BOTTOM = scale(146);
    private static final int TEXT_PRIMARY = 0xFFFFFF;
    private static final int TEXT_SECONDARY = 0xD8ECFF;
    private static final int TEXT_ERROR = 0xFF7777;
    private static final int TEXT_SUCCESS = 0x96FF9B;
    private static final float MESSAGE_TEXT_SCALE = 0.85f;
    private static final float MESSAGE_HINT_SCALE = 0.75f;
    private static final int MESSAGE_PHOTO_SIZE = scale(54);
    private static final int ALL_PHOTOS_ALBUM_INDEX = -1000;
    private static final int UNASSIGNED_ALBUM_INDEX = -1;
    private static final int NO_EDITING_ALBUM = Integer.MIN_VALUE;
    private static final String TAG_CAMERA_ALBUM_GROUPS = "cameraAlbumGroups";
    private static final String TAG_CAMERA_PHOTO_GROUPS = "cameraPhotoGroups";

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
    private EditBox albumNameField;
    private EditBox unlockPasswordField;
    private EditBox messageField;
    private String setupStatus = "";
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
    private String attachPhotoContactNumber = "";
    private ContactEntry attachPhotoContact;
    private Page attachPhotoBackPage = Page.CONVERSATION;
    private int selectedCameraAlbumIndex = 0;
    private int selectedUploadAlbumIndex = 0;
    private int editingAlbumIndex = NO_EDITING_ALBUM;
    private int contactScrollOffset = 0;
    private int albumScrollOffset = 0;
    private int smsScrollOffset = 0;
    private int messageScrollOffset = 0;
    private final Set<Integer> selectedCameraPhotoIndexes = new LinkedHashSet<>();
    private final Set<Integer> selectedUploadPhotoIndexes = new LinkedHashSet<>();
    private String pendingCameraPhotoMoveRefs = "";
    private int pendingCameraPhotoMoveReturnAlbum = ALL_PHOTOS_ALBUM_INDEX;
    private int observedCameraPhotoCount = -1;
    private final Map<String, UploadedTexture> uploadedTextures = new HashMap<>();

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
            case UPLOAD_ALBUM -> initUploadAlbum();
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
        numberField.setMaxLength(6);
        numberField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,6}"));
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
        int addRowY = phoneY + LIST_TOP - scale(4) - contactScrollOffset;

        if (addRowY + scale(20) >= phoneY + LIST_TOP && addRowY <= phoneY + LIST_BOTTOM) {
            addRenderableWidget(new InvisibleButton(
                    phoneX + scale(17), addRowY, scale(88), scale(20),
                    Component.literal("Ajouter un contact"), button -> {
                page = Page.ADD_CONTACT;
                contactStatus = "";
                rebuildWidgets();
            }));
        }

        int rowY = phoneY + LIST_TOP + scale(18) - contactScrollOffset;

        for (ContactEntry contact : getContacts()) {
            if (rowY + CONTACT_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
                addRenderableWidget(new InvisibleButton(
                        phoneX + scale(3), rowY, PHONE_WIDTH - scale(6), CONTACT_ROW_HEIGHT,
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
                        phoneX + scale(3), rowY, PHONE_WIDTH - scale(6), CONTACT_ROW_HEIGHT,
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
        contactNumberField.setMaxLength(6);
        contactNumberField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,6}"));
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
        int inputX = phoneX + scale(10);
        int inputY = phoneY + scale(158);

        messageField = new EditBox(this.font, inputX, inputY, scale(80), scale(16), Component.literal("Envoyer un message..."));
        messageField.setMaxLength(800);
        messageField.setTextColor(TEXT_PRIMARY);
        messageField.setTextColorUneditable(TEXT_PRIMARY);
        addRenderableWidget(messageField);

        HoverTextureButton sendButton = new HoverTextureButton(
                phoneX + scale(92), inputY + scale(1), scale(16), scale(16),
                SEND_MESSAGE_BUTTON, SEND_MESSAGE_BUTTON_HOVER, 14, 14,
                Component.literal("Envoyer"), button -> sendMessage()
        );
        addRenderableWidget(sendButton);

        addRenderableWidget(new RevealedTextureButton(
                phoneX + scale(93), inputY - scale(16), scale(15), scale(16),
                ADD_IMAGE_BUTTON, ADD_IMAGE_BUTTON_HOVER, 14, 15,
                Component.literal("Joindre une image"), button -> openAttachPhotoPicker(),
                () -> sendButton.isHoveredOrFocused()
        ));

        addSystemButtons(phoneX, phoneY);
    }

    private void initAlbums() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int rowY = phoneY + LIST_TOP - scale(5) - albumScrollOffset;
        List<CameraAlbumGroup> albumGroups = getCameraAlbumGroups();
        boolean movingPhotos = hasPendingCameraPhotoMove();

        if (rowY + ALBUM_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
            addRenderableWidget(new InvisibleButton(
                    phoneX + scale(13), rowY, PHONE_WIDTH - scale(26), ALBUM_ROW_HEIGHT,
                    Component.literal("Ajouter un album"), button -> createCameraAlbum()
            ));
        }

        rowY += ALBUM_ROW_HEIGHT;

        if (rowY + ALBUM_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
            if (!movingPhotos) {
                addRenderableWidget(new InvisibleButton(
                        phoneX + scale(13), rowY, PHONE_WIDTH - scale(26), ALBUM_ROW_HEIGHT,
                        Component.literal("Toutes mes photos"), button -> openCameraAlbum(ALL_PHOTOS_ALBUM_INDEX)
                ));
            }
        }

        rowY += ALBUM_ROW_HEIGHT;

        if (rowY + ALBUM_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
            addRenderableWidget(new InvisibleButton(
                    phoneX + scale(13), rowY, PHONE_WIDTH - scale(26), ALBUM_ROW_HEIGHT,
                    Component.literal("Non triees"), button -> {
                if (movingPhotos) {
                    assignSelectedPhotosToAlbum(UNASSIGNED_ALBUM_INDEX);
                } else {
                    openCameraAlbum(UNASSIGNED_ALBUM_INDEX);
                }
            }
            ));
        }

        rowY += ALBUM_ROW_HEIGHT;

        for (CameraAlbumGroup group : albumGroups) {
            int albumIndex = group.id();

            if (rowY + ALBUM_ROW_HEIGHT >= phoneY + LIST_TOP && rowY <= phoneY + LIST_BOTTOM) {
                if (editingAlbumIndex != albumIndex) {
                    addRenderableWidget(new InvisibleButton(
                            phoneX + scale(13), rowY, PHONE_WIDTH - scale(45), ALBUM_ROW_HEIGHT,
                            Component.literal(group.name()), button -> {
                        if (movingPhotos) {
                            assignSelectedPhotosToAlbum(albumIndex);
                        } else {
                            openCameraAlbum(albumIndex);
                        }
                    }
                    ));
                }

                if (editingAlbumIndex == albumIndex) {
                    albumNameField = new EditBox(
                            this.font,
                            phoneX + scale(17),
                            rowY + scale(3),
                            scale(58),
                            scale(14),
                            Component.literal("Nom album")
                    );
                    albumNameField.setMaxLength(24);
                    albumNameField.setTextColor(TEXT_PRIMARY);
                    albumNameField.setTextColorUneditable(TEXT_PRIMARY);
                    albumNameField.setValue(group.name());
                    addRenderableWidget(albumNameField);
                }

                Button editButton = Button.builder(
                                Component.literal(editingAlbumIndex == albumIndex ? "ok" : "edit"),
                                button -> toggleOrSaveAlbumEdit(albumIndex)
                        )
                        .bounds(phoneX + PHONE_WIDTH - scale(27), rowY + scale(3), scale(17), scale(14))
                        .build();
                editButton.setFGColor(TEXT_PRIMARY);
                addRenderableWidget(editButton);
            }

            rowY += ALBUM_ROW_HEIGHT;
        }

        addSystemButtons(phoneX, phoneY);
    }

    private void openCameraAlbum(int albumIndex) {
        selectedCameraAlbumIndex = albumIndex;
        selectedCameraPhotoIndexes.clear();
        page = Page.CAMERA_ALBUM;
        albumScrollOffset = 0;
        rebuildWidgets();
    }

    private void initCameraAlbum() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int gridTop = phoneY + LIST_TOP - albumScrollOffset;
        int listBottom = getPhotoGridBottom(phoneY);
        List<CameraPhotoEntry> images = getSelectedCameraPhotoEntries();
        boolean hasUploadSlot = selectedCameraAlbumIndex != ALL_PHOTOS_ALBUM_INDEX;

        if (hasUploadSlot) {
            int uploadX = getGridItemX(phoneX, 0);
            int uploadY = gridTop;

            if (uploadY + CAMERA_GRID_CELL >= phoneY + LIST_TOP && uploadY <= listBottom) {
                addRenderableWidget(new InvisibleButton(
                        uploadX, uploadY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE,
                        Component.literal("Upload"), button -> openUploadFilePicker(selectedCameraAlbumIndex)
                ));
            }
        }

        for (int i = 0; i < images.size(); i++) {
            int slotIndex = hasUploadSlot ? i + 1 : i;
            int column = slotIndex % CAMERA_GRID_COLUMNS;
            int row = slotIndex / CAMERA_GRID_COLUMNS;
            int imageX = getGridItemX(phoneX, column);
            int imageY = gridTop + row * CAMERA_GRID_CELL;
            ItemStack image = images.get(i).image();

            if (imageY + CAMERA_GRID_CELL >= phoneY + LIST_TOP && imageY <= listBottom) {
                addRenderableWidget(new InvisibleButton(
                        imageX, imageY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE,
                        Component.literal("Voir photo"), button -> openCameraImage(image)
                ));
            }
        }

        addCameraPhotoActionButtons(phoneX, phoneY);
        addSystemButtons(phoneX, phoneY);
    }

    private void initUploadAlbum() {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int gridTop = phoneY + LIST_TOP - albumScrollOffset;
        int listBottom = getPhotoGridBottom(phoneY);
        List<PhotoEntry> photos = getSelectedUploadAlbumPhotos();

        for (int i = 0; i < photos.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int imageX = getGridItemX(phoneX, column);
            int imageY = gridTop + row * CAMERA_GRID_CELL;
            PhotoEntry photo = photos.get(i);

            if (imageY + CAMERA_GRID_CELL >= phoneY + LIST_TOP && imageY <= listBottom) {
                addRenderableWidget(new InvisibleButton(
                        imageX, imageY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE,
                        Component.literal(photo.title()), button -> {
                    if (!attachPhotoContactNumber.isEmpty()) {
                        selectedPhoto = photo;
                        sendSelectedPhoto(attachPhotoContactNumber);
                    } else {
                        openUploadedImage(photo);
                    }
                }));
            }
        }

        addUploadPhotoActionButtons(phoneX, phoneY);
        addSystemButtons(phoneX, phoneY);
    }

    private void addCameraPhotoActionButtons(int phoneX, int phoneY) {
        int buttonY = phoneY + scale(151);
        if (!attachPhotoContactNumber.isEmpty()) {
            Button sendButton = Button.builder(Component.literal("Envoyer"), button -> sendSelectedCameraPhoto())
                    .bounds(phoneX + scale(33), buttonY, scale(56), scale(15))
                    .build();
            sendButton.active = !selectedCameraPhotoIndexes.isEmpty();
            sendButton.setFGColor(TEXT_PRIMARY);
            addRenderableWidget(sendButton);
            return;
        }

        Button takeButton = Button.builder(Component.literal("Prendre"), button -> runSelectedCameraPhotoAction("take"))
                .bounds(phoneX + scale(11), buttonY, scale(33), scale(15))
                .build();
        takeButton.active = !selectedCameraPhotoIndexes.isEmpty();
        takeButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(takeButton);

        Button deleteButton = Button.builder(Component.literal("Effacer"), button -> runSelectedCameraPhotoAction("delete"))
                .bounds(phoneX + scale(48), buttonY, scale(33), scale(15))
                .build();
        deleteButton.active = !selectedCameraPhotoIndexes.isEmpty();
        deleteButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(deleteButton);

        Button albumButton = Button.builder(Component.literal("Album"), button -> startCameraPhotoMove())
                .bounds(phoneX + scale(85), buttonY, scale(28), scale(15))
                .build();
        albumButton.active = !selectedCameraPhotoIndexes.isEmpty();
        albumButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(albumButton);
    }

    private void addUploadPhotoActionButtons(int phoneX, int phoneY) {
        if (attachPhotoContactNumber.isEmpty()) {
            return;
        }

        Button sendButton = Button.builder(Component.literal("Envoyer"), button -> sendSelectedUploadPhoto())
                .bounds(phoneX + scale(33), phoneY + scale(151), scale(56), scale(15))
                .build();
        sendButton.active = !selectedUploadPhotoIndexes.isEmpty();
        sendButton.setFGColor(TEXT_PRIMARY);
        addRenderableWidget(sendButton);
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
            pendingCameraPhotoMoveRefs = "";
            pendingCameraPhotoMoveReturnAlbum = ALL_PHOTOS_ALBUM_INDEX;
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
            pendingCameraPhotoMoveRefs = "";
            pendingCameraPhotoMoveReturnAlbum = ALL_PHOTOS_ALBUM_INDEX;
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
        } else if (hasPendingCameraPhotoMove() && page == Page.ALBUMS) {
            selectedCameraAlbumIndex = pendingCameraPhotoMoveReturnAlbum;
            pendingCameraPhotoMoveRefs = "";
            pendingCameraPhotoMoveReturnAlbum = ALL_PHOTOS_ALBUM_INDEX;
            photoStatus = "";
            page = Page.CAMERA_ALBUM;
        } else if (!attachPhotoContactNumber.isEmpty() && page == Page.ALBUMS) {
            selectedContact = attachPhotoContact;
            page = attachPhotoBackPage;
            attachPhotoContactNumber = "";
            attachPhotoContact = null;
        } else if (page == Page.SEND_PHOTO) {
            page = Page.ALBUMS;
        } else if (page == Page.CAMERA_ALBUM || page == Page.UPLOAD_ALBUM) {
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

    private void openAttachPhotoPicker() {
        if (selectedContact == null) {
            return;
        }

        attachPhotoContactNumber = selectedContact.number();
        attachPhotoContact = selectedContact;
        attachPhotoBackPage = page;
        selectedPhoto = null;
        photoStatus = "";
        pendingCameraPhotoMoveRefs = "";
        pendingCameraPhotoMoveReturnAlbum = ALL_PHOTOS_ALBUM_INDEX;
        albumScrollOffset = 0;
        page = Page.ALBUMS;
        rebuildWidgets();
    }

    private void setupPhone() {
        String name = nameField.getValue().trim();
        String number = numberField.getValue().trim();
        String password = passwordField.getValue().trim();

        if (name.isEmpty()) {
            setupStatus = "Nom obligatoire";
            return;
        }

        if (!isValidPhoneNumber(number)) {
            setupStatus = "Numero: 1 a 6 chiffres";
            return;
        }

        if (password.isEmpty()) {
            setupStatus = "Code obligatoire";
            return;
        }

        setupStatus = "";
        PacketDistributor.sendToServer(new SetupCrazyPhonePayload(mainHand, name, number, password));
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

    private boolean isValidPhoneNumber(String number) {
        return number != null && number.matches("\\d{1,6}");
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

    private List<PhotoEntry> getUploadPhotos() {
        List<PhotoEntry> uploads = new ArrayList<>();

        for (PhotoEntry photo : getPhotos()) {
            if ("upload".equals(photo.type())) {
                uploads.add(photo);
            }
        }

        return uploads;
    }

    private List<UploadAlbumEntry> getUploadAlbums() {
        List<PhotoEntry> uploads = getUploadPhotos();
        List<UploadAlbumEntry> albums = new ArrayList<>();

        for (int start = 0; start < uploads.size(); start += UPLOAD_ALBUM_CAPACITY) {
            int index = start / UPLOAD_ALBUM_CAPACITY;
            String name = index == 0 ? "upload" : "upload" + (index + 1);
            int end = Math.min(start + UPLOAD_ALBUM_CAPACITY, uploads.size());
            albums.add(new UploadAlbumEntry(name, start, end));
        }

        return albums;
    }

    private List<PhotoEntry> getSelectedUploadAlbumPhotos() {
        List<PhotoEntry> uploads = getUploadPhotos();
        int start = selectedUploadAlbumIndex * UPLOAD_ALBUM_CAPACITY;

        if (start < 0 || start >= uploads.size()) {
            return List.of();
        }

        int end = Math.min(start + UPLOAD_ALBUM_CAPACITY, uploads.size());
        return uploads.subList(start, end);
    }

    private String getSelectedUploadAlbumName() {
        List<UploadAlbumEntry> albums = getUploadAlbums();

        if (selectedUploadAlbumIndex < 0 || selectedUploadAlbumIndex >= albums.size()) {
            return "upload";
        }

        return albums.get(selectedUploadAlbumIndex).name();
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
                String kind = messageTag.getString("kind");

                if ("camera_photo".equals(kind) && minecraft != null && minecraft.level != null) {
                    CompoundTag imageTag = messageTag.getCompound("image");
                    ItemStack image = imageTag.contains("id")
                            ? ItemStack.parseOptional(minecraft.level.registryAccess(), imageTag)
                            : ItemStack.EMPTY;

                    if (!image.isEmpty()) {
                        messages.add(new MessageEntry(text.isEmpty() ? "[Photo]" : text, messageTag.getBoolean("outgoing"), kind, image, null));
                    }
                } else if ("uploaded_photo".equals(kind)) {
                    PhotoEntry photo = new PhotoEntry(
                            messageTag.getString("photoTitle"),
                            messageTag.getString("photoTexture"),
                            messageTag.getString("photoType")
                    );
                    messages.add(new MessageEntry(text.isEmpty() ? "[Photo]" : text, messageTag.getBoolean("outgoing"), kind, ItemStack.EMPTY, photo));
                } else if (!text.isEmpty()) {
                    messages.add(new MessageEntry(text, messageTag.getBoolean("outgoing"), "text", ItemStack.EMPTY, null));
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

        if (name.isEmpty() || !isValidPhoneNumber(number)) {
            contactStatus = "Numero introuvable";
            contactStatusColor = TEXT_ERROR;
            return;
        }

        if (number.equals(getTag().getString("number"))) {
            contactStatus = "Ce numero nous appartient";
            contactStatusColor = TEXT_ERROR;
            return;
        }

        PacketDistributor.sendToServer(new AddCrazyPhoneContactByNumberPayload(mainHand, name, number));
    }

    private void openUploadFilePicker(int targetAlbumIndex) {
        if (minecraft == null) {
            return;
        }

        var client = minecraft;

        new Thread(() -> {
            String pathResult;

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                var filters = memoryStack.mallocPointer(3);
                filters.put(memoryStack.UTF8("*.png"));
                filters.put(memoryStack.UTF8("*.jpg"));
                filters.put(memoryStack.UTF8("*.jpeg"));
                filters.flip();

                pathResult = TinyFileDialogs.tinyfd_openFileDialog(
                        "Ajouter une photo",
                        null,
                        filters,
                        "Images",
                        true
                );
            }

            if (pathResult == null || pathResult.isEmpty()) {
                return;
            }

            List<File> files = parseSelectedFiles(pathResult);

            List<BufferedImage> images = new ArrayList<>();
            int failed = 0;

            for (File file : files) {
                try {
                    BufferedImage image = ImageIO.read(file);

                    if (image == null) {
                        failed++;
                    } else {
                        images.add(image);
                    }
                } catch (IOException exception) {
                    failed++;
                }
            }

            int failedCount = failed;

            client.execute(() -> uploadCameraImages(images, failedCount, targetAlbumIndex));
        }, "CrazyPhoneUploadPicker").start();
    }

    private List<File> parseSelectedFiles(String pathResult) {
        String[] parts = pathResult.split("\\|");
        List<File> files = new ArrayList<>();

        if (parts.length <= 1) {
            files.add(new File(pathResult));
            return files;
        }

        File first = new File(parts[0]);

        if (first.isDirectory()) {
            for (int i = 1; i < parts.length; i++) {
                files.add(new File(first, parts[i]));
            }
        } else {
            for (String part : parts) {
                files.add(new File(part));
            }
        }

        return files;
    }

    private void uploadCameraImages(List<BufferedImage> images, int failedCount, int targetAlbumIndex) {
        if (images.isEmpty()) {
            photoStatus = "Image introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        if (targetAlbumIndex != ALL_PHOTOS_ALBUM_INDEX) {
            PacketDistributor.sendToServer(new CrazyPhoneCameraAlbumActionPayload(
                    mainHand,
                    "upload_target",
                    targetAlbumIndex,
                    "",
                    images.size()
            ));
        }

        for (BufferedImage image : images) {
            ImageProcessor.sendScreenshotThreaded(UUID.randomUUID(), image);
        }

        photoStatus = images.size() == 1 ? "Upload lance" : images.size() + " uploads lances";

        if (failedCount > 0) {
            photoStatus += " (" + failedCount + " ignore)";
        }

        photoStatusColor = TEXT_SUCCESS;
        page = Page.ALBUMS;
        albumScrollOffset = 0;
        rebuildWidgets();
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

        addPhotoLocally(title, normalizedTexture, "custom");
        PacketDistributor.sendToServer(new AddCrazyPhonePhotoPayload(mainHand, title, normalizedTexture, "custom"));
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

    public void handleSetupResult(CrazyPhoneSetupResultPayload payload) {
        setupStatus = payload.success() ? "" : payload.message();
        rebuildWidgets();
    }

    public void handlePhotoResult(CrazyPhonePhotoResultPayload payload) {
        photoStatus = payload.message();
        photoStatusColor = payload.success() ? TEXT_SUCCESS : TEXT_ERROR;

        if (payload.success()) {
            if (!attachPhotoContactNumber.isEmpty()) {
                selectedContact = attachPhotoContact == null ? selectedContact : attachPhotoContact;
                page = attachPhotoBackPage;
                attachPhotoContactNumber = "";
                attachPhotoContact = null;

                if (page == Page.CONVERSATION && selectedContact != null) {
                    messageScrollOffset = getMaxMessageScroll(selectedContact.number());
                }
            } else if (page != Page.CAMERA_ALBUM && page != Page.UPLOAD_ALBUM) {
                page = Page.ALBUMS;
            }

            selectedPhoto = null;
            selectedCameraPhotoIndexes.clear();
            selectedUploadPhotoIndexes.clear();
        }

        rebuildWidgets();
    }

    public void handleSync(CrazyPhoneSyncPayload payload) {
        if (payload.mainHand() != mainHand) {
            return;
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(payload.tag().copy()));
        observedCameraPhotoCount = getCameraPhotoCount();
        selectedCameraPhotoIndexes.removeIf(index -> index >= getSelectedCameraPhotoEntries().size());
        selectedUploadPhotoIndexes.removeIf(index -> index >= getSelectedUploadAlbumPhotos().size());
        albumScrollOffset = clamp(albumScrollOffset, 0, getMaxAlbumScroll());

        if (page == Page.CONVERSATION && selectedContact != null) {
            messageScrollOffset = getMaxMessageScroll(selectedContact.number());
        }

        rebuildWidgets();
    }

    public void handleMessageResult(CrazyPhoneMessageResultPayload payload) {
        messageStatus = payload.success() ? "" : payload.status();
        messageStatusColor = TEXT_ERROR;

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

    private void runSelectedCameraPhotoAction(String action) {
        if (selectedCameraPhotoIndexes.isEmpty()) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        PacketDistributor.sendToServer(new CrazyPhoneCameraPhotoActionPayload(
                mainHand,
                selectedCameraAlbumIndex,
                selectedIndexesToString(selectedCameraPhotoIndexes),
                action,
                ""
        ));

        selectedCameraPhotoIndexes.clear();
    }

    private void sendSelectedCameraPhoto() {
        if (selectedCameraPhotoIndexes.isEmpty() || attachPhotoContactNumber.isEmpty()) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        PacketDistributor.sendToServer(new CrazyPhoneCameraPhotoActionPayload(
                mainHand,
                selectedCameraAlbumIndex,
                selectedIndexesToString(selectedCameraPhotoIndexes),
                "send",
                attachPhotoContactNumber
        ));
    }

    private void startCameraPhotoMove() {
        if (selectedCameraPhotoIndexes.isEmpty()) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        pendingCameraPhotoMoveRefs = selectedIndexesToString(selectedCameraPhotoIndexes);

        if (pendingCameraPhotoMoveRefs.isEmpty()) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        pendingCameraPhotoMoveReturnAlbum = selectedCameraAlbumIndex;
        selectedCameraPhotoIndexes.clear();
        editingAlbumIndex = NO_EDITING_ALBUM;
        photoStatus = "Choisis un album";
        photoStatusColor = TEXT_SECONDARY;
        page = Page.ALBUMS;
        albumScrollOffset = 0;
        rebuildWidgets();
    }

    private void assignSelectedPhotosToAlbum(int albumIndex) {
        if (!hasPendingCameraPhotoMove()) {
            openCameraAlbum(albumIndex);
            return;
        }

        PacketDistributor.sendToServer(new CrazyPhoneCameraAlbumActionPayload(
                mainHand,
                "assign",
                albumIndex,
                pendingCameraPhotoMoveRefs,
                0
        ));

        selectedCameraAlbumIndex = albumIndex;
        pendingCameraPhotoMoveRefs = "";
        pendingCameraPhotoMoveReturnAlbum = ALL_PHOTOS_ALBUM_INDEX;
        selectedCameraPhotoIndexes.clear();
        photoStatus = albumIndex == UNASSIGNED_ALBUM_INDEX ? "Photos non triees" : "Photos rangees";
        photoStatusColor = TEXT_SUCCESS;
        page = Page.CAMERA_ALBUM;
        albumScrollOffset = 0;
        rebuildWidgets();
    }

    private boolean hasPendingCameraPhotoMove() {
        return !pendingCameraPhotoMoveRefs.isEmpty();
    }

    private void sendSelectedUploadPhoto() {
        List<PhotoEntry> photos = getSelectedUploadAlbumPhotos();

        if (selectedUploadPhotoIndexes.isEmpty()) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
            return;
        }

        int sent = 0;

        for (int index : selectedUploadPhotoIndexes) {
            if (index < 0 || index >= photos.size()) {
                continue;
            }

            PhotoEntry photo = photos.get(index);
            PacketDistributor.sendToServer(new SendCrazyPhonePhotoPayload(
                    mainHand,
                    attachPhotoContactNumber,
                    photo.title(),
                    photo.texture(),
                    "recu"
            ));
            sent++;
        }

        if (sent <= 0) {
            photoStatus = "Photo introuvable";
            photoStatusColor = TEXT_ERROR;
            rebuildWidgets();
        }
    }

    private String selectedIndexesToString(Set<Integer> indexes) {
        List<CameraPhotoEntry> entries = getSelectedCameraPhotoEntries();
        StringBuilder builder = new StringBuilder();

        for (int index : indexes) {
            if (index < 0 || index >= entries.size()) {
                continue;
            }

            CameraPhotoEntry entry = entries.get(index);

            if (builder.length() > 0) {
                builder.append(",");
            }

            builder.append(entry.albumIndex()).append(":").append(entry.imageIndex());
        }

        return builder.toString();
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

    private void addPhotoLocally(String title, String texture, String type) {
        CompoundTag tag = getTag();
        ListTag photos = tag.getList("photos", 10);
        CompoundTag photo = new CompoundTag();
        photo.putString("title", title);
        photo.putString("texture", texture);
        photo.putString("type", type);
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
        message.putString("kind", "text");
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
    public void tick() {
        super.tick();

        if (page != Page.ALBUMS && page != Page.CAMERA_ALBUM && page != Page.UPLOAD_ALBUM) {
            observedCameraPhotoCount = getCameraPhotoCount();
            return;
        }

        int cameraPhotoCount = getCameraPhotoCount();

        if (observedCameraPhotoCount < 0) {
            observedCameraPhotoCount = cameraPhotoCount;
            return;
        }

        if (cameraPhotoCount != observedCameraPhotoCount) {
            observedCameraPhotoCount = cameraPhotoCount;
            selectedCameraPhotoIndexes.clear();
            albumScrollOffset = clamp(albumScrollOffset, 0, getMaxAlbumScroll());
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page != Page.CONTACTS && page != Page.SMS && page != Page.CONVERSATION && page != Page.ALBUMS && page != Page.CAMERA_ALBUM && page != Page.UPLOAD_ALBUM && page != Page.SEND_PHOTO) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int listBottom = page == Page.CAMERA_ALBUM || page == Page.UPLOAD_ALBUM ? getPhotoGridBottom(phoneY) : phoneY + LIST_BOTTOM;

        if (mouseX < phoneX || mouseX > phoneX + PHONE_WIDTH || mouseY < phoneY + LIST_TOP || mouseY > listBottom) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maxScroll = page == Page.ALBUMS || page == Page.CAMERA_ALBUM || page == Page.UPLOAD_ALBUM ? getMaxAlbumScroll() : page == Page.CONVERSATION ? getMaxMessageScroll() : page == Page.SMS ? getMaxSmsScroll() : getMaxContactScroll();

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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && page == Page.CONVERSATION && openConversationPhotoAt(mouseX, mouseY)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (page == Page.CAMERA_ALBUM && selectCameraPhotoAt(mouseX, mouseY)) {
                return true;
            }

            if (page == Page.UPLOAD_ALBUM && selectUploadPhotoAt(mouseX, mouseY)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean openConversationPhotoAt(double mouseX, double mouseY) {
        if (selectedContact == null) {
            return false;
        }

        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int listTop = phoneY + scale(42);
        int listBottom = phoneY + scale(151);
        int rowY = listTop - messageScrollOffset;

        if (mouseY < listTop || mouseY > listBottom) {
            return false;
        }

        for (MessageEntry message : getMessages(selectedContact.number())) {
            int bubbleHeight = getMessageBubbleHeight(message);

            if (message.isPhoto() && rowY + bubbleHeight >= listTop && rowY <= listBottom) {
                int size = getMessagePhotoSize();
                int photoX = getMessagePhotoX(message, phoneX);

                if (isInside((int) mouseX, (int) mouseY, photoX, rowY, size, size)) {
                    if ("camera_photo".equals(message.kind()) && !message.image().isEmpty()) {
                        openCameraImage(message.image());
                    } else if (message.photo() != null) {
                        openUploadedImage(message.photo());
                    }

                    return true;
                }
            }

            rowY += bubbleHeight + scale(4);

            if (rowY > listBottom + scale(36)) {
                break;
            }
        }

        return false;
    }

    private boolean selectCameraPhotoAt(double mouseX, double mouseY) {
        int index = getCameraPhotoIndexAt(mouseX, mouseY, getSelectedCameraPhotoEntries().size());

        if (index < 0) {
            return false;
        }

        toggleSelectedIndex(selectedCameraPhotoIndexes, index);
        selectedUploadPhotoIndexes.clear();
        rebuildWidgets();
        return true;
    }

    private boolean selectUploadPhotoAt(double mouseX, double mouseY) {
        int index = getPhotoIndexAt(mouseX, mouseY, getSelectedUploadAlbumPhotos().size());

        if (index < 0) {
            return false;
        }

        toggleSelectedIndex(selectedUploadPhotoIndexes, index);
        selectedCameraPhotoIndexes.clear();
        rebuildWidgets();
        return true;
    }

    private void toggleSelectedIndex(Set<Integer> indexes, int index) {
        if (!indexes.remove(index)) {
            if (indexes.size() >= 5) {
                photoStatus = "5 photos maximum";
                photoStatusColor = TEXT_ERROR;
                return;
            }

            indexes.add(index);
        }

        if (indexes.size() < 5 || indexes.contains(index)) {
            photoStatus = "";
        }
    }

    private int getCameraPhotoIndexAt(double mouseX, double mouseY, int itemCount) {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int listTop = phoneY + LIST_TOP;
        int listBottom = getPhotoGridBottom(phoneY);
        int gridTop = listTop - albumScrollOffset;
        int slotOffset = selectedCameraAlbumIndex != ALL_PHOTOS_ALBUM_INDEX ? 1 : 0;

        if (mouseY < listTop || mouseY > listBottom) {
            return -1;
        }

        for (int i = 0; i < itemCount; i++) {
            int slotIndex = i + slotOffset;
            int column = slotIndex % CAMERA_GRID_COLUMNS;
            int row = slotIndex / CAMERA_GRID_COLUMNS;
            int imageX = getGridItemX(phoneX, column);
            int imageY = gridTop + row * CAMERA_GRID_CELL;

            if (isInside((int) mouseX, (int) mouseY, imageX, imageY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE)) {
                return i;
            }
        }

        return -1;
    }

    private int getPhotoIndexAt(double mouseX, double mouseY, int itemCount) {
        int phoneX = (this.width - PHONE_WIDTH) / 2;
        int phoneY = (this.height - PHONE_HEIGHT) / 2;
        int listTop = phoneY + LIST_TOP;
        int listBottom = getPhotoGridBottom(phoneY);
        int gridTop = listTop - albumScrollOffset;

        if (mouseY < listTop || mouseY > listBottom) {
            return -1;
        }

        for (int i = 0; i < itemCount; i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int imageX = getGridItemX(phoneX, column);
            int imageY = gridTop + row * CAMERA_GRID_CELL;

            if (isInside((int) mouseX, (int) mouseY, imageX, imageY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page == Page.CONVERSATION && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            sendMessage();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int getMaxContactScroll() {
        int contentHeight = scale(22) + getContacts().size() * CONTACT_ROW_HEIGHT;
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
            int contentHeight = getCameraAlbumGridHeight(getSelectedCameraAlbumViewItemCount());
            int viewportHeight = PHOTO_GRID_BOTTOM - LIST_TOP;
            return Math.max(0, contentHeight - viewportHeight);
        }

        if (page == Page.UPLOAD_ALBUM) {
            int contentHeight = getCameraAlbumGridHeight(getSelectedUploadAlbumPhotos().size());
            int viewportHeight = PHOTO_GRID_BOTTOM - LIST_TOP;
            return Math.max(0, contentHeight - viewportHeight);
        }

        int contentHeight = ALBUM_ROW_HEIGHT * (3 + getCameraAlbumGroups().size());
        int viewportHeight = LIST_BOTTOM - LIST_TOP;
        return Math.max(0, contentHeight - viewportHeight);
    }

    private int getSelectedCameraAlbumViewItemCount() {
        int count = getSelectedCameraPhotoEntries().size();
        return selectedCameraAlbumIndex != ALL_PHOTOS_ALBUM_INDEX ? count + 1 : count;
    }

    private int getMaxMessageScroll() {
        if (selectedContact == null) {
            return 0;
        }

        return getMaxMessageScroll(selectedContact.number());
    }

    private int getMaxMessageScroll(String number) {
        int contentHeight = 0;

        for (MessageEntry message : getMessages(number)) {
            contentHeight += getMessageBubbleHeight(message) + scale(4);
        }

        int viewportHeight = scale(109);
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

            if (!setupStatus.isEmpty()) {
                drawCenteredText(guiGraphics, trimToWidth(setupStatus, scale(92)), phoneX + PHONE_WIDTH / 2, phoneY + scale(166), TEXT_ERROR);
            }
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
            renderCameraAlbum(guiGraphics, phoneX, phoneY, mouseX, mouseY);
        } else if (page == Page.UPLOAD_ALBUM) {
            renderUploadAlbum(guiGraphics, phoneX, phoneY, mouseX, mouseY);
        } else if (page == Page.SEND_PHOTO) {
            renderSendPhoto(guiGraphics, phoneX, phoneY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderMessageHint(guiGraphics);
    }

    private String getTitle(CompoundTag tag) {
        if (!isSetup()) {
            return "Configuration";
        }

        return switch (page) {
            case CONTACTS -> "Contacts";
            case SMS -> "SMS";
            case ADD_CONTACT -> "Ajouter";
            case CONVERSATION -> selectedContact == null ? "" : trimToWidth(getConversationHeader(selectedContact), scale(100));
            case SEND_PHOTO -> "Envoyer";
            case CAMERA_ALBUM -> trimToWidth(getSelectedCameraAlbumTitle(), scale(100));
            case UPLOAD_ALBUM -> getSelectedUploadAlbumName();
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
            case CONVERSATION -> "";
            case SEND_PHOTO -> selectedPhoto == null ? "Choisir contact" : trimToWidth(selectedPhoto.title(), scale(86));
            case CAMERA_ALBUM -> getSelectedCameraPhotoEntries().size() + " photo(s)";
            case UPLOAD_ALBUM -> getSelectedUploadAlbumPhotos().size() + " photo(s)";
            case LOCKED -> "Verrouille";
            case ALBUMS -> getCameraAlbumGroups().size() + " album(s)";
            default -> tag.getString("number");
        };
    }

    private void renderContactList(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<ContactEntry> contacts = getContacts();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int rowY = listTop - scale(4) - contactScrollOffset;

        if (rowY + scale(20) >= listTop && rowY <= listBottom) {
            drawText(guiGraphics, "Ajouter un contact", phoneX + scale(18), rowY + scale(3), TEXT_PRIMARY);
            drawSeparator(guiGraphics, phoneX, rowY + scale(18));
        }
        rowY += scale(22);

        if (contacts.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucun contact", phoneX + PHONE_WIDTH / 2, rowY + scale(16), TEXT_SECONDARY);
            return;
        }

        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry contact = contacts.get(i);

            if (rowY + CONTACT_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                renderContactRow(guiGraphics, contact.name(), contact.number(), phoneX + scale(15), rowY);
                drawListSeparator(guiGraphics, phoneX, rowY + CONTACT_ROW_HEIGHT - 1);
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
                renderContactRow(guiGraphics, conversation.displayName(), conversation.preview(), phoneX + scale(15), rowY);
                drawListSeparator(guiGraphics, phoneX, rowY + CONTACT_ROW_HEIGHT - scale(3));
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

        List<MessageEntry> messages = getMessages(selectedContact.number());
        int listTop = phoneY + scale(42);
        int listBottom = phoneY + scale(151);
        int rowY = listTop - messageScrollOffset;

        if (messages.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucun message", phoneX + PHONE_WIDTH / 2, listTop + scale(28), TEXT_SECONDARY);
        }

        guiGraphics.enableScissor(phoneX + scale(13), listTop, phoneX + PHONE_WIDTH - scale(13), listBottom);

        try {
            for (MessageEntry message : messages) {
                int bubbleHeight = getMessageBubbleHeight(message);

                if (rowY + bubbleHeight >= listTop && rowY <= listBottom) {
                    renderMessageBubble(guiGraphics, message, phoneX, rowY);
                }

                rowY += bubbleHeight + scale(4);

                if (rowY > listBottom + scale(36)) {
                    break;
                }
            }
        } finally {
            guiGraphics.disableScissor();
        }

        drawSeparator(guiGraphics, phoneX, phoneY + scale(153));

        if (!messageStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(messageStatus, scale(86)), phoneX + PHONE_WIDTH / 2, phoneY + scale(141), messageStatusColor);
        }
    }

    private void renderAlbumList(GuiGraphics guiGraphics, int phoneX, int phoneY) {
        List<CameraAlbumGroup> albumGroups = getCameraAlbumGroups();
        int listTop = phoneY + LIST_TOP;
        int listBottom = phoneY + LIST_BOTTOM;
        int rowY = listTop - scale(5) - albumScrollOffset;

        if (rowY + ALBUM_ROW_HEIGHT >= listTop && rowY <= listBottom) {
            drawText(guiGraphics, "Ajouter un album", phoneX + scale(18), rowY + scale(5), TEXT_PRIMARY);
            drawListSeparator(guiGraphics, phoneX, rowY + ALBUM_ROW_HEIGHT - scale(2));
        }

        rowY += ALBUM_ROW_HEIGHT;

        if (rowY + ALBUM_ROW_HEIGHT >= listTop && rowY <= listBottom) {
            renderAlbumRow(guiGraphics, "Toutes mes photos", getCameraPhotoCount() + " photo(s)", phoneX, rowY, false);
        }

        rowY += ALBUM_ROW_HEIGHT;

        if (rowY + ALBUM_ROW_HEIGHT >= listTop && rowY <= listBottom) {
            renderAlbumRow(guiGraphics, "Non triees", getUnassignedCameraPhotoCount() + " photo(s)", phoneX, rowY, false);
        }

        rowY += ALBUM_ROW_HEIGHT;

        for (CameraAlbumGroup group : albumGroups) {

            if (rowY + ALBUM_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                renderAlbumRow(
                        guiGraphics,
                        editingAlbumIndex == group.id() ? "" : group.name(),
                        getCameraPhotoCountForGroup(group.id()) + " photo(s)",
                        phoneX,
                        rowY,
                        editingAlbumIndex == group.id()
                );
            }

            rowY += ALBUM_ROW_HEIGHT;

            if (rowY > listBottom + ALBUM_ROW_HEIGHT) {
                break;
            }
        }

        if (!photoStatus.isEmpty()) {
            drawCenteredText(guiGraphics, trimToWidth(photoStatus, scale(86)), phoneX + PHONE_WIDTH / 2, phoneY + scale(158), photoStatusColor);
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

    private List<CameraAlbumGroup> getCameraAlbumGroups() {
        List<CameraAlbumGroup> groups = new ArrayList<>();
        ListTag groupTags = getTag().getList(TAG_CAMERA_ALBUM_GROUPS, 10);

        for (int i = 0; i < groupTags.size(); i++) {
            CompoundTag groupTag = groupTags.getCompound(i);
            int id = groupTag.getInt("id");

            if (id < 0) {
                continue;
            }

            String name = groupTag.getString("name").trim();

            if (name.isEmpty()) {
                name = "Album " + (groups.size() + 1);
            }

            groups.add(new CameraAlbumGroup(id, name));
        }

        return groups;
    }

    private List<ItemStack> getSelectedCameraAlbumImages() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }

        if (selectedCameraAlbumIndex == ALL_PHOTOS_ALBUM_INDEX) {
            return CrazyPhoneCameraHelper.getCameraImages(minecraft.level.registryAccess(), stack);
        }

        List<ItemStack> albums = getCameraAlbums();

        if (selectedCameraAlbumIndex < 0 || selectedCameraAlbumIndex >= albums.size()) {
            return List.of();
        }

        return CrazyPhoneCameraHelper.getCameraImagesFromAlbum(minecraft.level.registryAccess(), albums.get(selectedCameraAlbumIndex));
    }

    private List<CameraPhotoEntry> getSelectedCameraPhotoEntries() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }

        List<CameraPhotoEntry> allEntries = getAllCameraPhotoEntries();

        if (selectedCameraAlbumIndex == ALL_PHOTOS_ALBUM_INDEX) {
            return allEntries;
        }

        List<CameraPhotoEntry> entries = new ArrayList<>();
        int targetGroupId = selectedCameraAlbumIndex;

        for (CameraPhotoEntry entry : allEntries) {
            int groupId = getCameraPhotoGroupId(entry.image());

            if (targetGroupId == UNASSIGNED_ALBUM_INDEX && groupId == UNASSIGNED_ALBUM_INDEX) {
                entries.add(entry);
            } else if (targetGroupId >= 0 && groupId == targetGroupId) {
                entries.add(entry);
            }
        }

        return entries;
    }

    private List<CameraPhotoEntry> getAllCameraPhotoEntries() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }

        List<CameraPhotoEntry> entries = new ArrayList<>();
        List<ItemStack> albums = getCameraAlbums();

        for (int albumIndex = 0; albumIndex < albums.size(); albumIndex++) {
            List<ItemStack> images = CrazyPhoneCameraHelper.getCameraImagesFromAlbum(minecraft.level.registryAccess(), albums.get(albumIndex));

            for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
                entries.add(new CameraPhotoEntry(albumIndex, imageIndex, images.get(imageIndex)));
            }
        }

        return entries;
    }

    private int getUnassignedCameraPhotoCount() {
        return getCameraPhotoCountForGroup(UNASSIGNED_ALBUM_INDEX);
    }

    private int getCameraPhotoCountForGroup(int groupId) {
        int count = 0;

        for (CameraPhotoEntry entry : getAllCameraPhotoEntries()) {
            if (getCameraPhotoGroupId(entry.image()) == groupId) {
                count++;
            }
        }

        return count;
    }

    private int getCameraPhotoGroupId(ItemStack image) {
        String imageId = getCameraImageId(image);

        if (imageId.isEmpty()) {
            return UNASSIGNED_ALBUM_INDEX;
        }

        ListTag photoGroups = getTag().getList(TAG_CAMERA_PHOTO_GROUPS, 10);

        for (int i = 0; i < photoGroups.size(); i++) {
            CompoundTag photoGroup = photoGroups.getCompound(i);

            if (imageId.equals(photoGroup.getString("imageId"))) {
                return photoGroup.getInt("groupId");
            }
        }

        return UNASSIGNED_ALBUM_INDEX;
    }

    private String getCameraImageId(ItemStack image) {
        ImageData data = ImageData.fromStack(image);
        return data == null ? "" : data.getId().toString();
    }

    private String getCameraAlbumName(ItemStack album, int albumIndex) {
        if (album.has(DataComponents.CUSTOM_NAME)) {
            return album.getHoverName().getString();
        }

        return "Album " + (albumIndex + 1);
    }

    private String getSelectedCameraAlbumTitle() {
        if (selectedCameraAlbumIndex == ALL_PHOTOS_ALBUM_INDEX) {
            return "Toutes mes photos";
        }

        if (selectedCameraAlbumIndex == UNASSIGNED_ALBUM_INDEX) {
            return "Non triees";
        }

        for (CameraAlbumGroup group : getCameraAlbumGroups()) {
            if (group.id() == selectedCameraAlbumIndex) {
                return group.name();
            }
        }

        return "Album";
    }

    private int getCameraAlbumPhotoCount(ItemStack album) {
        if (minecraft == null || minecraft.level == null || album.isEmpty()) {
            return 0;
        }

        return CrazyPhoneCameraHelper.getAlbumPhotoCount(minecraft.level.registryAccess(), album);
    }

    private int getCameraAlbumCapacity() {
        return CrazyPhoneCameraHelper.getAlbumCapacity();
    }

    private void createCameraAlbum() {
        PacketDistributor.sendToServer(new CrazyPhoneCameraAlbumActionPayload(mainHand, "create", -1, "", 0));
        photoStatus = "Album cree";
        photoStatusColor = TEXT_SUCCESS;
        rebuildWidgets();
    }

    private void toggleOrSaveAlbumEdit(int albumIndex) {
        if (editingAlbumIndex != albumIndex) {
            editingAlbumIndex = albumIndex;
            rebuildWidgets();
            return;
        }

        String name = albumNameField == null ? "" : albumNameField.getValue().trim();

        if (name.isEmpty()) {
            photoStatus = "Nom vide";
            photoStatusColor = TEXT_ERROR;
            return;
        }

        PacketDistributor.sendToServer(new CrazyPhoneCameraAlbumActionPayload(mainHand, "rename", albumIndex, name, 0));
        editingAlbumIndex = NO_EDITING_ALBUM;
        photoStatus = "Album renomme";
        photoStatusColor = TEXT_SUCCESS;
        rebuildWidgets();
    }

    private void openCameraImage(ItemStack image) {
        if (minecraft == null) {
            return;
        }

        minecraft.setScreen(new ReturningImageScreen(image.copy(), this));
    }

    private void openUploadedImage(PhotoEntry photo) {
        if (minecraft == null) {
            return;
        }

        minecraft.setScreen(new UploadedPhotoScreen(this, photo));
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

    private void renderCameraAlbum(GuiGraphics guiGraphics, int phoneX, int phoneY, int mouseX, int mouseY) {
        List<CameraPhotoEntry> images = getSelectedCameraPhotoEntries();
        int listTop = phoneY + LIST_TOP;
        int listBottom = getPhotoGridBottom(phoneY);
        int gridTop = listTop - albumScrollOffset;
        ItemStack hoveredImage = ItemStack.EMPTY;
        boolean hasUploadSlot = selectedCameraAlbumIndex != ALL_PHOTOS_ALBUM_INDEX;

        selectedCameraPhotoIndexes.removeIf(index -> index >= images.size());

        if (hasUploadSlot) {
            int uploadX = getGridItemX(phoneX, 0);
            int uploadY = gridTop;

            if (uploadY + CAMERA_GRID_CELL >= listTop && uploadY <= listBottom) {
                guiGraphics.blit(CAMERA_UPLOAD_PHOTO, uploadX + 1, uploadY + 1, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE, 0, 0, 16, 16, 16, 16);
            }
        }

        if (images.isEmpty() && !hasUploadSlot) {
            drawCenteredText(guiGraphics, "Aucune photo", phoneX + PHONE_WIDTH / 2, gridTop + scale(16), TEXT_SECONDARY);
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            int slotIndex = hasUploadSlot ? i + 1 : i;
            int column = slotIndex % CAMERA_GRID_COLUMNS;
            int row = slotIndex / CAMERA_GRID_COLUMNS;
            int imageX = getGridItemX(phoneX, column);
            int imageY = gridTop + row * CAMERA_GRID_CELL;
            ItemStack image = images.get(i).image();

            if (imageY + CAMERA_GRID_CELL >= listTop && imageY <= listBottom) {
                if (selectedCameraPhotoIndexes.contains(i)) {
                    renderSelectedSlot(guiGraphics, imageX, imageY);
                }

                renderCameraPhotoItem(guiGraphics, image, imageX, imageY);

                if (isInside(mouseX, mouseY, imageX, imageY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE)) {
                    hoveredImage = image;
                }
            }
        }

        if (!hoveredImage.isEmpty()) {
            renderCameraSidePreview(guiGraphics, hoveredImage, phoneX, phoneY);
        }
    }

    private void renderUploadAlbum(GuiGraphics guiGraphics, int phoneX, int phoneY, int mouseX, int mouseY) {
        List<PhotoEntry> photos = getSelectedUploadAlbumPhotos();
        int listTop = phoneY + LIST_TOP;
        int listBottom = getPhotoGridBottom(phoneY);
        int gridTop = listTop - albumScrollOffset;
        PhotoEntry hoveredPhoto = null;

        selectedUploadPhotoIndexes.removeIf(index -> index >= photos.size());

        if (photos.isEmpty()) {
            drawCenteredText(guiGraphics, "Aucune photo", phoneX + PHONE_WIDTH / 2, gridTop + scale(16), TEXT_SECONDARY);
            return;
        }

        for (int i = 0; i < photos.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int imageX = getGridItemX(phoneX, column);
            int imageY = gridTop + row * CAMERA_GRID_CELL;
            PhotoEntry photo = photos.get(i);

            if (imageY + CAMERA_GRID_CELL >= listTop && imageY <= listBottom) {
                if (selectedUploadPhotoIndexes.contains(i)) {
                    renderSelectedSlot(guiGraphics, imageX, imageY);
                }

                renderUploadedPhotoItem(guiGraphics, photo, imageX, imageY);

                if (isInside(mouseX, mouseY, imageX, imageY, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE)) {
                    hoveredPhoto = photo;
                }
            }
        }

        if (hoveredPhoto != null) {
            renderUploadedSidePreview(guiGraphics, hoveredPhoto, phoneX, phoneY);
        }
    }

    private int getCameraAlbumGridHeight(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }

        int rows = (itemCount + CAMERA_GRID_COLUMNS - 1) / CAMERA_GRID_COLUMNS;
        return rows * CAMERA_GRID_CELL + scale(4);
    }

    private int getGridItemX(int phoneX, int column) {
        int gridLeft = phoneX + scale(12);
        int gridWidth = PHONE_WIDTH - scale(24);
        int columnWidth = gridWidth / CAMERA_GRID_COLUMNS;
        return gridLeft + column * columnWidth + (columnWidth - PHOTO_THUMBNAIL_SIZE) / 2;
    }

    private int getPhotoGridBottom(int phoneY) {
        return phoneY + PHOTO_GRID_BOTTOM;
    }

    private void renderCameraAlbumGrid(GuiGraphics guiGraphics, List<ItemStack> albums, int phoneX, int y, int listTop, int listBottom) {
        for (int i = 0; i < albums.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int albumX = getGridItemX(phoneX, column);
            int albumY = y + row * CAMERA_GRID_CELL;

            if (albumY + CAMERA_GRID_CELL >= listTop && albumY <= listBottom) {
                renderCameraPhotoItem(guiGraphics, albums.get(i), albumX, albumY);
            }
        }
    }

    private void renderUploadAlbumGrid(GuiGraphics guiGraphics, List<UploadAlbumEntry> albums, int phoneX, int y, int listTop, int listBottom) {
        for (int i = 0; i < albums.size(); i++) {
            int column = i % CAMERA_GRID_COLUMNS;
            int row = i / CAMERA_GRID_COLUMNS;
            int albumX = getGridItemX(phoneX, column);
            int albumY = y + row * CAMERA_GRID_CELL;

            if (albumY + CAMERA_GRID_CELL >= listTop && albumY <= listBottom) {
                guiGraphics.blit(ALBUM_ICON, albumX + 1, albumY + 1, PHOTO_THUMBNAIL_SIZE, PHOTO_THUMBNAIL_SIZE, 0, 0, 52, 62, 52, 62);
                drawCenteredText(guiGraphics, trimToWidth(albums.get(i).name(), scale(18)), albumX + PHOTO_THUMBNAIL_SIZE / 2, albumY + PHOTO_THUMBNAIL_SIZE, TEXT_PRIMARY);
            }
        }
    }

    private void renderAlbumRow(GuiGraphics guiGraphics, String name, String count, int phoneX, int y, boolean editing) {
        if (!editing) {
            drawText(guiGraphics, trimToWidth(name, scale(78)), phoneX + scale(18), y + scale(2), TEXT_PRIMARY);
            drawText(guiGraphics, trimToWidth(count, scale(72)), phoneX + scale(18), y + scale(12), TEXT_SECONDARY);
        }

        drawListSeparator(guiGraphics, phoneX, y + ALBUM_ROW_HEIGHT - scale(2));
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

    private void renderSelectedSlot(GuiGraphics guiGraphics, int x, int y) {
        int slotX = x + (PHOTO_THUMBNAIL_SIZE - PHOTO_SELECTION_SIZE) / 2;
        int slotY = y + (PHOTO_THUMBNAIL_SIZE - PHOTO_SELECTION_SIZE) / 2;
        guiGraphics.blit(SLOT_SELECTED, slotX, slotY, PHOTO_SELECTION_SIZE, PHOTO_SELECTION_SIZE, 0, 0, 18, 18, 18, 18);
    }

    private void renderCameraPhotoItem(GuiGraphics guiGraphics, ItemStack image, int x, int y) {
        guiGraphics.renderItem(image, x + 1, y + 1);
    }

    private void renderCameraSidePreview(GuiGraphics guiGraphics, ItemStack image, int phoneX, int phoneY) {
        int previewSize = scale(58);
        int previewX = Math.max(scale(4), phoneX - previewSize - scale(8));
        int previewY = phoneY + LIST_TOP;

        ImageData data = ImageData.fromStack(image);

        if (data == null || minecraft == null) {
            renderScaledItem(guiGraphics, image, previewX, previewY, previewSize);
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(previewX, previewY, 0.0F);
        ImageScreen.drawImage(guiGraphics, minecraft, previewSize, previewSize, 200.0F, data.getId());
        guiGraphics.pose().popPose();
    }

    private void renderUploadedSidePreview(GuiGraphics guiGraphics, PhotoEntry photo, int phoneX, int phoneY) {
        UploadedTexture uploadedTexture = getUploadedTexture(photo.texture());

        if (uploadedTexture == null) {
            return;
        }

        int previewSize = scale(58);
        int previewX = Math.max(scale(4), phoneX - previewSize - scale(8));
        int previewY = phoneY + LIST_TOP;

        renderUploadedTextureFit(guiGraphics, uploadedTexture, previewX, previewY, previewSize);
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack image, int x, int y, int size) {
        float itemScale = size / 16.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(itemScale, itemScale, 1.0F);
        guiGraphics.renderItem(image, 0, 0);
        guiGraphics.pose().popPose();
    }

    private UploadedTexture getUploadedTexture(String path) {
        if (minecraft == null || path == null || path.isEmpty()) {
            return null;
        }

        UploadedTexture cached = uploadedTextures.get(path);

        if (cached != null) {
            return cached;
        }

        try (FileInputStream inputStream = new FileInputStream(path)) {
            NativeImage image = NativeImage.read(inputStream);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    "asmpthingsmod",
                    "dynamic/crazyphone_upload/" + Integer.toHexString(path.hashCode())
            );
            minecraft.getTextureManager().register(location, new DynamicTexture(image));
            UploadedTexture uploadedTexture = new UploadedTexture(location, image.getWidth(), image.getHeight());
            uploadedTextures.put(path, uploadedTexture);
            return uploadedTexture;
        } catch (IOException exception) {
            return null;
        }
    }

    private void renderUploadedPhotoItem(GuiGraphics guiGraphics, PhotoEntry photo, int x, int y) {
        UploadedTexture uploadedTexture = getUploadedTexture(photo.texture());

        if (uploadedTexture == null) {
            guiGraphics.fill(x + 1, y + 1, x + scale(17), y + scale(17), 0xFF324154);
            return;
        }

        renderUploadedTextureFit(guiGraphics, uploadedTexture, x + 1, y + 1, PHOTO_THUMBNAIL_SIZE);
    }

    private void renderUploadedTextureFit(GuiGraphics guiGraphics, UploadedTexture uploadedTexture, int x, int y, int size) {
        float imageRatio = (float) uploadedTexture.width() / uploadedTexture.height();
        int drawWidth = size;
        int drawHeight = Math.round(drawWidth / imageRatio);

        if (drawHeight > size) {
            drawHeight = size;
            drawWidth = Math.round(drawHeight * imageRatio);
        }

        int drawX = x + (size - drawWidth) / 2;
        int drawY = y + (size - drawHeight) / 2;

        guiGraphics.blit(
                uploadedTexture.location(),
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                0,
                0,
                uploadedTexture.width(),
                uploadedTexture.height(),
                uploadedTexture.width(),
                uploadedTexture.height()
        );
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void drawSeparator(GuiGraphics guiGraphics, int phoneX, int y) {
        guiGraphics.fill(phoneX + scale(17), y, phoneX + PHONE_WIDTH - scale(17), y + 1, 0x66FFFFFF);
    }

    private void drawListSeparator(GuiGraphics guiGraphics, int phoneX, int y) {
        guiGraphics.fill(phoneX + scale(24), y, phoneX + PHONE_WIDTH - scale(24), y + 1, 0x55FFFFFF);
    }

    private void renderContactRow(GuiGraphics guiGraphics, String name, String number, int x, int y) {
        drawText(guiGraphics, trimToWidth(name, scale(92)), x, y + scale(1), TEXT_PRIMARY);
        drawText(guiGraphics, trimToWidth(number, scale(92)), x, y + scale(8), TEXT_SECONDARY);
    }

    private void renderMessageBubble(GuiGraphics guiGraphics, MessageEntry message, int phoneX, int y) {
        int bubbleWidth = scale(68);

        if (message.isPhoto()) {
            renderMessagePhoto(guiGraphics, message, getMessagePhotoX(message, phoneX), y, getMessagePhotoSize());
            return;
        }

        int bubbleX = message.outgoing()
                ? phoneX + PHONE_WIDTH - scale(9) - bubbleWidth
                : phoneX + scale(12);
        int textPadding = message.outgoing() ? scale(4) : scale(7);
        int color = message.outgoing() ? 0xAA2F7D56 : 0xAA2E4F72;

        List<String> lines = wrapText(message.text(), getMessageTextWidth(textPadding));
        int bubbleHeight = getMessageBubbleHeight(lines.size());

        guiGraphics.fill(bubbleX, y, bubbleX + bubbleWidth, y + bubbleHeight, color);

        int lineY = y + scale(4);
        for (String line : lines) {
            drawScaledText(guiGraphics, line, bubbleX + textPadding, lineY, TEXT_PRIMARY, MESSAGE_TEXT_SCALE);
            lineY += getMessageLineHeight();
        }
    }

    private void renderMessagePhoto(GuiGraphics guiGraphics, MessageEntry message, int x, int y, int size) {
        if ("camera_photo".equals(message.kind()) && !message.image().isEmpty()) {
            if (renderCameraTextureCover(guiGraphics, message.image(), x, y, size)) {
                return;
            }

            renderScaledItem(guiGraphics, message.image(), x, y, size);
            return;
        }

        if (message.photo() != null) {
            UploadedTexture uploadedTexture = getUploadedTexture(message.photo().texture());

            if (uploadedTexture != null) {
                renderUploadedTextureFit(guiGraphics, uploadedTexture, x, y, size);
                return;
            }
        }

        guiGraphics.fill(x, y, x + size, y + size, 0xFF324154);
        drawCenteredText(guiGraphics, "Photo", x + size / 2, y + size / 2 - this.font.lineHeight / 2, TEXT_SECONDARY);
    }

    private int getMessagePhotoX(MessageEntry message, int phoneX) {
        int size = getMessagePhotoSize();
        return message.outgoing()
                ? phoneX + PHONE_WIDTH - scale(9) - size
                : phoneX + scale(9);
    }

    private int getMessagePhotoSize() {
        return MESSAGE_PHOTO_SIZE;
    }

    private boolean renderCameraTextureCover(GuiGraphics guiGraphics, ItemStack image, int x, int y, int size) {
        ImageData data = ImageData.fromStack(image);

        if (data == null) {
            return false;
        }

        ResourceLocation texture = TextureCache.instance().getImage(data.getId());
        NativeImage nativeImage = TextureCache.instance().getNativeImage(data.getId());

        if (texture == null || nativeImage == null) {
            return false;
        }

        int textureWidth = nativeImage.getWidth();
        int textureHeight = nativeImage.getHeight();
        int sourceX = 0;
        int sourceY = 0;
        int sourceSize;

        if (textureWidth > textureHeight) {
            sourceSize = textureHeight;
            sourceX = (textureWidth - sourceSize) / 2;
        } else {
            sourceSize = textureWidth;
            sourceY = (textureHeight - sourceSize) / 2;
        }

        guiGraphics.blit(
                texture,
                x,
                y,
                size,
                size,
                sourceX,
                sourceY,
                sourceSize,
                sourceSize,
                textureWidth,
                textureHeight
        );
        return true;
    }

    private void drawText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x, y, color, true);
    }

    private void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, int color, float textScale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(textScale, textScale, 1.0f);
        guiGraphics.drawString(this.font, text, Math.round(x / textScale), Math.round(y / textScale), color, true);
        guiGraphics.pose().popPose();
    }

    private void drawCenteredText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x - this.font.width(text) / 2, y, color, true);
    }

    private void renderMessageHint(GuiGraphics guiGraphics) {
        if (page != Page.CONVERSATION || messageField == null || !messageField.getValue().isEmpty()) {
            return;
        }

        drawScaledText(
                guiGraphics,
                "Envoyer un message...",
                messageField.getX() + scale(4),
                messageField.getY() + scale(5),
                0xA8D8ECFF,
                MESSAGE_HINT_SCALE
        );
    }

    private String getConversationHeader(ContactEntry contact) {
        String name = contact.name();

        if (name == null || name.isBlank() || name.equals(contact.number()) || "Inconnu".equalsIgnoreCase(name)) {
            name = "Inconnu";
        }

        return name + " (" + contact.number() + ")";
    }

    private int getMessageBubbleHeight(MessageEntry message) {
        if (message.isPhoto()) {
            return MESSAGE_PHOTO_SIZE;
        }

        int textPadding = message.outgoing() ? scale(4) : scale(7);
        return getMessageBubbleHeight(wrapText(message.text(), getMessageTextWidth(textPadding)).size());
    }

    private int getMessageBubbleHeight(int lineCount) {
        return scale(8) + lineCount * getMessageLineHeight();
    }

    private int getMessageLineHeight() {
        return Math.max(1, Math.round(this.font.lineHeight * MESSAGE_TEXT_SCALE) + 1);
    }

    private int getMessageTextWidth() {
        return getMessageTextWidth(scale(4));
    }

    private int getMessageTextWidth(int textPadding) {
        return Math.round((scale(68) - textPadding - scale(4)) / MESSAGE_TEXT_SCALE);
    }

    private List<String> wrapText(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String text = value == null ? "" : value.trim();

        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }

        for (String paragraph : text.split("\\R", -1)) {
            wrapParagraph(paragraph, maxWidth, lines);
        }

        return lines;
    }

    private void wrapParagraph(String paragraph, int maxWidth, List<String> lines) {
        String remaining = paragraph.trim();

        if (remaining.isEmpty()) {
            lines.add("");
            return;
        }

        StringBuilder line = new StringBuilder();

        for (String word : remaining.split("\\s+")) {
            if (line.isEmpty()) {
                appendWrappedWord(word, maxWidth, lines, line);
                continue;
            }

            String candidate = line + " " + word;

            if (this.font.width(candidate) <= maxWidth) {
                line.append(" ").append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                appendWrappedWord(word, maxWidth, lines, line);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
    }

    private void appendWrappedWord(String word, int maxWidth, List<String> lines, StringBuilder line) {
        String remaining = word;

        while (this.font.width(remaining) > maxWidth && remaining.length() > 1) {
            String part = this.font.plainSubstrByWidth(remaining, maxWidth);

            if (part.isEmpty()) {
                break;
            }

            lines.add(part);
            remaining = remaining.substring(part.length());
        }

        line.append(remaining);
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
        UPLOAD_ALBUM,
        SEND_PHOTO,
        LOCKED
    }

    private record ContactEntry(String uuid, String name, String number) {
    }

    private record PhotoEntry(String title, String texture, String type) {
    }

    private record UploadAlbumEntry(String name, int start, int end) {
    }

    private record CameraAlbumGroup(int id, String name) {
    }

    private record UploadedTexture(ResourceLocation location, int width, int height) {
    }

    private record CameraPhotoEntry(int albumIndex, int imageIndex, ItemStack image) {
    }

    private record ConversationEntry(String number, String displayName, String preview) {
    }

    private record MessageEntry(String text, boolean outgoing, String kind, ItemStack image, PhotoEntry photo) {
        private boolean isPhoto() {
            return "camera_photo".equals(kind) || "uploaded_photo".equals(kind);
        }
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
                renderTexture(guiGraphics, this.texture);
                return;
            }

            int color = this.isHoveredOrFocused() ? 0xFFE6F2FF : 0xFFFFFFFF;
            int centerX = this.getX() + this.getWidth() / 2;
            int centerY = this.getY() + this.getHeight() / 2;
            guiGraphics.fill(this.getX() + 4, centerY - 2, this.getX() + this.getWidth() - 4, centerY + 2, color);
            guiGraphics.fill(centerX - 2, this.getY() + 4, centerX + 2, this.getY() + this.getHeight() - 4, color);
        }

        protected void renderTexture(GuiGraphics guiGraphics, ResourceLocation texture) {
            if (texture != null) {
                guiGraphics.blit(
                        texture,
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
            }
        }
    }

    private static class HoverTextureButton extends TextureButton {
        private final ResourceLocation hoverTexture;

        HoverTextureButton(
                int x,
                int y,
                int width,
                int height,
                ResourceLocation texture,
                ResourceLocation hoverTexture,
                int textureWidth,
                int textureHeight,
                Component message,
                OnPress onPress
        ) {
            super(x, y, width, height, texture, textureWidth, textureHeight, message, onPress);
            this.hoverTexture = hoverTexture;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderTexture(guiGraphics, this.isHoveredOrFocused() ? hoverTexture : null);
            if (!this.isHoveredOrFocused()) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private static class RevealedTextureButton extends HoverTextureButton {
        private final BooleanSupplier visibleSupplier;

        RevealedTextureButton(
                int x,
                int y,
                int width,
                int height,
                ResourceLocation texture,
                ResourceLocation hoverTexture,
                int textureWidth,
                int textureHeight,
                Component message,
                OnPress onPress,
                BooleanSupplier visibleSupplier
        ) {
            super(x, y, width, height, texture, hoverTexture, textureWidth, textureHeight, message, onPress);
            this.visibleSupplier = visibleSupplier;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visibleSupplier.getAsBoolean() || super.isMouseOver(mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (visibleSupplier.getAsBoolean() || super.isMouseOver(mouseX, mouseY)) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private static class ReturningImageScreen extends ImageScreen {
        private final Screen parent;

        ReturningImageScreen(ItemStack image, Screen parent) {
            super(image);
            this.parent = parent;
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            onClose();
            return true;
        }
    }

    private static class UploadedPhotoScreen extends Screen {
        private final CrazyPhoneScreen parent;
        private final PhotoEntry photo;

        UploadedPhotoScreen(CrazyPhoneScreen parent, PhotoEntry photo) {
            super(Component.literal(photo.title()));
            this.parent = parent;
            this.photo = photo;
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            onClose();
            return true;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderTransparentBackground(guiGraphics);

            UploadedTexture texture = parent.getUploadedTexture(photo.texture());

            if (texture == null) {
                guiGraphics.drawCenteredString(
                        parent.font,
                        "Image introuvable",
                        width / 2,
                        height / 2,
                        TEXT_ERROR
                );
                return;
            }

            int maxWidth = Math.round(width * 0.8f);
            int maxHeight = Math.round(height * 0.8f);
            float imageRatio = (float) texture.width() / texture.height();
            int drawWidth = maxWidth;
            int drawHeight = Math.round(drawWidth / imageRatio);

            if (drawHeight > maxHeight) {
                drawHeight = maxHeight;
                drawWidth = Math.round(drawHeight * imageRatio);
            }

            int x = (width - drawWidth) / 2;
            int y = (height - drawHeight) / 2;

            guiGraphics.blit(
                    texture.location(),
                    x,
                    y,
                    drawWidth,
                    drawHeight,
                    0,
                    0,
                    texture.width(),
                    texture.height(),
                    texture.width(),
                    texture.height()
            );
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
