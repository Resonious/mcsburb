package net.resonious.sburb.hax;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"net.resonious.sburb.hax"})
@IFMLLoadingPlugin.DependsOn({"sburb"})
public class SburbLoadPlugin implements IFMLLoadingPlugin, IFMLCallHook {
	@Override
	public String[] getASMTransformerClass() {
		return new String[] {
			"net.resonious.sburb.hax.transformers.CollisionTransformer",
			"net.resonious.sburb.hax.transformers.NBTTransformer"
		};
	}
	@Override
	public String getModContainerClass() {
		return SburbCore.class.getName();
	}
	@Override
	public String getSetupClass() { return getClass().getName(); }

	@Override
	public void injectData(Map<String, Object> data) {
	}
	@Override
	public String getAccessTransformerClass() {return null;}
	@Override
	public Void call() {return null;}
}
