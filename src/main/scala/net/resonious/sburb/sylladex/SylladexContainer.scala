package net.resonious.sburb.sylladex

import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryCraftResult
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.util.IIcon

class SylladexContainer(par1InventoryPlayer: InventoryPlayer, par2: Boolean, par3EntityPlayer: EntityPlayer) extends Container {
  var craftMatrix: InventoryCrafting = new InventoryCrafting(this, 2, 2)
  var craftResult: IInventory = new InventoryCraftResult()
  var isLocalWorld: Boolean = par2
  private val thePlayer = par3EntityPlayer

  var i: Int = 0
  var j: Int = 0
  i = 0
  // Armor slots; crafting slots are taken out
  while (i < 4) {
    val k = i
    this.addSlotToContainer(new Slot(par1InventoryPlayer, par1InventoryPlayer.getSizeInventory - 1 - i, 8, 8 + i * 18) {
      override def getSlotStackLimit(): Int = return 1

      override def isItemValid(par1ItemStack: ItemStack): Boolean = {
        if (par1ItemStack == null) return false
        return par1ItemStack.getItem.isValidArmor(par1ItemStack, k, thePlayer)
      }

      @SideOnly(Side.CLIENT)
      override def getBackgroundIconIndex(): IIcon = return ItemArmor.func_94602_b(k)
    })
    i += 1
  }

  // What's this?
  i = 0
  while (i < 3) {
    j = 0
    while (j < 9) {
      this.addSlotToContainer(new Slot(par1InventoryPlayer, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18))
      j += 1
    }
    i += 1
  }

  // Mmmm hotbar slots... I think
  i = 0
  while (i < 9) {
    this.addSlotToContainer(new Slot(par1InventoryPlayer, i, 8 + i * 18, 142))
    i += 1
  }

  // Probably don't want this
  // this.onCraftMatrixChanged(this.craftMatrix)

  // Haha who cares
  override def onCraftMatrixChanged(par1IInventory: IInventory) {
    this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance.findMatchingRecipe(this.craftMatrix, this.thePlayer.worldObj))
  }

  // Alright, alright...
  override def onContainerClosed(par1EntityPlayer: EntityPlayer) {
    super.onContainerClosed(par1EntityPlayer)
    for (i <- 0 until 4) {
      val itemstack = this.craftMatrix.getStackInSlotOnClosing(i)
      if (itemstack != null) {
        par1EntityPlayer.dropPlayerItemWithRandomChoice(itemstack, false)
      }
    }
    this.craftResult.setInventorySlotContents(0, null.asInstanceOf[ItemStack])
  }

  def canInteractWith(par1EntityPlayer: EntityPlayer): Boolean = true

  override def getSlot(i: Int): Slot = {
    if (i < 0 || i >= inventorySlots.size)
      return new Slot(thePlayer.inventory, 35, 0, 0)
    inventorySlots.get(i) match {
    	case s: Slot => return s
    }
  }
  
  // Shift-click event. This might have to be fucked with
  override def transferStackInSlot(par1EntityPlayer: EntityPlayer, par2: Int): ItemStack = {
    var itemstack: ItemStack = null
    val slot = this.inventorySlots.get(par2).asInstanceOf[Slot]
    if (slot != null && slot.getHasStack) {
      val itemstack1 = slot.getStack
      itemstack = itemstack1.copy()
      if (par2 == 0) {
        if (!this.mergeItemStack(itemstack1, 9, 45, true)) {
          return null
        }
        slot.onSlotChange(itemstack1, itemstack)
      } else if (par2 >= 1 && par2 < 5) {
        if (!this.mergeItemStack(itemstack1, 9, 45, false)) {
          return null
        }
      } else if (par2 >= 5 && par2 < 9) {
        if (!this.mergeItemStack(itemstack1, 9, 45, false)) {
          return null
        }
      } else if (itemstack.getItem.isInstanceOf[ItemArmor] && !this.inventorySlots.get(5 + itemstack.getItem.asInstanceOf[ItemArmor].armorType).asInstanceOf[Slot].getHasStack) {
        val j = 5 + itemstack.getItem.asInstanceOf[ItemArmor].armorType
        if (!this.mergeItemStack(itemstack1, j, j + 1, false)) {
          return null
        }
      } else if (par2 >= 9 && par2 < 36) {
        if (!this.mergeItemStack(itemstack1, 36, 45, false)) {
          return null
        }
      } else if (par2 >= 36 && par2 < 45) {
        if (!this.mergeItemStack(itemstack1, 9, 36, false)) {
          return null
        }
      } else if (!this.mergeItemStack(itemstack1, 9, 45, false)) {
        return null
      }
      if (itemstack1.stackSize == 0) {
        slot.putStack(null.asInstanceOf[ItemStack])
      } else {
        slot.onSlotChanged()
      }
      if (itemstack1.stackSize == itemstack.stackSize) {
        return null
      }
      slot.onPickupFromSlot(par1EntityPlayer, itemstack1)
    }
    itemstack
  }

  // Something to do with crafting :/
  override def func_94530_a(par1ItemStack: ItemStack, par2Slot: Slot): Boolean = {
    par2Slot.inventory != this.craftResult && super.func_94530_a(par1ItemStack, par2Slot)
  }

/*
Original Java:
package net.resonious.sburb.sylladex;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.IIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SylladexContainer extends Container
{
    |** The crafting matrix inventory. *|
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();
    |** Determines if inventory manipulation should be handled. *|
    public boolean isLocalWorld;
    private final EntityPlayer thePlayer;
    private static final String __OBFID = "CL_00001754";

    public SylladexContainer(final InventoryPlayer par1InventoryPlayer, boolean par2, EntityPlayer par3EntityPlayer)
    {
        this.isLocalWorld = par2;
        this.thePlayer = par3EntityPlayer;
        // No crafting slots
        // this.addSlotToContainer(new SlotCrafting(par1InventoryPlayer.player, this.craftMatrix, this.craftResult, 0, 144, 36));
        int i;
        int j;

        |*for (i = 0; i < 2; ++i)
        {
            for (j = 0; j < 2; ++j)
            {
                this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 2, 88 + j * 18, 26 + i * 18));
            }
        }*|

        // We can keep the armor slots I GUESSSSS
        for (i = 0; i < 4; ++i)
        {
            final int k = i;
            this.addSlotToContainer(new Slot(par1InventoryPlayer, par1InventoryPlayer.getSizeInventory() - 1 - i, 8, 8 + i * 18)
            {
                private static final String __OBFID = "CL_00001755";
                |**
                 * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1
                 * in the case of armor slots)
                 *|
                public int getSlotStackLimit()
                {
                    return 1;
                }
                |**
                 * Check if the stack is a valid item for this slot. Always true beside for the armor slots.
                 *|
                public boolean isItemValid(ItemStack par1ItemStack)
                {
                    if (par1ItemStack == null) return false;
                    return par1ItemStack.getItem().isValidArmor(par1ItemStack, k, thePlayer);
                }
                |**
                 * Returns the icon index on items.png that is used as background image of the slot.
                 *|
                @SideOnly(Side.CLIENT)
                public IIcon getBackgroundIconIndex()
                {
                    return ItemArmor.func_94602_b(k);
                }
            });
        }

        for (i = 0; i < 3; ++i)
        {
            for (j = 0; j < 9; ++j)
            {
                this.addSlotToContainer(new Slot(par1InventoryPlayer, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (i = 0; i < 9; ++i)
        {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, i, 8 + i * 18, 142));
        }

        this.onCraftMatrixChanged(this.craftMatrix);
    }

    |**
     * Callback for when the crafting matrix is changed.
     *|
    public void onCraftMatrixChanged(IInventory par1IInventory)
    {
        this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.thePlayer.worldObj));
    }

    |**
     * Called when the container is closed.
     *|
    public void onContainerClosed(EntityPlayer par1EntityPlayer)
    {
        super.onContainerClosed(par1EntityPlayer);

        for (int i = 0; i < 4; ++i)
        {
            ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

            if (itemstack != null)
            {
                par1EntityPlayer.dropPlayerItemWithRandomChoice(itemstack, false);
            }
        }

        this.craftResult.setInventorySlotContents(0, (ItemStack)null);
    }

    public boolean canInteractWith(EntityPlayer par1EntityPlayer)
    {
        return true;
    }

    |**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     *|
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2)
    {
        ItemStack itemstack = null;
        Slot slot = (Slot)this.inventorySlots.get(par2);

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (par2 == 0)
            {
                if (!this.mergeItemStack(itemstack1, 9, 45, true))
                {
                    return null;
                }

                slot.onSlotChange(itemstack1, itemstack);
            }
            else if (par2 >= 1 && par2 < 5)
            {
                if (!this.mergeItemStack(itemstack1, 9, 45, false))
                {
                    return null;
                }
            }
            else if (par2 >= 5 && par2 < 9)
            {
                if (!this.mergeItemStack(itemstack1, 9, 45, false))
                {
                    return null;
                }
            }
            else if (itemstack.getItem() instanceof ItemArmor && !((Slot)this.inventorySlots.get(5 + ((ItemArmor)itemstack.getItem()).armorType)).getHasStack())
            {
                int j = 5 + ((ItemArmor)itemstack.getItem()).armorType;

                if (!this.mergeItemStack(itemstack1, j, j + 1, false))
                {
                    return null;
                }
            }
            else if (par2 >= 9 && par2 < 36)
            {
                if (!this.mergeItemStack(itemstack1, 36, 45, false))
                {
                    return null;
                }
            }
            else if (par2 >= 36 && par2 < 45)
            {
                if (!this.mergeItemStack(itemstack1, 9, 36, false))
                {
                    return null;
                }
            }
            else if (!this.mergeItemStack(itemstack1, 9, 45, false))
            {
                return null;
            }

            if (itemstack1.stackSize == 0)
            {
                slot.putStack((ItemStack)null);
            }
            else
            {
                slot.onSlotChanged();
            }

            if (itemstack1.stackSize == itemstack.stackSize)
            {
                return null;
            }

            slot.onPickupFromSlot(par1EntityPlayer, itemstack1);
        }

        return itemstack;
    }

    public boolean func_94530_a(ItemStack par1ItemStack, Slot par2Slot)
    {
        return par2Slot.inventory != this.craftResult && super.func_94530_a(par1ItemStack, par2Slot);
    }
}
*/
}