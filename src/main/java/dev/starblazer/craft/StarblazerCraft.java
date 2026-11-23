package dev.starblazer.craft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(StarblazerCraft.ID)
public class StarblazerCraft {
    public static final String ID = "starblazercraft";
    public static final TagKey<Block> CORES = TagKey.create(Registries.BLOCK, new ResourceLocation(ID, "assembly_cores"));
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final RegistryObject<Block> CORE_BLOCK = BLOCKS.register("core", () -> new Block(
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 8.0F)
            .sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> 7)));
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CORE_ITEM = ITEMS.register("core", () -> new BlockItem(CORE_BLOCK.get(), new Item.Properties()));

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.starblazercraft.main"))
        .icon(() -> new ItemStack(WRENCH.get()))
        .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
        .build());

    public StarblazerCraft() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(this::creative);
    }

    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(WRENCH);
    }
}
