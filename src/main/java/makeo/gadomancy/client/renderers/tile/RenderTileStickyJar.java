package makeo.gadomancy.client.renderers.tile;

import makeo.gadomancy.common.blocks.tiles.TileStickyJar;
import makeo.gadomancy.common.utils.StickyJarBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.tiles.TileJarFillable;

/**
 * This class is part of the Gadomancy Mod
 * Gadomancy is Open Source and distributed under the
 * GNU LESSER GENERAL PUBLIC LICENSE
 * for more read the LICENSE file
 *
 * Created by makeo @ 14.07.2015 21:05
 */
public class RenderTileStickyJar extends TileEntitySpecialRenderer {
    private static final RenderBlocks RENDER_BLOCKS = new RenderBlocks();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileStickyJar) || !((TileStickyJar) tile).isValid())
            return;

        TileStickyJar stickyJar = (TileStickyJar) tile;

        TileJarFillable parent = stickyJar.getParent();
        World world = tile.getWorldObj();
        Block parentBlock = stickyJar.getParentBlock();
        // if (world == null || parentBlock == null)
        //     return;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        ForgeDirection placedOn = ForgeDirection.getOrientation(
                world.getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord));

        rotateJar(placedOn, ForgeDirection.getOrientation(stickyJar.facing));

        // ---------- TESR родителя ----------
        TileEntitySpecialRenderer renderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(parent);
        if (renderer != null) {
            stickyJar.syncToParent();
            renderer.renderTileEntityAt(parent, 0, 0, 0, partialTicks);
            stickyJar.syncFromParent();
        }

        // ---------- ISimpleBlockHandler ----------
        GL11.glPushMatrix();
        // GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        bindTexture(TextureMap.locationBlocksTexture);

        // proxy: подмена метадаты (ТИП банки) в текущих координатах
        int parentMeta = stickyJar.getParentMetadata() != null ? stickyJar.getParentMetadata() : 0;
        IBlockAccess proxy = new StickyJarBlockAccess(world, tile.xCoord, tile.yCoord, tile.zCoord, parentMeta);

        IBlockAccess prevAccess = RENDER_BLOCKS.blockAccess;
        try {

            RENDER_BLOCKS.blockAccess = proxy;

            RENDER_BLOCKS.renderAllFaces = false;
            RENDER_BLOCKS.enableAO = false;

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();

            tess.setNormal(0, -placedOn.offsetY, -Math.abs(placedOn.offsetZ + placedOn.offsetX));
            tess.setTranslation(-tile.xCoord, -tile.yCoord, -tile.zCoord);

            // РЕНДЕРИМ ИМЕННО parentBlock, но мета берётся из proxy (parentMeta)
            RENDER_BLOCKS.renderBlockByRenderType(parentBlock, tile.xCoord, tile.yCoord, tile.zCoord);

            tess.setTranslation(0, 0, 0);
            tess.draw();

            tess.setBrightness(0);
        } finally {
            // вернуть blockAccess
            RENDER_BLOCKS.blockAccess = prevAccess;
        }

        // GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glPopMatrix(); // <-- закрываем самый первый PushMatrix()
    }

    private void rotateJar(ForgeDirection placedOn, ForgeDirection facing) {
        if (placedOn == ForgeDirection.DOWN)
            return;

        GL11.glTranslatef(0.5f, 0.5f, 0.5f);

        switch (placedOn) {
            case UP:
                GL11.glRotatef(180, 1, 0, 0);
                break;

            case NORTH:
                GL11.glRotatef(90, 1, 0, 0);
                break;

            case SOUTH:
                GL11.glRotatef(-90, 1, 0, 0);
                break;

            case WEST:
                GL11.glRotatef(-90, 0, 0, 1);
                break;

            case EAST:
                GL11.glRotatef(90, 0, 0, 1);
                break;
        }

        GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
    }
}
