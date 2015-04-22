package net.resonious.sburb.abstracts

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.ICrafting

object DummyContainer extends Container {
  def apply() = {
    this
  }
  override def canInteractWith(p: EntityPlayer) = true
  override def addCraftingToCrafters(c: ICrafting) = {}
}