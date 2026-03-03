package makeo.gadomancy.coremod;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.AccessTransformer;
import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;

/**
 * This class is part of the Gadomancy Mod
 * Gadomancy is Open Source and distributed under the
 * GNU LESSER GENERAL PUBLIC LICENSE
 * for more read the LICENSE file
 * <p/>
 * Created by makeo @ 07.12.2015 21:48
 */
public class GadomancyTransformer extends AccessTransformer {
    public static final String NAME_ENCHANTMENT_HELPER = "net.minecraft.enchantment.EnchantmentHelper";
    public static final String NAME_WANDMANAGER = "thaumcraft.common.items.wands.WandManager";
    public static final String NAME_NODE_RENDERER = "thaumcraft.client.renderers.tile.TileNodeRenderer";
    public static final String NAME_RENDER_EVENT_HANDLER = "thaumcraft.client.lib.RenderEventHandler";
    public static final String NAME_NEI_ITEMPANEL = "codechicken.nei.ItemPanel";
    public static final String NAME_ENTITY_LIVING_BASE = "net.minecraft.entity.EntityLivingBase";
    public static final String NAME_GOLEM_ENUM = "thaumcraft.common.entities.golems.EnumGolemType";

    public GadomancyTransformer() throws IOException {
        super("gadomancy_at.cfg");
    }

    public byte[] transform(String name, String transformedName, byte[] bytes) {
        boolean needsTransform;
        boolean bl = needsTransform = transformedName.equalsIgnoreCase(NAME_ENCHANTMENT_HELPER) || transformedName.equalsIgnoreCase(NAME_WANDMANAGER) || transformedName.equalsIgnoreCase(NAME_NODE_RENDERER) || transformedName.equalsIgnoreCase(NAME_RENDER_EVENT_HANDLER) || transformedName.equals(NAME_NEI_ITEMPANEL) || transformedName.equalsIgnoreCase(NAME_ENTITY_LIVING_BASE) || transformedName.equalsIgnoreCase(NAME_GOLEM_ENUM);
        if (!needsTransform) {
            return super.transform(name, transformedName, bytes);
        }
        FMLLog.info((String)("[GadomancyTransformer] Transforming " + name + ": " + transformedName), (Object[])new Object[0]);
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytes);
        reader.accept((ClassVisitor)node, 0);
        if (transformedName.equalsIgnoreCase(NAME_ENCHANTMENT_HELPER)) {
            for (MethodNode mn : node.methods) {
                if (mn.name.equals("getFortuneModifier") || mn.name.equals("func_77517_e")) {
                    mn.instructions = new InsnList();
                    mn.instructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
                    mn.instructions.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "getFortuneLevel", "(Lnet/minecraft/entity/EntityLivingBase;)I", false));
                    mn.instructions.add((AbstractInsnNode)new InsnNode(172));
                    continue;
                }
                if (!mn.name.equals("getEnchantmentLevel") && !mn.name.equals("func_77506_a")) continue;
                mn.instructions = new InsnList();
                mn.instructions.add((AbstractInsnNode)new VarInsnNode(21, 0));
                mn.instructions.add((AbstractInsnNode)new VarInsnNode(25, 1));
                mn.instructions.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "onGetEnchantmentLevel", "(ILnet/minecraft/item/ItemStack;)I", false));
                mn.instructions.add((AbstractInsnNode)new InsnNode(172));
            }
        } else if (transformedName.equalsIgnoreCase(NAME_WANDMANAGER)) {
            for (MethodNode mn : node.methods) {
                if (!mn.name.equals("getTotalVisDiscount")) continue;
                InsnList updateTotal = new InsnList();
                updateTotal.add((AbstractInsnNode)new VarInsnNode(25, 0));
                updateTotal.add((AbstractInsnNode)new VarInsnNode(25, 1));
                updateTotal.add((AbstractInsnNode)new VarInsnNode(21, 2));
                updateTotal.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "getAdditionalVisDiscount", "(Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/aspects/Aspect;I)I", false));
                mn.instructions.insertBefore(mn.instructions.get(mn.instructions.size() - 5), updateTotal);
            }
        } else if (transformedName.equalsIgnoreCase(NAME_NODE_RENDERER)) {
            for (MethodNode mn : node.methods) {
                if (!mn.name.equals("renderTileEntityAt")) continue;
                InsnList setBefore = new InsnList();
                setBefore.add((AbstractInsnNode)new VarInsnNode(25, 1));
                setBefore.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "preNodeRender", "(Lnet/minecraft/tileentity/TileEntity;)V", false));
                mn.instructions.insertBefore(mn.instructions.get(0), setBefore);
                AbstractInsnNode next = mn.instructions.get(0);
                while (next != null) {
                    AbstractInsnNode insnNode = next;
                    next = insnNode.getNext();
                    if (insnNode.getOpcode() != 177) continue;
                    InsnList setAfter = new InsnList();
                    setAfter.add((AbstractInsnNode)new VarInsnNode(25, 1));
                    setAfter.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "postNodeRender", "(Lnet/minecraft/tileentity/TileEntity;)V", false));
                    mn.instructions.insertBefore(insnNode, setAfter);
                }
            }
        } else if (transformedName.equalsIgnoreCase(NAME_RENDER_EVENT_HANDLER)) {
            for (MethodNode mn : node.methods) {
                if (!mn.name.equals("blockHighlight")) continue;
                InsnList setBefore = new InsnList();
                setBefore.add((AbstractInsnNode)new VarInsnNode(25, 1));
                setBefore.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "preBlockHighlight", "(Lnet/minecraftforge/client/event/DrawBlockHighlightEvent;)V", false));
                mn.instructions.insertBefore(mn.instructions.get(0), setBefore);
                AbstractInsnNode next = mn.instructions.get(0);
                while (next != null) {
                    AbstractInsnNode insnNode = next;
                    next = insnNode.getNext();
                    if (insnNode.getOpcode() != 177) continue;
                    InsnList setAfter = new InsnList();
                    setAfter.add((AbstractInsnNode)new VarInsnNode(25, 1));
                    setAfter.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/events/EventHandlerRedirect", "postBlockHighlight", "(Lnet/minecraftforge/client/event/DrawBlockHighlightEvent;)V", false));
                    mn.instructions.insertBefore(insnNode, setAfter);
                }
            }
        } else if (transformedName.equalsIgnoreCase(NAME_NEI_ITEMPANEL)) {
            for (MethodNode mn : node.methods) {
                if (!mn.name.equals("updateItemList")) continue;
                InsnList newInstructions = new InsnList();
                newInstructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
                newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "makeo/gadomancy/common/integration/IntegrationNEI", "checkItems", "(Ljava/util/ArrayList;)V", false));
                newInstructions.add(mn.instructions);
                mn.instructions = newInstructions;
            }
        } else if (transformedName.equalsIgnoreCase(NAME_GOLEM_ENUM)) {
            MethodVisitor methodVisitor = node.visitMethod(9, "gadomancyRawCreate", "(Ljava/lang/String;IIIFZIIII)Lthaumcraft/common/entities/golems/EnumGolemType;", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitTypeInsn(187, "thaumcraft/common/entities/golems/EnumGolemType");
            methodVisitor.visitInsn(89);
            methodVisitor.visitVarInsn(25, 0);
            methodVisitor.visitVarInsn(21, 1);
            methodVisitor.visitVarInsn(21, 2);
            methodVisitor.visitVarInsn(21, 3);
            methodVisitor.visitVarInsn(23, 4);
            methodVisitor.visitVarInsn(21, 5);
            methodVisitor.visitVarInsn(21, 6);
            methodVisitor.visitVarInsn(21, 7);
            methodVisitor.visitVarInsn(21, 8);
            methodVisitor.visitVarInsn(21, 9);
            methodVisitor.visitMethodInsn(183, "thaumcraft/common/entities/golems/EnumGolemType", "<init>", "(Ljava/lang/String;IIIFZIIII)V", false);
            methodVisitor.visitInsn(176);
            methodVisitor.visitMaxs(12, 10);
            methodVisitor.visitEnd();
        }
        ClassWriter writer = new ClassWriter(1);
        node.accept((ClassVisitor)writer);
        bytes = writer.toByteArray();
        return super.transform(name, transformedName, bytes);
    }
}
