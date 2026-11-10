package net.maximlvr.asmpthings.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class ArmiBossAutoWalk {
    private List<BlockPos> nodes = List.of();
    private int index;
    private int stalledTicks;
    private double bestDistance = Double.MAX_VALUE;

    void setPath(List<BlockPos> nodes) {
        this.nodes = List.copyOf(nodes);
        index = 0;
        stalledTicks = 0;
        bestDistance = Double.MAX_VALUE;
    }

    void clear() {
        if (index < nodes.size() && Minecraft.getInstance().player != null) {
            var player = Minecraft.getInstance().player;
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        }
        setPath(List.of());
    }

    void applyInput(Input input) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        while (index < nodes.size()) {
            Vec3 delta = Vec3.atBottomCenterOf(nodes.get(index)).subtract(player.position());
            double distance = delta.horizontalDistance();
            double tolerance = index == nodes.size() - 1 ? 0.08 : 0.2;
            if (distance <= tolerance && Math.abs(delta.y) < 0.6) {
                index++;
                bestDistance = Double.MAX_VALUE;
                stalledTicks = 0;
                if (index == nodes.size()) {
                    player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                }
                continue;
            }
            double remaining = delta.lengthSqr();
            if (remaining < bestDistance - 0.01) {
                bestDistance = remaining;
                stalledTicks = 0;
            } else if (++stalledTicks >= 40) {
                clear();
                player.displayClientMessage(Component.literal("Le passage est bloque."), true);
                return;
            }
            float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setYBodyRot(yaw);
            player.setSprinting(false);
            input.forwardImpulse = (float) Math.min(1, distance / 0.3);
            input.up = true;
            input.jumping = delta.y > 0.35 && distance < 1.3 && player.onGround();
            return;
        }
    }
}
