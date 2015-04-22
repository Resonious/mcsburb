package net.resonious.sburb.hax;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;

public class SburbCore extends DummyModContainer {
	public SburbCore() {
		super(createMetadata());
	}
	private static ModMetadata createMetadata() {
		ModMetadata meta = new ModMetadata();
		meta.modId = "sburb_core";
		meta.name = "Sburb Core";
		meta.version = "0.1";
		meta.credits = "Nigel Baillie made this thing";
		meta.authorList = Arrays.asList(new String[]{"Nigel Baillie"});
		meta.description = "Coremod stuff for Sburb.";
		meta.url = "";
		meta.updateUrl = "";
		meta.screenshots = new String[0];
		meta.logoFile = "";
		return meta;
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}
}
