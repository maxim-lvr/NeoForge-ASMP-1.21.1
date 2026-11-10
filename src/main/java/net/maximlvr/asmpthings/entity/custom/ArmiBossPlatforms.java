package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.maximlvr.asmpthings.block.custom.ArmiPlatformBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ArmiBossPlatforms {
    private static volatile Vec3 clientCenter;

    private ArmiBossPlatforms() {}

    public static void setClientCenter(Vec3 center) {
        clientCenter = center;
    }

    public static int heldColors(Player player) {
        int colors = 0;
        for (var stack : player.getHandSlots()) {
            if (stack.is(Items.OAK_LOG)) colors |= 1;
            if (stack.is(Items.OAK_DOOR)) colors |= 2;
        }
        return colors;
    }

    public static boolean hidden(BlockState state, BlockPos pos, Player player) {
        int color = state.getBlock() instanceof ArmiPlatformBlock platform ? platform.color() : 0;
        if (color == 0 || player == null) return false;
        Vec3 center = player.level().isClientSide ? clientCenter : ArmiBossEntity.combatCenter(player);
        return center != null && Math.abs(pos.getX() - Math.floor(center.x)) <= ArmiBossEntity.ARENA_HALF_SIZE
                && Math.abs(pos.getZ() - Math.floor(center.z)) <= ArmiBossEntity.ARENA_HALF_SIZE
                && (heldColors(player) & color) == 0;
    }
}
