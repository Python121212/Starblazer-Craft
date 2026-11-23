package dev.starblazer.craft.environment;

import java.util.UUID;
import dev.starblazer.craft.StarblazerCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Same-world prototype. Never switches dimension or expands chunk height. */
@Mod.EventBusSubscriber(modid = StarblazerCraft.ID)
public final class AtmosphereEvents {
    private static final UUID GRAVITY_ID = UUID.fromString("ad87582c-41ef-47f8-a66c-a7072e89e305");
    private static final String EXPOSURE = "StarblazerHypoxiaExposure";
    private AtmosphereEvents() {}

    private static void gravity(ServerPlayer player, double multiplier) {
        var attribute = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (attribute == null) return;
        var previous = attribute.getModifier(GRAVITY_ID);
        // 1% increments avoid synchronizing a changed attribute every tiny vertical step.
        double quantized = multiplier > 0 ? Math.max(.01, Math.round(multiplier * 100) / 100.0) : 0;
        double amount = quantized - 1;
        if (previous != null && Math.abs(previous.getAmount() - amount) < 1e-9) return;
        if (previous != null) attribute.removeModifier(GRAVITY_ID);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(
            GRAVITY_ID, "Starblazer atmosphere", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.level().dimension() != Level.OVERWORLD) {
            gravity(player, 1);
            player.getPersistentData().remove(EXPOSURE);
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            gravity(player, 1);
            player.getPersistentData().remove(EXPOSURE);
            return;
        }
        double y = player.getY();
        AtmosphereProfile.Sample air = AtmosphereProfile.sample(y);
        gravity(player, player.isPassenger() ? 1 : dev.starblazer.craft.space.SpaceBodies.gravity(
            player.getX(), y, player.getZ(), air.gravity()));
        applyMotion(player);
        if (air.gravity() < .05) player.fallDistance = 0;
        float exposure = player.getPersistentData().getFloat(EXPOSURE);
        exposure = AtmosphereProfile.advanceExposure(exposure, air);
        player.getPersistentData().putFloat(EXPOSURE, exposure);
        if (exposure >= 200 && player.tickCount % 40 == 0) player.hurt(player.damageSources().drown(), 1.0F);
    }

    /** Local prediction and server simulation share elytra loss, not oxygen damage. */
    public static void applyMotion(Player player) {
        if (!player.isAlive() || player.level().dimension() != Level.OVERWORLD
                || player.isCreative() || player.isSpectator() || player.isPassenger()) return;
        double y = player.getY();
        double lift = AtmosphereProfile.sample(y).elytraLift();
        if (player.isFallFlying() && y >= 1200) {
            if (lift <= 0) player.stopFallFlying();
            else {
                Vec3 v = player.getDeltaMovement();
                double loss = 1 - lift;
                player.setDeltaMovement(v.x * (1 - .04 * loss), v.y - .025 * loss, v.z * (1 - .04 * loss));
            }
        }
        // Temporary descent control in space before propulsion exists.
        if (y >= 4999.9 && player.isShiftKeyDown()) player.setDeltaMovement(player.getDeltaMovement().add(0, -.04, 0));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) gravity(player, 1);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            gravity(player, 1);
            player.getPersistentData().remove(EXPOSURE);
        }
    }
}
