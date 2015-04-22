package net.resonious.sburb.game.grist

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import Grist._
import net.resonious.sburb.packets.ActivePacket
import net.resonious.sburb.Sburb
import net.resonious.sburb.game.SburbProperties
import net.resonious.sburb.blocks.HouseExtension1
import net.minecraft.item.Item
import net.minecraft.block.Block
import net.resonious.sburb.blocks.Cruxtruder
import net.resonious.sburb.blocks.MultiBlock

object GristShopInventory extends IInventory {
  val INV_SIZE = 3*9

  private val name = "Grist Shop"

  private val tagName = "GristShop"

  private var inventory: Array[ItemStack] = new Array[ItemStack](3*9+1)

  availableItems(HouseExtension1, HouseExtension1.stairs, Cruxtruder)

  def containsStack(stack: ItemStack): Boolean = {
    inventory foreach { s =>
      if (s == stack)
        return true
    }
    false
  }

  private def availableItems(items: Any*) = {
    var count = 0
    items foreach { x =>
    	x match {
    	  case i: Item => inventory(count) = new ItemStack(i)
    	  case mb: MultiBlock => inventory(count) = new ItemStack(mb.item)
    	  case b: Block => inventory(count) = new ItemStack(b)
    	}
    	count += 1}}

  override def getSizeInventory(): Int = inventory.length

  // override def getStackInSlot(slot: Int): ItemStack = inventory(slot)
  override def getStackInSlot(slot: Int): ItemStack =
    if (inventory(slot) == null)
      null
    else
      new ItemStack(inventory(slot).getItem, 1)

  override def decrStackSize(slot: Int, amount: Int): ItemStack = {
    // var stack = getStackInSlot(slot)
    // if (stack != null) {
    //   if (stack.stackSize > amount) {
    //     stack = stack.splitStack(amount)
    //     if (stack.stackSize == 0) {
    //       setInventorySlotContents(slot, null)
    //     }
    //   } else {
    //     setInventorySlotContents(slot, null)
    //   }
    //   this.markDirty()
    // }
    // stack
    getStackInSlot(slot)
  }

  override def getStackInSlotOnClosing(slot: Int): ItemStack = {
    getStackInSlot(slot)
  }

  override def setInventorySlotContents(slot: Int, itemstack: ItemStack) {
    if (itemstack == null) return
    this.inventory(slot) = itemstack
    if (itemstack != null && itemstack.stackSize > this.getInventoryStackLimit) {
      itemstack.stackSize = this.getInventoryStackLimit
    }
    this.markDirty()
  }

  override def getInventoryName(): String = name

  override def hasCustomInventoryName(): Boolean = name.length > 0

  override def getInventoryStackLimit(): Int = 1

  override def markDirty() {
    for (i <- 0 until this.getSizeInventory
        if this.getStackInSlot(i) != null && this.getStackInSlot(i).stackSize == 0)
      this.setInventorySlotContents(i, null)
  }

  override def isUseableByPlayer(entityplayer: EntityPlayer) = (SburbProperties of entityplayer).serverMode.activated

  override def openInventory() {
  }

  override def closeInventory() {
  }

  override def isItemValidForSlot(slot: Int, itemstack: ItemStack): Boolean = true
}
