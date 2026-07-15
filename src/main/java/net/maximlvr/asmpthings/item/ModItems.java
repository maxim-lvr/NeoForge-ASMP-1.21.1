package net.maximlvr.asmpthings.item;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.item.custom.BlueCardItem;
import net.maximlvr.asmpthings.item.custom.CrazyPhoneItem;
import net.maximlvr.asmpthings.item.custom.ScratchTicketItem;
import net.maximlvr.asmpthings.item.custom.WeatherStaff;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AsmpThingsMod.MOD_ID);

    public static final DeferredItem<Item> CORONA = ITEMS.register("corona",
            () -> new Item(new Item.Properties().food(ModFoods.BEER)));

    public static final DeferredItem<Item> DESPERADOS = ITEMS.register("desperados",
            () -> new Item(new Item.Properties().food(ModFoods.BEER)));

    public static final DeferredItem<Item> HUIT_SIX = ITEMS.register("huit_six",
            () -> new Item(new Item.Properties().food(ModFoods.BEER)));

    public static final DeferredItem<Item> KRONENBOURG = ITEMS.register("kronenbourg",
            () -> new Item(new Item.Properties().food(ModFoods.BEER)));

    public static final DeferredItem<Item> CRAZY_COIN = ITEMS.register("crazy_coin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> GOLDEN_NUT = ITEMS.register("golden_nut",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COSMECOIN = ITEMS.register("cosmecoin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WEATHER_STAFF = ITEMS.register("weather_staff",
            () -> new WeatherStaff(new Item.Properties().durability(32)));

    public static final DeferredItem<Item> WEATHER_TANK = ITEMS.register("weather_tank",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CRAZY_PHONE_14 = ITEMS.register("crazy_phone_14",
            () -> new CrazyPhoneItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CRAZY_PHONE_GOLDEN = ITEMS.register("crazy_phone_golden",
            () -> new CrazyPhoneItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CRAZY_PHONE_BLUE_NIGHT = ITEMS.register("crazy_phone_blue_night",
            () -> new CrazyPhoneItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLUE_CARD = ITEMS.register("blue_card",
            () -> new BlueCardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RED_CARD = ITEMS.register("red_card",
            () -> new BlueCardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> GREEN_CARD = ITEMS.register("green_card",
            () -> new BlueCardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLACK_CARD = ITEMS.register("black_card",
            () -> new BlueCardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> GREY_CARD = ITEMS.register("grey_card",
            () -> new BlueCardItem(new Item.Properties().stacksTo(1)));

    public static boolean isBankCard(ItemStack stack) {
        return stack.is(BLUE_CARD.get())
                || stack.is(RED_CARD.get())
                || stack.is(GREEN_CARD.get())
                || stack.is(BLACK_CARD.get())
                || stack.is(GREY_CARD.get());
    }

    public static boolean isCrazyPhone(ItemStack stack) {
        return stack.is(CRAZY_PHONE_14.get())
                || stack.is(CRAZY_PHONE_GOLDEN.get())
                || stack.is(CRAZY_PHONE_BLUE_NIGHT.get());
    }

    public static final DeferredItem<Item> GOAL_SMALL_TICKET = ITEMS.register("card_goal",
            () -> new ScratchTicketItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .component(ModDataComponents.SCRATCH_DATA.get(), "")
                            .component(ModDataComponents.SCRATCH_PRIZE.get(), -1)
            ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
