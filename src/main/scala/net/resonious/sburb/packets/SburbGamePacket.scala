package net.resonious.sburb.packets

import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayer
import net.resonious.sburb.commands.SburbCommand.PlayerWithChat
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.SburbException
import net.resonious.sburb.game.SburbProperties
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.resonious.sburb.abstracts.PacketPipeline
import net.resonious.sburb.game.After
import net.minecraft.util.ChunkCoordinates

object SburbGamePacket {
  // This packet adds a new player to a Sburb game
	class NewPlayerPacket extends ActivePacket {
	  var houseId = 0

	  @SideOnly(Side.CLIENT)
	  def send(houseId: Int) = {
	    this.houseId = houseId
	    PacketPipeline.sendToServer(this)
	  }

	  override def write(buf: ByteBuf) = {
	    buf writeInt houseId
	  }
	  override def read(buf: ByteBuf) = {
	    houseId = buf.readInt
	  }

	  override def onServer(player: EntityPlayer) = {
	  	val props = SburbProperties of player
	    if (props.hasGame)
	      throw new SburbException("Can't assign a new game to a player when they're already in one!")

	    val game = Sburb.games.size match {
	      case 0 => Sburb.newGame
	      case 1 => Sburb.games.values.iterator.next
	      case _ => throw new SburbException("Don't know what to do with more than 1 game yet!")
	    }
	  	if (!game.newPlayer(player, houseId, true))
	  	  throw new SburbException("Something went wrong adding "+player.getDisplayName+" to a Sburb game!")
	  	else {
	  	  val housePos = props.gameEntry.house.spawn
	  	  player.setPositionAndUpdate(
	  	      housePos.x,
	  	      housePos.y,
	  	      housePos.z)
				val coords = new ChunkCoordinates(housePos.x, housePos.y, housePos.z)
				player.setSpawnChunk(coords, true, 0)
	  	}
	  }
	}
	val newPlayer = new NewPlayerPacket

	class ServerModePacket extends ActivePacket {
		var activated = false

		@SideOnly(Side.CLIENT)
		def deactivate() = {
			this.activated = false
			PacketPipeline.sendToServer(this)
		}
		override def write(buf: ByteBuf) = {
			buf writeBoolean activated
		}
		override def read(buf: ByteBuf) = {
			activated = buf.readBoolean
		}

		override def onServer(player: EntityPlayer) = {
			val props = SburbProperties of player
			if (!props.hasGame)
				throw new SburbException("This guy doesn't even have a game")

			props.serverMode.activated = activated
		}
	}
	val serverMode = new ServerModePacket
}
