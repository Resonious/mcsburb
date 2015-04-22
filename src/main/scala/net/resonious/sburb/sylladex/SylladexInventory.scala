package net.resonious.sburb.sylladex

import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.entity.player.EntityPlayer
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.inventory.ContainerPlayer

class SylladexInventory(plr: EntityPlayer) extends InventoryPlayer(plr) {
  @SideOnly(Side.CLIENT)
	override def changeCurrentItem(par1: Int) = {
	  var dir = par1
	  if (dir > 0) dir = 1
	  else if (dir < 0) dir = -1
	  
	  currentItem += 1
	  currentItem %= 9
	  
	  println("FUCK YEAH. CHANGED THAT SLOT. dir: " + dir + " current: " + currentItem + " par1: " + par1)
	}
}
