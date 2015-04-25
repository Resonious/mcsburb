package net.dinkyman.sburb.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.dinkyman.sburb.help.Reference;
import net.dinkyman.sburb.tabs.ModTabs;

public class BlockPurpleDirt extends Block
{
	public BlockPurpleDirt()
	{
		super(Material.grass);
		setBlockName("purpleDirt");
		setBlockTextureName("sburb:" + getUnlocalizedName().substring(5));
		setCreativeTab(ModTabs.tabDreamDimensions);
	}
}
