package net.resonious.sburb.hax.injections;

import net.minecraft.entity.Entity;
import net.resonious.sburb.hax.Injection;
import net.resonious.sburb.hax.InjectionHolder;

public class EntityCollisionInjector {
	public static InjectionHolder<Entity, Entity> injector = new InjectionHolder<Entity, Entity>();
	public static void inject(Injection<Entity, Entity> i) {
		injector.inject(i);
	}
	public static boolean test(Object e1, Object e2) {
		return injector.test((Entity)e1, (Entity)e2);
	}
}
