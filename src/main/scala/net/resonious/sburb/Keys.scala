package net.resonious.sburb

import cpw.mods.fml.client.registry.ClientRegistry
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import net.minecraft.client.Minecraft
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

object Keys {
  val gristShop = new KeyBinding("key.grist_shop", Keyboard.KEY_R, "key.categories.sburb");
  val debug = new KeyBinding("key.debug", Keyboard.KEY_M, "key.categories.sburb")
  val debug2 = new KeyBinding("key.otherdebug", Keyboard.KEY_N, "key.categories.sburb")
  
  def inventory = Minecraft.getMinecraft.gameSettings.keyBindInventory
  
  // TODO make this a helper function somewhere
  var inited = false
  def init():Unit = {
    if (inited) return
    this.getClass.getDeclaredFields foreach { f =>
      f.setAccessible(true)
      f.get(this) match {
      	case k: KeyBinding => ClientRegistry registerKeyBinding k
      	case _ =>
      }
    }
    inited = true
  }
}