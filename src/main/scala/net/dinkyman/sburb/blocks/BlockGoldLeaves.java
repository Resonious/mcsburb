package net.dinkyman.sburb.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.dinkyman.sburb.help.Reference;
import net.dinkyman.sburb.tabs.ModTabs;

public class BlockGoldLeaves extends Block
{
	public BlockGoldLeaves()
	{
		super(Material.leaves);
		setBlockName("goldLeaves");
		setBlockTextureName("sburb:" + getUnlocalizedName().substring(5));
		setCreativeTab(ModTabs.tabDreamDimensions);
	}
}
