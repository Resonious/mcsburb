package net.dinkyman.sburb.tabs;

import net.dinkyman.sburb.init.ModBlocks;
import net.dinkyman.sburb.blocks.BlockGoldDirt;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.dinkyman.sburb.tabs.ModTabs;

public class DreamDimensionsTab extends CreativeTabs 
{
	String name;
	
	public DreamDimensionsTab(int par1, String par2Str)
	{
		//again not too sure what this stuff does
		super(par1, par2Str);
		this.name = par2Str;
		this.setBackgroundImageName("dreamdimensionbg.png");
	}
	@SideOnly(Side.CLIENT)
	public Item getTabIconItem()
	{
		//NOTE TO SELF NULL RETURN IS FUCKING BAD I GUESS 
		if (this.name == "tabDreamDimensions") 
		{
			return Item.getItemFromBlock(ModBlocks.goldDirt);
		}
		//in case i want more tabs(disregard what this actually returns):
		//
		//else if (this.name == "tabAscensionTest")
		//{
		//	return ModStructureItems.spawnAbandonedHouse;
		//}
		return null;
	}
}