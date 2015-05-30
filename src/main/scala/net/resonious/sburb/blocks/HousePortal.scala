package net.resonious.sburb.blocks

import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.ActiveBlock
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.world.World
import net.minecraft.block.material.Material
import net.resonious.sburb.abstracts.ActiveTileEntity
import net.resonious.sburb.abstracts.ActiveTileEntityRenderer
import net.minecraft.util.ResourceLocation
import net.minecraft.world.IBlockAccess

// TODO adding this to entity instead. remove

object HousePoprtalRenderer
extends ActiveTileEntityRenderer(classOf[HousePortalEntity], HousePortal) {
  private val _model = new net.resonious.sburb.models.HousePortal
  private val _texture = new ResourceLocation("sburb", "textures/tile_entities/houseportal.png")
  
  def model = _model
  def texture = _texture
}

class HousePortalEntity extends ActiveTileEntity

object HousePortal extends ActiveBlock("House Portal") {
  this hasTileEntity classOf[HousePortalEntity]

  setHardness(-1.0f)
  setResistance(6000000.0f)

  override def shouldSideBeRendered(access: IBlockAccess, i:Int,j:Int,k:Int,l:Int) = false
  override def isOpaqueCube() = false
  override def getRenderType() = -1

  // TODO this sucks use entity collision or something
  override def onEntityWalking(world: World, x: Int, y: Int, z: Int, entity: Entity): Unit = {
    Sburb log "O BOY"
  }
}