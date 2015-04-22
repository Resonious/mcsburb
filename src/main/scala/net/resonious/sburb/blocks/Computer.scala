package net.resonious.sburb.blocks

import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.resonious.sburb.abstracts.ActiveBlock
import cpw.mods.fml.relauncher.Side
import net.resonious.sburb.Sburb
import net.resonious.sburb.gui.ComputerGui
import cpw.mods.fml.common.network.internal.FMLNetworkHandler

object Computer extends ActiveBlock("Computer") {
	override def onBlockActivated(world:World, x:Int, y:Int, z:Int, player:EntityPlayer, something:Int, px:Float, py:Float, pz:Float):Boolean = {
	  if (Sburb.isClient) {
	    Sburb log "COMPUTER ACTIVATO"
	    ComputerGui.open()
	  }
	  true
	}
}