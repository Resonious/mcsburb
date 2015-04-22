package net.resonious.sburb.hax;

import java.util.ArrayList;

public class InjectionHolder<T1,T2> {
	public ArrayList<Injection<T1,T2>> injections = new ArrayList();
	
	public void inject(Injection<T1,T2> i) {
		injections.add(i);
	}
	public boolean remove(Injection<T1,T2> i) {
		return injections.remove(i);
	}
	// Returns true if ALL injections return true
	public boolean test(T1 a1, T2 a2) {
		for(int i = 0; i < injections.size(); i++) {
			if (!injections.get(i).apply(a1, a2))
				return false;
		}
		return true;
	}
}
