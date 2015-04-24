package net.dinkyman.sburb.init;

import net.dinkyman.sburb.blocks.BlockGoldDirt;
import net.minecraft.block.Block;
import net.dinkyman.sburb.help.RegisterHelper;

public class ModBlocks
{
	public static Block goldDirt = new BlockGoldDirt();
	
	public static void init()
	{
			RegisterHelper.registerBlock(goldDirt);
	}
}