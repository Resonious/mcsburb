package net.resonious.sburb.blocks

import net.resonious.sburb.abstracts.ActiveBlock
import net.resonious.sburb.abstracts.ActiveTileEntity
import net.resonious.sburb.abstracts.ActiveTileEntityRenderer
import net.minecraft.util.ResourceLocation
import net.minecraft.world.IBlockAccess

object CruxtruderRenderer 
extends ActiveTileEntityRenderer(classOf[CruxtruderEntity], Cruxtruder){
  private val _model = new net.resonious.sburb.models.Cruxtruder
  private val _texture = new ResourceLocation("sburb", "textures/tile_entities/cruxtruder.png")
  
  def model = _model
  def texture = _texture
}

class CruxtruderEntity extends ActiveTileEntity {
}

object Cruxtruder extends MultiBlock("Cruxtruder") {
	this hasTileEntity classOf[CruxtruderEntity]

	override def gristShopCost = 0
	
	override def shouldSideBeRendered(access: IBlockAccess, i:Int,j:Int,k:Int,l:Int) = false
  override def isOpaqueCube() = false
  override def getRenderType() = -1
}