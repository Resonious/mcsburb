package net.resonious.sburb.blocks

import net.resonious.sburb.abstracts.ActiveBlock
import net.resonious.sburb.abstracts.ActiveTileEntity
import net.resonious.sburb.abstracts.ActiveTileEntityRenderer
import net.minecraft.util.ResourceLocation
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.resonious.sburb.game.SburbProperties
import net.resonious.sburb.game.Medium
import net.resonious.sburb.Sburb
import net.minecraft.entity.player.EntityPlayer

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

	override def gristShopCost = 100
	
	override def shouldSideBeRendered(access: IBlockAccess, i:Int,j:Int,k:Int,l:Int) = false
  override def isOpaqueCube() = false
  override def getRenderType() = -1

  override def onBlockActivated(
    world: World, x: Int, y: Int, z: Int,
    player: EntityPlayer, something: Int,
    px: Float, py: Float, pz: Float
  ): Boolean = {
    if (Sburb.isClient) {
      true
    } else {
      val props = SburbProperties of player
      if (!props.hasGame) {
        Sburb log "fuck you!!!!"
        return false
      }
      if (props.serverMode.activated) return false

      val entry = props.gameEntry
      if (entry.mediumId != 0) return false

      // NOTE this is temporary - the cruxtruder doesn't actually warp the player to the medium instantly!!
      Medium.generate(player)
      true
    }
  }
}