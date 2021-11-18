package wily.ultimatefurnaces.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wily.betterfurnaces.gui.BlockForgeScreenBase;
import wily.ultimatefurnaces.container.BlockUltimateForgeContainer;

@OnlyIn(Dist.CLIENT)
public class BlockUltimateForgeScreen extends BlockForgeScreenBase<BlockUltimateForgeContainer> {


    public BlockUltimateForgeScreen(BlockUltimateForgeContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }

}
