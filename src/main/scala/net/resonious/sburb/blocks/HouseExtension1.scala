package net.resonious.sburb.blocks

import net.resonious.sburb.abstracts.ActiveBlock
import net.minecraft.block.Block
import net.minecraft.block.material.Material

object HouseExtension1 extends ActiveBlock("House Extension 1", Material.rock) with GristShopItem {
	setHardness(10)
	setStepSound(Block.soundTypeMetal)
	hasStairs()
	override def cost = 1
}

// object HouseExtension1Stairs extends ActiveBlock
