package net.resonious.sburb.blocks

import net.minecraft.entity.player.EntityPlayer
import net.resonious.sburb.Sburb
import net.minecraft.world.World
import net.resonious.sburb.gui.ComputerGui
import net.resonious.sburb.abstracts.ActiveBlock
import net.minecraft.world.IBlockAccess
import net.resonious.sburb.abstracts.ActiveTileEntity
import net.minecraft.nbt.NBTTagCompound
import net.resonious.sburb.abstracts.Vector3._
import net.resonious.sburb.abstracts.Pimp
import net.resonious.sburb.abstracts.Pimp._
import net.minecraft.block.Block

class DummyBlockEntity extends ActiveTileEntity {
  var primary = (0,0,0).vec
  
  override def writeToNBT(comp: NBTTagCompound) = {
    super.readFromNBT(comp)
    primary foreach {(s,n) => comp.setInteger("p"+s, n)}
  }
  override def readFromNBT(comp: NBTTagCompound) = {
    super.readFromNBT(comp)
    primary foreach {(s,n) => primary.set(s,comp.getInteger("p"+s))}
  }
}

object DummyBlock extends ActiveBlock("Dummy") {
  this hasTileEntity classOf[DummyBlockEntity]
  
  override def shouldSideBeRendered(access: IBlockAccess, i:Int,j:Int,k:Int,l:Int) = false
  override def isOpaqueCube() = false
  
  // Destroy the primary block when broken
  // The primary block should deal with the rest of the dummies
  override def breakBlock(world:World, x:Int,y:Int,z:Int, block:Block,par6:Int):Unit = {
    world.getTileEntity(x,y,z) match {
      case te: DummyBlockEntity => {
        val p = te.primary
        world.func_147480_a(p.x,p.y,p.z, false)
        world.removeTileEntity(p.x,p.y,p.z)
      }
      case _ => Sburb log "No dummy???"
    }
    world.removeTileEntity(x,y,z)
  }
}