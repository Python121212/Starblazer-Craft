package dev.starblazer.craft.space;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import dev.starblazer.craft.StarblazerCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Analytic player-only ground, independent of vanilla's buildable block height. */
@Mod.EventBusSubscriber(modid=StarblazerCraft.ID)
public final class MoonLanding {
    private static final Map<Player,Vec3> PREVIOUS=Collections.synchronizedMap(new WeakHashMap<>());
    private MoonLanding() {}
    public static void tick(Player player, TickEvent.Phase phase) {
        if (player.level().dimension()!=Level.OVERWORLD || !player.isAlive() || player.isSpectator()
                || player.isPassenger() || player.getAbilities().flying) {
            PREVIOUS.remove(player); return;
        }
        if (phase==TickEvent.Phase.START) { PREVIOUS.put(player,player.position()); return; }
        Vec3 old=PREVIOUS.remove(player);
        if (old==null) return;
        var hit=SpaceBodies.contact(old.x,old.y,old.z,player.getX(),player.getY(),player.getZ(),player.getDeltaMovement().y);
        if (hit==null) return;
        player.setPos(hit.x(),hit.y(),hit.z());
        Vec3 v=player.getDeltaMovement();
        player.setDeltaMovement(v.x,0,v.z);
        player.setOnGround(true);
        player.fallDistance=0;
    }
    @SubscribeEvent
    public static void serverTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.level().isClientSide) tick(event.player,event.phase);
    }
}
