package net.resonious.sburb.proxy

import org.lwjgl.input.Keyboard
import cpw.mods.fml.common.network.IGuiHandler
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.resonious.sburb.Keys
import java.io.File
import scala.collection.mutable.HashMap
import net.resonious.sburb.game.SburbGame
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.GuiId
import cpw.mods.fml.client.registry.RenderingRegistry
import net.resonious.sburb.entities.HousePortalRenderer
import net.resonious.sburb.entities.HousePortal
import net.resonious.sburb.entities.ReturnNodeRenderer
import net.resonious.sburb.entities.ReturnNode

class CommonProxy extends IGuiHandler {
  def onGameStart = {}
  
	override def getServerGuiElement(guiId:Int, player:EntityPlayer, world:World, x:Int, y:Int, z:Int):Object = {
		GuiId.get(guiId).createContainer(player,x,y,z)
	}
	override def getClientGuiElement(guiId:Int, player:EntityPlayer, world:World, x:Int, y:Int, z:Int):Object = {
  	GuiId.get(guiId).createGuiScreen(player,x,y,z)
	}

  def registerRenderers(): Unit = {}
}

class ClientProxy extends CommonProxy {
  
  override def registerRenderers() {
    RenderingRegistry.registerEntityRenderingHandler(classOf[HousePortal], HousePortalRenderer)
    RenderingRegistry.registerEntityRenderingHandler(classOf[ReturnNode], ReturnNodeRenderer)
  }

  override def onGameStart = {
    // Actually this is probably garbage anyway
    // For now, keep default inventory
    /*
    val invkey = Minecraft.getMinecraft.gameSettings.keyBindInventory
    invkey.setKeyCode(Keyboard.KEY_UNLABELED)
    Keys.init
    */
    //Minecraft.getMinecraft.gameSettings.keyBindInventory.setKeyCode(Keyboard.KEY_E)
    //println("FFFFFFFIIIIIIIIIITTTTTTSSSSSSSSTTTTTTTTAAAAAAAARRRRRRRRTTTTTTTTNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN")
  }
}
