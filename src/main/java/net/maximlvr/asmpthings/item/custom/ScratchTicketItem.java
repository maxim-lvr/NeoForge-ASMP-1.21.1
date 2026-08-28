package net.maximlvr.asmpthings.item.custom;

import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.network.payload.OpenScratchTicketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

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
                int generatedPrize = ScratchTicketPrize.generate(level.random);
                stack.set(ModDataComponents.SCRATCH_PRIZE, generatedPrize);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                int finalPrize = stack.getOrDefault(ModDataComponents.SCRATCH_PRIZE, -1);

                PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new OpenScratchTicketPayload(
                                usedHand == InteractionHand.MAIN_HAND,
                                finalPrize
                        )
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

}
