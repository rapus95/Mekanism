package mekanism.common.tile;

import mekanism.common.content.boiler.BoilerSteamTank;
import mekanism.common.content.boiler.BoilerTank;
import mekanism.common.content.boiler.BoilerWaterTank;
import mekanism.common.util.PipeUtils;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityThermoelectricValve extends TileEntityThermoelectricBoiler implements IFluidHandler
{
	public BoilerTank waterTank;
	public BoilerTank steamTank;

	public TileEntityThermoelectricValve()
	{
		super("Boiler Valve");
		waterTank = new BoilerWaterTank(this);
		steamTank = new BoilerSteamTank(this);
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing from)
	{
		return ((!worldObj.isRemote && structure != null) || (worldObj.isRemote && clientHasStructure)) ? new FluidTankInfo[] {waterTank.getInfo(), steamTank.getInfo()} : PipeUtils.EMPTY;
	}

	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill)
	{
		return waterTank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain)
	{
		if(structure != null && structure.steamStored != null)
		{
			if(resource.getFluid() == structure.steamStored.getFluid())
			{
				return steamTank.drain(resource.amount, doDrain);
			}
		}

		return null;
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain)
	{
		if(structure != null)
		{
			return steamTank.drain(maxDrain, doDrain);
		}

		return null;
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid)
	{
		return true;
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid)
	{
		return true;
	}
}
