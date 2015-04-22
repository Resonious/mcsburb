package net.resonious.sburb.abstracts

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTBase
import io.netty.buffer.ByteBuf
import cpw.mods.fml.common.network.ByteBufUtils
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World
import net.resonious.sburb.Sburb

object MathStuff {
  implicit class TupleVec(tup: (Double,Double,Double)) {
    def x = tup._1; def y = tup._2; def z = tup._3

    def +(other: (Double,Double,Double)) = (tup._1+other._1, tup._2+other._2, tup._3+other._3)
    def -(other: (Double,Double,Double)) = (tup._1-other._1, tup._2-other._2, tup._3-other._3)
    def *(scalar: Double) = (tup._1 * scalar, tup._2 * scalar, tup._3 * scalar)
    def /(scalar: Double) = (tup._1 / scalar, tup._2 / scalar, tup._3 / scalar)
  }
  implicit def Vec32Tuple3(v: Vec3) = (v.xCoord, v.yCoord, v.zCoord)
}

object Pimp {
  
  implicit class SlightlyDeobfuscatedWorld(world:World) {
    def destroyBlock(x:Int,y:Int,z:Int, drops:Boolean) = 
      world.func_147480_a(x, y, z, drops)
  }

  implicit class StringUtils(str: String) {
    def or(substitute: String) = if (str.isEmpty) substitute else str
  }
    
  implicit class IntBoolean(i: Int) {
    def toBoolean = if (i == 0) false else true
  }
  implicit class BooleanInt(b: Boolean) {
    def toInt: Int = if (b) 1 else 0
  }
  
  implicit class SuperByteBuf(buf: ByteBuf) {
    def <<(param: Any) = {
      param match {
        case b: Boolean => buf writeBoolean b
        case i: Int     => buf writeInt i
        case l: Long    => buf writeLong l
        case b: Byte    => buf writeByte b
        case f: Float   => buf writeFloat f
        case d: Double  => buf writeDouble d
        case s: String  => {
          val bytes = s.getBytes
          buf.writeInt(bytes.length)
          buf.writeBytes(s.getBytes);
        }
        
      }
    }
    // God, why does ByteBuf not have functions for strings
    def writeString(str: String) = {
      ByteBufUtils.writeUTF8String(buf, str)
    }
    
    def readString() = {
      ByteBufUtils.readUTF8String(buf)
    }
    
    def writePlayer(plr: EntityPlayer) = {
      writeString(plr.getGameProfile.getName)
    }
    
    def readPlayer(plr: EntityPlayer) = {
      Sburb.playerOfName(readString)
    }
    
    def writeTag(tag: NBTTagCompound) = {
      ByteBufUtils.writeTag(buf, tag)
    }
    
    def readTag() = {
      ByteBufUtils.readTag(buf)
    }
    def readAny(clazz: Class[_]):Any = {
      if (clazz == classOf[Int])
      	return buf.readInt
      if (clazz == classOf[Long])
        return buf.readLong
      if (clazz == classOf[Byte])
        return buf.readByte
      if (clazz == classOf[Float])
        return buf.readFloat
      if (clazz == classOf[Double])
        return buf.readDouble
      if (clazz == classOf[String])
        return readString()
    }
  }

}
