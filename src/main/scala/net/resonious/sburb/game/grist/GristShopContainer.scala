package net.resonious.sburb.game.grist

import scala.collection.JavaConversions._
import GristShopContainer._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.resonious.sburb.Sburb
import net.resonious.sburb.blocks.GristShopItem
import net.resonious.sburb.game.SburbProperties
import net.minecraft.item.ItemBlock
import net.minecraft.block.BlockStairs
import net.resonious.sburb.blocks.MultiBlock

object GristShopContainer {

  private val INV_START = 3*9

  private val INV_END = INV_START + 26

  private val HOTBAR_START = INV_END + 1

  private val HOTBAR_END = HOTBAR_START + 8
}

class GristShopSlot(
    inventory: IInventory,
    slotIndex: Int,
    x: Int,
    y: Int) extends Slot(inventory, slotIndex, x, y) {

  // For placement...
  override def isItemValid(itemstack: ItemStack): Boolean = false

}

class GristShopContainer(player: EntityPlayer) extends Container {
  private val inventoryPlayer = player.inventory
  private val inventory = GristShopInventory

  var count = 0
  for (i <- 0 until 3) {
    for (j <- 0 until 9) {
    	this.addSlotToContainer(new GristShopSlot(inventory, count, 8 + j * 18, 27 + i * 18))
    	count += 1
    }
  }

  for (i <- 0 until 3) {
    for (j <- 0 until 9) {
      this.addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18))
    }
  }

  for (i <- 0 until 9) {
    this.addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18, 142))
  }

  override def canInteractWith(player: EntityPlayer): Boolean = inventory.isUseableByPlayer(player)

  // On shift-click. God fucking dammit.
  // or is this right click?
  override def transferStackInSlot(par1EntityPlayer: EntityPlayer, index: Int): ItemStack = {
    // null
    inventorySlots.get(index).asInstanceOf[Slot].getStack
  }

  override def slotClick(slot: Int, button: Int, flag: Int, player: EntityPlayer): ItemStack = {
    lazy val result = super.slotClick(slot, button, flag, player)
    if (slot < 0 || slot >= INV_START) return result

    // Get the item involved
    val stack = inventory.getStackInSlot(slot)
    if (stack == null) return result
    val item = stack.getItem

    // Acquire useful information
    val props = SburbProperties of player
    val clientsGrist = if (Sburb.isServer)
      								   props.gameEntry.clientEntry.grist(Grist.Build)
      								 else
      								   props.serverMode.clientsBuildGrist

    // Confusing ass figuring out how to find the fucking cost
    val possibleItem =
      (if (item.isInstanceOf[ItemBlock])
        if (item.asInstanceOf[ItemBlock].field_150939_a.isInstanceOf[MultiBlock])
        	item.asInstanceOf[ItemBlock].field_150939_a.asInstanceOf[MultiBlock].item
        else if (item.asInstanceOf[ItemBlock].field_150939_a.isInstanceOf[BlockStairs])
          GristShopItem.blockField.get(
            item.asInstanceOf[ItemBlock].field_150939_a.asInstanceOf[BlockStairs]
          ).asInstanceOf[GristShopItem]
        else
          item.asInstanceOf[ItemBlock].field_150939_a
      else
      	item).asInstanceOf[GristShopItem]
    val cost = possibleItem.cost

    // Now only charge them if they can afford it!
    if (cost <= clientsGrist) {
      if (result != null && Sburb.isServer) {
        // TODO does this shit even work anymore??!?!?!?!?!??!
        props.gameEntry.clientEntry.grist(Grist.Build) -= cost
        props.serverMode.clientsBuildGristPacket.send()
      }
      result
    }
    else null
  }
}
