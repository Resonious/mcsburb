package net.dinkyman.sburb;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.resonious.sburb.Sburb;
import net.dinkyman.sburb.init.ModBlocks;

public class DMain {
  // Okay, so if you get `error: cannot find symbol` when trying to compile, you 
  // need need an import.
  //
  //nigels note for textures is in BlockGoldDirt.java just fyi
  public static void preInit(FMLPreInitializationEvent event) 
  {
  	//make my fucking blocks happen
  	ModBlocks.init();
  }
  public static void init(FMLInitializationEvent event) {
  	//what even goes in here
  }
  public static void postInit(FMLPostInitializationEvent event) {

  }
}