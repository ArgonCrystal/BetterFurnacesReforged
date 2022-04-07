package wily.betterfurnaces.init;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import wily.betterfurnaces.BetterFurnacesReforged;
import wily.betterfurnaces.blocks.*;
import wily.betterfurnaces.container.*;
import wily.betterfurnaces.items.*;
import wily.betterfurnaces.recipes.CobblestoneGeneratorRecipes;
import wily.betterfurnaces.tileentity.*;

public class Registration {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BetterFurnacesReforged.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BetterFurnacesReforged.MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, BetterFurnacesReforged.MOD_ID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, BetterFurnacesReforged.MOD_ID);
    private static final DeferredRegister<IRecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BetterFurnacesReforged.MOD_ID);
    //private static final DeferredRegister<EntityType<?>> ENTITIES = new DeferredRegister<>(ForgeRegistries.ENTITIES, MOD_ID);
    //private static final DeferredRegister<ModDimension> DIMENSIONS = new DeferredRegister<>(ForgeRegistries.MOD_DIMENSIONS, MOD_ID);

    public static void init() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        TILES.register(eventBus);
        CONTAINERS.register(eventBus);
        RECIPES.register(eventBus);
        recipeRegister("rock_generating");
        //ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        //DIMENSIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
    private static <T extends IRecipe<?>> IRecipeType<T> recipeRegister(final String key) {
        return Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(BetterFurnacesReforged.MOD_ID, key), new IRecipeType<T>() {
            @Override
            public String toString() {
                return key;
            }
        });
    }
    public static IRecipeType<CobblestoneGeneratorRecipes> COB_GENERATION_RECIPE = recipeRegister("rock_generating");
    public static final RegistryObject<CobblestoneGeneratorRecipes.Serializer> COB_GENERATION_SERIALIZER = RECIPES.register("rock_generating", () -> CobblestoneGeneratorRecipes.SERIALIZER);

    public static final RegistryObject<BlockIronFurnace> IRON_FURNACE = BLOCKS.register(BlockIronFurnace.IRON_FURNACE, () -> new BlockIronFurnace(AbstractBlock.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Item> IRON_FURNACE_ITEM = ITEMS.register(BlockIronFurnace.IRON_FURNACE, () -> new BlockItem(IRON_FURNACE.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockIronFurnaceTile>> IRON_FURNACE_TILE = TILES.register(BlockIronFurnace.IRON_FURNACE, () -> TileEntityType.Builder.of(BlockIronFurnaceTile::new, IRON_FURNACE.get()).build(null));

    public static final RegistryObject<ContainerType<BlockIronFurnaceContainer>> IRON_FURNACE_CONTAINER = CONTAINERS.register(BlockIronFurnace.IRON_FURNACE, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockIronFurnaceContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockGoldFurnace> GOLD_FURNACE = BLOCKS.register(BlockGoldFurnace.GOLD_FURNACE, () -> new BlockGoldFurnace(AbstractBlock.Properties.copy(Blocks.GOLD_BLOCK)));
    public static final RegistryObject<Item> GOLD_FURNACE_ITEM = ITEMS.register(BlockGoldFurnace.GOLD_FURNACE, () -> new BlockItem(GOLD_FURNACE.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockGoldFurnaceTile>> GOLD_FURNACE_TILE = TILES.register(BlockGoldFurnace.GOLD_FURNACE, () -> TileEntityType.Builder.of(BlockGoldFurnaceTile::new, GOLD_FURNACE.get()).build(null));

    public static final RegistryObject<ContainerType<BlockGoldFurnaceContainer>> GOLD_FURNACE_CONTAINER = CONTAINERS.register(BlockGoldFurnace.GOLD_FURNACE, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockGoldFurnaceContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<ContainerType<ItemUpgradeColor.ContainerColorUpgrade>> COLOR_UPGRADE_CONTAINER = CONTAINERS.register("color_upgrade", () -> IForgeContainerType.create((windowId, inv, data) -> {
        ItemStack helditem = inv.player.getMainHandItem();
        return new ItemUpgradeColor.ContainerColorUpgrade(windowId, inv, helditem);
    }));

    public static final RegistryObject<BlockDiamondFurnace> DIAMOND_FURNACE = BLOCKS.register(BlockDiamondFurnace.DIAMOND_FURNACE, () -> new BlockDiamondFurnace(AbstractBlock.Properties.copy(Blocks.DIAMOND_BLOCK)));
    public static final RegistryObject<Item> DIAMOND_FURNACE_ITEM = ITEMS.register(BlockDiamondFurnace.DIAMOND_FURNACE, () -> new BlockItem(DIAMOND_FURNACE.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockDiamondFurnaceTile>> DIAMOND_FURNACE_TILE = TILES.register(BlockDiamondFurnace.DIAMOND_FURNACE, () -> TileEntityType.Builder.of(BlockDiamondFurnaceTile::new, DIAMOND_FURNACE.get()).build(null));

    public static final RegistryObject<ContainerType<BlockDiamondFurnaceContainer>> DIAMOND_FURNACE_CONTAINER = CONTAINERS.register(BlockDiamondFurnace.DIAMOND_FURNACE, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockDiamondFurnaceContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockNetherhotFurnace> NETHERHOT_FURNACE = BLOCKS.register(BlockNetherhotFurnace.NETHERHOT_FURNACE, () -> new BlockNetherhotFurnace(AbstractBlock.Properties.copy(Blocks.REDSTONE_BLOCK)));
    public static final RegistryObject<Item> NETHERHOT_FURNACE_ITEM = ITEMS.register(BlockNetherhotFurnace.NETHERHOT_FURNACE, () -> new BlockItem(NETHERHOT_FURNACE.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockNetherhotFurnaceTile>> NETHERHOT_FURNACE_TILE = TILES.register(BlockNetherhotFurnace.NETHERHOT_FURNACE, () -> TileEntityType.Builder.of(BlockNetherhotFurnaceTile::new, NETHERHOT_FURNACE.get()).build(null));

    public static final RegistryObject<ContainerType<BlockNetherhotFurnaceContainer>> NETHERHOT_FURNACE_CONTAINER = CONTAINERS.register(BlockNetherhotFurnace.NETHERHOT_FURNACE, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockNetherhotFurnaceContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockExtremeFurnace> EXTREME_FURNACE = BLOCKS.register(BlockExtremeFurnace.EXTREME_FURNACE, () -> new BlockExtremeFurnace(AbstractBlock.Properties.copy(Blocks.DIAMOND_BLOCK).strength(20.0F, 3000.0F)));
    public static final RegistryObject<Item> EXTREME_FURNACE_ITEM = ITEMS.register(BlockExtremeFurnace.EXTREME_FURNACE, () -> new BlockItem(EXTREME_FURNACE.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockExtremeFurnaceTile>> EXTREME_FURNACE_TILE = TILES.register(BlockExtremeFurnace.EXTREME_FURNACE, () -> TileEntityType.Builder.of(BlockExtremeFurnaceTile::new, EXTREME_FURNACE.get()).build(null));

    public static final RegistryObject<ContainerType<BlockExtremeFurnaceContainer>> EXTREME_FURNACE_CONTAINER = CONTAINERS.register(BlockExtremeFurnace.EXTREME_FURNACE, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockExtremeFurnaceContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockExtremeForge> EXTREME_FORGE = BLOCKS.register(BlockExtremeForge.EXTREME_FORGE, () -> new BlockExtremeForge(AbstractBlock.Properties.copy(Blocks.NETHERITE_BLOCK).strength(30.0F, 6000.0F)));
    public static final RegistryObject<Item> EXTREME_FORGE_ITEM = ITEMS.register(BlockExtremeForge.EXTREME_FORGE, () -> new BlockItem(EXTREME_FORGE.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockExtremeForgeTile>> EXTREME_FORGE_TILE = TILES.register(BlockExtremeForge.EXTREME_FORGE, () -> TileEntityType.Builder.of(BlockExtremeForgeTile::new, EXTREME_FORGE.get()).build(null));

    public static final RegistryObject<ContainerType<BlockExtremeForgeContainer>> EXTREME_FORGE_CONTAINER = CONTAINERS.register(BlockExtremeForge.EXTREME_FORGE, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockExtremeForgeContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockCobblestoneGenerator> COBBLESTONE_GENERATOR = BLOCKS.register(BlockCobblestoneGenerator.COBBLESTONE_GENERATOR, () -> new BlockCobblestoneGenerator(AbstractBlock.Properties.copy(Blocks.BLACKSTONE)) {});
    public static final RegistryObject<Item> COBBLESTONE_GENERATOR_ITEM = ITEMS.register(BlockCobblestoneGenerator.COBBLESTONE_GENERATOR, () -> new BlockItem(COBBLESTONE_GENERATOR.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockCobblestoneGeneratorTile.BlockCobblestoneGeneratorTileDefinition>> COB_GENERATOR_TILE = TILES.register(BlockCobblestoneGenerator.COBBLESTONE_GENERATOR, () -> TileEntityType.Builder.of(BlockCobblestoneGeneratorTile.BlockCobblestoneGeneratorTileDefinition::new, COBBLESTONE_GENERATOR.get()).build(null));

    public static final RegistryObject<ContainerType<BlockCobblestoneGeneratorTile.BlockCobblestoneGeneratorContainer>> COB_GENERATOR_CONTAINER = CONTAINERS.register(BlockCobblestoneGenerator.COBBLESTONE_GENERATOR, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockCobblestoneGeneratorTile.BlockCobblestoneGeneratorContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockFuelVerifier> FUEL_VERIFIER = BLOCKS.register(BlockFuelVerifier.FUEL_VERIFIER, () -> new BlockFuelVerifier(AbstractBlock.Properties.copy(Blocks.COBBLESTONE)) {});
    public static final RegistryObject<Item> FUEL_VERIFIER_ITEM = ITEMS.register(BlockFuelVerifier.FUEL_VERIFIER, () -> new BlockItem(FUEL_VERIFIER.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<TileEntityType<BlockFuelVerifierTile.BlockFuelVerifierTileDefinition>> FUEL_VERIFIER_TILE = TILES.register(BlockFuelVerifier.FUEL_VERIFIER, () -> TileEntityType.Builder.of(BlockFuelVerifierTile.BlockFuelVerifierTileDefinition::new, FUEL_VERIFIER.get()).build(null));

    public static final RegistryObject<ContainerType<BlockFuelVerifierTile.BlockFuelVerifierTileContainer>> FUEL_VERIFIER_CONTAINER = CONTAINERS.register(BlockFuelVerifier.FUEL_VERIFIER, () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntity().level;
        return new BlockFuelVerifierTile.BlockFuelVerifierTileContainer(windowId, world, pos, inv, inv.player);
    }));

    public static final RegistryObject<BlockConductorBase> IRON_CONDUCTOR_BLOCK = BLOCKS.register("iron_conductor_block", () -> new BlockConductorBase(AbstractBlock.Properties.copy(Blocks.IRON_BLOCK).strength(8.0F, 20.0F)));
    public static final RegistryObject<Item>  IRON_CONDUCTOR_ITEM = ITEMS.register("iron_conductor_block", () -> new BlockItem(IRON_CONDUCTOR_BLOCK.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));

    public static final RegistryObject<BlockConductorBase> GOLD_CONDUCTOR_BLOCK = BLOCKS.register("gold_conductor_block", () -> new BlockConductorBase(AbstractBlock.Properties.copy(Blocks.GOLD_BLOCK).strength(8.0F, 20.0F)));
    public static final RegistryObject<Item>  GOLD_CONDUCTOR_ITEM = ITEMS.register("gold_conductor_block", () -> new BlockItem(GOLD_CONDUCTOR_BLOCK.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));

    public static final RegistryObject<BlockConductorBase> NETHERHOT_CONDUCTOR_BLOCK = BLOCKS.register("netherhot_conductor_block", () -> new BlockConductorBase(AbstractBlock.Properties.copy(Blocks.REDSTONE_BLOCK).strength(10.0F, 40.0F)));
    public static final RegistryObject<Item>  NETHERHOT_CONDUCTOR_ITEM = ITEMS.register("netherhot_conductor_block", () -> new BlockItem(NETHERHOT_CONDUCTOR_BLOCK.get(), new Item.Properties().tab(ModObjects.ITEM_GROUP)));

    public static final RegistryObject<ItemUpgradeIron> IRON_UPGRADE = ITEMS.register("iron_upgrade", () -> new ItemUpgradeIron(new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<ItemUpgradeGold> GOLD_UPGRADE = ITEMS.register("gold_upgrade", () -> new ItemUpgradeGold(new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<ItemUpgradeDiamond> DIAMOND_UPGRADE = ITEMS.register("diamond_upgrade", () -> new ItemUpgradeDiamond(new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<ItemUpgradeNetherhot> NETHERHOT_UPGRADE = ITEMS.register("netherhot_upgrade", () -> new ItemUpgradeNetherhot(new Item.Properties().tab(ModObjects.ITEM_GROUP)));
    public static final RegistryObject<ItemUpgradeExtreme> EXTREME_UPGRADE = ITEMS.register("extreme_upgrade", () -> new ItemUpgradeExtreme(new Item.Properties().tab(ModObjects.ITEM_GROUP)));


    public static final RegistryObject<ItemUpgradeFuelEfficiency> FUEL = ITEMS.register("fuel_efficiency_upgrade", () -> new ItemUpgradeFuelEfficiency(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1).durability(256),2));
    public static final RegistryObject<ItemUpgradeOreProcessing> ORE_PROCESSING = ITEMS.register("ore_processing_upgrade", () -> new ItemUpgradeOreProcessing(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1).durability(128),2));
    public static final RegistryObject<ItemUpgradeFuelEfficiency> ADVFUEL = ITEMS.register("advanced_fuel_efficiency_upgrade", () -> new ItemUpgradeFuelEfficiency(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),2));
    public static final RegistryObject<ItemUpgradeOreProcessing> ADVORE_PROCESSING = ITEMS.register("advanced_ore_processing_upgrade", () -> new ItemUpgradeOreProcessing(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),2));
    public static final RegistryObject<ItemUpgrade> REDSTONE = ITEMS.register("redstone_signal_upgrade", () -> new ItemUpgradeFactory(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"redstone",false,false,false,true));
    public static final RegistryObject<ItemUpgrade> PIPING = ITEMS.register("piping_upgrade", () -> new ItemUpgradeFactory(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"piping",false,false,true,false));
    public static final RegistryObject<ItemUpgrade> INPUT_FACTORY = ITEMS.register("autoinput_upgrade", () -> new ItemUpgradeFactory(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"input",false,true,true,false));
    public static final RegistryObject<ItemUpgrade> OUTPUT_FACTORY = ITEMS.register("autooutput_upgrade", () -> new ItemUpgradeFactory(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1), "output",true,false,true,false));
    public static final RegistryObject<ItemUpgrade> FACTORY = ITEMS.register("factory_upgrade", () -> new ItemUpgradeFactory(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"factory",true,true,true,true));
    public static final RegistryObject<ItemUpgradeColor> COLOR = ITEMS.register("color_upgrade", () -> new ItemUpgradeColor(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"color"));
    public static final RegistryObject<ItemUpgradeLiquidFuel> LIQUID = ITEMS.register("liquid_fuel_upgrade", () -> new ItemUpgradeLiquidFuel(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"liquid"));
    public static final RegistryObject<ItemUpgradeEnergyFuel> ENERGY = ITEMS.register("energy_upgrade", () -> new ItemUpgradeEnergyFuel(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"energy"));
    public static final RegistryObject<ItemUpgradeXpTank> XP = ITEMS.register("xp_tank_upgrade", () -> new ItemUpgradeXpTank(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"xp"));
    public static final RegistryObject<ItemUpgradeType> BLAST = ITEMS.register("blasting_upgrade", () -> new ItemUpgradeType(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"blasting"));
    public static final RegistryObject<ItemUpgradeType> SMOKE = ITEMS.register("smoking_upgrade", () -> new ItemUpgradeType(new Item.Properties().tab(ModObjects.ITEM_GROUP).stacksTo(1),"smoking"));


    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
    }


    public static void registerItems(RegistryEvent.Register<Item> event)
    {
    }


}
