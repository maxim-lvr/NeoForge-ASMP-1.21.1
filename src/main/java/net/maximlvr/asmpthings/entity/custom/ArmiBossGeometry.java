package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ArmiBossGeometry {
    public static final double HEIGHT = 60.0;
    public static final float SCALE = (float) (HEIGHT / (1.5 + 1.2119157314300537));
    public static final double OFFSET_X = 0.0;
    public static final double OFFSET_Y = 1.2119157314300537;
    public static final double OFFSET_Z = 0.0;
    // Only the 8 x 7 x 2 cube owned by "hand", not its finger children.
    public static final float HAND_WIDTH = 8.0F / 16 * SCALE;
    public static final float HAND_HEIGHT = 7.0F / 16 * SCALE;
    public static final float HAND_DEPTH = 2.0F / 16 * SCALE;

    private ArmiBossGeometry() {
    }

    public static AABB handBounds(Vec3 origin, float yaw) {
        double angle = Math.toRadians(180.0 - yaw);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double localX = OFFSET_X * SCALE;
        double localZ = OFFSET_Z * SCALE;
        double centerX = origin.x + cos * localX + sin * localZ;
        double centerZ = origin.z - sin * localX + cos * localZ;
        double minY = origin.y + (OFFSET_Y - 15.0 / 16) * SCALE;
        double halfX = (Math.abs(cos) * HAND_WIDTH + Math.abs(sin) * HAND_DEPTH) * 0.5;
        double halfZ = (Math.abs(sin) * HAND_WIDTH + Math.abs(cos) * HAND_DEPTH) * 0.5;
        return new AABB(centerX - halfX, minY, centerZ - halfZ,
                centerX + halfX, minY + HAND_HEIGHT, centerZ + halfZ);
    }
}
