package net.dinkyman.sburb.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.dinkyman.sburb.help.Reference;
import net.dinkyman.sburb.tabs.ModTabs;

public class BlockGoldDirt extends Block
{
	public BlockGoldDirt()
	{
		super(Material.grass);
		setBlockName("goldDirt");
                // FIX: It may have not worked because the mod id in your
                // Reference class wasn't actually a registered mod.
                // For now, we'll use the 'sburb' mod ID (which is registered
                // in sburb.scala)
                // Additionally, I have moved your texture to the sburb
                // directory.
		//leave nigels comment here in case i dunking forget texture shite
		setBlockTextureName("sburb:" + getUnlocalizedName().substring(5));
		setCreativeTab(ModTabs.tabDreamDimensions);
	}
}
