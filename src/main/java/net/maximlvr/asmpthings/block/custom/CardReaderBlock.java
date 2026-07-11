package net.maximlvr.asmpthings.block.custom;

import net.maximlvr.asmpthings.block.entity.CardReaderBlockEntity;
import net.maximlvr.asmpthings.block.entity.ModBlockEntities;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.payload.OpenCardReaderConfigPayload;
import net.maximlvr.asmpthings.network.payload.OpenCardReaderPinPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class CardReaderBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public CardReaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof CardReaderBlockEntity blockEntity) {
            blockEntity.setOwner(player.getUUID());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && ModItems.isBankCard(stack)) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenCardReaderPinPayload(pos));
        }

        if (ModItems.isBankCard(stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CardReaderBlockEntity blockEntity) {
            if (blockEntity.getOwner() == null) {
                blockEntity.setOwner(player.getUUID());
            }

            if (player.getUUID().equals(blockEntity.getOwner())) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCardReaderConfigPayload(
                        pos,
                        blockEntity.getTargetAccountId(),
                        blockEntity.getAmount()
                ));
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Seul le proprietaire peut modifier ce lecteur."), true);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
            level.updateNeighborsAt(pos, this);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CardReaderBlockEntity(pos, state);
    }
}
