package net.maximlvr.asmpthings.item.custom;

public record GunSettings(int capacity, int refillTicks, int shotCooldownTicks, float damage, double range) {
    public GunSettings {
        if (capacity < 1 || refillTicks < 1 || shotCooldownTicks < 1
                || !Float.isFinite(damage) || damage <= 0 || !Double.isFinite(range) || range <= 0) {
            throw new IllegalArgumentException("Invalid gun settings");
        }
    }
}
