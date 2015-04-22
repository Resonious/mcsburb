package net.resonious.sburb.packets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import net.minecraft.entity.player.EntityPlayer
import net.resonious.sburb.abstracts.AbstractPacket
import net.resonious.sburb.abstracts.Pimp._
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import net.minecraft.server.MinecraftServer
import cpw.mods.fml.common.network.ByteBufUtils
import net.minecraft.nbt.NBTTagCompound

class ActivePacket extends AbstractPacket {
  // Much easier to write than encodeInto blah blah ChannelHandlerBullshit ugh
  def write(buffer: ByteBuf) = {}
  def read(buffer: ByteBuf) = {}
  
  // Also much easier to write than HANDLELCIENTSIETADF
  def onClient(player: EntityPlayer) = {}
  def onServer(player: EntityPlayer) = {}
  
	override def encodeInto(ctx: ChannelHandlerContext, buffer: ByteBuf) = {
	  write(buffer)
	}
	override def decodeInto(ctx: ChannelHandlerContext, buffer: ByteBuf) = {
	  read(buffer)
	}
	
	override def handleClientSide(player: EntityPlayer) = {
	  onClient(player)
	}
	override def handleServerSide(player: EntityPlayer) = {
	  onServer(player)
	}
}