package net.resonious.sburb.abstracts

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.Tessellator
import net.minecraft.world.World
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft

abstract class ActiveTileEntityRenderer(
    val tileEntityType: Class[_ <: ActiveTileEntity],
    val block: Block) 
	extends TileEntitySpecialRenderer {
  
  def model: ModelBase
  def texture: ResourceLocation
  // Override this to have the offset or whatever (before it is rotated..hopefully not confusing)
  def makeAdjustments() = {
    GL11.glTranslatef(0.5F, 1.5F, 0.5F)
  }
  
  override def renderTileEntityAt(tileEntity:TileEntity, x:Double,y:Double,z:Double, whatisthis:Float) = {
    GL11.glPushMatrix()
    def f(d:Double) = d.asInstanceOf[Float]
    GL11.glTranslatef(f(x), f(y), f(z))

    val t = tileEntity.asInstanceOf[ActiveTileEntity]
    renderBlock(t, t.getWorldObj, t.xCoord,t.yCoord,t.zCoord, block)
    
    GL11.glPopMatrix()
  }
  
  def renderBlock(tl: ActiveTileEntity, 
      world: World, 
      x: Int, 
      y: Int, 
      z: Int, 
      block: Block) {
    val tessellator = Tessellator.instance
    val f = world.getBlockLightValue(x, y, z)
    val l = world.getLightBrightnessForSkyBlocks(x, y, z, 0)
    val l1 = l % 65536
    val l2 = l / 65536
    tessellator.setColorOpaque_F(f, f, f)
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l1.toFloat, l2.toFloat)
    val dir = world.getBlockMetadata(x, y, z)
    
    GL11.glPushMatrix()
    makeAdjustments()
    GL11.glRotatef(180F, 0F, 0F, 1F)
    Minecraft.getMinecraft.getTextureManager.bindTexture(texture)
    model.render(null, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F)
    GL11.glPopMatrix()
  }
}