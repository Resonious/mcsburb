package net.resonious.sburb.hax.transformers;

import java.util.Iterator;

import javassist.bytecode.Opcode;
import net.resonious.sburb.hax.SburbTransformer;
import net.resonious.sburb.hax.injections.EntityCollisionInjector;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.Opcodes;

public class NBTTransformer extends SburbTransformer {
  private void removeBullshitFrom(InsnList insns) {
    Iterator<AbstractInsnNode> iter = insns.iterator();

    AbstractInsnNode instanceOfLame = null;
    AbstractInsnNode aloadSizeChecker = null;
    LabelNode afterThrow = null;
    JumpInsnNode jump = null;

    while (iter.hasNext()) {
      AbstractInsnNode node = iter.next();

      if (node.getOpcode() == Opcodes.IF_ICMPLE) {
        afterThrow = ((JumpInsnNode)node).label;

        // Plan: If parameter 3 is an instance of Structure.LameSizeTracker,
        //       then jump to same place this IF_ICMPLE would jump to.
        //       (which is AFTER the throw new RuntimeException("im paranoid about nbt size"))

        // Push parameter 3 (The NBTSizeChecker) to stack
        aloadSizeChecker = new VarInsnNode(Opcodes.ALOAD, 3);
        insns.insert(node, aloadSizeChecker);

        // Instanceof("LameSizeTracker") (turns stack top into 1 or 0 depending on result)
        instanceOfLame = new TypeInsnNode(
          Opcodes.INSTANCEOF, "net/resonious/sburb/Structure$LameSizeTracker$"
        );
        insns.insert(aloadSizeChecker, instanceOfLame);

        // Consume single stack value, jump to our noted label if it was NOT equal to zero
        jump = new JumpInsnNode(Opcodes.IFNE, afterThrow);
        insns.insert(instanceOfLame, jump);

        break;
      }
    }

    if (instanceOfLame == null)
      throw new RuntimeException("Couldn't find an IF_ICMPLE instruction!");
    if (aloadSizeChecker == null)
      throw new RuntimeException("Didn't end up placing aload_3 instruction? This is a bug.");
    if (afterThrow == null)
      throw new RuntimeException("Didn't end up grabbing afterThrow label? This is a bug.");
    if (jump == null)
      throw new RuntimeException("Didn't end up jumping after the type check? This is a bug.");
  }

  private class ClassName {
    String unobf, obf;
    ClassName(String unobf, String obf) {
      this.unobf = "net.minecraft.nbt." + unobf;
      this.obf   = obf;
    }
  }

  @Override
  protected void transform() {
    ClassName[] classesToPatch = new ClassName[] {
      new ClassName("NBTTagList", "dq"),
      new ClassName("NBTTagCompound", "dh")
    };

    for (ClassName names : classesToPatch) {
      inClass(names.unobf, names.obf, new InClass() { protected void within() {
        String nbtSizeTracker = null;
        if (obfuscated)
          nbtSizeTracker = "ds";
        else
          nbtSizeTracker = "net/minecraft/nbt/NBTSizeTracker";

        inMethod("load", "a", "(Ljava/io/DataInput;IL"+nbtSizeTracker+";)V", new InMethod() { protected void within() {
          removeBullshitFrom(methodNode.instructions);
        }});
      }});
    }
  }
}