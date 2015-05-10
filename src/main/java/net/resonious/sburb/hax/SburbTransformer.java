package net.resonious.sburb.hax;

import java.util.Iterator;

import javassist.bytecode.Opcode;
import net.minecraft.launchwrapper.IClassTransformer;
import net.resonious.sburb.hax.injections.EntityCollisionInjector;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class SburbTransformer implements IClassTransformer {
	public static void print(Object o) {
		System.out.println("************************************");
		System.out.println(o);
		System.out.println("************************************");
	}
	
	protected abstract class InClass {
		protected ClassNode classNode;
		protected boolean obfuscated = true;
		void prepare(ClassNode cn) {
			classNode = cn;
			if (cn.name.contains("."))
				obfuscated = false;
		}
		
		abstract protected void within();
		
		protected void inMethod(String unobfName, String obfName, String desc, InMethod callback) {
			Iterator<MethodNode> methods = classNode.methods.iterator();
			boolean found = false;

			while(methods.hasNext()) {
				MethodNode m = methods.next();

				// print("Found method of name "+m.name+" and descriptor "+m.desc);

				if (m.name.equals(unobfName) || m.name.equals(obfName) 
						&& (desc == null || m.desc.equals(desc.replace("$", "")))) {
					print("INSIDE METHOD "+m.name);
					curMethodNode = m;
					callback.prepare(m);
					callback.within();
					curMethodNode = null;
					found = true;
				}
			}

			if (!found) {
				throw new RuntimeException("Failed to find method "+unobfName+" ("+obfName+")");
			}
		}
		protected void inMethod(String unobfName, String obfName, InMethod callback) {
			inMethod(unobfName, obfName, null, callback);
		}
	}
	protected abstract class InMethod {
		protected abstract class InsnProc {
			abstract protected boolean check(AbstractInsnNode n);
		}
		protected MethodNode methodNode;
		void prepare(MethodNode mn) {
			methodNode = mn;
		}
		
		abstract protected void within();
		
		protected AbstractInsnNode findInstruction(InsnProc checker) {
			Iterator<AbstractInsnNode> insns = methodNode.instructions.iterator();
			for(int i = 0; insns.hasNext(); i++) {
				AbstractInsnNode node = insns.next();
				if (checker.check(node))
					return node;
			}
			throw new RuntimeException("Couldn't find matching instruction node.");
		}
	}
	
	protected abstract void transform();
	
	protected byte[] curData;
	protected String curClassName;
	private MethodNode curMethodNode;
	
	protected void inClass(String unobfName, String obfName, InClass callback) {
		if (curClassName.equals(unobfName) || curClassName.equals(obfName)) {
			print("PATCHING "+curClassName);
			
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(curData);
			classReader.accept(classNode, 0);
			
			callback.prepare(classNode);
			callback.within();
			
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(writer);
			curData = writer.toByteArray();
		}
	}
	
	// HELPER METHODS
	protected AbstractInsnNode firstInsnNodeOfType(Class<? extends AbstractInsnNode> t) {
		Iterator<AbstractInsnNode> iter = curMethodNode.instructions.iterator();
		LineNumberNode n = null;
		while (iter.hasNext()) {
			AbstractInsnNode node = iter.next();
			if (node.getClass() == t)
				return node;
		}
		throw new RuntimeException("No instruction of type "+t.getSimpleName()+" found in method "+curMethodNode.name);
	}
	
	
	@Override
	public byte[] transform(String className, String something, byte[] data) {
		curClassName = className;
		curData = data;
		transform();
		return curData;
	}
}
