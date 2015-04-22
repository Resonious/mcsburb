package net.resonious.sburb.game.grist

import net.minecraftforge.client.IItemRenderer
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import net.minecraft.util.IIcon
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import org.lwjgl.opengl.GL11
import net.minecraft.client.renderer.entity.RenderItem
import net.resonious.sburb.blocks.GristShopItem

object GristShopItemRenderer extends IItemRenderer {
  val iRender = new RenderItem
  
  override def handleRenderType(stack: ItemStack, itype: ItemRenderType): Boolean = {
    return itype == ItemRenderType.INVENTORY;
  }
  
  override def shouldUseRenderHelper(itype: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper): Boolean = {
    return false;
  }
  
  override def renderItem(itype: ItemRenderType, stack: ItemStack, data: Object*):Unit = {
    val fontRenderer = Minecraft.getMinecraft().fontRenderer;
    
    // ====================== Render item texture ======================
    val icon = stack.getIconIndex();
    iRender.renderIcon(0, 0, icon, 16, 16);
    
    // ====================== Render OpenGL square shape ======================
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glDepthMask(false);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    
    val tessellator = Tessellator.instance;
    tessellator.startDrawing(GL11.GL_QUADS);
    tessellator.setColorRGBA(0, 0, 0, 128);
    tessellator.addVertex(0, 0, 0);
    tessellator.addVertex(0, 8, 0);
    tessellator.addVertex(8, 8, 0);
    tessellator.addVertex(8, 0, 0);
    tessellator.draw();
    
    GL11.glDepthMask(true);
    GL11.glDisable(GL11.GL_BLEND);
    
    // ====================== Render text ======================
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    val text = Integer.toString(stack.getItemDamage());
    fontRenderer.drawStringWithShadow(text, 1, 1, 0xFFFFFF);
  }
}