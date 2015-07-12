package net.resonious.sburb.game

import net.resonious.sburb.commands.SburbCommand.PlayerWithChat
import net.minecraft.entity.passive.EntityPig
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.util.Random
import io.netty.buffer.ByteBuf
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts._
import net.minecraft.world.biome.BiomeGenBase
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.World
import net.resonious.sburb.abstracts.ActiveCommand
import net.resonious.sburb.abstracts.Pimp._
import net.resonious.sburb.abstracts.Command
import net.resonious.sburb.game._
import net.resonious.sburb.packets.ActivePacket
import net.minecraft.client.entity.EntityClientPlayerMP
import net.resonious.sburb.Structure
import scala.util.control.Breaks
import net.resonious.sburb.entities.HousePortal
import net.resonious.sburb.entities.HousePortalRenderer
import net.resonious.sburb.entities.ReturnNode
import net.minecraft.block.BlockWood
import net.minecraft.block.BlockLeaves
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockStaticLiquid
import net.minecraft.block.BlockDynamicLiquid
import net.minecraft.block.material.Material
import net.minecraft.item.ItemStack
import net.minecraft.util.ChunkCoordinates
import com.xcompwiz.mystcraft.api.impl.InternalAPI
import com.xcompwiz.mystcraft.world.agedata.AgeData
import com.xcompwiz.mystcraft.world.WorldProviderMyst
import com.xcompwiz.mystcraft.api.util.Color
import com.xcompwiz.mystcraft.page.Page
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.ForgeChunkManager
import net.minecraft.nbt.NBTTagString
import net.minecraft.nbt.NBTTagInt
import net.minecraft.block.Block
import greymerk.roguelike.catacomb.Catacomb
import greymerk.roguelike.citadel.Citadel
import greymerk.roguelike.worldgen.Coord
import greymerk.roguelike.catacomb.settings.SpawnCriteria
import greymerk.roguelike.catacomb.settings.CatacombSettingsResolver
import greymerk.roguelike.catacomb.settings.CatacombSettings
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsBasicLoot
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsColdTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsDesertTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsEniTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsEthoTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsForestTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsJungleTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsMesaTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsMountainTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsPyramidTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsRooms
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsSecrets
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsSegments
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsSize
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsSwampTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsTaigaTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsTempleTheme
import greymerk.roguelike.catacomb.settings.builtin.CatacombSettingsWitchTheme

import scala.math

object Medium {
  class MediumSettingsBasicLoot extends CatacombSettingsBasicLoot {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsColdTheme extends CatacombSettingsColdTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsDesertTheme extends CatacombSettingsDesertTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsEniTheme extends CatacombSettingsEniTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsEthoTheme extends CatacombSettingsEthoTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsForestTheme extends CatacombSettingsForestTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsJungleTheme extends CatacombSettingsJungleTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsMesaTheme extends CatacombSettingsMesaTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsMountainTheme extends CatacombSettingsMountainTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsPyramidTheme extends CatacombSettingsPyramidTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsRooms extends CatacombSettingsRooms {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsSecrets extends CatacombSettingsSecrets {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsSegments extends CatacombSettingsSegments {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsSize extends CatacombSettingsSize {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsSwampTheme extends CatacombSettingsSwampTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsTaigaTheme extends CatacombSettingsTaigaTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsTempleTheme extends CatacombSettingsTempleTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }
  class MediumSettingsWitchTheme extends CatacombSettingsWitchTheme {
    override def isValid(world: World, coord: Coord): Boolean = true
  }

  val settingsClasses = Array(
      classOf[MediumSettingsBasicLoot],
      classOf[MediumSettingsColdTheme],
      classOf[MediumSettingsDesertTheme],
      classOf[MediumSettingsEniTheme],
      classOf[MediumSettingsEthoTheme],
      classOf[MediumSettingsForestTheme],
      classOf[MediumSettingsJungleTheme],
      classOf[MediumSettingsMesaTheme],
      classOf[MediumSettingsMountainTheme],
      classOf[MediumSettingsPyramidTheme],
      classOf[MediumSettingsRooms],
      classOf[MediumSettingsSecrets],
      classOf[MediumSettingsSegments],
      classOf[MediumSettingsSize],
      classOf[MediumSettingsSwampTheme],
      classOf[MediumSettingsTaigaTheme],
      classOf[MediumSettingsTempleTheme],
      classOf[MediumSettingsWitchTheme]
    )

  val baseSettings = classOf[CatacombSettingsResolver].getDeclaredField("base")
  baseSettings.setAccessible(true)

  def generate(player: EntityPlayer): Unit = {
    val props = SburbProperties of player
    if (!props.hasGame) {
      player chat "No sburb game!"
      return
    }
    if (props.gameEntry.houseCurrentlyBeingMoved) {
      player chat "Your medium is still being generated. No worries."
      return
    }

    player chat "Generating medium..."

    val serverPlayer = props.gameEntry.serverPlayer
    if (serverPlayer != null) {
      val serverProps = SburbProperties of serverPlayer
      serverProps.serverMode.activated = false
    }

    val playerEntry = props.gameEntry
    val house       = playerEntry.house

    val savedHouse = new Structure(
      player.worldObj,
      (house.minX, house.minY, house.minZ),
      (house.maxX, 500,        house.maxZ),
      Map("minecraft:dirt" -> 'ignore)
    )
    savedHouse.centerOffset.y = house.centerY
    savedHouse.spawnPoint = new Vector3[Int](
      house.spawn.x - house.minX,
      house.spawn.y - house.minY,
      house.spawn.z - house.minZ
      // I think this is good
    )

    // Keeping this flag around so that maybe we can tell the player that it's being worked on
    // if they're antsy and clicking buttons a lot.
    playerEntry.houseCurrentlyBeingMoved = true

    // I guess grabbing the structure AND generating the world in one tick is
    // just too much.
    After(20, 'ticks) execute {
      val dimensionId = InternalAPI.dimension.createAge
      val age         = AgeData.getAge(dimensionId, false)

      age.setInstabilityEnabled(false)
      val rand = new Random

      var symbols = new ListBuffer[String]

      def twoFrom(list: Array[String]) = {
        var a = rand.nextInt(list.length)
        var b = rand.nextInt(list.length)
        while (b == a) b = rand.nextInt(list.length)
        List(list(a), list(b))
      }

      symbols ++= List("DenseOres", "Caves", "BioConSingle")

      val obstructions = Array(
        "TerModSpheres", "FloatIslands", "Tendrils", "Obelisks",
        "StarFissure", "HugeTrees", "GenSpikes", "CryForm"
      )
      symbols ++= twoFrom(obstructions)
      val hasFloatIslands = symbols contains "FloatIslands"

      if (!hasFloatIslands) {

        val terrain = Array(
          "TerrainNormal", "TerrainAplified", "TerrainEnd", "Skylands"
        )
        symbols += terrain(rand.nextInt(terrain.length))

        val exploration = Array(
          "Villages", "NetherFort", "Mineshafts", "Dungeons",
          "Ravines", "LakesDeep"
        )
        symbols ++= twoFrom(exploration)

      }

      val colors = List(
        "ModBlack", "ModRed", "ModGreen", "ModBlue",
        "ModYellow", "ModWhite"
      )
      val availableColors = new ArrayBuffer[String](colors.length)
      availableColors ++= colors
      availableColors --= playerEntry.game.mediumColors

      val complimentColors = Map(
        "ModBlack"  -> Array("ModBlack", "ModRed", "ModWhite"),
        "ModBlue"   -> Array("ModBlue", "ModYellow", "ModBlack"),
        "ModRed"    -> Array("ModRed", "ModWhite", "ModBlack"),
        "ModYellow" -> Array("ModYellow", "ModBlack"),
        "ModGreen"  -> Array("ModGreen", "ModRed")
      )

      val color = if (availableColors.length > 0) availableColors(rand.nextInt(availableColors.length))
                  else colors(rand.nextInt(colors.length))

      val compliment = complimentColors.get(color) match {
        case Some(compliments) => { compliments(rand.nextInt(compliments.length)) }
        case None => color
      }

      if (rand.nextInt(7) == 1) {
        // Totally random colors for everything.
        symbols ++= List(
          colors(rand.nextInt(colors.length)), "ColorFog",
          colors(rand.nextInt(colors.length)), "ColorFogNat",
          colors(rand.nextInt(colors.length)), "ColorSky",
          colors(rand.nextInt(colors.length)), "ColorSkyNat",
          colors(rand.nextInt(colors.length)), "ColorWater",
          colors(rand.nextInt(colors.length)), "ColorWaterNat",
          colors(rand.nextInt(colors.length)), "ColorGrass",
          colors(rand.nextInt(colors.length)), "ColorFoliage",
          colors(rand.nextInt(colors.length)), "ColorCloud",
          colors(rand.nextInt(colors.length)), "ColorSkyNight",
          "Rainbow"
        )
      }
      else {
        // Normal color/compliment distribution.
        symbols ++= List(
          color, "ColorFog",
          color, "ColorFogNat",
          color, "ColorSky",
          color, "ColorSkyNat",
          compliment, "ColorWater",
          compliment, "ColorWaterNat",
          color, "ColorGrass",
          color, "ColorFoliage",
          compliment, "ColorCloud",
          compliment, "ColorSkyNight"
        )
        if (rand.nextInt(3) == 1) symbols += "Rainbow"
      }

      // TODO the land of ?? and ??
      val colorName = color.replace("Mod", "")
      playerEntry.mediumColor = colorName
      age setAgeName player.getDisplayName() + " medium"
      age.cruft.put("sburbcolor", new NBTTagString(colorName))
      age setPages (symbols.map(Page.createSymbolPage))
        .asJava
        .asInstanceOf[java.util.List[ItemStack]]

      playerEntry.mediumId = dimensionId

      // Force Mystcraft to generate the world,
      val dummyPig = new EntityPig(player.worldObj)
      Sburb.warpEntity(dummyPig, dimensionId, new Vector3(0, 50, 0))

      val newWorld       = DimensionManager.getWorld(dimensionId)
      val ticket         = ForgeChunkManager.requestTicket(Sburb, newWorld, ForgeChunkManager.Type.NORMAL)
      val forceLoadChunk = new ChunkCoordIntPair(0, 0)
      // Keep player name around in case they log out/die/yadda yadda
      val playerName     = player.getCommandSenderName
      // Then keep the world alive until we're ready to warp the player.
      ForgeChunkManager.forceChunk(ticket, forceLoadChunk)

      Sburb log "Medium symbols: "+symbols.mkString(", ")

      // Give Mystcraft enough time to build the world before scanning the shit out of it.
      After(5, 'seconds) execute {
        Sburb log "PLACING HOUSE INTO MEDIUM"

        def houseWasPlaced(point: Vector3[Int]) = {
          // Informs SburbServerMode that whatever position/dimension you have saved is now wrong.
          house.wasMoved = true
          playerEntry.houseCurrentlyBeingMoved = false
          Sburb log "PLACING HOUSE INTO MEDIUM: DONE"

          val pointOfInterest = new Vector3[Int](
            house.spawn.x - 2000 + rand.nextInt(1000),
            150,
            house.spawn.z - 2000 + rand.nextInt(1000)
          )
          pointOfInterest.y = caveLevelAt(newWorld, pointOfInterest)
          if (pointOfInterest.y <= 0) {
            pointOfInterest.y = 150
            pointOfInterest.y = groundLevelAt(newWorld, pointOfInterest)
          }
          playerEntry.mediumPointOfInterest = new Vector3[Int](pointOfInterest)

          playerEntry.mediumCatacombsThemes += rand.nextInt(18)
          playerEntry.mediumCatacombsThemes += rand.nextInt(18)

          After(8, 'seconds) execute {
            Sburb log "Spawning THE DUNGEON"
            spawnDungeon(newWorld, rand, playerEntry.mediumCatacombsThemes,
              pointOfInterest.instead(
                { v => { v.x += 50; v.z += 50 }}
              )
            )
          }

          val portal = new HousePortal(newWorld)
          portal.targetPos = pointOfInterest
          portal.targetDim = dimensionId
          portal.setColorFromString(color.replace("Mod", ""))
          val portalY = groundLevelAt(newWorld, point.instead(_.y += 30)) + 10
          portal.setPosition(point.x, portalY, point.z)
          newWorld.spawnEntityInWorld(portal)

          playerEntry.lastPortalSpot = new Vector3[Int](point.x, portalY, point.z)

          playerEntry eachClient { clientEntry =>
            if (clientEntry.mediumId != 0) spawnPortalsToMedium(clientEntry, playerEntry)
          }
          playerEntry eachServer { serverEntry =>
            if (serverEntry.mediumId != 0) spawnPortalsToMedium(playerEntry, serverEntry)
          }

          startPlacingStuff(newWorld, playerEntry)

          ForgeChunkManager.unforceChunk(ticket, forceLoadChunk)

          Sburb playerOfName playerName match {
            case null => {
              playerEntry.spawnPointDirty = true
            }

            case player => {
              val housePos = house.spawn
              val coords = new ChunkCoordinates(housePos.x, housePos.y, housePos.z)
              player.setSpawnChunk(coords, true, dimensionId)
              Sburb.warpPlayer(player, dimensionId, housePos)
            }
          }
        }

        house.placeIntoWorld(savedHouse, newWorld, houseWasPlaced)

        house whenFailedToPlace { tryCount =>
          val newSpot = new Vector3[Int](0, 100, 0)
          Sburb log "Might be a blank world.. Placing house in the air somewhere."
          house.placeAt(savedHouse, newWorld, newSpot)
          houseWasPlaced(newSpot)
        }
      }
    }
  }

  def spawnPortalsToMedium(from: SburbGame.PlayerEntry, to: SburbGame.PlayerEntry): Unit = {
    val portalDistance = 30
    val fromWorld = DimensionManager.getWorld(from.mediumId)
    val toWorld   = DimensionManager.getWorld(to.mediumId)

    from.lastPortalSpot.y += portalDistance

    val portalToHouse = new HousePortal(fromWorld)
    portalToHouse.targetPos = new Vector3[Int](to.house.spawn)
    portalToHouse.targetDim = to.mediumId
    portalToHouse.setColorFromWorld()
    portalToHouse.setPosition(from.lastPortalSpot.x, from.lastPortalSpot.y, from.lastPortalSpot.z)
    fromWorld.spawnEntityInWorld(portalToHouse)

    from.lastPortalSpot.y += portalDistance

    val portalToPoi = new HousePortal(fromWorld)
    portalToPoi.targetPos = new Vector3[Int](to.mediumPointOfInterest)
    portalToPoi.targetDim = to.mediumId
    portalToPoi.setColorFromWorld()
    portalToPoi.setPosition(from.lastPortalSpot.x, from.lastPortalSpot.y, from.lastPortalSpot.z)
    fromWorld.spawnEntityInWorld(portalToPoi)
  }

  def groundLevelAt(world: World, point: Vector3[Int]): Int = {
    var s: Vector3[Int] = new Vector3[Int](point)
    while (true) {
      val block = world.getBlock(s.x, s.y, s.z)

      if (s.y > 0 &&
          (world.isAirBlock(s.x, s.y, s.z) ||
          block.isInstanceOf[BlockWood]    ||
          block.isInstanceOf[BlockLeaves])) {
        s.y -= 1
      }
      else {
        if (s.y == 0) return 50
        else return s.y + 1
      }
    }
    0
  }

  def caveLevelAt(world: World, point: Vector3[Int]): Int = {
    var s: Vector3[Int] = new Vector3[Int](point)
    var hitGround = false

    def ignoreBlock(block: Block) = {
      world.isAirBlock(s.x, s.y, s.z) ||
      block.isInstanceOf[BlockWood]   ||
      block.isInstanceOf[BlockLeaves]
    }

    while (true) {
      val block = world.getBlock(s.x, s.y, s.z)

      if (hitGround) {
        s.y -= 1
        if (s.y <= 0)
          return 0
        else if (ignoreBlock(block))
          return groundLevelAt(world, s)
      }
      else {
        if (s.y > 0 && ignoreBlock(block)) {
          s.y -= 1
        }
        else {
          hitGround = true
        }
      }
    }
    0
  }

  def spawnDungeon(world: World, rand: Random, themes: ArrayBuffer[Int], pos: Vector3[Int]) {
    if (rand.nextInt(10) == 1) {
      // catacomb
      val theme = settingsClasses(themes(rand.nextInt(themes.length))).newInstance
      val base = Medium.baseSettings.get(Catacomb.settingsResolver)
      val settings = new CatacombSettings(base.asInstanceOf[CatacombSettings], theme.asInstanceOf[CatacombSettings])

      try Catacomb.generate(world, settings, pos.x, pos.z)
      catch {
        case e: NullPointerException => { Sburb logError "Catacombs fucked up" }
      }
    }
    else {
      // citadel
      Citadel.generate(world, pos.x, pos.z)
    }
  }

  def spawnReturnNode(world: World, gameEntry: SburbGame.PlayerEntry, pos: Vector3[Int]) = {
    val returnNode = new ReturnNode(world)
    returnNode.setPosition(pos.x, pos.y, pos.z)
    returnNode.targetDim = gameEntry.mediumId
    returnNode.targetPos = gameEntry.house.spawn
    returnNode.setColorFromWorld()
    world.spawnEntityInWorld(returnNode)

    Sburb log "SPAWNED RETURN NODE AT "+pos.disp
  }

  // For now, this just spawns some return nodes.
  // Hopefully in the near future, we will have traps as well!
  def startPlacingStuff(world: World, gameEntry: SburbGame.PlayerEntry) = {
    val rand = new Random
    def randomRange(radius: Int) = {
      rand.nextInt(radius * 2) - radius
    }

    var poi1 = new Vector3[Int]

    After(1, 'second) execute {
      val age = AgeData.getAge(world.provider.dimensionId, false)

      poi1 = new Vector3[Int](gameEntry.mediumPointOfInterest)

      val returnNode1Pos = new Vector3[Int](
        poi1.x + randomRange(10),
        groundLevelAt(world, poi1) + rand.nextInt(6),
        poi1.z + randomRange(10)
      )
      if (returnNode1Pos.x == poi1.x) returnNode1Pos.x = poi1.x + 2
      if (returnNode1Pos.z == poi1.z) returnNode1Pos.z = poi1.z + 2

      spawnReturnNode(world, gameEntry, returnNode1Pos)
    }

    After(2, 'seconds) execute {
      var dungeonCount = 1

      for (i <- 0 until 100) {
        val spawn = gameEntry.house.spawn

        val returnNodeSpot = new Vector3[Int](
          spawn.x + randomRange(500),
          256,
          spawn.z + randomRange(500)
        )
        // We don't want return nodes real close by.
        val xDif = spawn.x - returnNodeSpot.x
        val zDif = spawn.z - returnNodeSpot.z
        if (math.abs(xDif) < 50) returnNodeSpot.x += 100 * math.signum(xDif)
        if (math.abs(zDif) < 50) returnNodeSpot.z += 100 * math.signum(zDif)
        returnNodeSpot.y = groundLevelAt(world, returnNodeSpot) + rand.nextInt(5)

        if (rand.nextInt(10) == 1) {
          After(dungeonCount * 10, 'seconds) execute {
            val dungeonSpot = returnNodeSpot.instead(
              { v => { v.x += 75; v.z += 75 }}
            )
            Sburb log "DUNGEON @ "+dungeonSpot.disp
            spawnDungeon(world, rand, gameEntry.mediumCatacombsThemes, dungeonSpot)
          }
          dungeonCount += 1
        }

        spawnReturnNode(world, gameEntry, returnNodeSpot)
      }
    }

    // Find caves!
    After(3, 'seconds) execute {
      for (i <- 0 until 100) {
        After(i*2, 'ticks) execute {
          val spawn = gameEntry.house.spawn

          val returnNodeSpot = new Vector3[Int](
            spawn.x + randomRange(500),
            150,
            spawn.z + randomRange(500)
          )
          val xDif = spawn.x - returnNodeSpot.x
          val zDif = spawn.z - returnNodeSpot.z
          if (math.abs(xDif) < 30) returnNodeSpot.x += 30 * math.signum(xDif)
          if (math.abs(zDif) < 30) returnNodeSpot.z += 30 * math.signum(zDif)
          returnNodeSpot.y = caveLevelAt(world, returnNodeSpot)

          if (returnNodeSpot.y > 0)
            spawnReturnNode(world, gameEntry, returnNodeSpot)
        }
      }
    }
  }
}
