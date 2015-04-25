package net.dinkyman.sburb.init;

import net.dinkyman.sburb.blocks.BlockGoldDirt;
import net.dinkyman.sburb.blocks.BlockGoldSand;
import net.dinkyman.sburb.blocks.BlockGoldCobble;
import net.dinkyman.sburb.blocks.BlockGoldStone;
import net.dinkyman.sburb.blocks.BlockGoldGrass;
import net.dinkyman.sburb.blocks.BlockGoldLeaves;
import net.dinkyman.sburb.blocks.BlockGoldPlanks;
import net.dinkyman.sburb.blocks.BlockGoldWood;
import net.dinkyman.sburb.blocks.BlockPurpleDirt;
import net.dinkyman.sburb.blocks.BlockPurpleSand;
import net.dinkyman.sburb.blocks.BlockPurpleCobble;
import net.dinkyman.sburb.blocks.BlockPurpleStone;
import net.dinkyman.sburb.blocks.BlockPurpleGrass;
import net.dinkyman.sburb.blocks.BlockPurpleLeaves;
import net.dinkyman.sburb.blocks.BlockPurplePlanks;
import net.dinkyman.sburb.blocks.BlockPurpleWood;
import net.minecraft.block.Block;
import net.dinkyman.sburb.help.RegisterHelper;
//to add:
//gold+purple glass
//gold+purple saplings(might be item instead of block. probably item actually wtf leaving here anyways)
//considering gold+purple diamonds. just for the hell of it. combine to get a vanilla diamond maybe? alone you can make armor/tools equal to diamond with???
//most likely Gold + purple quartz with which to make the buildings. just recolors

public class ModBlocks
{
	//gold block store
	public static Block goldDirt = new BlockGoldDirt();
	public static Block goldSand = new BlockGoldSand();
	public static Block goldCobble = new BlockGoldCobble();
	public static Block goldStone = new BlockGoldStone();
	public static Block goldGrass = new BlockGoldGrass();
	public static Block goldLeaves = new BlockGoldLeaves();
	public static Block goldPlanks = new BlockGoldPlanks();
	public static Block goldWood = new BlockGoldWood();

	//purple block store
	public static Block purpleDirt = new BlockPurpleDirt();
	public static Block purpleSand = new BlockPurpleSand();
	public static Block purpleCobble = new BlockPurpleCobble();
	public static Block purpleStone = new BlockPurpleStone();
	public static Block purpleGrass = new BlockPurpleGrass();
	public static Block purpleLeaves = new BlockPurpleLeaves();	
	public static Block purplePlanks = new BlockPurplePlanks();
	public static Block purpleWood = new BlockPurpleWood();

	public static void init()
	{
			//register gold blocks
			RegisterHelper.registerBlock(goldDirt);
			RegisterHelper.registerBlock(goldSand);
			RegisterHelper.registerBlock(goldCobble);
			RegisterHelper.registerBlock(goldStone);
			RegisterHelper.registerBlock(goldGrass);
			RegisterHelper.registerBlock(goldLeaves);
			RegisterHelper.registerBlock(goldPlanks);
			RegisterHelper.registerBlock(goldWood);
			//register purple blocks
			RegisterHelper.registerBlock(purpleDirt);
			RegisterHelper.registerBlock(purpleSand);
			RegisterHelper.registerBlock(purpleCobble);
			RegisterHelper.registerBlock(purpleStone);
			RegisterHelper.registerBlock(purpleGrass);
			RegisterHelper.registerBlock(purpleLeaves);
			RegisterHelper.registerBlock(purplePlanks);
			RegisterHelper.registerBlock(purpleWood);
			//fuck all of this it looks too simple
	}
}