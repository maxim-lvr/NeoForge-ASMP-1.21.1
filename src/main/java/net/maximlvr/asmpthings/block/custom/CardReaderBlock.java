package net.maximlvr.asmpthings.block.custom;

import com.mojang.serialization.MapCodec;
import net.maximlvr.asmpthings.bank.BankSavedData;
import net.maximlvr.asmpthings.block.entity.CardReaderBlockEntity;
import net.maximlvr.asmpthings.block.entity.ModBlockEntities;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.payload.OpenCardReaderConfigPayload;
import net.maximlvr.asmpthings.network.payload.OpenCardReaderPinPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class CardReaderBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CardReaderBlock> CODEC = simpleCodec(CardReaderBlock::new);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final int PAYMENT_COOLDOWN_TICKS = 60;
    private static final VoxelShape WALL_NORTH = Block.box(5.0D, 3.0D, 0.0D, 12.0D, 13.0D, 2.0D);
    private static final VoxelShape WALL_SOUTH = Block.box(4.0D, 3.0D, 14.0D, 11.0D, 13.0D, 16.0D);
    private static final VoxelShape WALL_WEST = Block.box(0.0D, 3.0D, 4.0D, 2.0D, 13.0D, 11.0D);
    private static final VoxelShape WALL_EAST = Block.box(14.0D, 3.0D, 5.0D, 16.0D, 13.0D, 12.0D);
    private static final VoxelShape FLOOR_NORTH = Block.box(5.0D, 0.0D, 3.0D, 12.0D, 2.0D, 13.0D);
    private static final VoxelShape FLOOR_EAST = Block.box(3.0D, 0.0D, 5.0D, 13.0D, 2.0D, 12.0D);
    private static final VoxelShape FLOOR_SOUTH = Block.box(4.0D, 0.0D, 3.0D, 11.0D, 2.0D, 13.0D);
    private static final VoxelShape FLOOR_WEST = Block.box(3.0D, 0.0D, 4.0D, 13.0D, 2.0D, 11.0D);
    private static final VoxelShape CEILING_NORTH = Block.box(5.0D, 14.0D, 3.0D, 12.0D, 16.0D, 13.0D);
    private static final VoxelShape CEILING_EAST = Block.box(3.0D, 14.0D, 5.0D, 13.0D, 16.0D, 12.0D);
    private static final VoxelShape CEILING_SOUTH = Block.box(4.0D, 14.0D, 3.0D, 11.0D, 16.0D, 13.0D);
    private static final VoxelShape CEILING_WEST = Block.box(3.0D, 14.0D, 4.0D, 13.0D, 16.0D, 11.0D);

    public CardReaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACE, AttachFace.WALL)
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, POWERED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttachFace face = state.getValue(FACE);
        Direction facing = state.getValue(FACING);

        if (face == AttachFace.FLOOR) {
            return switch (facing) {
                case SOUTH -> FLOOR_SOUTH;
                case WEST -> FLOOR_WEST;
                case EAST -> FLOOR_EAST;
                default -> FLOOR_NORTH;
            };
        }

        if (face == AttachFace.CEILING) {
            return switch (facing) {
                case SOUTH -> CEILING_SOUTH;
                case WEST -> CEILING_WEST;
                case EAST -> CEILING_EAST;
                default -> CEILING_NORTH;
            };
        }

        return switch (facing) {
            case SOUTH -> WALL_NORTH;
            case WEST -> WALL_EAST;
            case EAST -> WALL_WEST;
            default -> WALL_SOUTH;
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof CardReaderBlockEntity blockEntity) {
            blockEntity.setOwner(player.getUUID());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ModItems.CRAZY_COIN.get())) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                payWithCrazyCoins(serverPlayer, level, pos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && ModItems.isBankCard(stack)) {
            if (level.getBlockEntity(pos) instanceof CardReaderBlockEntity blockEntity
                    && !blockEntity.canAcceptPayment(level.getGameTime(), PAYMENT_COOLDOWN_TICKS)) {
                player.displayClientMessage(Component.literal("Lecteur en attente."), true);
            } else {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCardReaderPinPayload(pos));
            }
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

    private void payWithCrazyCoins(ServerPlayer player, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CardReaderBlockEntity blockEntity)) {
            return;
        }

        BankSavedData bank = BankSavedData.get(player.server);

        if (blockEntity.getTargetAccountId().isEmpty() || bank.getAccount(blockEntity.getTargetAccountId()) == null) {
            player.displayClientMessage(Component.literal("Lecteur non configure."), true);
            return;
        }

        long gameTime = level.getGameTime();

        if (!blockEntity.canAcceptPayment(gameTime, PAYMENT_COOLDOWN_TICKS)) {
            player.displayClientMessage(Component.literal("Lecteur en attente."), true);
            return;
        }

        int amount = blockEntity.getAmount();

        if (countCrazyCoins(player) < amount) {
            player.displayClientMessage(Component.literal("Pas assez de crazycoins sur toi."), true);
            return;
        }

        removeCrazyCoins(player, amount);
        bank.deposit(blockEntity.getTargetAccountId(), amount);
        blockEntity.markPayment(gameTime);
        triggerPaymentSignal(level, pos);
        player.displayClientMessage(Component.literal("Paiement accepte."), true);
    }

    private int countCrazyCoins(Player player) {
        int count = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CRAZY_COIN.get())) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private void removeCrazyCoins(Player player, int amount) {
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

    public static void triggerPaymentSignal(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof CardReaderBlock && !state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            level.scheduleTick(pos, state.getBlock(), 20);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CardReaderBlockEntity(pos, state);
    }
}
