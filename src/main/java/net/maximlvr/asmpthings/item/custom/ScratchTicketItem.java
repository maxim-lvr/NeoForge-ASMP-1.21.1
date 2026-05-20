package net.maximlvr.asmpthings.item.custom;

import net.maximlvr.asmpthings.component.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ScratchTicketItem extends Item {
    public ScratchTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide()) {
            int prize = stack.getOrDefault(ModDataComponents.SCRATCH_PRIZE, -1);

            if (prize == -1) {
                int generatedPrize = generatePrize(level.random);
                stack.set(ModDataComponents.SCRATCH_PRIZE, generatedPrize);
            }

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                int finalPrize = stack.getOrDefault(ModDataComponents.SCRATCH_PRIZE, -1);

                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new net.maximlvr.asmpthings.network.payload.OpenScratchTicketPayload(
                                usedHand == InteractionHand.MAIN_HAND,
                                finalPrize
                        )
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private int generatePrize(RandomSource random) {
        int roll = random.nextInt(10);

        if (roll == 0) {
            return 10;
        }

        if (roll <= 2) {
            return 5;
        }

        if (roll <= 5) {
            return 1;
        }

        return 0;
    }
}