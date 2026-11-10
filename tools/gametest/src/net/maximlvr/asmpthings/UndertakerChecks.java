package net.maximlvr.asmpthings;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.maximlvr.asmpthings.entity.custom.UndertakerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("asmpthingsmod")
@PrefixGameTestTemplate(false)
public class UndertakerChecks {
    private static void floor(GameTestHelper h) {
        for (int x=0;x<40;x++) for(int z=0;z<12;z++) h.setBlock(x,0,z,Blocks.STONE);
    }

    private static void wall(GameTestHelper h, boolean present) {
        for (int z=1;z<10;z++) for(int y=1;y<6;y++) h.setBlock(8,y,z,present?Blocks.STONE:Blocks.AIR);
    }

    @GameTest(template="undertaker_empty",batch="undertaker_combat",timeoutTicks=450)
    public static void detectionPreparationAndObstacles(GameTestHelper h) {
        floor(h);
        UndertakerEntity mob = h.spawn(ModEntities.UNDERTAKER.get(),new BlockPos(3,1,4));
        Vec3 base=mob.position();
        int[] shots={0};
        FakePlayer player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"undertaker-test")) {
            @Override public boolean hurt(DamageSource source,float amount) {
                h.assertTrue(amount==4,"Wrong laser damage");
                shots[0]++;
                return true;
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(base.add(32,0,0));
        h.getLevel().addNewPlayer(player);
        // Isolate distance checks from wandering, which has its own test below.
        h.onEachTick(()->{mob.setPos(base);mob.setDeltaMovement(Vec3.ZERO);});
        h.runAtTickTime(15,()->h.assertFalse(mob.isAiming(),"Detected beyond 30 blocks"));
        h.runAtTickTime(20,()->player.setPos(base.add(25,0,0)));
        h.runAtTickTime(55,()->{
            h.assertTrue(mob.isAiming(),"No preparation inside 30 blocks");
            h.assertTrue(shots[0]==0,"Shot outside 20 blocks");
        });
        h.runAtTickTime(60,()->player.setPos(base.add(19,0,0)));
        h.runAtTickTime(62,()->h.assertTrue(mob.hasLaser(),"Missing visible laser"));
        h.runAtTickTime(70,()->{
            h.assertTrue(shots[0]==1,"No hit inside 20 blocks");
            h.assertFalse(mob.hasLaser(),"Laser did not expire");
        });
        h.runAtTickTime(105,()->h.assertTrue(shots[0]==1,"Laser cadence too fast"));
        h.runAtTickTime(125,()->h.assertTrue(shots[0]==2,"Second laser missing"));
        h.runAtTickTime(130,()->wall(h,true));
        h.runAtTickTime(190,()->{
            h.assertFalse(mob.isAiming(),"Detected through wall");
            h.assertTrue(shots[0]==2,"Shot through wall");
        });
        h.runAtTickTime(200,()->{wall(h,false);player.setPos(base.add(32,0,0));});
        h.runAtTickTime(215,()->h.assertFalse(mob.isAiming(),"Kept distant target"));
        h.runAtTickTime(220,()->{player.setGameMode(GameType.CREATIVE);player.setPos(base.add(19,0,0));});
        h.runAtTickTime(250,()->h.assertFalse(mob.isAiming(),"Targeted creative player"));
        h.runAtTickTime(255,()->player.setGameMode(GameType.SPECTATOR));
        h.runAtTickTime(275,()->h.assertFalse(mob.isAiming(),"Targeted spectator"));
        h.runAtTickTime(280,()->player.setGameMode(GameType.SURVIVAL));
        h.runAtTickTime(290,()->h.assertTrue(shots[0]==2,"Skipped preparation"));
        h.runAtTickTime(315,()->h.assertTrue(shots[0]==3,"Failed to reacquire target"));
        h.runAtTickTime(320,()->player.setPos(base.add(25,0,0)));
        h.runAtTickTime(390,()->{
            h.assertTrue(shots[0]==3,"Shot after target backed away");
            h.assertTrue(mob.isAiming(),"Stopped preparing inside 30 blocks");
        });
        h.runAtTickTime(395,()->player.setPos(base.add(32,0,0)));
        h.runAtTickTime(415,()->{
            h.assertFalse(mob.isAiming(),"Did not return to calm");
            h.getLevel().removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
            h.succeed();
        });
    }

    @GameTest(template="undertaker_empty",batch="undertaker_walk",timeoutTicks=240)
    public static void slowForwardPatrol(GameTestHelper h) {
        floor(h);
        UndertakerEntity mob=h.spawn(ModEntities.UNDERTAKER.get(),new BlockPos(3,1,5));
        mob.getRandom().setSeed(1234);
        mob.setYRot(-90);
        mob.setYBodyRot(-90);
        Vec3 start=mob.position();
        h.runAtTickTime(180,()->{
            h.assertTrue(mob.getAttributeValue(Attributes.MOVEMENT_SPEED)==0.12,"Wrong walking speed");
            h.assertTrue(mob.getX()>start.x+1,"Did not patrol forward: start="+start+", end="+mob.position()+", yaw="+mob.getYRot()+", path="+mob.getNavigation().getPath());
            h.assertFalse(mob.isAiming(),"Aiming without a player");
            h.succeed();
        });
    }
}
