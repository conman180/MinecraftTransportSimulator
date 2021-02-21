package minecrafttransportsimulator.items.components;

import java.util.List;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import net.minecraft.item.ItemStack;

/**Base item class for all MTS items.  Contains multiple methods to define the item's behavior,
 * such as display name, additional text to add to the tooltip, how the item handles left and
 * right-click usage, and so on.
 * 
 * @author don_bruce
 */
public abstract class AItemBase{
	
	/**
	 *  Returns the registration name of this item.  This MUST be unique for all items, or Bad Stuff will happen.
	 */
	public String getRegistrationName(){
		return getClass().getSimpleName().substring("Item".length()).toLowerCase();
	}
	
	/**
	 *  Returns the name of this item.  Will be displayed to the player in-game, but is NOT used
	 *  for item registration, so may change depending on item state.  By default this gets the 
	 *  registration name, with item. applied at the front, and .name applied at the end. 
	 *  This is then translated by the language system.  While this is the default, is by no means
	 *  set in stone, so feel free to modify it as you see fit.
	 */
	public String getItemName(){
		return InterfaceCore.translate("item." + getRegistrationName() + ".name");
	}
	
	/**
	 *  Called when the item tooltip is being displayed.  The passed-in list will contain
	 *  all the lines in the tooltip, so add or remove lines as you see fit.  If you don't
	 *  want to add any lines just leave this method blank. Data is assured not to be null.
	 *  However, this does not mean the data block will be populated with values.  If the
	 *  item is fresh from crafting, it may not have any data.
	 */
	public abstract void addTooltipLines(List<String> tooltipLines, WrapperNBT data);
	
	/**
	 *  Gets all item data values for the given item, and adds them to the passed-in list.
	 *  By default, this method does nothing, which means no additional blocks are present.
	 */
	public void getDataBlocks(List<WrapperNBT> list){}
	
	/**
	 *  Called when the player clicks a block with this item.  The position of the block
	 *  clicked and what axis it was hit at is passed-in for reference.  If this item did a thing
	 *  due to this clicking, return true, as this prevents calling the block's clicked method. 
	 */
	public boolean onBlockClicked(WrapperWorld world, WrapperPlayer player, Point3d position, Axis axis){
		if(this instanceof IItemBlock){
			return ((IItemBlock) this).placeBlock(world, player, position, axis);
		}else{
			return false;
		}
	}
	
	/**
	 *  Called when the player right-clicks with this item.  {@link AItemBase#onBlockClicked(WrapperWorld, WrapperPlayer, Point3d, Axis)}
	 *  is called before this method, and if and only if that method returns false will this method be called.
	 *  If this item does something, return true.
	 */
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		return false;
	}
	
	/**
	 *  Returns the stack size for this item.  Defaults to 1, as most items shouldn't be stack-able.
	 */
	public int getStackSize(){
		return 1;
	}
	
	/**
	 *  Returns the builder for this item.
	 */
	public BuilderItem getBuilder(){
		return BuilderItem.itemMap.get(this);
	}
	
	/**
	 *  Returns a stack with a single item of this type.
	 */
	public ItemStack getNewStack(){
		return new ItemStack(getBuilder());
	}
	
	/**
	 *  Gets the ID of the creative tab for this item to be displayed on.  Tabs are auto-created as required.
	 */
	public String getCreativeTabID(){
		return MasterLoader.resourceDomain;
	}
}
