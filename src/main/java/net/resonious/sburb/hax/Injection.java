package net.resonious.sburb.hax;

public interface Injection<T1, T2> {
	boolean apply(T1 a1, T2 a2);
}
