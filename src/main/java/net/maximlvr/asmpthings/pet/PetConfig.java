package net.maximlvr.asmpthings.pet;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.EnumMap;

public final class PetConfig {
    public record Bonus(ModConfigSpec.DoubleValue health, ModConfigSpec.DoubleValue armor) {}
    public static final ModConfigSpec SPEC;
    private static final EnumMap<PetKind, Bonus> BONUSES = new EnumMap<>(PetKind.class);
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (PetKind kind : PetKind.values()) {
            builder.push(kind.id);
            var health = builder.comment("Points de vie supplémentaires : 2 = un cœur.")
                    .defineInRange("max_health", kind == PetKind.PET_1 ? 2.0 : 0.0, 0.0, 40.0);
            var armor = builder.comment("Points d’armure supplémentaires : 2 = une icône d’armure.")
                    .defineInRange("armor", kind == PetKind.PET_2 ? 2.0 : 0.0, 0.0, 30.0);
            BONUSES.put(kind, new Bonus(health, armor));
            builder.pop();
        }
        SPEC = builder.build();
    }
    public static Bonus bonus(PetKind kind) { return BONUSES.get(kind); }
    private PetConfig() {}
}
