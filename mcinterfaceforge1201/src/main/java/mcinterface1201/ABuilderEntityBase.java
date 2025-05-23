package mcinterface1201;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Builder for a basic MC Entity class.  This builder provides basic entity logic that's common
 * to all entities we may want to spawn.
 *
 * @author don_bruce
 */
//Need to extend LivingEntity since spawn syncing packets don't work with the base Entity class.
public abstract class ABuilderEntityBase extends Entity {
    protected static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, InterfaceLoader.MODID);
    protected static EntityType<ABuilderEntityBase> E_TYPE;

    /**
     * This flag is true if we need to get server data for syncing.  Set on construction tick, but only used on clients.
     **/
    private boolean needDataFromServer = true;
    /**
     * Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
     * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
     * to do their funky logic.  I'm looking at YOU The One Probe!  This should be either set by NBT loaded from disk
     * on servers, or set by packet on clients.
     */
    public CompoundTag lastLoadedNBT;
    /**
     * Set to true when NBT is loaded on servers from disk, or when NBT arrives from clients on servers.  This is set on the update loop when data is
     * detected from server NBT loading, but for clients this is set when a data packet arrives.  This prevents loading client-based NBT before
     * the packet arrives, which is possible if a partial NBT load is performed by the core game or a mod.
     **/
    public boolean loadFromSavedNBT;
    /**
     * Set to true when loaded NBT is parsed and loaded.  This is done to prevent re-parsing of NBT from triggering a second load command.
     * Note that if this entity is being spawned manually rather than loaded from disk, this should be set prior to ticking.
     **/
    public boolean loadedFromSavedNBT;
    /**
     * Players requesting data for this builder.  This is populated by packets sent to the server.  Each tick players in this list are
     * sent data about this builder, and the list cleared.  Done this way to prevent the server from trying to handle the packet before
     * it has created the entity, as the entity is created on the update call, but the packet might get here due to construction.
     **/
    public final List<IWrapperPlayer> playersRequestingData = new ArrayList<>();
    /**
     * An idle tick counter.  This is set to 0 each time the update method is called, but is incremented each game tick.
     * This allows us to track how long this entity has been idle, and do logic if it's been idle too long.
     **/
    public int idleTickCounter;

    public ABuilderEntityBase(Level level) {
        super(E_TYPE, level);
    }

    public ABuilderEntityBase(EntityType<?> etype, Level level) {
        super(etype, level);
    }

    @Override
    public void tick() {
        //Don't call the super, because some mods muck with our logic here.
        //Said mods are Sponge plugins, but I'm sure there are others.
        //super.tick();
        idleTickCounter = 0;
        baseTick();
    }

    @Override
    public void baseTick() {
        //Don't call the super, because some mods muck with our logic here.
        //Said mods are Sponge plugins, but I'm sure there are others.
        //super.baseTick();

        if (level().isClientSide) {
            //No data.  Wait for NBT to be loaded.
            //As we are on a client we need to send a packet to the server to request NBT data.
            ///Although we could call this in the constructor, some mods will create random
            //entities on the client.  By waiting for an update, we will know we're valid.
            //I'm looking at YOU: The One Probe!
            if (needDataFromServer) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityCSHandshakeClient(InterfaceManager.clientInterface.getClientPlayer(), this));
                needDataFromServer = false;
            }
        } else if (loadedFromSavedNBT) {
            //Send any packets to clients that requested them.
            if (!playersRequestingData.isEmpty()) {
                for (IWrapperPlayer player : playersRequestingData) {
                    IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
                    saveWithoutId(((WrapperNBT) data).tag);
                    player.sendPacket(new PacketEntityCSHandshakeServer(this, data));
                }
                playersRequestingData.clear();
            }
        }

        //If we are on the server, set the NBT flag.
        if (!loadedFromSavedNBT && lastLoadedNBT != null && !level().isClientSide) {
            loadFromSavedNBT = true;
        }
    }

    @Override
    public void absMoveTo(double pX, double pY, double pZ, float pYRot, float pXRot) {
        //Overridden due to stupid tracker behavior.
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        //Don't render entities, this gets done by the overrider.
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        //Save the NBT for loading in the next update call.
        lastLoadedNBT = tag;
    }

    @Override
    public CompoundTag saveWithoutId(CompoundTag tag) {
        super.saveWithoutId(tag);
        //Need to have this here as some mods will load us from NBT and then save us back
        //without ticking.  This causes data loss if we don't merge the last loaded NBT tag.
        //If we did tick, then the last loaded will be null and this doesn't apply.
        if (lastLoadedNBT != null) {
            tag.merge(lastLoadedNBT);
        }
        return tag;
    }

    //Junk methods.
    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
