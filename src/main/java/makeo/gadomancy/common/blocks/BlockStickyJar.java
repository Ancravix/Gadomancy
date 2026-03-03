package makeo.gadomancy.common.blocks;

import java.util.ArrayList;
import java.util.Map;

import cpw.mods.fml.common.registry.GameData;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.blocks.tiles.TileStickyJar;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.registration.StickyJarInfo;
import makeo.gadomancy.common.utils.NBTHelper;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.blocks.ItemJarFilled;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.tiles.TileJarFillable;
import thaumcraft.common.tiles.TileJarFillableVoid;

public class BlockStickyJar extends BlockJar implements IBlockTransparent {

    public BlockStickyJar() {
        setCreativeTab(null);
    }

    private IIcon iconTransparent;

    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);

        if (tile instanceof TileStickyJar) {
            TileStickyJar sticky = (TileStickyJar) tile;

            Block parent = sticky.getParentBlock();
            if (parent != null) {
                IIcon ico = parent.getIcon(world, x, y, z, side);

                if (ico != null)
                    return ico;

                // fallback — через meta
                int meta = sticky.getParentMetadata() != null ? sticky.getParentMetadata() : 0;

                return parent.getIcon(side, meta);
            }
        }

        return super.getIcon(world, x, y, z, side);
    }

    @Override
    public void registerBlockIcons(IIconRegister ir) {
        super.registerBlockIcons(ir);
        iconTransparent = ir.registerIcon(Gadomancy.MODID + ":transparent");
    }

    @Override
    public IIcon getTransparentIcon() {
        return iconTransparent;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        super.setBlockBoundsBasedOnState(world, x, y, z);

        int meta = world.getBlockMetadata(x, y, z);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);

        if (dir != null && dir != ForgeDirection.DOWN) {
            flipBlockBounds(dir);
        }
    }

    private void flipBlockBounds(ForgeDirection placedOn) {
        if (placedOn == null || placedOn == ForgeDirection.DOWN)
            return;

        float minX = (float) getBlockBoundsMinX();
        float minY = (float) getBlockBoundsMinY();
        float minZ = (float) getBlockBoundsMinZ();
        float maxX = (float) getBlockBoundsMaxX();
        float maxY = (float) getBlockBoundsMaxY();
        float maxZ = (float) getBlockBoundsMaxZ();

        float temp;

        if (placedOn == ForgeDirection.NORTH || placedOn == ForgeDirection.SOUTH) {
            temp = minZ;
            minZ = minY;
            minY = temp;
            temp = maxZ;
            maxZ = maxY;
            maxY = temp;
        } else if (placedOn == ForgeDirection.WEST || placedOn == ForgeDirection.EAST) {
            temp = minX;
            minX = minY;
            minY = temp;
            temp = maxX;
            maxX = maxY;
            maxY = temp;
        }

        if (placedOn == ForgeDirection.UP ||
                placedOn == ForgeDirection.SOUTH ||
                placedOn == ForgeDirection.EAST) {

            minX = 1F - minX;
            minY = 1F - minY;
            minZ = 1F - minZ;
            maxX = 1F - maxX;
            maxY = 1F - maxY;
            maxZ = 1F - maxZ;

            temp = minX;
            minX = maxX;
            maxX = temp;
            temp = minY;
            minY = maxY;
            maxY = temp;
            temp = minZ;
            minZ = maxZ;
            maxZ = temp;
        }

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    // @Override
    // public ItemStack getPickBlock(MovingObjectPosition target,
    // World world,
    // int x, int y, int z,
    // EntityPlayer player) {

    // TileEntity tile = world.getTileEntity(x, y, z);
    // if (!(tile instanceof TileStickyJar))
    // return null;

    // TileStickyJar sticky = (TileStickyJar) tile;

    // if (!sticky.isValid())
    // return null;

    // // ВАЖНО: использовать родительский getPickBlock
    // ItemStack stack = sticky.getParentBlock()
    // .getPickBlock(target, world, x, y, z, player);

    // if (stack != null) {
    // NBTTagCompound nbt = NBTHelper.getData(stack);
    // nbt.setBoolean("isStickyJar", true);

    // // тип родителя для onBlockPlacedBy
    // String parentId =
    // GameData.getBlockRegistry().getNameForObject(sticky.getParentBlock());
    // if (parentId != null && !parentId.isEmpty()) {
    // nbt.setString("parentType", parentId);
    // nbt.setInteger("parentMetadata", sticky.getParentMetadata() != null ?
    // sticky.getParentMetadata() : 0);
    // }

    // // опционально: если хочешь переносить содержимое при pick block
    // // sticky.syncToParent();
    // // NBTTagCompound parentData = new NBTTagCompound();
    // // sticky.getParent().writeToNBT(parentData);
    // // nbt.setTag("parent", parentData);
    // }

    // return stack;
    // }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target,
            World world,
            int x, int y, int z,
            EntityPlayer player) {

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileStickyJar))
            return null;

        TileStickyJar sticky = (TileStickyJar) tile;
        if (!sticky.isValid())
            return null;

        return createParentJarStack(sticky);
    }

    @Override
    public TileStickyJar createTileEntity(World world, int metadata) {
        return new TileStickyJar();
    }

    @Override
    public int getRenderType() {
        return RegisteredBlocks.rendererTransparentBlock;
    }

    @Override
    public int onBlockPlaced(World world,
            int x, int y, int z,
            int side,
            float hitX, float hitY, float hitZ,
            int meta) {

        ForgeDirection dir = ForgeDirection.getOrientation(side);

        if (dir != null) {
            return dir.ordinal();
        }

        return ForgeDirection.DOWN.ordinal();
    }

    public void onBlockPlacedOn(World world, int x, int y, int z, EntityLivingBase player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileStickyJar))
            return;

        TileStickyJar sticky = (TileStickyJar) tile;

        ForgeDirection placedOn = ForgeDirection.getOrientation(
                world.getBlockMetadata(x, y, z));

        if (placedOn != ForgeDirection.UP && placedOn != ForgeDirection.DOWN) {

            float pitch = player.rotationPitch;
            float yaw = MathHelper.wrapAngleTo180_float(player.rotationYaw);

            switch (placedOn) {
                case WEST:
                    yaw -= 90;
                    break;
                case NORTH:
                    yaw = (180 - Math.abs(yaw)) * (yaw < 0 ? 1 : -1);
                    break;
                case EAST:
                    yaw += 90;
                    break;
                case SOUTH:
                case DOWN:
                case UP:
                case UNKNOWN:
                    break;
            }

            if (Math.abs(yaw) < Math.abs(pitch)) {
                sticky.facing = pitch < 0
                        ? ForgeDirection.SOUTH.ordinal()
                        : ForgeDirection.NORTH.ordinal();
            } else {
                sticky.facing = yaw < 0
                        ? ForgeDirection.EAST.ordinal()
                        : ForgeDirection.WEST.ordinal();
            }
        }
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z,
            EntityLivingBase ent, ItemStack stack) {

        TileEntity tile = world.getTileEntity(x, y, z);

        if (!(tile instanceof TileStickyJar))
            return;

        TileStickyJar sticky = (TileStickyJar) tile;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return;

        if (!tag.hasKey("parentType") || !tag.hasKey("parentMetadata"))
            return;

        String parentId = tag.getString("parentType");
        int meta = tag.getInteger("parentMetadata");

        Block parentBlock = GameData.getBlockRegistry().getObject(parentId);
        if (parentBlock == null)
            return;

        TileEntity parentTile = null;

        // если предмет несёт полноценный NBT родительского TE — поднимаем именно его
        // класс
        if (tag.hasKey("parent")) {
            NBTTagCompound parentTag = tag.getCompoundTag("parent");
            parentTile = TileEntity.createAndLoadEntity(parentTag);
        }

        // иначе фоллбек: создаём дефолтный TE для блока
        if (parentTile == null) {
            parentTile = parentBlock.createTileEntity(world, meta);
        }

        if (parentTile instanceof TileJarFillable) {
            sticky.init((TileJarFillable) parentTile, parentBlock, meta);
            onBlockPlacedOn(world, x, y, z, ent);
        }
    }

    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {

        ArrayList<ItemStack> drops = new ArrayList<>();

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileStickyJar))
            return drops;

        TileStickyJar sticky = (TileStickyJar) tile;
        if (!sticky.isValid())
            return drops;

        ItemStack stack = createParentJarStack(sticky);
        if (stack != null)
            drops.add(stack);

        return drops;
    }

    private ItemStack createParentJarStack(TileStickyJar sticky) {

        TileJarFillable parent = sticky.getParent();
        if (parent == null)
            return null;

        ItemStack stack;

        if (parent.amount <= 0 && parent.aspectFilter == null) {
            stack = new ItemStack(sticky.getParentBlock());
        } else {
            stack = new ItemStack(ConfigItems.itemJarFilled);
        }

        if (parent instanceof TileJarFillableVoid) {
            stack.setItemDamage(3);
        }

        if (parent.amount > 0 && parent.aspect != null) {
            ((ItemJarFilled) stack.getItem()).setAspects(
                    stack,
                    new AspectList().add(parent.aspect, parent.amount));
        }

        if (parent.aspectFilter != null) {
            if (!stack.hasTagCompound())
                stack.setTagCompound(new NBTTagCompound());

            stack.getTagCompound().setString(
                    "AspectFilter",
                    parent.aspectFilter.getTag());
        }

        // sticky метка
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());

        NBTTagCompound tag = stack.getTagCompound();

        tag.setBoolean("isStickyJar", true);

        String parentId = GameData.getBlockRegistry().getNameForObject(sticky.getParentBlock());

        if (parentId != null) {
            tag.setString("parentType", parentId);
            tag.setInteger("parentMetadata",
                    sticky.getParentMetadata() != null
                            ? sticky.getParentMetadata()
                            : 0);
        }

        return stack;
    }

    // @Override
    // public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int
    // metadata, int fortune) {
    // TileEntity tile = world.getTileEntity(x, y, z);

    // if (tile instanceof TileStickyJar && ((TileStickyJar) tile).isValid()) {

    // TileStickyJar sticky = (TileStickyJar) tile;

    // sticky.syncToParent();

    // TileJarFillable parent = sticky.getParent();
    // Block parentBlock = sticky.getParentBlock();
    // int parentMeta = sticky.getParentMetadata();

    // int originalMeta = world.getBlockMetadata(x, y, z);

    // parent.validate();
    // world.setTileEntity(x, y, z, parent);

    // world.setBlockMetadataWithNotify(x, y, z, parentMeta, 0);

    // ArrayList<ItemStack> drops = parentBlock.getDrops(world, x, y, z, parentMeta,
    // fortune);

    // parent.invalidate();
    // world.setTileEntity(x, y, z, sticky);

    // world.setBlockMetadataWithNotify(x, y, z, originalMeta, 0);

    // boolean found = false;
    // for (ItemStack drop : drops) {
    // if (RegisteredItems.isStickyableJar(drop)) {
    // NBTTagCompound nbt = NBTHelper.getData(drop);
    // nbt.setBoolean("isStickyJar", true);

    // String parentId = GameData.getBlockRegistry().getNameForObject(parentBlock);
    // if (parentId != null && !parentId.isEmpty()) {
    // nbt.setString("parentType", parentId);
    // nbt.setInteger("parentMetadata", parentMeta);
    // }

    // // опционально: перенос содержимого при дропе
    // // NBTTagCompound parentData = new NBTTagCompound();
    // // parent.writeToNBT(parentData);
    // // nbt.setTag("parent", parentData);

    // found = true;
    // break;
    // }
    // }

    // if (!found) {
    // drops.add(new ItemStack(Items.slime_ball, 1));
    // }

    // return drops;
    // }

    // return new ArrayList<ItemStack>();
    // }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float these,
            float are, float variables) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileStickyJar) {
            TileStickyJar tileStickyJar = (TileStickyJar) tile;

            StickyJarInfo info = RegisteredBlocks.getStickyJarInfo(tileStickyJar.getParentBlock(),
                    tileStickyJar.getParentMetadata());

            if (info == null)
                return false;

            ItemStack heldItem = player.getHeldItem();

            if (info.needsPhialHandling() && heldItem != null && heldItem.getItem() == ConfigItems.itemEssence) {
                if (!world.isRemote) {
                    handlePhial(world, x, y, z, player, heldItem, tileStickyJar);
                }
                return true;
            }

            boolean handleAspectFilter = info.needsLabelHandling() && heldItem != null
                    && heldItem.getItem() == ConfigItems.itemResource
                    && heldItem.getItemDamage() == 13 && tileStickyJar.aspectFilter == null
                    && (((IEssentiaContainerItem) heldItem.getItem()).getAspects(heldItem) != null
                            || tileStickyJar.amount != 0);

            ForgeDirection newDir = tileStickyJar.changeDirection(ForgeDirection.getOrientation(side));
            boolean result = ((TileStickyJar) tile).getParentBlock().onBlockActivated(world, x, y, z, player,
                    newDir.ordinal(), these, are, variables);

            if (handleAspectFilter) {
                tileStickyJar.facing = newDir.ordinal();
                tileStickyJar.markDirty();
            }

            return result;
        }
        return false;
    }

    public static void handlePhial(World world, int x, int y, int z, EntityPlayer player, ItemStack stack,
            TileJarFillable tile) {
        ItemEssence itemEssence = (ItemEssence) ConfigItems.itemEssence;

        AspectList aspects = itemEssence.getAspects(stack);
        if (aspects != null) {
            Map.Entry<Aspect, Integer> entry = aspects.aspects.entrySet().iterator().next();

            Aspect jarAspect = tile.aspectFilter == null ? tile.aspect : tile.aspectFilter;

            if ((jarAspect == null || entry.getKey() == jarAspect)
                    && tile.amount + entry.getValue() <= tile.maxAmount) {
                tile.addToContainer(entry.getKey(), entry.getValue());
                stack.stackSize--;

                ItemStack essenceStack = new ItemStack(itemEssence, 1, 0);
                if (!player.inventory.addItemStackToInventory(essenceStack)) {
                    world.spawnEntityInWorld(new EntityItem(world, x + 0.5F, y + 0.5F, z + 0.5F, essenceStack));
                }
                player.inventoryContainer.detectAndSendChanges();
            }
        } else {
            Aspect aspect = tile.aspect;
            if (aspect != null && tile.takeFromContainer(aspect, 8)) {
                stack.stackSize--;

                ItemStack essenceStack = new ItemStack(itemEssence, 1, 1);
                itemEssence.setAspects(essenceStack, new AspectList().add(aspect, 8));

                if (!player.inventory.addItemStackToInventory(essenceStack)) {
                    world.spawnEntityInWorld(new EntityItem(world, x + 0.5F, y + 0.5F, z + 0.5F, essenceStack));
                }
                player.inventoryContainer.detectAndSendChanges();
            }
        }
    }
}