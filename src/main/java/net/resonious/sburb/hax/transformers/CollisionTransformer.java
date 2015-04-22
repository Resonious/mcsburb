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
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class CollisionTransformer extends SburbTransformer {
	@Override
	protected void transform() {
		inClass("net.minecraft.entity.Entity", "qn", new InClass() { protected void within() {
				inMethod("applyEntityCollision", "f",
						(obfuscated ? "(Lqn;)" : "(Lnet.minecraft.entity.Entity;)")+"V", new InMethod() { protected void within() {
						
							InsnList insns = methodNode.instructions;
							Iterator<AbstractInsnNode> iter = insns.iterator();
							// Gotta find the second to last label instruction.
							int curLabelIndex = -1, prevLabelIndex = -1;
							for(int i = 0; iter.hasNext(); i++) {
								AbstractInsnNode node = iter.next();
								if (node instanceof LabelNode) {
									prevLabelIndex = curLabelIndex;
									curLabelIndex = i;
								}
							} // So this all means that prevLabelIndex is the index of the return label!
							LabelNode returnLabelNode = (LabelNode)insns.get(prevLabelIndex);
							
							// Grab first line number node.
							iter = insns.iterator();
							AbstractInsnNode n = firstInsnNodeOfType(LineNumberNode.class);
							
							// We start placing instructions before the first line number node.
							insns.insertBefore(n, new InsnNode(Opcode.ALOAD_0));
							insns.insertBefore(n, new InsnNode(Opcode.ALOAD_1));
							insns.insertBefore(n, new MethodInsnNode(Opcode.INVOKESTATIC,
									EntityCollisionInjector.class.getName().replace('.', '/'),
									"test",
									"(Ljava/lang/Object;Ljava/lang/Object;)Z"));
							// Jump to the return label we found earlier if the method returns false.
							insns.insertBefore(n, new JumpInsnNode(Opcode.IFEQ, returnLabelNode));
					}
				});
			}
		});
	}
	
	Object test(int i1, int i2, int i3) {
		return null;
	}
}
