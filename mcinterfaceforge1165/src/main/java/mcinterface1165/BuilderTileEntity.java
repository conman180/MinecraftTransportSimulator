package mcinterface1165;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Builder for the MC Tile Entity class   This class interfaces with all the MC-specific
 * code, and is constructed on the server automatically by MC.  After construction, a tile entity
 * class that extends {@link ATileEntityBase} should be assigned to it.  This is either
 * done manually on the first placement, or automatically via loading from NBT.
 * <br><br>
 * Of course, one might ask, "why not just construct the TE class when we construct this one?".
 * That's a good point, but MC doesn't work like that.  MC waits to assign the world and position
 * to TEs, so if we construct our TE right away, we'll end up with TONs of null checks.  To avoid this,
 * we only construct our TE after the world and position get assigned, and if we have NBT
 * At that point, we make the TE if we're on the server.  If we're on the client, we always way
 * for NBT, as we need to sync with the server's data.
 *
 * @author don_bruce
 */
public class BuilderTileEntity extends TileEntity implements ITickableTileEntity {
    protected static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, InterfaceLoader.MODID);
    protected static RegistryObject<TileEntityType<BuilderTileEntity>> TE_TYPE;
    
    protected ATileEntityBase<?> tileEntity;

    /**
     * Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
     * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
     * to do their funky logic.  I'm looking at YOU The One Probe!  This should be either set by NBT loaded from disk
     * on servers, or set by packet on clients.
     */
    protected CompoundNBT lastLoadedNBT;
    /**
     * Set to true when NBT is loaded on servers from disk, or when NBT arrives from clients on servers.  This is set on the update loop when data is
     * detected from server NBT loading, but for clients this is set when a data packet arrives.  This prevents loading client-based NBT before
     * the packet arrives, which is possible if a partial NBT load is performed by the core game or a mod.
     **/
    protected boolean loadFromSavedNBT;
    /**
     * Set to true when loaded NBT is parsed and loaded.  This is done to prevent re-parsing of NBT from triggering a second load command.
     **/
    protected boolean loadedFromSavedNBT;

    public BuilderTileEntity() {
        this(TE_TYPE.get());
        //Blank constructor for MC.
    }

    public BuilderTileEntity(TileEntityType<?> teType) {
        super(teType);
        //Override type constructor.
    }

    @Override
    public void tick() {
        //World and pos might be null on first few scans.
        if (level != null && worldPosition != null) {
            //If we are on the server, and need to make our TE, do it now.
            //Hold off on loading until blocks load: this can take longer than 1 update if the server/client is laggy.
            if (!level.isClientSide && !loadedFromSavedNBT && lastLoadedNBT != null && level.isLoaded(worldPosition)) {
                try {
                    //Get the block that makes this TE and restore it from saved state.
                    WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(level);
                    Point3D position = new Point3D(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                    ABlockBaseTileEntity block = (ABlockBaseTileEntity) worldWrapper.getBlock(position);
                    setTileEntity(block.createTileEntity(worldWrapper, position, null, new WrapperNBT(lastLoadedNBT)));
                    tileEntity.world.addEntity(tileEntity);
                    loadedFromSavedNBT = true;
                    lastLoadedNBT = null;
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Failed to load tile entity on builder from saved NBT.  Did a pack change?");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    level.removeBlock(worldPosition, false);
                }
            }
        }
    }

    /**
     * Called to set the tileEntity on this builder, allows for sub-classes to do logic too.
     **/
    protected void setTileEntity(ATileEntityBase<?> tile) {
        this.tileEntity = tile;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        //Invalidate happens when we break the block this TE is on.
        if (tileEntity != null) {
            tileEntity.remove();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        //Catch unloaded TEs from when the chunk goes away and kill them.
        //MC forgets to do this normally.
        if (tileEntity != null && tileEntity.isValid) {
            setRemoved();
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        //Don't directly load the TE here.  This causes issues because Minecraft loads TEs before blocks.
        //This is horridly stupid, because then you can't get the block for the TE, but whatever, Mojang be Mojang.
        lastLoadedNBT = tag;
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        if (tileEntity != null) {
            tileEntity.save(new WrapperNBT(tag));
        } else if (lastLoadedNBT != null) {
            //Need to have this here as some mods will load us from NBT and then save us back
            //without ticking.  This causes data loss if we don't merge the last loaded NBT tag.
            //If we did tick, then the last loaded will be null and this doesn't apply.
            tag = lastLoadedNBT;
        }
        return tag;
    }
}