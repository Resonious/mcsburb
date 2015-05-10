package net.resonious.sburb.items

import net.resonious.sburb.abstracts.ActiveItem
import net.resonious.sburb.Sburb
import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.tileentity.TileEntity
import net.minecraft.block.Block

object StructureGrabber extends ActiveItem("Structure Grabber") {
  var globalCapturedBlock: Block = null
  var globalCapturedMeta: Int = -1
  var globalTileEntitySave: NBTTagCompound = null

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, xPos: Int, yPos: Int, zPos: Int, par7: Int, xSub: Float, ySub: Float, zSub: Float) = {
    if (Sburb.isServer)
      if (globalCapturedBlock == null) {
        val x = xPos; val y = yPos; val z = zPos;

        globalCapturedBlock = world.getBlock(x, y, z)
        globalCapturedMeta  = world.getBlockMetadata(x, y, z)
        val tileEntity = world.getTileEntity(x, y, z)

        globalTileEntitySave = new NBTTagCompound
        if (tileEntity != null)
          tileEntity.writeToNBT(globalTileEntitySave)

        Sburb log "GRABBED "+globalCapturedBlock.getLocalizedName()
      } else {
        val x = xPos; val y = yPos + 1; val z = zPos;

        world.setBlock(x, y, z, globalCapturedBlock, globalCapturedMeta, 0)
        val tileEntity = world.getTileEntity(x, y, z)

        if (tileEntity != null) {
          if ((globalTileEntitySave hasKey "x") && (globalTileEntitySave hasKey "y") && (globalTileEntitySave hasKey "z")) {
            globalTileEntitySave.setInteger("x", x);
            globalTileEntitySave.setInteger("y", y);
            globalTileEntitySave.setInteger("z", z);
          }
          tileEntity.readFromNBT(globalTileEntitySave)
        }

        Sburb log "PLACED "+globalCapturedBlock.getLocalizedName()
        globalCapturedBlock = null
      }

    false
  }
}