package wily.betterfurnaces.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraftforge.fluids.FluidStack;

public class FluidRenderUtil {
    public static void renderTiledFluid(MatrixStack matrix, ContainerScreen screen, int x, int y, int sizeX, int sizeY, FluidStack fluid, boolean hasColor){
            TextureAtlasSprite fluidSprite = screen.getMinecraft().getTextureAtlas(PlayerContainer.BLOCK_ATLAS)
                    .apply(fluid.getFluid().getAttributes().getStillTexture(fluid)
                    );
            if (hasColor){
                int color = fluid.getFluid().getAttributes().getColor();
                float a = ((color & 0xFF000000) >> 24) / 255F;
                a = a <= 0.001F ? 1 : a;
                float r = ((color & 0xFF0000) >> 16) / 255F;
                float g = ((color & 0xFF00) >> 8) / 255F;
                float b = (color & 0xFF) / 255F;
                RenderSystem.color4f(r,g,b,a);
            }
            screen.getMinecraft().getTextureManager().bind(fluidSprite.atlas().location());
            screen.blit(matrix, screen.getGuiLeft() + x, screen.getGuiTop() + y, screen.getBlitOffset(), sizeX, sizeY, fluidSprite);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
