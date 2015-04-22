package net.resonious.sburb.blocks

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.MathHelper
import net.minecraft.world.World
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.ActiveBlock
import net.resonious.sburb.abstracts.ActiveItem

class GristShopMultiBlockItem(block: MultiBlock) extends MultiBlockItem(block) with GristShopItem {
  override def cost = block.gristShopCost
}

class MultiBlockItem(block: MultiBlock) extends ActiveItem(block.name) {
  
  // TODO perhaps instead of this shitty idea, use onBlockPlaced?
  
  override def onItemUse(item: ItemStack, player: EntityPlayer, world: World, _x: Int, _y: Int, _z: Int, 
    side: Int, xOffset: Float, yOffset: Float, zOffSet: Float): Boolean = {
    var x=_x;var y=_y;var z=_z
    if (!player.capabilities.isCreativeMode) {
      item.stackSize -= 1
    }
    if (Sburb.isServer) {
      y += 1
      val dir = MathHelper.floor_double(((player.rotationYaw * 4F) / 360F).toDouble + 0.5D) & 3
      val gagShift = Array(Array(-1, 0, -1), Array(-1, 0, 0), Array(-1, 0, 1))
      var shift: Array[Int] = null
      var canPlace = true
      for (i <- 0 until gagShift.length) {
        shift = rotXZByDir(gagShift(i)(0), y, gagShift(i)(1), dir)
        if (!world.isAirBlock(x + shift(0), y + shift(1), z + shift(2))) {
          canPlace = false
        }
      }
      if (canPlace) {
        world.setBlock(x, y, z, block, dir, 0x02)
        for (i <- 0 until gagShift.length) {
          shift = rotXZByDir(gagShift(i)(0), y, gagShift(i)(1), dir)
          world.setBlock(x + shift(0), y + shift(1), z + shift(2), DummyBlock, dir, 0x02)
          val tileGag = world.getTileEntity(x + shift(0), y, z + shift(1)).asInstanceOf[DummyBlockEntity]
          if (tileGag != null) {
            tileGag.primary.x = x
            tileGag.primary.y = y
            tileGag.primary.z = z
          } else
            Sburb logError "FUCK"
        }
      }
      return true
    }
    false
  }

  def rotXZByDir(x: Int, y: Int, z: Int, dir: Int): Array[Int] = {
    if (dir == 0) {
      Array(x, y, z)
    } else if (dir == 1) {
      Array(-z, y, x)
    } else if (dir == 2) {
      Array(-x, y, -z)
    } else {
      Array(z, y, -x)
    }
  }
}

// TODO loop through descendants of MultiBlock and create items for them (in Sburb.scala)
// TODO also width and height other than 2x2? agh
abstract class MultiBlock(val name: String) extends ActiveBlock(name+" Block") {
  // Called in initialization, kind of like the grist generateItems deal
  def generateItem() = {
    item = if (isGristShopItem)
      new GristShopMultiBlockItem(this)
    else
    	new MultiBlockItem(this)
    item
  }
  
  var item: MultiBlockItem = null
  
  // If grist shop cost is 0 or more, this.item will implement GristShopItem
  def isGristShopItem = gristShopCost > -1
  def gristShopCost = -1
  
  override def onBlockPreDestroy(world: World, p2:Int, p3:Int, p4:Int, p5:Int) = {
    Sburb log "OK. NOW DESTROY THE DUMMIES"
  }
}