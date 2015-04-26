package net.resonious.sburb.items

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.resonious.sburb.abstracts
import net.resonious.sburb.Sburb
import net.resonious.sburb.abstracts.ActiveItem
import li.cil.oc.api.{FileSystem => IFileSystem}
import li.cil.oc.api.FileSystem
import li.cil.oc.api.fs
import li.cil.oc.api.Network
import li.cil.oc.api.driver._
import li.cil.oc.api.driver.item.Slot
import li.cil.oc.api.network._
import li.cil.oc.api.detail.FileSystemAPI
import li.cil.oc.api.prefab.DriverItem
import java.io
import li.cil.oc.server.component
import li.cil.oc.api.fs.Label
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import net.minecraft.nbt.NBTTagCompound

object SburbDisc extends ActiveItem("Sburb Disc") {
  li.cil.oc.api.Driver.add(DriverFloppySburbDisc)
}

object DriverFloppySburbDisc extends DriverItem(new ItemStack(SburbDisc)) {
  object SburbDiscLabel extends Label() {
    def getLabel() = "sburb"
    def setLabel(_l: String) = {}
    def load(x: NBTTagCompound): Unit = {}
    def save(x: NBTTagCompound): Unit = {}
  }

  class Environment(filesys: fs.FileSystem, host: Option[EnvironmentHost] )
  extends component.FileSystem(filesys, SburbDiscLabel, host, Option(li.cil.oc.Settings.resourceDomain + ":floppy_access")) {

    @Callback(direct = true, limit = 15, doc = """function(handle:number, count:number):string or nil -- Reads up to the specified amount of data from an open file descriptor with the specified handle. Returns nil when EOF is reached.""")
    override def read(context: Context, args: Arguments): Array[AnyRef] = super.read(context, args)

    @Callback(direct = true, limit = 15, doc = """function(handle:number, whence:string, offset:number):number -- Seeks in an open file descriptor with the specified handle. Returns the new pointer position.""")
    override def seek(context: Context, args: Arguments): Array[AnyRef] = super.seek(context, args)

    // @Callback(direct = true, limit = 6, doc = """function(handle:number, value:string):boolean -- Writes the specified data to an open file descriptor with the specified handle.""")
    // override def write(context: Context, args: Arguments): Array[AnyRef] = super.write(context, args)

    @Callback(direct = false, limit = 20)
    def sburbTest(context: Context, args: Arguments): Array[AnyRef] = {
      Sburb log "OMG. From lua: " + args.checkString(0)
      Array("this is it")
    }
  }

  override def slot(stack: ItemStack) = Slot.Floppy

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = {
    val filesys = FileSystem.fromClass(Sburb.getClass, "sburb", "sburbdisk")
    /*
    FileSystem.asManagedEnvironment(
      filesys, "sburb", host,
      li.cil.oc.Settings.resourceDomain + ":hdd_access"
    )
    */
    new Environment(filesys, Option(host))
  }

}