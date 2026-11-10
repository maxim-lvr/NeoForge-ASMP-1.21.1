package net.maximlvr.asmpthings.item.custom;

import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class KraberItem extends GunItem {
    public KraberItem(Properties properties, GunSettings settings) {
        super(properties, settings);
    }

    @Override
    protected void onBlockShot(ServerLevel level, Player player, BlockHitResult hit) {
        var sphere = ModEntities.KRABER_SPHERE.get().create(level);
        if (sphere != null) {
            sphere.setPos(hit.getLocation());
            sphere.setOwner(player);
            level.addFreshEntity(sphere);
        }
    }
}
