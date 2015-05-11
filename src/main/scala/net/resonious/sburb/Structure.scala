package net.resonious.sburb

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Field
import java.util.Scanner

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
    }

    override def read(buffer: ByteBuf) = {
      this.x = buffer.readInt
      this.y = buffer.readInt
      this.z = buffer.readInt
      this.structComp = ByteBufUtils.readTag(buffer)
    }

    override def onClient(player: EntityPlayer): Unit =
      Structure.fromTagComp(structComp).placeAt(player.worldObj, x, y, z)
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

  def serializeBlock(world: World, x: Int, y: Int, z: Int, blacklist: Map[String, Symbol]): Option[(NBTTagCompound, String)] = {
    val tagComp = new NBTTagCompound

    val block = world.getBlock(x, y, z)
    val blockName = Block.blockRegistry.getNameForObject(block)

    if (blacklist contains blockName.replace("m:", "minecraft:")) {
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

    tagComp.setString("blockName", Block.blockRegistry.getNameForObject(block))
    tagComp.setInteger("blockMeta", meta)
    if (tileEntity != null) {
      val teSave = new NBTTagCompound
      tileEntity.writeToNBT(teSave)
      tagComp.setTag("tileEntity", teSave)
    }

    Some((tagComp, blockName))
  }
  def serializeBlock(world: World, x: Int, y: Int, z: Int): Option[(NBTTagCompound, String)] = {
    serializeBlock(world, x, y, z, Map())
  }

  def placeBlock(comp: NBTTagCompound, world: World, x: Int, y: Int, z: Int): Unit = {
    val block = Block.getBlockFromName(comp.getString("blockName"))
    val meta = comp.getInteger("blockMeta")
    val tileEntityData = if (comp.hasKey("tileEntity"))
          comp.getTag("tileEntity").asInstanceOf[NBTTagCompound]
        else null

    world.setBlock(x, y, z, block, meta, 0)
    val tileEntity = world.getTileEntity(x, y, z)

    if (tileEntityData != null && tileEntity != null) {
      if ((tileEntityData hasKey "x") && (tileEntityData hasKey "y") && (tileEntityData hasKey "z")) {
        tileEntityData.setInteger("x", x);
        tileEntityData.setInteger("y", y);
        tileEntityData.setInteger("z", z);
      }
      tileEntity.readFromNBT(tileEntityData)
    }
  }


  // =========== Pseudo-constructors for Structure class. Kind of awkward but whatever ==============

  def fromTagComp(comp: NBTTagCompound) = {
    var struct = new Structure(null, (0,0,0), (0,0,0), null)
    struct.round1 = comp.getTag("round1").asInstanceOf[NBTTagList]
    struct.round2 = comp.getTag("round2").asInstanceOf[NBTTagList]
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
  corner1: (Int,Int,Int),
  corner2: (Int,Int,Int),
  blacklist: Map[String, Symbol]
) {
  // Round 1 is for the majority of blocks
  var round1 = new NBTTagList
  // Round 2 contains stuff that might fall, like torches
  var round2 = new NBTTagList

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

    // Grab EVERY BLOCK.
    for (x <- min.x to max.x)
    for (y <- min.y to max.y)
    for (z <- min.z to max.z) {
      val relX = x - min.x
      val relY = y - min.y
      val relZ = z - min.z

      Structure.serializeBlock(world, x, y, z, blacklist) match {
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

  // x,y,z starting at bottom corner
  def placeAt(world: World, x: Int, y: Int, z: Int) = {
    def place(blocks: NBTTagList, i: Int) = {
      val comp = blocks.getCompoundTagAt(i)
      Structure.placeBlock(comp, world,
        x + comp.getInteger("relX"),
        y + comp.getInteger("relY"),
        z + comp.getInteger("relZ")
      )
    }

    if (Sburb.isServer)
      Structure.updateClientsPacket.send(world, this, x, y, z)

    for (i <- 0 until round1.tagCount) place(round1, i)
    for (i <- 0 until round2.tagCount) place(round2, i)
  }

  def toTagComp(additionalInfo: NBTTagCompound => Unit): NBTTagCompound = {
    var comp = new NBTTagCompound
    comp.setTag("round1", round1)
    comp.setTag("round2", round2)
    if (additionalInfo != null)
      additionalInfo(comp)
    comp
  }
  def toTagComp(): NBTTagCompound = toTagComp(null)

  def saveToFile(fileName: String) = {
    var fileOut = new FileOutputStream(fileName)
    var out = new ObjectOutputStream(fileOut)

    Structure.nbtWrite.invoke(toTagComp, out)

    out.close()
  }
}