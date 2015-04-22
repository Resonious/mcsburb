package net.resonious.sburb.sylladex

import net.resonious.sburb.abstracts.ActiveItem
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object TestModus extends ActiveItem("test_modus") {
	override def onItemRightClick(itemStack: ItemStack, world: World, player: EntityPlayer):ItemStack = {
	  println("SBURB: le CLICK")
		itemStack
	}
}