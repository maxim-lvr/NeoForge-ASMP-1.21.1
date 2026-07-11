package net.maximlvr.asmpthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class CardReaderBlockEntity extends BlockEntity {
    private UUID owner;
    private String targetAccountId = "";
    private int amount = 1;

    public CardReaderBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CARD_READER.get(), pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (owner != null) {
            tag.putUUID("owner", owner);
        }

        tag.putString("targetAccountId", targetAccountId);
        tag.putInt("amount", amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        targetAccountId = tag.getString("targetAccountId");
        amount = Math.max(1, tag.getInt("amount"));
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public String getTargetAccountId() {
        return targetAccountId;
    }

    public int getAmount() {
        return amount;
    }

    public void configure(String targetAccountId, int amount) {
        this.targetAccountId = targetAccountId;
        this.amount = Math.max(1, amount);
        setChanged();
    }
}
