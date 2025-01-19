package mcinterface1201;

import javax.annotation.Nonnull;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Builder for tile entities that contain fluids.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityFluidTank extends BuilderTileEntity implements IFluidTank, IFluidHandler {
    protected static RegistryObject<BlockEntityType<BuilderTileEntityFluidTank>> TE_TYPE2;

    private EntityFluidTank tank;

    public BuilderTileEntityFluidTank(BlockPos pos, BlockState state) {
        super(TE_TYPE2.get(), pos, state);
    }

    @Override
    protected void setTileEntity(ATileEntityBase<?> tile) {
        super.setTileEntity(tile);
        this.tank = ((ITileEntityFluidTankProvider) tile).getTank();
    }

    @Override
    public void tick() {
        super.tick();
        if (tank != null) {
            if (tileEntity instanceof TileEntityFluidLoader && ((TileEntityFluidLoader) tileEntity).isUnloader()) {
                int currentFluidAmount = getFluidAmount();
                if (currentFluidAmount > 0) {
                    //Pump out fluid to handler below, if we have one.
                    BlockEntity teBelow = level.getBlockEntity(getBlockPos().below());
                    if (teBelow != null) {
                        IFluidHandler fluidHandler = teBelow.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
                        if (fluidHandler != null) {
                            int amountDrained = fluidHandler.fill(getFluid(), FluidAction.EXECUTE);
                            if (amountDrained > 0 && currentFluidAmount == getFluidAmount()) {
                                //Need to drain from our tank as the system didn't do this.
                                drain(amountDrained, FluidAction.EXECUTE);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public FluidStack getFluid() {
        if (tank != null && !tank.getFluid().isEmpty()) {
            //Need to find the mod that registered this fluid, Forge is stupid and has them per-mod vs just all with a single name.
            for (ResourceLocation fluidKey : ForgeRegistries.FLUIDS.getKeys()) {
                if (fluidKey.getPath().equals(tank.getFluid())) {
                    return new FluidStack(ForgeRegistries.FLUIDS.getValue(fluidKey), (int) tank.getFluidLevel());
                }
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getFluidAmount() {
        return (int) (tank != null ? tank.getFluidLevel() : 0);
    }

    @Override
    public int getCapacity() {
        return tank != null ? tank.getMaxLevel() : 0;
    }

    @Override
    public boolean isFluidValid(FluidStack fluid) {
        return true;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack stack, FluidAction doFill) {
        if (tank != null) {
            ResourceLocation fluidLocation = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
            return (int) tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), stack.getAmount(), doFill == FluidAction.EXECUTE);
        } else {
            return 0;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction doDrain) {
        if (getFluidAmount() > 0) {
            return this.drain(new FluidStack(getFluid().getFluid(), maxDrain), doDrain);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction doDrain) {
        ResourceLocation fluidLocation = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return new FluidStack(stack.getFluid(), (int) (tank != null ? tank.drain(fluidLocation.getPath(), fluidLocation.getNamespace(), stack.getAmount(), doDrain == FluidAction.EXECUTE) : 0));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && facing == Direction.DOWN) {
            return LazyOptional.of(() -> (T) this);
        } else {
            return super.getCapability(capability, facing);
        }
    }
}
