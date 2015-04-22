package net.resonious.sburb.game.grist

import net.minecraft.item.Item
import net.resonious.sburb.abstracts.ActiveItem
import scala.collection.mutable.HashMap
import net.resonious.sburb.abstracts.SburbException

object Grist extends Enumeration {
	type Grist = Value
	val Build, Amber = Value

	implicit class NonGristItem(item: Item) {
	  def isGristItem = item.isInstanceOf[GristItem]
	}
	private var _generatedGristItems = false
	def generateGristItems = 
	  if (_generatedGristItems)
	    throw new SburbException("Attempted to generate grist items twice.")
	  else {
	    _generatedGristItems = true
	    values map { gtype => 
    	  val i = new GristItem(gtype)
    	  item(gtype) = i
    	  i
    	}
	  }
	@transient
	val item = new HashMap[Grist.Value, GristItem]
}

// With how this is set up, the unlocalized name will be <gtype>_grist
// Meaning, for example, the icon for build grist should be at:
//    src/main/resources/assets/sburb/textures/items/build_grist.png
// and localization should be:
//	  src/main/resources/assets/sburb/lang/en_US.lang => item.build_grist.name=Build Grist
class GristItem(gtype: Grist.Value) extends ActiveItem(gtype + " Grist") {
  def gristType = gtype
  setMaxStackSize(64)
}
