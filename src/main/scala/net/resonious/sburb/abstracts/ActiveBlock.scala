package net.resonious.sburb.abstracts

import net.minecraft.block.material.Material
import net.minecraft.block.Block
import net.minecraft.block.BlockStairs
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.block.ITileEntityProvider
import net.minecraft.world.World
import net.minecraft.tileentity.TileEntity

object ActiveBlock {

}

class ActiveBlock(name: String, material: Material=Material.iron) extends Block(material) with ITileEntityProvider {
  private var tileEntityClass: Class[_ <: ActiveTileEntity] = null
  val stairsConstructor = classOf[BlockStairs].getDeclaredConstructors()(0)
  stairsConstructor.setAccessible(true)

  var stairs: BlockStairs = null

	setBlockName(name.replace(' ', '_').toLowerCase)
	setBlockTextureName("sburb:"+getUnlocalizedName.substring(5))
  setCreativeTab(CreativeTabs.tabMisc)
  setHardness(5F)

  // The stairs of a block can then be accesed like: `HouseExtension1.stairs` for example.
  protected def hasStairs(): Unit = {
    val stairsName = name.replace(' ', '_').toLowerCase + "_stairs"
    stairs = stairsConstructor.newInstance(this, 0.asInstanceOf[java.lang.Object]).asInstanceOf[BlockStairs]
    stairs.setBlockName(stairsName)
  }

  // Call this in construction to use a tile entity with a block
  protected def hasTileEntity(c: Class[_ <: ActiveTileEntity]) = {
    if (isBlockContainer) throw new SburbException("You can only assign one tile entity to a block!")
	  isBlockContainer = true
	  tileEntityClass = c
	}

  override def hasTileEntity() = tileEntityClass != null
  override def hasTileEntity(i:Int) = tileEntityClass != null

  override def onBlockEventReceived(world:World, p2:Int,p3:Int,p4:Int,p5:Int,p6:Int): Boolean = {
    super.onBlockEventReceived(world, p2, p3, p4, p5, p6)
    val tileEntity = world.getTileEntity(p2, p3, p4)
    if (tileEntity == null)  false
    else tileEntity.receiveClientEvent(p5, p6)
  }

  override def breakBlock(world:World, x:Int,y:Int,z:Int, block:Block, p6:Int) = {
    super.breakBlock(world, x, y, z, block, p6)
    if (hasTileEntity)
    	world.removeTileEntity(x, y, z)
  }

  // Override this if you need to be picky
  override def createNewTileEntity(world: World, i: Int): TileEntity = {
	  val constructors = tileEntityClass.getConstructors
	  constructors foreach { constr =>
	    val paramTypes = constr.getParameterTypes.deep
	    if (paramTypes == Array(classOf[World], classOf[Int]).deep)
	      return constr.newInstance(world, i.asInstanceOf[Integer]).asInstanceOf[ActiveTileEntity]
	    else if (paramTypes == Array(classOf[World]).deep)
	      return constr.newInstance(world).asInstanceOf[ActiveTileEntity]
	    else if (paramTypes == Array(classOf[Int]).deep)
	      return constr.newInstance(i.asInstanceOf[Integer]).asInstanceOf[ActiveTileEntity]
	    else if (paramTypes.length == 0)
	      return constr.newInstance().asInstanceOf[ActiveTileEntity]
	  }
	  throw new IllegalArgumentException("The Tile Entity class "+tileEntityClass.getName+" has no constructor with proper arguments")
	}
}
