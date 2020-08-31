package minecrafttransportsimulator.packets.instances;

import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.core.IItemVehicleInteractable;
import minecrafttransportsimulator.items.core.IItemVehicleInteractable.CallbackType;
import minecrafttransportsimulator.items.core.IItemVehicleInteractable.PlayerOwnerState;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.item.ItemStack;

/**Packet used to interact with vehicles.  Initially sent from clients to the server
 * to handle players clicking on the vehicle.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(ItemStack, EntityVehicleF_Physics, APart, WrapperPlayer, PlayerOwnerState, boolean)}
 * 
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketVehicle{
	private final Point3d hitPosition;
	private PacketVehicleInteractType type;
		
	public PacketVehicleInteract(EntityVehicleF_Physics vehicle, Point3d hitPosition, PacketVehicleInteractType type){
		super(vehicle);
		this.hitPosition = hitPosition;
		this.type = type;
	}
	
	public PacketVehicleInteract(ByteBuf buf){
		super(buf);
		this.hitPosition = readPoint3dFromBuffer(buf);
		this.type = PacketVehicleInteractType.values()[buf.readByte()];
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(hitPosition, buf);
		buf.writeByte(type.ordinal());
	}

	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		boolean canPlayerEditVehicle = player.isOP() || vehicle.ownerUUID.isEmpty() || player.getUUID().equals(vehicle.ownerUUID);
		PlayerOwnerState ownerState = player.isOP() ? PlayerOwnerState.ADMIN : (canPlayerEditVehicle ? PlayerOwnerState.OWNER : PlayerOwnerState.USER);
		ItemStack heldStack = player.getHeldStack();
		APart part = vehicle.getPartAtLocation(hitPosition);
		
		//If we clicked with with an item that can interact with a part or vehicle, perform that interaction.
		//Otherwise, try to do part-based interaction.
		if(heldStack.getItem() instanceof IItemVehicleInteractable){
			CallbackType callback = ((IItemVehicleInteractable) heldStack.getItem()).doVehicleInteraction(vehicle, part, player, ownerState, type.rightClick);
			if(callback.equals(CallbackType.ALL)){
				return true;
			}else if(callback.equals(CallbackType.PLAYER)){
				player.sendPacket(this);
			}
		}else{
			//Not holding an item that can interact with a vehicle.  Try to interact with parts or slots.
			if(type.equals(PacketVehicleInteractType.PART_RIGHTCLICK)){
				part.interact(player);
			}else if(type.equals(PacketVehicleInteractType.PART_LEFTCLICK)){
				part.attack(new Damage("player", 1.0F, part.boundingBox, player));
			}else if(type.equals(PacketVehicleInteractType.PART_SLOT_RIGHTCLICK)){
				//Only owners can add vehicle parts.
				if(!canPlayerEditVehicle){
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
				}else{
					//Attempt to add the part.  Vehicle is responsible for callback packet here.
					if(heldStack.getItem() instanceof AItemPart){
						if(vehicle.addPartFromItem((AItemPart) heldStack.getItem(), heldStack.hasTagCompound() ? new WrapperNBT(heldStack) : null, hitPosition)){				
							player.removeItem(heldStack, 1);
						}
					}
				}
			}else if(type.equals(PacketVehicleInteractType.DOOR_RIGHTCLICK)){
				//Can't open locked vehicles.
				if(vehicle.locked){
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
				}else{
					//Open the clicked door.
					for(Entry<BoundingBox, String> doorEntry : vehicle.doorBoxes.entrySet()){
						if(doorEntry.getKey().localCenter.equals(hitPosition)){
							if(vehicle.doorsOpen.contains(doorEntry.getValue())){
								vehicle.doorsOpen.remove(doorEntry.getValue());
							}else{
								vehicle.doorsOpen.add(doorEntry.getValue());
							}
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public static enum PacketVehicleInteractType{
		COLLISION_RIGHTCLICK(true),
		COLLISION_LEFTCLICK(false),
		PART_RIGHTCLICK(true),
		PART_LEFTCLICK(false),
		PART_SLOT_RIGHTCLICK(true),
		DOOR_RIGHTCLICK(true);
		
		private final boolean rightClick;
		
		private PacketVehicleInteractType(boolean rightClick){
			this.rightClick = rightClick;
		}
	}
}
