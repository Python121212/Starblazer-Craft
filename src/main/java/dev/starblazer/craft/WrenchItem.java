package dev.starblazer.craft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = StarblazerCraft.ID)
public class WrenchItem extends Item {
    private static final String KEY = "StarblazerSelection";
    public WrenchItem(Properties properties) { super(properties); }

    private static SelectionBounds.Point point(BlockPos p) { return new SelectionBounds.Point(p.getX(), p.getY(), p.getZ()); }
    private static void message(Player p, String key) { p.displayClientMessage(Component.translatable("message.starblazercraft." + key), true); }
    private static boolean usable(Player p) { return !p.isSpectator() && p.mayBuild(); }
    private static boolean session(CompoundTag s, Player p, Level level) {
        return s.hasUUID("Owner") && s.getUUID("Owner").equals(p.getUUID())
            && s.getString("Dimension").equals(level.dimension().location().toString())
            && s.contains("Core", Tag.TAG_LONG);
    }
    private static boolean coreValid(CompoundTag s, Level level) {
        BlockPos core = BlockPos.of(s.getLong("Core"));
        return level.hasChunkAt(core) && level.getBlockState(core).is(StarblazerCraft.CORES);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Select containers and future interactive cores without opening their GUI first.
        return useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player p = context.getPlayer();
        if (p == null || !usable(p)) return InteractionResult.FAIL;
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        CompoundTag root = context.getItemInHand().getOrCreateTag();
        if (level.getBlockState(pos).is(StarblazerCraft.CORES)) {
            CompoundTag fresh = new CompoundTag();
            fresh.putUUID("Owner", p.getUUID());
            fresh.putString("Dimension", level.dimension().location().toString());
            fresh.putLong("Core", pos.asLong());
            root.put(KEY, fresh);
            message(p, "started");
            return InteractionResult.CONSUME;
        }
        CompoundTag s = root.getCompound(KEY);
        if (!session(s, p, level)) { message(p, "need_core"); return InteractionResult.CONSUME; }
        if (!coreValid(s, level)) { message(p, "core_unavailable"); return InteractionResult.CONSUME; }
        if (!s.contains("First", Tag.TAG_LONG) || s.contains("Second", Tag.TAG_LONG)) {
            s.putLong("First", pos.asLong());
            s.remove("Second");
            message(p, "first");
        } else {
            BlockPos first = BlockPos.of(s.getLong("First"));
            if (SelectionBounds.volume(point(first), point(pos)) > SelectionBounds.MAX_VOLUME) {
                message(p, "too_large"); return InteractionResult.CONSUME;
            }
            BlockPos core = BlockPos.of(s.getLong("Core"));
            if (!SelectionBounds.contains(point(first), point(pos), point(core)) && !extraContains(s, core.asLong())) {
                message(p, "include_core"); return InteractionResult.CONSUME;
            }
            s.putLong("Second", pos.asLong());
            // Intentionally no block removal/entity spawn until safe assembly is implemented.
            message(p, "selected");
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (!p.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && usable(p)) {
            if (stack.hasTag()) stack.getTag().remove(KEY);
            message(p, "cleared");
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean extraContains(CompoundTag s, long pos) {
        ListTag list = s.getList("Extras", Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) if (((LongTag) list.get(i)).getAsLong() == pos) return true;
        return false;
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        Player p = event.getEntity();
        ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof WrenchItem) || !p.isShiftKeyDown()) return;
        // Cancel on both sides, even for an invalid session, to prevent accidental mining.
        event.setCanceled(true);
        if (event.getLevel().isClientSide || !usable(p)) return;
        CompoundTag s = stack.getOrCreateTag().getCompound(KEY);
        if (!session(s, p, event.getLevel())) { message(p, "need_core"); return; }
        if (!coreValid(s, event.getLevel())) { message(p, "core_unavailable"); return; }
        BlockPos pos = event.getPos();
        if (!event.getLevel().hasChunkAt(pos) || event.getLevel().getBlockState(pos).isAir()) return;
        if (extraContains(s, pos.asLong())) { message(p, "already_added"); return; }
        ListTag extras = s.getList("Extras", Tag.TAG_LONG);
        if (extras.size() >= SelectionBounds.MAX_EXTRAS) { message(p, "too_many_extras"); return; }
        // Bound distance without scanning/loading any blocks or chunks.
        if (SelectionBounds.volume(point(BlockPos.of(s.getLong("Core"))), point(pos)) > SelectionBounds.MAX_VOLUME) {
            message(p, "too_far"); return;
        }
        extras.add(LongTag.valueOf(pos.asLong()));
        s.put("Extras", extras);
        message(p, "added");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.starblazercraft.wrench"));
        lines.add(Component.translatable("tooltip.starblazercraft.wrench_reset"));
        if (stack.hasTag() && stack.getTag().contains(KEY)) {
            CompoundTag s = stack.getTag().getCompound(KEY);
            lines.add(Component.translatable("tooltip.starblazercraft.selection", s.contains("Second") ? 2 : s.contains("First") ? 1 : 0, s.getList("Extras", Tag.TAG_LONG).size()));
        }
    }
}
