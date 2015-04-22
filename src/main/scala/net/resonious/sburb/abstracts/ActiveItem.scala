package net.resonious.sburb.abstracts

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.client.renderer.texture.IIconRegister
import net.resonious.sburb.Sburb
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.creativetab.CreativeTabs

class ActiveItem(name:String) extends Item {
  setUnlocalizedName(name.replace(' ', '_').toLowerCase)
  setMaxStackSize(1)
  setCreativeTab(CreativeTabs.tabMisc)
  
  override def getMaxItemUseDuration(stack:ItemStack) = 1
	
  @SideOnly(Side.CLIENT)
	override def registerIcons(iconRegister:IIconRegister):Unit = 
	  itemIcon = iconRegister.registerIcon("sburb:"+getUnlocalizedName.substring(5))
}