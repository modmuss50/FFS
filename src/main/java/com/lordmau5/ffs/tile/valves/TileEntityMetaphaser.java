package com.lordmau5.ffs.tile.valves;

/**
 * Created by Dustin on 07.02.2016.
 */

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.compat.Capabilities;
import com.lordmau5.ffs.compat.Compatibility;
import com.lordmau5.ffs.compat.energy.eu.MetaphaserEU;
import com.lordmau5.ffs.compat.energy.forgeEnergy.MetaphaserForgeEnergy;
import com.lordmau5.ffs.compat.energy.rf.MetaphaserRF;
import com.lordmau5.ffs.compat.energy.tesla.MetaphaserTesla;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Optional;

/**
 * - Slave-valve to extract the fluid
 * - Bring to other tank (?)
 * - 2 energy valves possible.
 * <p>
 * - 1:1 ratio
 */
@Optional.InterfaceList(value = {
		@Optional.Interface(iface = "cofh.api.energy.IEnergyProvider", modid = "CoFHAPI|energy"),
		@Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHAPI|energy"),

		@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2API"),
		@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "IC2API")
})
public class TileEntityMetaphaser extends AbstractTankValve implements
		IEnergyReceiver, IEnergyProvider, // CoFH
		IEnergySink, IEnergySource // IC2
{

	public double ic2Overflow = 0.0d;
	public boolean addedToEnet = false;
	private MetaphaserTesla teslaContainer;
	private final MetaphaserForgeEnergy forgeEnergyContainer;

	public TileEntityMetaphaser() {
		super();

		if(Compatibility.INSTANCE.isTeslaLoaded) {
			teslaContainer = new MetaphaserTesla(this);
		}

		forgeEnergyContainer = new MetaphaserForgeEnergy(this);
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(Compatibility.INSTANCE.isIC2Loaded) {
			MetaphaserEU.INSTANCE.unload(this);
		}
	}

	@Override
	public void update() {
		super.update();

		if(Compatibility.INSTANCE.isIC2Loaded && !addedToEnet)
			MetaphaserEU.INSTANCE.load(this);

		if(Compatibility.INSTANCE.isTeslaLoaded) {
			teslaContainer.outputToTile();
		}
		if(Compatibility.INSTANCE.isCoFHLoaded) {
			MetaphaserRF.INSTANCE.outputToTile(this);
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityEnergy.ENERGY || capability == Capabilities.Tesla.RECEIVER || capability == Capabilities.Tesla.PROVIDER || capability == Capabilities.Tesla.HOLDER || super.hasCapability(capability, facing);

	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityEnergy.ENERGY) {
			return (T) forgeEnergyContainer;
		}

		if(capability == Capabilities.Tesla.RECEIVER || capability == Capabilities.Tesla.PROVIDER || capability == Capabilities.Tesla.HOLDER) {
			return (T) teslaContainer;
		}

		return super.getCapability(capability, facing);
	}

	/**
	 * This method is being used to check if the Metaphaser can extract energy in the first place
	 */
	public boolean containsMetaphasedFlux() {
		return getTankConfig().getFluidStack() != null && getTankConfig().getFluidStack().isFluidEqual(new FluidStack(FancyFluidStorage.fluidMetaphasedFlux, 1000));
	}

	/**
	 * -------------------------------------------------------------------------
	 * Start of the CoFH Energy implementation, also known as Redstone Flux (RF)
	 * -------------------------------------------------------------------------
	 */

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int extractEnergy(EnumFacing facing, int maxExtract, boolean simulate) {
		return MetaphaserRF.INSTANCE.extractEnergy(this, facing, maxExtract, simulate);
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int receiveEnergy(EnumFacing facing, int maxReceive, boolean simulate) {
		return MetaphaserRF.INSTANCE.receiveEnergy(this, facing, maxReceive, simulate);
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int getEnergyStored(EnumFacing facing) {
		return MetaphaserRF.INSTANCE.convertForOutput(getTankConfig().getFluidAmount());
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int getMaxEnergyStored(EnumFacing facing) {
		return MetaphaserRF.INSTANCE.getMaxEnergyStored(this, facing);
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public boolean canConnectEnergy(EnumFacing facing) {
		return MetaphaserRF.INSTANCE.canConnectEnergy(this, facing);
	}

	/**
	 * -----------------------------------------------------------------------
	 * Start of the IC2 Energy implementation, also known as Energy Units (EU)
	 * -----------------------------------------------------------------------
	 */

	@Optional.Method(modid = "IC2API")
	@Override
	public double getDemandedEnergy() {
		return MetaphaserEU.INSTANCE.getDemandedEnergy(this);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public int getSinkTier() {
		return MetaphaserEU.INSTANCE.getSinkTier(this);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
		return MetaphaserEU.INSTANCE.injectEnergy(this, directionFrom, amount, voltage);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) {
		return MetaphaserEU.INSTANCE.acceptsEnergyFrom(this, emitter, side);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public double getOfferedEnergy() {
		return MetaphaserEU.INSTANCE.getOfferedEnergy(this);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public void drawEnergy(double amount) {
		MetaphaserEU.INSTANCE.drawEnergy(this, amount);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public int getSourceTier() {
		return MetaphaserEU.INSTANCE.getSourceTier(this);
	}

	@Optional.Method(modid = "IC2API")
	@Override
	public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
		return MetaphaserEU.INSTANCE.emitsEnergyTo(this, receiver, side);
	}
}
