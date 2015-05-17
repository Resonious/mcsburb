package net.resonious.sburb

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Field
import java.util.Scanner
import scala.collection.mutable.HashMap

import li.cil.oc.common.tileentity.Case
import net.minecraft.item.ItemStack
import net.resonious.sburb.abstracts.PacketPipeline
import net.minecraftforge.common.DimensionManager
import io.netty.buffer.ByteBuf
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.common.network.ByteBufUtils
import net.minecraft.world.World
import net.resonious.sburb.abstracts.ActiveCommand
import net.resonious.sburb.commands.SburbCommand.PlayerWithChat
import scala.collection.JavaConverters.seqAsJavaListConverter
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.ServerConfigurationManager
import net.minecraft.util.RegistryNamespaced
import net.minecraft.util.ObjectIntIdentityMap
import scala.collection.JavaConversions._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTSizeTracker
import net.resonious.sburb.abstracts.Vector3
import net.resonious.sburb.abstracts.SburbException
import scala.collection.Map
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.DataInput
import java.io.DataOutput
import net.resonious.sburb.packets.ActivePacket
import net.minecraft.block.BlockWood
import net.minecraft.block.BlockLeaves
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockStaticLiquid
import net.minecraft.block.BlockDynamicLiquid
import net.minecraft.block.material.Material;
import scala.math

object Structure {
  // This object is used in conjunction with the NBTTransformer to
  // circumvent Minecraft's anti-ddos limitations on NBT compound/lists
  object LameSizeTracker extends NBTSizeTracker(0) {
    // public void func_152450_a(long p_152450_1_)
    override def func_152450_a(p: Long): Unit = {}
  }

  // Packet sent from server to client when a structure is generated.
  class UpdateClientsPacket extends ActivePacket {
    var structComp: NBTTagCompound = null
    var x: Int = -1
    var y: Int = -1
    var z: Int = -1

    @SideOnly(Side.SERVER)
    def send(world: World, struct: Structure, x: Int, y: Int, z: Int) = {
      this.x = x
      this.y = y
      this.z = z
      this.structComp = struct.toTagComp
      
      PacketPipeline.sendToDimension(this, world.provider.dimensionId)
    }

    override def write(buffer: ByteBuf) = {
      buffer.writeInt(x)
      buffer.writeInt(y)
      buffer.writeInt(z)
      ByteBufUtils.writeTag(buffer, structComp)

      if (structComp == null)
        throw new SburbException("Ended up writing null structure tag compound to packet")
    }

    override def read(buffer: ByteBuf) = {
      this.x = buffer.readInt
      this.y = buffer.readInt
      this.z = buffer.readInt
      this.structComp = ByteBufUtils.readTag(buffer)
    }

    override def onClient(player: EntityPlayer): Unit = {
      Structure.fromTagComp(structComp).placeAt(player.worldObj, x, y, z)
    }
  }
  val updateClientsPacket = new UpdateClientsPacket


  // ========== Hacking internal nbt method accessibility ============

  val nbtWrite = try {
    classOf[NBTTagCompound].getDeclaredMethod("func_74734_a", classOf[DataOutput])
    } catch {
      case e: NoSuchMethodException =>
        classOf[NBTTagCompound].getDeclaredMethod("write", classOf[DataOutput])
    }
  nbtWrite.setAccessible(true)

  val nbtLoad = try {
    classOf[NBTTagCompound].getDeclaredMethod("func_152446_a", classOf[DataInput], Integer.TYPE, classOf[NBTSizeTracker])
    } catch {
      case e: NoSuchMethodException =>
        classOf[NBTTagCompound].getDeclaredMethod("load", classOf[DataInput], Integer.TYPE, classOf[NBTSizeTracker])
    }
  nbtLoad.setAccessible(true)


  // ========== Utility methods for block things ===========

  // Returns NBTTagCompound of all block data, and the block's name, so that the Structure
  // can determine whether to add it to round1, or round2.
  def serializeBlock(
      world: World,
      x: Int, y: Int, z: Int,
      blacklist: Map[String, Symbol],
      assumeSameOCState: Boolean): Option[(NBTTagCompound, String)] = {
    val tagComp = new NBTTagCompound

    val block = world.getBlock(x, y, z)
    val blockName = Block.blockRegistry.getNameForObject(block)

    if (blacklist contains blockName) {
      blacklist(blockName) match {
        case 'air => {
          tagComp.setString("blockName", "minecraft:air")
          return Some((tagComp, blockName))
        }

        case 'ignore => return None

        case s => throw new SburbException("Unknown blacklist type "+s)
      }
    }

    val meta = world.getBlockMetadata(x, y, z)
    val tileEntity = world.getTileEntity(x, y, z)

    tagComp.setString("blockName", blockName)
    tagComp.setInteger("blockMeta", meta)
    if (tileEntity != null) {
      val teSave = new NBTTagCompound

      def writeNormally() = {
        tileEntity.writeToNBT(teSave)
        tagComp.setTag("tileEntity", teSave)
      }

      if (assumeSameOCState) writeNormally()
      else {
        tileEntity match {
          case computerCase: Case => {
            val items = new NBTTagList

            for (i <- 0 until computerCase.items.length) {
              computerCase.items[i] match {
                case Some(stack) => {
                  val itemName = Item.itemRegistry.getNameFromObject(stack.getItem)
                  val stackTag = new NBTTagCompound

                  stackTag.setInteger("slot", i)
                  stackTag.setString("itemName", itemName)

                  items.appendTag(stackTag)
                }

                case None => {}
              }
            }

            teSave.setTag("items", items)
            tagComp.setTag("computerCase", teSave)
          }

          case _ => writeNormally()
        }
      }
    }

    Some((tagComp, blockName))
  }
  def serializeBlock(world: World, x: Int, y: Int, z: Int): Option[(NBTTagCompound, String)] = {
    serializeBlock(world, x, y, z, Map(), true)
  }
  def serializeBlock(world: World, x: Int, y: Int, z: Int, blacklist: Map[String, Symbol]): Option[(NBTTagCompound, String)] = {
    serializeBlock(world, x, y, z, blacklist, true)
  }

  def placeBlock(comp: NBTTagCompound, world: World, x: Int, y: Int, z: Int): Unit = {
    val block = Block.getBlockFromName(comp.getString("blockName"))
    val meta = comp.getInteger("blockMeta")
    val tileEntityData = if (comp hasKey "tileEntity")
          comp.getTag("tileEntity").asInstanceOf[NBTTagCompound]
        else null
    val computerCaseData = if (comp hasKey "computerCase")
          comp.getTag("computerCase").asInstanceOf[NBTTagCompound]
        else null

    world.setBlock(x, y, z, block, meta, 0)
    val tileEntity = world.getTileEntity(x, y, z)

    if (tileEntity != null) {
      if (tileEntityData != null) {
        if ((tileEntityData hasKey "x") && (tileEntityData hasKey "y") && (tileEntityData hasKey "z")) {
          tileEntityData.setInteger("x", x);
          tileEntityData.setInteger("y", y);
          tileEntityData.setInteger("z", z);
        }
        tileEntity.readFromNBT(tileEntityData)
      }

      else if (computerCaseData != null) {
        tileEntity match {
          case computerCase: Case => {
            val items = computerCaseData.getTag("items").asInstanceOf[NBTTagList]

            for (i <- 0 until items.tagCount) {
              val stackTag = items.getCompoundTagAt(i)
              val slot     = stackTag.getInteger("slot")
              val itemName = stackTag.getString("itemName")

              val item = Item.itemRegistry.getObject(itemName).asInstanceOf[Item]
              val stack = new ItemStack(item)

              computerCase.updateItems(slot, stack)
            }
          }
        }
      }
    }
  }


  // =========== Pseudo-constructors for Structure class. Kind of awkward but whatever ==============

  def fromTagComp(comp: NBTTagCompound) = {
    var struct = new Structure(null, (0,0,0), (0,0,0), null)
    if (comp == null)
      throw new SburbException("Trying to load structure from null tag compound!")
    if (!comp.hasKey("round1"))
      throw new SburbException("Structure loading: Bad tag compound: "+comp.toString)
    struct.round1 = comp.getTag("round1").asInstanceOf[NBTTagList]
    struct.round2 = comp.getTag("round2").asInstanceOf[NBTTagList]
    struct.centerOffset.x = comp.getInteger("centerOffsetX")
    struct.centerOffset.y = comp.getInteger("centerOffsetY")
    struct.centerOffset.z = comp.getInteger("centerOffsetZ")
    struct.corner1 = (
      comp.getInteger("corner1X"),
      comp.getInteger("corner1Y"),
      comp.getInteger("corner1Z")
    )
    struct.corner2 = (
      comp.getInteger("corner2X"),
      comp.getInteger("corner2Y"),
      comp.getInteger("corner2Z")
    )

    struct
  }

  def load(fileName: String): Structure = {
    var fileIn = new FileInputStream(fileName)
    var in = new ObjectInputStream(fileIn)

    var fileTag = new NBTTagCompound
    nbtLoad.invoke(fileTag, in, 0.asInstanceOf[java.lang.Object], LameSizeTracker)

    fromTagComp(fileTag)
  }

}

class Structure(
  world: World,
  var corner1: (Int,Int,Int),
  var corner2: (Int,Int,Int),
  blacklist: Map[String, Symbol],
  assumeSameOCState: Boolean = true
) {
  // Round 1 is for the majority of blocks
  var round1 = new NBTTagList
  // Round 2 contains stuff that might fall, like torches
  var round2 = new NBTTagList

  // Keep track of this so that spawn coordinates can corrospond to the center
  // of the structure.
  var centerOffset = new Vector3[Int]

  // Here lies the "real" constructor logic for this class. I'd refactor it, but
  // why fix what's not broken at this point.
  if (world != null) {

    val min = new Vector3
    val max = new Vector3

    // Get minimum and maximum bounds of the structure
    val c1 = new Vector3(corner1)
    val c2 = new Vector3(corner2)
    c1 foreach {(s: Symbol, v1: Int) =>
      val v2 = c2(s)

      if (v1 > v2) {
        max(s) = v1
        min(s) = v2
      }
      else {
        min(s) = v1
        max(s) = v2
      }
    }

    centerOffset.x = (max.x - min.x) / 2
    centerOffset.z = (max.z - min.z) / 2

    // Grab EVERY BLOCK.
    for (x <- min.x to max.x)
    for (y <- min.y to max.y)
    for (z <- min.z to max.z) {
      val relX = x - min.x
      val relY = y - min.y
      val relZ = z - min.z

      Structure.serializeBlock(world, x, y, z, blacklist, assumeSameOCState) match {
        case Some((tagComp, blockName)) => {
          tagComp.setInteger("relX", relX)
          tagComp.setInteger("relY", relY)
          tagComp.setInteger("relZ", relZ)

          // Remember, stuff that might fall or depend on other blocks go last (round2)
          if (
            (blockName contains "torch")   ||
            (blockName contains "water")   ||
            (blockName contains "lava")    ||
            (blockName contains "sapling") ||
            (blockName contains "sand")    ||
            (blockName contains "reeds")   ||
            (blockName contains "flower")
          )
            round2 appendTag tagComp
          else
            round1 appendTag tagComp
        }

        case None => {}
      }
    }
  }

  // x and z are the center of the structure's top-down defining area, y is the bottom of the structure.
  def placeAt(world: World, x: Int, y: Int, z: Int, sendPacket: Boolean): Unit = {
    def place(blocks: NBTTagList, i: Int) = {
      val comp = blocks.getCompoundTagAt(i)
      Structure.placeBlock(comp, world,
        x + comp.getInteger("relX") - centerOffset.x,
        y + comp.getInteger("relY") - centerOffset.y,
        z + comp.getInteger("relZ") - centerOffset.z
      )
    }

    if (Sburb.isServer && sendPacket)
      Structure.updateClientsPacket.send(world, this, x, y, z)

    for (i <- 0 until round1.tagCount) place(round1, i)
    for (i <- 0 until round2.tagCount) place(round2, i)
  }

  def placeAt(world: World, s: Vector3[Int], sendPacket: Boolean): Unit = {
    placeAt(world, s.x, s.y, s.z, sendPacket)
  }

  def placeAt(world: World, s: Vector3[Int]): Unit = {
    placeAt(world, s.x, s.y, s.z, true)
  }

  def placeAt(world: World, x: Int, y: Int, z: Int): Unit = {
    placeAt(world, x, y, z, true)
  }

  def toTagComp(additionalInfo: NBTTagCompound => Unit): NBTTagCompound = {
    var comp = new NBTTagCompound
    comp.setTag("round1", round1)
    comp.setTag("round2", round2)
    comp.setInteger("centerOffsetX", centerOffset.x)
    comp.setInteger("centerOffsetY", centerOffset.y)
    comp.setInteger("centerOffsetZ", centerOffset.z)
    corner1 match {
      case (x, y, z) => {
        comp.setInteger("corner1X", x)
        comp.setInteger("corner1Y", y)
        comp.setInteger("corner1Z", z)
      }
    }
    corner2 match {
      case (x, y, z) => {
        comp.setInteger("corner2X", x)
        comp.setInteger("corner2Y", y)
        comp.setInteger("corner2Z", z)
      }
    }
    if (additionalInfo != null)
      additionalInfo(comp)
    return comp
  }
  def toTagComp(): NBTTagCompound = return toTagComp(null)

  def saveToFile(fileName: String) = {
    var fileOut = new FileOutputStream(fileName)
    var out = new ObjectOutputStream(fileOut)

    Structure.nbtWrite.invoke(toTagComp, out)

    out.close()
  }


  def findReasonableSpawnPoint(world: World, start: Vector3[Int], radius: Int): Option[Vector3[Int]] = {
    // Gonna be a bumpy ride...
    def blockAt(s: Vector3[Int]) = world.getBlock(s.x, s.y, s.z)

    // Ignore and trees, for now.
    def ignoreBlockAt(s: Vector3[Int]) = {
      val block = blockAt(s)

      world.isAirBlock(s.x, s.y, s.z) ||
      block.isInstanceOf[BlockWood]   ||
      block.isInstanceOf[BlockLeaves]
    }

    def heightAt(s: Vector3[Int]): Int =
      if (ignoreBlockAt(s))
        if (ignoreBlockAt(s.instead(_.y -= 1)))
          heightAt(s.instead( _.y -= 1 ))
        else
          s.y
      else
        heightAt(s.instead( _.y += 1 ))

    // Cache of heights and also whether or not the height above a water block.
    var heights = new HashMap[(Int, Int), (Int, Boolean)]

    // Estimate of top-down structure area - we assume the structure has some
    // padding in it (that its bounds are greater than the actual thing contained)
    // and so we use 3.5 instead of 4, arbitrarily.
    val structArea = 3.5 * centerOffset.x * centerOffset.z
    // Accept up to 1/16th of the approx structure area of extremes.
    val extremesTolerance = structArea / 16

    // Accept up to 1/4th of the ground below the structure to be water.
    val watersTolerance = structArea / 4

    // This will get called tonnnnnnnnnns of times. Returns a position with the same
    // x and z coords as input, and y value of the smallest (sane) height recorded
    // during the scan IF it determines the given center is deemed suitable.
    def acceptableCenterAt(center: Vector3[Int]): Option[Vector3[Int]] = {
      var curPos = new Vector3[Int](center)

      var maxHeight = heightAt(curPos)
      var minHeight = maxHeight

      // We'll call any sudden large drop or rise in block height an "extreme".
      // Too many of these, and we will consider this an center unacceptable.
      var extremes = 0

      // Count how many ground blocks are water. We don't want houses on rivers or lakes.
      var waters = 0

      // Assume the x/z of centerOffset is effectively the "radius" of the
      // structure's top-down area.
      for (xOffset <- -centerOffset.x to centerOffset.x)
      for (zOffset <- -centerOffset.z to centerOffset.z) {
        curPos = center instead { s => s.x += xOffset; s.z += zOffset }

        val heightInfo = heights.get((curPos.x, curPos.z)) match {
          case Some(values) => values

          case None => {
            val h = heightAt(curPos)
            val b = blockAt(curPos.instead(_.y = h - 1))
            val bIsWater = b.getMaterial == Material.water

            heights((curPos.x, curPos.z)) = (h, bIsWater)
            (h, bIsWater)
          }
        }

        // === Check if we're over too much water ===
        val isWater = heightInfo match { case (_, b) => b }

        if (isWater) waters += 1
        if (waters > watersTolerance) return None

        // === Make sure terrain is still acceptable ===
        val height = heightInfo match { case (h, _) => h }

        // If we encounter an extreme, don't count it toward min/max height
        if (height - maxHeight > 5) {
          extremes += 1
        } else if (minHeight - height > 5) {
          extremes += 1
        }
        else if (height < minHeight) minHeight = height
        else if (height > maxHeight) maxHeight = height

        if (extremes > extremesTolerance) return None
        else if (maxHeight - minHeight > 6) return None
      }

      Some(center.instead(_.y = minHeight))
    }

    for (checkX <- start.x - radius until start.x + radius)
    for (checkZ <- start.z - radius until start.z + radius) {
      acceptableCenterAt(new Vector3[Int](checkX, start.y, checkZ)) match {
        case None => {}
        case result => return result
      }
    }

    None
  }
}