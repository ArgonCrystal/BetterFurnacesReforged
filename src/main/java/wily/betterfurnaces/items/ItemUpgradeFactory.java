package wily.betterfurnaces.items;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wily.betterfurnaces.BetterFurnacesReforged;
import wily.betterfurnaces.tileentity.FurnaceSettings;

import java.util.List;

public class ItemUpgradeFactory extends ItemUpgrade {
public boolean canOutput;
public boolean canInput;
public boolean pipeSide;
public boolean redstoneSignal;
FurnaceSettings furnaceSettings;

    public ItemUpgradeFactory(Properties properties, String tooltip, boolean output, boolean input, boolean pipe, boolean redstone) {
        super(properties,5,tooltip);
        canOutput = output;
        canInput = input;
        pipeSide = pipe;
        redstoneSignal = redstone;
        furnaceSettings = new FurnaceSettings() {
        };
    }
    public void inventoryTick(ItemStack stack, World world, Entity player, int slot, boolean selected) {
        super.inventoryTick(stack, world, player, slot, selected);
        CompoundNBT nbt;
        nbt = stack.getOrCreateTag();
        if (!(nbt.contains("Settings") && nbt.contains("AutoIO") && nbt.contains("Redstone"))) {
            placeConfig();
            furnaceSettings.write(nbt);
            stack.setTag(nbt);
        }
    }
    public void placeConfig() {

        if (this.furnaceSettings != null) {
            this.furnaceSettings.set(0, 2);
            this.furnaceSettings.set(1, 1);
            for (Direction dir : Direction.values()) {
                if (dir != Direction.DOWN && dir != Direction.UP) {
                    this.furnaceSettings.set(dir.ordinal(), 4);
                }
            }
        }

    }
}
