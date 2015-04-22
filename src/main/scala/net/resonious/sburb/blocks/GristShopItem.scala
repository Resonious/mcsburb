package net.resonious.sburb.blocks

import net.minecraft.block.material.Material
import net.resonious.sburb.abstracts.ActiveBlock
import scala.collection.mutable.ArrayBuffer
import net.minecraft.block.Block
import net.minecraft.block.BlockStairs
import net.resonious.sburb.abstracts.SburbException
import net.resonious.sburb.abstracts.ActiveItem
import java.util.Random
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemBlock

object GristShopItem {
  val all = new ArrayBuffer[GristShopItem]
  val blockField = classOf[BlockStairs].getDeclaredField("field_150149_b")
  blockField.setAccessible(true)

  def of(a: Any): GristShopItem = {
    try {
      (a match {
        case ib: ItemBlock => {
          ib.field_150939_a match {
            case sb: BlockStairs => blockField.get(sb).asInstanceOf[ActiveBlock]
            case b => b
          }
        }
        case bs: BlockStairs => blockField.get(bs).asInstanceOf[ActiveBlock]
        case i: ActiveItem => i
        case mb: MultiBlock => mb.item
        case b: ActiveBlock => b
        case _ => return null
      }).asInstanceOf[GristShopItem]
    }
    catch {
      case e: Exception => null
    }
  }
}

trait GristShopItem {
	def cost: Int
	GristShopItem.all += this
}
