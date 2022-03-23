package wily.betterfurnaces.tileentity;

import com.google.common.collect.Lists;
import harmonised.pmmo.events.FurnaceHandler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.AirItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.antlr.v4.runtime.misc.NotNull;
import org.apache.commons.lang3.ArrayUtils;
import wily.betterfurnaces.BetterFurnacesReforged;
import wily.betterfurnaces.Config;
import wily.betterfurnaces.blocks.BlockForgeBase;
import wily.betterfurnaces.blocks.BlockIronFurnace;
import wily.betterfurnaces.init.Registration;
import wily.betterfurnaces.items.*;
import wily.betterfurnaces.util.DirectionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class BlockSmeltingTileBase extends TileEntityInventory implements ITickableTileEntity, IRecipeHolder, IRecipeHelperPopulator {
    public final int[] provides = new int[Direction.values().length];
    private final int[] lastProvides = new int[this.provides.length];

    public int FUEL() {return 1;}
    public int UPGRADES()[]{ return new int[]{3,4,5};}
    public int FINPUT(){ return INPUTS()[0];}
    public int LINPUT(){ return INPUTS()[INPUTS().length - 1];}
    public int FOUTPUT(){ return OUTPUTS()[0];}
    public int LOUTPUT(){ return OUTPUTS()[OUTPUTS().length - 1];}
    public int[] INPUTS(){ return new int[]{0};}
    public int[] OUTPUTS(){ return new int[]{2};}
    public int[] FSLOTS(){ return  ArrayUtils.addAll(ArrayUtils.addAll(ISLOTS(), OUTPUTS()));}
    public int[] ISLOTS(){ return  ArrayUtils.addAll(INPUTS(),new int[FUEL()]);}

    private Random rand = new Random();

    public int show_inventory_settings;
    protected int timer;
    public int EnergyUse() {return 600;}
    public int LiquidCapacity() {return 4000;}
    public int EnergyCapacity() {return 16000;}
    private int furnaceBurnTime;
    public int cookTime;
    public int totalCookTime = this.getCookTime();
    private int recipesUsed;
    private final Object2IntOpenHashMap<ResourceLocation> recipes = new Object2IntOpenHashMap<>();
    public boolean isForge(){ return false;}

    public IRecipeType<? extends AbstractCookingRecipe> recipeType;

    public FurnaceSettings furnaceSettings;

    private LRUCache<Item, Optional<AbstractCookingRecipe>> cache = LRUCache.newInstance(Config.cache_capacity.get());
    private LRUCache<Item, Optional<AbstractCookingRecipe>> blasting_cache = LRUCache.newInstance(Config.cache_capacity.get());
    private LRUCache<Item, Optional<AbstractCookingRecipe>> smoking_cache = LRUCache.newInstance(Config.cache_capacity.get());

    public Direction facing(){
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
    public BlockSmeltingTileBase(TileEntityType<?> tileentitytypeIn, int invsize) {
        super(tileentitytypeIn, invsize);
        this.recipeType = IRecipeType.SMELTING;
        furnaceSettings = new FurnaceSettings() {
            @Override
            public void onChanged() {
                setChanged();
            }
            @Override
            public void set(int index, int value) {
                if (hasUpgradeType(Registration.FACTORY.get()))
                     read(getUpgradeTypeSlotItem(Registration.FACTORY.get()).getOrCreateTag());
                super.set(index,value);
                if (hasUpgradeType(Registration.FACTORY.get()))
                    write(getUpgradeTypeSlotItem(Registration.FACTORY.get()).getOrCreateTag());
            }
            @Override
            public int get(int index) {
                if (hasUpgradeType(Registration.FACTORY.get()))
                    read(getUpgradeTypeSlotItem(Registration.FACTORY.get()).getOrCreateTag());
                return super.get(index);
            }
        };

    }

    private int getFromCache(LRUCache<Item, Optional<AbstractCookingRecipe>> c, Item key) {
        if (c.get(key) == null)
        {
            return 0;
        }
        return c.get(key).orElse(null) == null ? 0 : c.get(key).orElse(null).getCookingTime();
    }

    public boolean hasRecipe(ItemStack stack) {
        return grabRecipe(stack).isPresent();
    }

    private LRUCache<Item, Optional<AbstractCookingRecipe>> getCache() {
        if (this.recipeType == IRecipeType.BLASTING) {
            return blasting_cache;
        }
        if (this.recipeType == IRecipeType.SMOKING) {
            return smoking_cache;
        }
        return cache;
    }

    private Optional<AbstractCookingRecipe> grabRecipe() {
        Item item = getItem(FINPUT()).getItem();
        if (item instanceof AirItem)
        {
            return Optional.empty();
        }
        Optional<AbstractCookingRecipe> recipe = getCache().get(item);
        if (recipe == null) {
            recipe = this.level.getRecipeManager().getRecipeFor((IRecipeType<AbstractCookingRecipe>) this.recipeType, this, this.level);
            getCache().put(item, recipe);
        }
        return recipe;
    }

    private Optional<AbstractCookingRecipe> grabRecipe(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof AirItem)
        {
            return Optional.empty();
        }
        Optional<AbstractCookingRecipe> recipe = getCache().get(item);
        if (recipe == null) {
            recipe = this.level.getRecipeManager().getRecipeFor((IRecipeType<AbstractCookingRecipe>) this.recipeType, new Inventory(stack), this.level);
            getCache().put(item, recipe);
        }
        return recipe;
    }
    public boolean isLiquid() {
       return (hasUpgrade(Registration.LIQUID.get()));
    }
    public boolean hasXPTank() {
        return (hasUpgrade(Registration.XP.get()) && ItemUpgradeXpTank.isWorking());
    }
    private boolean isEnergy() {
        return ((hasUpgrade(Registration.ENERGY.get())) && energyStorage.getEnergyStored() >= EnergyUse());
    }
    protected int getCookTime() {

        if (this.getItem(FINPUT()).getItem() == Items.AIR) {
            return totalCookTime;
        }
        int speed = getSpeed();
        if (speed == -1) {
            return -1;
        }


        return Math.max(1, speed);


    }

    protected int getSpeed() {
        int i = getCookTimeConfig().get();
        int j = getFromCache(getCache(), getItem(FINPUT()).getItem());
        if (j == 0) {
            Optional<AbstractCookingRecipe> recipe = grabRecipe();
            j = !recipe.isPresent() ? -1 : recipe.orElse(null).getCookingTime();
            getCache().put(this.getItem(FINPUT()).getItem(), recipe);

            if (j == -1) {
                return -1;
            }
        }
        if (j < i) {
            int k = j - (200 - i);
            return k;
        } else {
            return i;
        }


    }

    public ForgeConfigSpec.IntValue getCookTimeConfig() {
        return null;
    }

    public final IIntArray fields = new IIntArray() {
        public int get(int index) {
            switch (index) {
                case 0:
                    return BlockSmeltingTileBase.this.furnaceBurnTime;
                case 1:
                    return BlockSmeltingTileBase.this.recipesUsed;
                case 2:
                    return BlockSmeltingTileBase.this.cookTime;
                case 3:
                    return BlockSmeltingTileBase.this.totalCookTime;
                case 4:
                    return BlockSmeltingTileBase.this.show_inventory_settings;
                default:
                    return 0;
            }
        }

        public void set(int index, int value) {
            switch (index) {
                case 0:
                    BlockSmeltingTileBase.this.furnaceBurnTime = value;
                    break;
                case 1:
                    BlockSmeltingTileBase.this.recipesUsed = value;
                    break;
                case 2:
                    BlockSmeltingTileBase.this.cookTime = value;
                    break;
                case 3:
                    BlockSmeltingTileBase.this.totalCookTime = value;
                    break;
                case 4:
                    BlockSmeltingTileBase.this.show_inventory_settings = value;
                    break;
            }

        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public boolean hasUpgrade(Item upg) {
        for (int slot : UPGRADES())
            if (upg == getItem(slot).getItem()) return true;
        return false;
    }

    public boolean hasUpgradeType(ItemUpgrade upg) {
        for (int slot : UPGRADES()) {
            if (getItem(slot).getItem() instanceof ItemUpgrade && upg.upgradeType == ((ItemUpgrade)getItem(slot).getItem()).upgradeType) return true;
        }
        return hasUpgrade(upg);
    }

    public ItemStack getUpgradeTypeSlotItem(ItemUpgrade upg) {
        for (int slot : UPGRADES())
            if (getItem(slot).getItem() instanceof ItemUpgrade && upg.upgradeType == ((ItemUpgrade) getItem(slot).getItem()).upgradeType) return getItem(slot);
        return getItem(UPGRADES()[0]);
    }

    public ItemStack getUpgradeSlotItem(Item upg) {
        for (int slot : UPGRADES())
            if (upg == getItem(slot).getItem()) return getItem(slot);
        return getItem(UPGRADES()[0]);
    }


    public int correspondentOutputSlot(int input){return 4 + input;}

    protected final FluidTank fluidTank = new FluidTank(LiquidCapacity(), fs -> {
        if (ForgeHooks.getBurnTime(new ItemStack(fs.getFluid().getBucket())) > 0)
            return true;
        return false;
    }){
        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            setChanged();
            level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), getBlockState(), 2);
        }
    };
    protected final FluidTank xpTank = new FluidTank(2000, xp -> {
        if (xp.getFluid().getRegistryName().toString().equals(Config.getLiquidXPType()) && ModList.get().isLoaded(Config.getLiquidXPMod()))
            return true;
        return false;
    }){
        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            setChanged();
            level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), getBlockState(), 2);
        }
    };

    public void forceUpdateAllStates() {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getValue(BlockStateProperties.LIT) != this.isBurning()) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, this.isBurning()), 3);
        }
    }
    private final EnergyStorage energyStorage = new EnergyStorage(EnergyCapacity(),3400,3400, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int retval = super.receiveEnergy(maxReceive, simulate);
            if (!simulate) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), getBlockState(), 2);
            }
            return retval;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int retval = super.extractEnergy(maxExtract, simulate);
            if (!simulate) {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), getBlockState(), 2);
            }
            return retval;
        }
    };
    public void trySmelt(){
        this.smeltItem(irecipeSlot(FINPUT()).orElse(null), FINPUT(), FOUTPUT());
    }
    public Optional<AbstractCookingRecipe> irecipeSlot(int input){
        if (!isForge() && input > FINPUT()) return Optional.empty();
        if (!getItem(input).isEmpty())
            return grabRecipe(getItem(input));
        else
        return Optional.empty();
    }
    public boolean inputSlotsEmpty(){
        return !this.inventory.get(FINPUT()).isEmpty();
    }
    public boolean smeltValid(){
        return this.canSmelt(irecipeSlot(FINPUT()).orElse(null), FINPUT(), FOUTPUT());
    }
    @Override
    public void tick() {
        if (furnaceSettings.size() <= 0) {
            furnaceSettings = new FurnaceSettings() {
                @Override
                public void onChanged() {
                    setChanged();
                }
            };
        }

        boolean wasBurning = this.isBurning();
        boolean flag1 = false;
        boolean flag2 = false;

        if (this.isBurning()) {
            --this.furnaceBurnTime;
        }
        if ((hasUpgrade(Registration.COLOR.get()))){
            if (!(level.getBlockState(getBlockPos()).getValue(BlockIronFurnace.COLORED)))
            level.setBlock(getBlockPos(), level.getBlockState(getBlockPos()).setValue(BlockIronFurnace.COLORED, true), 3);
        }else level.setBlock(getBlockPos(), level.getBlockState(getBlockPos()).setValue(BlockIronFurnace.COLORED, false), 3);

        if (this.recipeType != IRecipeType.SMELTING) {
                this.recipeType = IRecipeType.SMELTING;
        }

        if (!this.level.isClientSide) {

            int get_cook_time = getCookTime();
            timer++;

            if (this.totalCookTime != get_cook_time) {
                this.totalCookTime = get_cook_time;
            }
            int mode = this.getRedstoneSetting();
            if (mode != 0) {
                if (mode == 2) {
                    int i = 0;
                    for (Direction side : Direction.values()) {
                        if (level.getSignal(worldPosition.offset(side.getNormal()), side) > 0) {
                            i++;
                        }
                    }
                    if (i != 0) {
                        this.cookTime = 0;
                        this.furnaceBurnTime = 0;
                        forceUpdateAllStates();
                        return;
                    }
                }
                if (mode == 1) {
                    boolean flag = false;
                    for (Direction side : Direction.values()) {

                        if (level.getSignal(worldPosition.offset(side.getNormal()), side) > 0) {
                            flag = true;
                        }
                    }
                    if (!flag) {
                        this.cookTime = 0;
                        this.furnaceBurnTime = 0;
                        forceUpdateAllStates();
                        return;
                    }
                }
                for (int i = 0; i < Direction.values().length; i++)
                    this.provides[i] = getBlockState().getDirectSignal(this.level, worldPosition, DirectionUtil.fromId(i));

            } else {
                for (int i = 0; i < Direction.values().length; i++)
                    this.provides[i] = 0;
            }
            if (this.doesNeedUpdateSend()) {
                this.onUpdateSent();
            }

            if (hasXPTank()) grantStoredRecipeExperience(level, null);
            if (!hasUpgradeType(Registration.FACTORY.get()) && isForge() && (level.getBlockState(getBlockPos()).getValue(BlockForgeBase.SHOW_ORIENTATION))) level.setBlock(getBlockPos(), level.getBlockState(getBlockPos()).setValue(BlockForgeBase.SHOW_ORIENTATION, false), 3);
            getItem(FUEL()).getCapability(CapabilityEnergy.ENERGY).ifPresent(E -> {
                if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
                    E.extractEnergy(energyStorage.receiveEnergy(E.getEnergyStored(), false), false);
                }
            });

            ItemStack itemstack = this.inventory.get(FUEL());
            if (isLiquid() && itemstack.hasContainerItem()) {
                FluidActionResult res = FluidUtil.tryEmptyContainer(itemstack, fluidTank, 1000, null, true);
                if ( res.isSuccess()) {
                    level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.BUCKET_FILL_LAVA, SoundCategory.PLAYERS, 0.6F, 0.8F);
                    inventory.set(FUEL(), res.result);
                }
            }
                    if ((isBurning() || !itemstack.isEmpty() || isLiquid() || isEnergy())  && inputSlotsEmpty()) {
                        boolean valid = smeltValid();
                        if (!this.isBurning() && valid) {
                            if (isLiquid() && (fluidTank.getFluidAmount() >= 10)){
                                int f = getBurnTime(new ItemStack(fluidTank.getFluidInTank(1).getFluid().getBucket()));
                                this.furnaceBurnTime = f * get_cook_time / 20000;
                                if (hasUpgradeType(Registration.FUEL.get()))
                                    this.furnaceBurnTime = 2 * f * get_cook_time / 20000;
                                this.recipesUsed = this.furnaceBurnTime;
                                fluidTank.drain(10, IFluidHandler.FluidAction.EXECUTE);
                            }else if (isEnergy() && isForge()) {
                                furnaceBurnTime = 200 * get_cook_time / 200;
                                if (hasUpgradeType(Registration.FUEL.get()))
                                    furnaceBurnTime = 2 * 200 * get_cook_time / 200;
                                recipesUsed = furnaceBurnTime;
                                    for(int i : INPUTS())
                                    energyStorage.extractEnergy(EnergyUse() * OreProcessingMultiplier(getItem(i)), false);
                            }else{
                                if (hasUpgradeType(Registration.FUEL.get())){
                                    this.furnaceBurnTime = 2 * (getBurnTime(itemstack)) * get_cook_time / 200;
                                }else{
                                    this.furnaceBurnTime = getBurnTime(itemstack) * get_cook_time / 200;
                                }
                                this.recipesUsed = this.furnaceBurnTime;
                            }
                            if (this.isBurning()) {
                                flag1 = true;
                                if ((!isLiquid() || fluidTank.getFluidAmount() < 10) && !isEnergy())
                                    if (itemstack.hasContainerItem()) this.inventory.set(FUEL(), ForgeHooks.getContainerItem(itemstack));
                                    else if (!itemstack.isEmpty() && isItemFuel(itemstack)) {
                                        itemstack.shrink(1);
                                        if (hasUpgrade(Registration.FUEL.get())) {
                                            breakDurabilityItem(getUpgradeSlotItem(Registration.FUEL.get()));
                                        }
                                    }
                            }
                        }
                        if (this.isBurning() && valid) {
                            ++this.cookTime;
                            if (this.cookTime >= this.totalCookTime) {
                                this.cookTime = 0;
                                this.totalCookTime = this.getCookTime();
                                trySmelt();
                                if (hasUpgradeType(Registration.FACTORY.get()))
                                    this.autoIO();
                                flag1 = true;
                            }
                        } else {
                            if (cookTime > 0)
                                --this.cookTime;
                        }
                    } else if (!this.isBurning() && this.cookTime > 0) {
                        this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.totalCookTime);
                    }
                    if (wasBurning != this.isBurning()) {
                        flag1 = true;
                        this.level.setBlock(this.worldPosition, this.level.getBlockState(this.worldPosition).setValue(BlockStateProperties.LIT, this.isBurning()), 3);
                    }
                    if ((timer % 24 == 0) && (hasUpgradeType(Registration.FACTORY.get()))){
                        if (this.cookTime <= 0 ) {
                            int a = 0;
                            for (int i: INPUTS())
                                a = a + getItem(i).getCount();
                            if (inputSlotsEmpty()) {
                                this.autoIO();
                                flag1 = true;
                            } else if ((FINPUT() - LINPUT() * 3 > a)) {
                                this.autoIO();
                                flag1 = true;
                            }
                            if (this.getItem(FUEL()).isEmpty()) {
                                this.autoIO();
                                flag1 = true;
                            } else if (this.getItem(FUEL()).getCount() < this.getItem(FUEL()).getMaxStackSize()) {
                                this.autoIO();
                                flag1 = true;
                            }
                        }
                }
            }

        if (flag1) {
            this.setChanged();
        }

    }
    public int hex() {
        CompoundNBT nbt = getUpgradeSlotItem(Registration.COLOR.get()).getTag();

        return ((nbt.getInt("red") & 0x0ff) << 16) | ((nbt.getInt("green") & 0x0ff) << 8) | (nbt.getInt("blue") & 0x0ff);
    }

    private void autoIO() {
        for (Direction dir : Direction.values()) {
            TileEntity tile = level.getBlockEntity(worldPosition.offset(dir.getNormal()));
            if (tile == null) {
                continue;
            }
            if (this.furnaceSettings.get(dir.ordinal()) == 1 || this.furnaceSettings.get(dir.ordinal()) == 2 || this.furnaceSettings.get(dir.ordinal()) == 3 || this.furnaceSettings.get(dir.ordinal()) == 4) {
                if (tile != null) {
                    IItemHandler other = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).map(other1 -> other1).orElse(null);

                    if (other == null) {
                        continue;
                    }
                            if (other != null) {
                                if (this.getAutoInput() != 0 || this.getAutoOutput() != 0) {
                                    if (this.getAutoInput() == 1) {
                                        if (this.furnaceSettings.get(dir.ordinal()) == 1 || this.furnaceSettings.get(dir.ordinal()) == 3) {
                                            for (int input : INPUTS()) {
                                                if (this.getItem(input).getCount() >= this.getItem(input).getMaxStackSize()) {
                                                    continue;
                                                }
                                                for (int i = 0; i < other.getSlots(); i++) {
                                                    if (other.getStackInSlot(i).isEmpty()) {
                                                        continue;
                                                    }
                                                    ItemStack stack = other.extractItem(i, other.getStackInSlot(i).getMaxStackSize(), true);
                                                    if (hasRecipe(stack) && getItem(input).isEmpty() || ItemHandlerHelper.canItemStacksStack(getItem(input), stack)) {
                                                        insertItemInternal(input, other.extractItem(i, other.getStackInSlot(i).getMaxStackSize() - this.getItem(input).getCount(), false), false);
                                                    }
                                                }
                                            }
                                        }
                                        if (this.furnaceSettings.get(dir.ordinal()) == 4) {
                                            if (this.getItem(FUEL()).getCount() >= this.getItem(FUEL()).getMaxStackSize()) {
                                                continue;
                                            }
                                            for (int i = 0; i < other.getSlots(); i++) {
                                                if (other.getStackInSlot(i).isEmpty()) {
                                                    continue;
                                                }
                                                ItemStack stack = other.extractItem(i, other.getStackInSlot(i).getMaxStackSize(), true);
                                                if (isItemFuel(stack) && getItem(FUEL()).isEmpty() || ItemHandlerHelper.canItemStacksStack(getItem(FUEL()), stack)) {
                                                    insertItemInternal(FUEL(), other.extractItem(i, other.getStackInSlot(i).getMaxStackSize() - this.getItem(FUEL()).getCount(), false), false);
                                                }
                                            }
                                        }
                                    }
                                    if (this.getAutoOutput() == 1) {

                                        if (this.furnaceSettings.get(dir.ordinal()) == 4) {
                                            if (this.getItem(FUEL()).isEmpty()) {
                                                continue;
                                            }
                                            ItemStack stack = extractItemInternal(FUEL(), 1, true);
                                            if (stack.getItem() != Items.BUCKET) {
                                                continue;
                                            }
                                            for (int i = 0; i < other.getSlots(); i++) {
                                                if (other.isItemValid(i, stack) && (other.getStackInSlot(i).isEmpty() || other.isItemValid(i, stack) && (ItemHandlerHelper.canItemStacksStack(other.getStackInSlot(i), stack) && other.getStackInSlot(i).getCount() + stack.getCount() <= other.getSlotLimit(i)))) {
                                                    other.insertItem(i, extractItemInternal(FUEL(), stack.getCount(), false), false);
                                                }
                                            }
                                        }
                                        for (int output : OUTPUTS()) {
                                            if (this.furnaceSettings.get(dir.ordinal()) == 2 || this.furnaceSettings.get(dir.ordinal()) == 3) {
                                                if (this.getItem(output).isEmpty()) {
                                                    continue;
                                                }
                                                if (tile.getBlockState().getBlock().getRegistryName().toString().contains("storagedrawers:")) {
                                                    continue;
                                                }
                                                for (int i = 0; i < other.getSlots(); i++) {
                                                    ItemStack stack = extractItemInternal(output, this.getItem(output).getMaxStackSize() - other.getStackInSlot(i).getCount(), true);
                                                    if (other.isItemValid(i, stack) && (other.getStackInSlot(i).isEmpty() || other.isItemValid(i, stack)&& (ItemHandlerHelper.canItemStacksStack(other.getStackInSlot(i), stack) && other.getStackInSlot(i).getCount() + stack.getCount() <= other.getSlotLimit(i)))) {
                                                        other.insertItem(i, extractItemInternal(output, stack.getCount(), false), false);
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                    }
                }
            }
        }
    }

    @Nonnull
    public ItemStack insertItemInternal(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        if (!canPlaceItemThroughFace(slot, stack, null))
            return stack;

        ItemStack existing = this.inventory.get(slot);

        int limit = stack.getMaxStackSize();

        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate) {
            if (existing.isEmpty()) {
                this.inventory.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            this.setChanged();
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Nonnull
    private ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        if (amount == 0)
            return ItemStack.EMPTY;

        ItemStack existing = this.getItem(slot);

        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.setItem(slot, ItemStack.EMPTY);
                this.setChanged();
                return existing;
            } else {
                return existing.copy();
            }
        } else {
            if (!simulate) {
                this.setItem(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                this.setChanged();
            }

            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }

    //CLIENT SYNC
    public int getSettingBottom() {
        return this.furnaceSettings.get(getIndexBottom());
    }
    public int getSettingTop() {
        return this.furnaceSettings.get(getIndexTop());
    }
    public int getSettingFront() {
        return this.furnaceSettings.get(getIndexFront());
    }
    public int getSettingBack() {
        return this.furnaceSettings.get(getIndexBack());
    }
    public int getSettingLeft() {
        return this.furnaceSettings.get(getIndexLeft());
    }
    public int getSettingRight() {
        return this.furnaceSettings.get(getIndexRight());
    }

    public int getIndexFront() {
        int i = facing().ordinal();
        return i;
    }

    public int getIndexBack() {
        int i = facing().getOpposite().ordinal();
        return i;
    }

    public int getIndexLeft() {
        if (facing() == Direction.NORTH) {
            return Direction.EAST.ordinal();
        } else if (facing() == Direction.WEST) {
            return Direction.NORTH.ordinal();
        } else if (facing() == Direction.SOUTH) {
            return Direction.WEST.ordinal();
        } else {
            return Direction.SOUTH.ordinal();
        }
    }

    public int getIndexRight() {
        if (facing() == Direction.NORTH) {
            return Direction.WEST.ordinal();
        } else if (facing() == Direction.WEST) {
            return Direction.SOUTH.ordinal();
        } else if (facing() == Direction.SOUTH) {
            return Direction.EAST.ordinal();
        } else {
            return Direction.NORTH.ordinal();
        }
    }

    public int getAutoInput() {
        return this.furnaceSettings.get(6);
    }

    public int getAutoOutput() {
        return this.furnaceSettings.get(7);
    }

    public int getRedstoneSetting() {
        return this.furnaceSettings.get(8);
    }

    public int getRedstoneComSub() {
        return this.furnaceSettings.get(9);
    }



    public boolean isBurning() {
        return this.furnaceBurnTime > 0;
    }
    ITag<Item> ore = ItemTags.getAllTags().getTag(new ResourceLocation("forge", "ores"));
    protected boolean isOre(ItemStack input){
        return (input.getItem().is(ore) || input.getItem().getRegistryName().toString().contains("ore"));
    }
    protected int OreProcessingMultiplier(ItemStack input){
        if (hasUpgradeType(Registration.ORE_PROCESSING.get())){
            ItemUpgradeOreProcessing oreup = (ItemUpgradeOreProcessing)getUpgradeTypeSlotItem(Registration.ORE_PROCESSING.get()).getItem();
            if  (isOre( input)) return oreup.getMultiplier;

        } else if (input == ItemStack.EMPTY) return 0;
        return 1;
    }

    protected boolean canSmelt(@Nullable IRecipe<?> recipe, int INPUT, int OUTPUT) {
        ItemStack input = this.inventory.get(INPUT);
        if (!input.isEmpty() && recipe != null) {
            ItemStack recipeOutput = recipe.getResultItem();
            if (!recipeOutput.isEmpty()) {
                ItemStack output = this.inventory.get(OUTPUT);
                if (output.isEmpty()) return true;
                else if (!output.sameItem(recipeOutput)) return false;
                else {
                    return output.getCount() + recipeOutput.getCount() * OreProcessingMultiplier(input) <= output.getMaxStackSize();
                }
            }
        }
        return false;
    }
    private ItemStack getResult(@Nullable IRecipe<?> recipe, ItemStack input) {
        ItemStack out = recipe.getResultItem().copy();
        out.setCount(out.getCount() * OreProcessingMultiplier(input));
        return out;
    }

    protected void smeltItem(@Nullable IRecipe<?> recipe, int INPUT, int OUTPUT) {
        timer = 0;
        if (recipe != null && this.canSmelt(recipe, INPUT, OUTPUT)) {
            ItemStack itemstack = this.inventory.get(INPUT);
            ItemStack itemstack2 = this.inventory.get(OUTPUT);
            if (itemstack2.isEmpty()) {
                this.inventory.set(OUTPUT, getResult(recipe, itemstack));
                if (hasUpgrade(Registration.ORE_PROCESSING.get()) && ((isOre(itemstack)))) {
                    breakDurabilityItem(getUpgradeSlotItem(Registration.ORE_PROCESSING.get()));
                }
            } else if (itemstack2.getItem() == getResult(recipe, itemstack).getItem()) {
                itemstack2.grow(getResult(recipe, itemstack).getCount());
                if (hasUpgrade(Registration.ORE_PROCESSING.get()) && (isOre(itemstack))) {
                    breakDurabilityItem(getUpgradeSlotItem(Registration.ORE_PROCESSING.get()));
                }
            }
            this.checkXP(recipe);
            if (!this.level.isClientSide) {
                this.setRecipeUsed(recipe);
            }

            if (itemstack.getItem() == Blocks.WET_SPONGE.asItem() && !this.inventory.get(FUEL()).isEmpty() && this.inventory.get(FUEL()).getItem() == Items.BUCKET) {
                this.inventory.set(FUEL(), new ItemStack(Items.WATER_BUCKET));
            }
            if (ModList.get().isLoaded("pmmo")) {
                FurnaceHandler.handleSmelted(itemstack, itemstack2, level, worldPosition, 0);
                if (this.recipeType == IRecipeType.SMOKING) {
                    FurnaceHandler.handleSmelted(itemstack, itemstack2, level, worldPosition, 1);
                }
            }
            itemstack.shrink(1);
        }
    }


    @Override
    public void load(BlockState state, CompoundNBT tag) {
        ItemStackHelper.loadAllItems(tag, this.inventory);
        this.furnaceBurnTime = tag.getInt("BurnTime");
        this.cookTime = tag.getInt("CookTime");
        this.totalCookTime = tag.getInt("CookTimeTotal");
        this.timer = 0;
        this.recipesUsed = this.getBurnTime(this.inventory.get(1));
        if (tag.get("fluidTank") != null)
            CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.readNBT(fluidTank, null, tag.get("fluidTank"));
        if (tag.get("xpTank") != null)
            CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.readNBT(xpTank, null, tag.get("xpTank"));
        CompoundNBT compoundnbt = tag.getCompound("RecipesUsed");
            CapabilityEnergy.ENERGY.readNBT(energyStorage, null, tag.get("energyStorage"));
            for (String s : compoundnbt.getAllKeys()) {
            this.recipes.put(new ResourceLocation(s), compoundnbt.getInt(s));
        }
        this.show_inventory_settings = tag.getInt("ShowInvSettings");

        super.load(state, tag);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        ItemStackHelper.saveAllItems(tag, this.inventory);
        tag.putInt("BurnTime", this.furnaceBurnTime);
        tag.putInt("CookTime", this.cookTime);
        tag.putInt("CookTimeTotal", this.totalCookTime);
        tag.put("fluidTank", CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.writeNBT(fluidTank, null));
        tag.put("xpTank", CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.writeNBT(xpTank, null));
        tag.put("energyStorage", CapabilityEnergy.ENERGY.writeNBT(energyStorage, null));
        tag.putInt("ShowInvSettings", this.show_inventory_settings);
        CompoundNBT compoundnbt = new CompoundNBT();
        this.recipes.forEach((recipeId, craftedAmount) -> {
            compoundnbt.putInt(recipeId.toString(), craftedAmount);
        });
        tag.put("RecipesUsed", compoundnbt);

        return tag;
    }

    protected static int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        } else {
            Item item = stack.getItem();
            int ret = stack.getBurnTime();
            return net.minecraftforge.event.ForgeEventFactory.getItemBurnTime(stack, ret == -1 ? AbstractFurnaceTileEntity.getFuel().getOrDefault(item, 0) : ret);
        }
    }


    public static boolean isItemFuel(ItemStack stack) {
        return getBurnTime(stack) > 0;
    }
    SidedInvWrapper invHandler = new
            SidedInvWrapper (this, null){
                @Override
                public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                    return IisItemValidForSlot(slot, stack);
                }
            };
    LazyOptional<? extends IItemHandler>[] invHandlers =
            invHandler.create(this, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    @Nonnull
    @Override
    public <
            T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (!this.isRemoved()) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                if (facing != null) {
                    if (facing == Direction.DOWN)
                        return invHandlers[0].cast();
                    else if (facing == Direction.UP)
                        return invHandlers[1].cast();
                    else if (facing == Direction.NORTH)
                        return invHandlers[2].cast();
                    else if (facing == Direction.SOUTH)
                        return invHandlers[3].cast();
                    else if (facing == Direction.WEST)
                        return invHandlers[4].cast();
                    else
                        return invHandlers[5].cast();
                }
            }
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                if ((facing == null || facing.ordinal() == getIndexTop() || facing.ordinal() == getIndexBottom())) {
                    if(isLiquid())
                        return (LazyOptional.of(() -> fluidTank).cast());
                }
                else {
                    if (hasXPTank())
                        return (LazyOptional.of(() -> xpTank).cast());
                }
            }
            if ((hasUpgrade(Registration.ENERGY.get())) && capability == CapabilityEnergy.ENERGY)
                return (LazyOptional.of(() -> energyStorage).cast());
        }
        return super.getCapability(capability, facing);
    }
    public int getIndexBottom() {
        return 0;
    }
    public int getIndexTop() {
        return 1;
    }


    @Override
    public int[] IgetSlotsForFace(Direction side) {
        if (hasUpgradeType(Registration.FACTORY.get())) {
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 0) {
                return new int[]{};
            } else if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 1) {
                return ISLOTS();
            } else if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 2) {
                return OUTPUTS();
            } else if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 3) {
                return FSLOTS();
            } else if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 4) {
                return new int[]{FUEL()};
            }
        }else {
            if (side == side.UP) return INPUTS();
            else if (side == side.DOWN) return OUTPUTS();
            else return new int[]{FUEL()};
        }

        return new int[]{};
    }

    @Override
    public boolean IcanExtractItem(int index, ItemStack stack, Direction direction) {
        if (hasUpgradeType(Registration.FACTORY.get())) {
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 0) {
                return false;
            } else if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 1) {
                return false;
            } else if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 2) {
                return (index >= FOUTPUT() && index <= LOUTPUT());
            } else if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 3) {
                return (index >= FOUTPUT() && index <= LOUTPUT());
            } else if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 4 && stack.getItem() != Items.BUCKET) {
                return false;
            } else if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 4 && stack.getItem() == Items.BUCKET) {
                return true;
            }
        }else{
            if (direction == direction.DOWN && index >= FOUTPUT() && index <= LOUTPUT()) return true;
        }
        return false;
    }

    @Override
    public boolean IisItemValidForSlot(int index, ItemStack stack) {
        if (index >= FOUTPUT() && index <= LOUTPUT())
            return false;

        if (index >= FINPUT() && index <= LINPUT()) {
            if (stack.isEmpty()) {
                return false;
            }

            return hasRecipe(stack);
        }

        if (index == FUEL()) {
            ItemStack itemstack = getItem(FUEL());
            return getBurnTime(stack) > 0 || (stack.getItem() == Items.BUCKET && itemstack.getItem() != Items.BUCKET);
        }
        if (index > LOUTPUT()) {
            return (stack.getItem() instanceof ItemUpgrade || (stack.getItem() instanceof ItemUpgradeLiquidFuel && !(this instanceof BlockForgeTileBase))) && !hasUpgrade(stack.getItem()) && !hasUpgradeType((ItemUpgrade) stack.getItem());
        }
        return false;
    }

    public void checkXP(@Nullable IRecipe<?> recipe) {
        if (!level.isClientSide) {
            boolean flag2 = false;
            if (this.recipes.size() > Config.furnaceXPDropValue.get()) {
                this.grantStoredRecipeExperience(level, new Vector3d(worldPosition.getX() + rand.nextInt(2) - 1, worldPosition.getY(), worldPosition.getZ() + rand.nextInt(2) - 1));
                this.recipes.clear();
            } else {
                for (Object2IntMap.Entry<ResourceLocation> entry : this.recipes.object2IntEntrySet()) {
                    if (level.getRecipeManager().byKey(entry.getKey()).isPresent()) {
                        if (entry.getIntValue() > Config.furnaceXPDropValue2.get()) {
                            if (!flag2) {
                                this.grantStoredRecipeExperience(level, new Vector3d(worldPosition.getX() + rand.nextInt(2) - 1, worldPosition.getY(), worldPosition.getZ() + rand.nextInt(2) - 1));
                            }
                            flag2 = true;
                        }
                    }

                }
                if (flag2) {
                    this.recipes.clear();
                }
            }
        }
    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> recipe) {

        if (recipe != null) {
            ResourceLocation resourcelocation = recipe.getId();
            this.recipes.addTo(resourcelocation, 1);
        }

    }

    @Nullable
    @Override
    public IRecipe<?> getRecipeUsed() {
        return null;
    }

    public void unlockRecipes(PlayerEntity player) {
        List<IRecipe<?>> list = this.grantStoredRecipeExperience(player.level, player.position());
        player.awardRecipes(list);
        this.recipes.clear();
    }

    public List<IRecipe<?>> grantStoredRecipeExperience(World level, Vector3d worldPosition) {
        List<IRecipe<?>> list = Lists.newArrayList();
        if (this.recipes.object2IntEntrySet() != null)
            for (Object2IntMap.Entry<ResourceLocation> entry : this.recipes.object2IntEntrySet()) {
                level.getRecipeManager().byKey(entry.getKey()).ifPresent((h) -> {
                    list.add(h);
                    int amountLiquidXp = MathHelper.floor((float) entry.getIntValue() * ((AbstractCookingRecipe) h).getExperience()) * 5;
                    if (hasXPTank()) {
                        if (amountLiquidXp >= 1) {
                            xpTank.fill(new FluidStack(Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(Config.getLiquidXPType()))), amountLiquidXp), IFluidHandler.FluidAction.EXECUTE);
                            recipes.clear();
                        }
                    } else {
                        if (worldPosition != null)
                            splitAndSpawnExperience(level, worldPosition, entry.getIntValue(), ((AbstractCookingRecipe) h).getExperience());
                    }
                });
            }

        return list;
    }

    private static void splitAndSpawnExperience(World level, Vector3d worldPosition, int craftedAmount, float experience) {
        int i = MathHelper.floor((float) craftedAmount * experience);
        float f = MathHelper.frac((float) craftedAmount * experience);
        if (f != 0.0F && Math.random() < (double) f) {
            ++i;
        }

        while (i > 0) {
            int j = ExperienceOrbEntity.getExperienceValue(i);
            i -= j;
            level.addFreshEntity(new ExperienceOrbEntity(level, worldPosition.x, worldPosition.y, worldPosition.z, j));
        }

    }

    @Override
    public void fillStackedContents(RecipeItemHelper helper) {
        for (ItemStack itemstack : this.inventory) {
            helper.accountStack(itemstack);
        }

    }

    protected boolean doesNeedUpdateSend() {
        return !Arrays.equals(this.provides, this.lastProvides);
    }

    public void onUpdateSent() {
        System.arraycopy(this.provides, 0, this.lastProvides, 0, this.provides.length);
        this.level.updateNeighborsAt(this.worldPosition, getBlockState().getBlock());
    }


}