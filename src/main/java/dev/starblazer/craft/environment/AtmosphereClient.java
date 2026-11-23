package dev.starblazer.craft.environment;

import dev.starblazer.craft.StarblazerCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only classes stay out of dedicated server class loading. */
@Mod.EventBusSubscriber(modid = StarblazerCraft.ID, value = Dist.CLIENT)
public final class AtmosphereClient {
    private AtmosphereClient() {}

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide && event.player == Minecraft.getInstance().player) {
            if (event.phase == TickEvent.Phase.START) AtmosphereEvents.applyMotion(event.player);
            dev.starblazer.craft.space.MoonLanding.tick(event.player, event.phase);
        }
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        var level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.OVERWORLD) return;
        // Preserve water/lava/powder-snow fog and blindness effects.
        if (event.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
        if (event.getCamera().getEntity() instanceof net.minecraft.world.entity.LivingEntity living
                && (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                || living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))) return;
        double blend = AtmosphereProfile.sample(event.getCamera().getPosition().y).darkness();
        event.setRed((float) (event.getRed() * (1 - blend) + .002 * blend));
        event.setGreen((float) (event.getGreen() * (1 - blend) + .004 * blend));
        event.setBlue((float) (event.getBlue() * (1 - blend) + .015 * blend));
    }
}
