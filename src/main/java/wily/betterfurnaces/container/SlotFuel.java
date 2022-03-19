package wily.betterfurnaces.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import wily.betterfurnaces.blockentity.BlockEntitySmeltingBase;
import wily.betterfurnaces.init.Registration;

public class SlotFuel extends Slot {
    BlockEntitySmeltingBase be;
    public SlotFuel(Container te, int index, int x, int y) {
        super(te, index, x, y);
        if (te instanceof  BlockEntitySmeltingBase)
            be = (BlockEntitySmeltingBase) te;
    }

    /**
     * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
     */
    public boolean mayPlace(ItemStack stack) {
        return BlockEntitySmeltingBase.isItemFuel(stack) && stack.getItem() != Items.BUCKET || stack.getCapability(CapabilityEnergy.ENERGY).isPresent() && be.hasUpgrade(Registration.ENERGY.get()) || isContainer(stack) && be.isLiquid() && stack.getCount() == 1;
    }

    public int getMaxStackSize(ItemStack stack) {
        return isContainer(stack) ? 1 : super.getMaxStackSize(stack);
    }

    public static boolean isContainer(ItemStack stack) {
        return stack.hasContainerItem();
    }
}
