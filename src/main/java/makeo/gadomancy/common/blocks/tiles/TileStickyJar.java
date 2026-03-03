package makeo.gadomancy.common.blocks.tiles;

import cpw.mods.fml.common.registry.GameData;
import makeo.gadomancy.common.utils.Injector;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileJarFillable;
import thaumcraft.common.tiles.TileJarFillableVoid;

import java.lang.reflect.Field;

/**
 * This class is part of the Gadomancy Mod
 * Gadomancy is Open Source and distributed under the
 * GNU LESSER GENERAL PUBLIC LICENSE
 * for more read the LICENSE file
 *
 * Created by makeo @ 13.07.2015 15:48
 */
public class TileStickyJar extends TileJarFillable {

    private Block parentBlock;
    private Integer parentMetadata = 0;

    private TileJarFillable parent = null;

    public ForgeDirection placedOn = ForgeDirection.DOWN;

    private final Injector injector;
    private Field fieldCount;

    public TileStickyJar() {
        injector = new Injector(null, TileJarFillable.class);
        fieldCount = Injector.getField("count", TileJarFillable.class);
    }

    public Block getParentBlock() {
        return parentBlock;
    }

    public TileJarFillable getParent() {
        return parent;
    }

    public boolean isValid() {
        return parentBlock != null && parent != null;
    }

    public ForgeDirection getPlacedOn() {
        if (worldObj == null)
            return ForgeDirection.DOWN;

        ForgeDirection metaDir = ForgeDirection.getOrientation(
                worldObj.getBlockMetadata(xCoord, yCoord, zCoord));

        return metaDir != null ? metaDir : ForgeDirection.DOWN;
    }

    private boolean needsRenderUpdate = false;

    // public void init(TileJarFillable parent,
    // Block parentBlock,
    // int parentMetadata,
    // ForgeDirection placedOn) {
    // // this.placedOn = (placedOn != null ? placedOn.getOpposite() :
    // ForgeDirection.DOWN);
    // this.placedOn = getPlacedOn();

    // // if (worldObj != null && !worldObj.isRemote) {
    // // worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord,
    // this.placedOn.ordinal(), 2);
    // // }

    // init(parent, parentBlock, parentMetadata);
    // }

    public void init(TileJarFillable parent, Block parentBlock, int parentMetadata) {
        this.parent = parent;
        this.parentBlock = parentBlock;
        this.parentMetadata = parentMetadata;

        this.parent.setWorldObj(worldObj);
        this.parent.xCoord = xCoord;
        this.parent.yCoord = yCoord;
        this.parent.zCoord = zCoord;

        this.injector.setObject(this.parent);

        syncFromParent();

        markDirty();
        needsRenderUpdate = true;

        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void initFromNBT(NBTTagCompound parentData, Block parentBlock, int parentMetadata) {
        this.parentBlock = parentBlock;
        this.parentMetadata = parentMetadata;

        if (parentMetadata >= 0 && parentMetadata <= 6) {
            System.out.println("WARN: parentMetadata looks like placedOn meta = " + parentMetadata);
        }

        TileEntity t = TileEntity.createAndLoadEntity(parentData);
        if (!(t instanceof TileJarFillable)) {
            this.parent = null;
            return;
        }

        this.parent = (TileJarFillable) t;
        this.injector.setObject(this.parent);

        // важно: мир/координаты
        this.parent.setWorldObj(worldObj);
        this.parent.xCoord = xCoord;
        this.parent.yCoord = yCoord;
        this.parent.zCoord = zCoord;

        // подтянуть поля в sticky (aspect/amount/и т.п.)
        syncFromParent();

        markDirty();
        needsRenderUpdate = true;

        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    private void sync(TileJarFillable from, TileJarFillable to) {
        if (from == null || to == null)
            return;
        
        to.aspect = from.aspect;
        to.aspectFilter = from.aspectFilter;
        to.amount = from.amount;
        to.maxAmount = from.maxAmount;
        to.facing = from.facing;
        to.forgeLiquid = from.forgeLiquid;
        to.lid = from.lid;
    }

    public void syncToParent() {
        sync(this, parent);
    }

    public void syncFromParent() {
        sync(parent, this);
    }

    public Integer getParentMetadata() {
        return parentMetadata;
    }

    public void setParentMetadata(int meta) {
        this.parentMetadata = meta;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();

        if (parent != null) {
            syncToParent();
        }

        writeCustomNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readCustomNBT(pkt.func_148857_g());

        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    private void syncPlacedOnFromBlock() {
        placedOn = getPlacedOn();
    }

    private int count;

    @Override
    public void updateEntity() {
        syncPlacedOnFromBlock();

        if (!isValid()) {
            return;
        }

        if (worldObj.isRemote && needsRenderUpdate) {
            needsRenderUpdate = false;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        syncToParent();

        boolean canTakeEssentia = amount < maxAmount;
        if (parent instanceof TileJarFillableVoid) {
            canTakeEssentia = true;
        }

        if (!worldObj.isRemote && (++count % 5 == 0) && canTakeEssentia) {
            fillJar();
        }

        injector.setField(fieldCount, 1);

        if (!worldObj.isRemote) {
            parent.updateEntity();
        }

        syncFromParent();
    }

    @Override
    public void setWorldObj(World world) {
        super.setWorldObj(world);
        if (parent != null)
            parent.setWorldObj(worldObj);
    }

    private void fillJar() {
        ForgeDirection inputDir = getPlacedOn();

        TileEntity te = ThaumcraftApiHelper.getConnectableTile(parent.getWorldObj(), parent.xCoord, parent.yCoord,
                parent.zCoord, inputDir);
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canOutputTo(ForgeDirection.DOWN)) {
                return;
            }
            Aspect ta = null;
            if (parent.aspectFilter != null) {
                ta = parent.aspectFilter;
            } else if ((parent.aspect != null) && (parent.amount > 0)) {
                ta = parent.aspect;
            } else if ((ic.getEssentiaAmount(inputDir.getOpposite()) > 0) &&
                    (ic.getSuctionAmount(inputDir.getOpposite()) < getSuctionAmount(ForgeDirection.UP))
                    && (getSuctionAmount(ForgeDirection.UP) >= ic.getMinimumSuction())) {
                ta = ic.getEssentiaType(inputDir.getOpposite());
            }
            if ((ta != null) && (ic.getSuctionAmount(inputDir.getOpposite()) < getSuctionAmount(ForgeDirection.UP))) {
                addToContainer(ta, ic.takeEssentia(ta, 1, inputDir.getOpposite()));
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {

        // A) parentBlock + parentMetadata (ТИП банки) — для текстур/логики
        String parentType = compound.getString("parentType");
        if (parentType != null && !parentType.isEmpty()) {
            Block block = GameData.getBlockRegistry().getObject(parentType);
            if (block != null) {
                this.parentBlock = block;
                this.parentMetadata = compound.hasKey("parentMetadata") ? compound.getInteger("parentMetadata") : 0;
            }
        }

        // B) placedOn (СТОРОНА крепления) — это МЕТАДАТА sticky-блока
        if (worldObj != null && compound.hasKey("placedOn")) {
            int placed = compound.getInteger("placedOn");
            if (placed < 0 || placed > 6)
                placed = ForgeDirection.DOWN.ordinal();
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, placed, 2);
        }

        // C) если нет parent NBT — выходим, но parentBlock/parentMetadata уже сохранены
        // (рендер сможет жить)
        if (parentBlock == null || !compound.hasKey("parent")) {
            return;
        }

        // D) восстановить ТЕ родителя из NBT (важно: не через createTileEntity, а через
        // createAndLoadEntity)
        NBTTagCompound parentTag = compound.getCompoundTag("parent");

        // через один вход: создаём parent TE из NBT и настраиваем всё одинаково
        initFromNBT(parentTag, parentBlock, this.parentMetadata != null ? this.parentMetadata : 0);

        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {

        // 1) Всегда пишем хотя бы тип и мету родителя (если знаем блок)
        if (parentBlock != null) {
            String id = GameData.getBlockRegistry().getNameForObject(parentBlock);
            if (id != null && !id.isEmpty()) {
                compound.setString("parentType", id);
                compound.setInteger("parentMetadata", parentMetadata != null ? parentMetadata : 0);
            }
        }

        // 2) placedOn можно писать всегда, если мир уже есть
        if (worldObj != null) {
            compound.setInteger("placedOn", getPlacedOn().ordinal());
        }

        // 3) Сам NBT родителя пишем только если parent уже создан
        if (parent != null) {
            syncToParent();
            NBTTagCompound data = new NBTTagCompound();
            parent.writeToNBT(data);
            compound.setTag("parent", data);
        }
    }

    @Override
    public AspectList getAspects() {
        if (isValid()) {
            syncToParent();
            AspectList result = parent.getAspects();
            syncFromParent();
            return result;
        }
        return new AspectList();
    }

    @Override
    public void setAspects(AspectList paramAspectList) {
        if (isValid()) {
            syncToParent();
            parent.setAspects(paramAspectList);
            syncFromParent();
        }
    }

    @Override
    public boolean doesContainerAccept(Aspect paramAspect) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.doesContainerAccept(paramAspect);
            syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public int addToContainer(Aspect paramAspect, int paramInt) {
        if (isValid()) {
            syncToParent();
            int result = parent.addToContainer(paramAspect, paramInt);
            syncFromParent();
            return result;
        }
        return paramInt;
    }

    @Override
    public boolean takeFromContainer(Aspect paramAspect, int paramInt) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.takeFromContainer(paramAspect, paramInt);
            syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList paramAspectList) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.takeFromContainer(paramAspectList);
            syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect paramAspect, int paramInt) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.doesContainerContainAmount(paramAspect, paramInt);
            syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList paramAspectList) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.doesContainerContain(paramAspectList);
            syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public int containerContains(Aspect paramAspect) {
        if (isValid()) {
            syncToParent();
            int result = parent.containerContains(paramAspect);
            syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public boolean isConnectable(ForgeDirection face) {
        if (isValid()) {
            syncToParent();
            return parent.isConnectable(changeDirection(face));
        }
        return false;
    }

    @Override
    public boolean canInputFrom(ForgeDirection face) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.canInputFrom(changeDirection(face));
            syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean canOutputTo(ForgeDirection face) {
        if (isValid()) {
            syncToParent();
            boolean result = parent.canOutputTo(changeDirection(face));
            syncFromParent();
            return result;
        }
        return false;
    }

    public ForgeDirection changeDirection(ForgeDirection face) {
        ForgeDirection placedOn = getPlacedOn();

        if (placedOn == ForgeDirection.UP) {
            if (face == ForgeDirection.UP || face == ForgeDirection.DOWN) {
                return face.getOpposite();
            }
            return face;
        }

        if (placedOn == ForgeDirection.DOWN) {
            return face;
        }

        if (face == ForgeDirection.UP) {
            return ForgeDirection.NORTH;
        }
        if (face == ForgeDirection.DOWN) {
            return ForgeDirection.SOUTH;
        }
        if (face == placedOn) {
            return ForgeDirection.DOWN;
        }
        if (face == placedOn.getOpposite()) {
            return ForgeDirection.UP;
        }

        switch (placedOn) {
            case EAST:
                return face == ForgeDirection.NORTH ? ForgeDirection.WEST : ForgeDirection.EAST;
            case SOUTH:
                return face.getOpposite();
            case WEST:
                return face == ForgeDirection.SOUTH ? ForgeDirection.WEST : ForgeDirection.EAST;
        }

        return face;
    }

    @Override
    public void setSuction(Aspect paramAspect, int paramInt) {
        if (isValid()) {
            syncToParent();
            parent.setSuction(paramAspect, paramInt);
            syncFromParent();
        }
    }

    @Override
    public Aspect getSuctionType(ForgeDirection paramForgeDirection) {
        if (isValid()) {
            syncToParent();
            Aspect result = parent.getSuctionType(changeDirection(paramForgeDirection));
            syncFromParent();
            return result;
        }
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection paramForgeDirection) {
        if (isValid()) {
            syncToParent();
            int result = parent.getSuctionAmount(changeDirection(paramForgeDirection));
            syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public int takeEssentia(Aspect paramAspect, int paramInt, ForgeDirection paramForgeDirection) {
        if (isValid()) {
            syncToParent();
            int result = parent.takeEssentia(paramAspect, paramInt, changeDirection(paramForgeDirection));
            syncFromParent();
            return result;
        }
        return paramInt;
    }

    @Override
    public int addEssentia(Aspect paramAspect, int paramInt, ForgeDirection paramForgeDirection) {
        if (isValid()) {
            syncToParent();
            int result = parent.addEssentia(paramAspect, paramInt, changeDirection(paramForgeDirection));
            syncFromParent();
            return result;
        }
        return paramInt;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection paramForgeDirection) {
        if (isValid()) {
            syncToParent();
            Aspect result = parent.getEssentiaType(changeDirection(paramForgeDirection));
            syncFromParent();
            return result;
        }
        return null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection paramForgeDirection) {
        if (isValid()) {
            syncToParent();
            int result = parent.getEssentiaAmount(changeDirection(paramForgeDirection));
            syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        if (isValid()) {
            syncToParent();
            int result = parent.getMinimumSuction();
            syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        if (isValid()) {
            syncToParent();
            boolean result = parent.renderExtendedTube();
            syncFromParent();
            return result;
        }
        return false;
    }
}
