package makeo.gadomancy.common.utils;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class StickyJarBlockAccess implements IBlockAccess {
    private final IBlockAccess world;
    private final int x;
    private final int y;
    private final int z;
    private final int fakeMeta;

    public StickyJarBlockAccess(IBlockAccess world, int x, int y, int z, int fakeMeta) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.fakeMeta = fakeMeta;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (x == this.x && y == this.y && z == this.z)
            return fakeMeta;

        return world.getBlockMetadata(x, y, z);
    }

    @Override
    public int getHeight() {
        return world.getHeight();
    };

    @Override
    public boolean extendedLevelsInChunkCache() {
        return world.extendedLevelsInChunkCache();
    };

    @Override
    public BiomeGenBase getBiomeGenForCoords(int arg0, int arg1) {
        return world.getBiomeGenForCoords(arg0, arg1);
    };

    @Override
    public Block getBlock(int arg0, int arg1, int arg2) {
        return world.getBlock(arg0, arg1, arg2);
    };

    @Override
    public int getLightBrightnessForSkyBlocks(int arg0, int arg1, int arg2, int arg3) {
        return world.getLightBrightnessForSkyBlocks(arg0, arg1, arg2, arg3);
    };

    @Override
    public TileEntity getTileEntity(int arg0, int arg1, int arg2) {
        return world.getTileEntity(arg0, arg1, arg2);
    };

    @Override
    public boolean isAirBlock(int arg0, int arg1, int arg2) {
        return world.isAirBlock(arg0, arg1, arg2);
    };

    @Override
    public int isBlockProvidingPowerTo(int arg0, int arg1, int arg2, int arg3) {
        return world.isBlockProvidingPowerTo(arg0, arg1, arg2, arg3);
    };

    @Override
    public boolean isSideSolid(int arg0, int arg1, int arg2, net.minecraftforge.common.util.ForgeDirection arg3, boolean arg4) {
        return world.isSideSolid(arg0, arg1, arg2, arg3, arg4);
    };
}
