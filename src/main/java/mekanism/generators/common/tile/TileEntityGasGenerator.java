package mekanism.generators.common.tile;

import io.netty.buffer.ByteBuf;
import mekanism.api.MekanismConfig.general;
import mekanism.api.gas.*;
import mekanism.common.FuelHandler;
import mekanism.common.FuelHandler.FuelGas;
import mekanism.common.base.ISustainedData;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;

public class TileEntityGasGenerator extends TileEntityGenerator implements IGasHandler, ITubeConnection, ISustainedData
{
	/** The maximum amount of gas this block can store. */
	public int MAX_GAS = 18000;

	/** The tank this block is storing fuel in. */
	public GasTank fuelTank;

	public int burnTicks = 0;
	public double generationRate = 0;

	public TileEntityGasGenerator()
	{
		super("gas", "GasGenerator", general.FROM_H2*100, general.FROM_H2*2);
		inventory = new ItemStack[2];
		fuelTank = new GasTank(MAX_GAS);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(!worldObj.isRemote)
		{
			ChargeUtils.charge(1, this);

			if(inventory[0] != null && fuelTank.getStored() < MAX_GAS)
			{
				Gas gasType = null;
				
				if(fuelTank.getGas() != null)
				{
					gasType = fuelTank.getGas().getGas();
				}
				else if(inventory[0] != null && inventory[0].getItem() instanceof IGasItem)
				{
					if(((IGasItem)inventory[0].getItem()).getGas(inventory[0]) != null)
					{
						gasType = ((IGasItem)inventory[0].getItem()).getGas(inventory[0]).getGas();
					}
				}
				
				if(gasType != null && FuelHandler.getFuel(gasType) != null)
				{
					GasStack removed = GasTransmission.removeGas(inventory[0], gasType, fuelTank.getNeeded());
					boolean isTankEmpty = (fuelTank.getGas() == null);
					
					int fuelReceived = fuelTank.receive(removed, true);
					
					if(fuelReceived > 0 && isTankEmpty)
					{
						output = FuelHandler.getFuel(fuelTank.getGas().getGas()).energyPerTick * 2;
					}
				}
			}

			if(canOperate())
			{
				setActive(true);

				if(burnTicks > 0)
				{
					burnTicks--;
					setEnergy(electricityStored + generationRate);
				}
				else if(fuelTank.getStored() > 0)
				{
					FuelGas fuel = FuelHandler.getFuel(fuelTank.getGas().getGas());
					
					if(fuel != null)
					{
						burnTicks = fuel.burnTicks - 1;
						generationRate = fuel.energyPerTick;
						fuelTank.draw(1, true);
						setEnergy(getEnergy() + generationRate);
					}
				}
				else {
					burnTicks = 0;
					generationRate = 0;
				}
			}
			else {
				setActive(false);
			}
		}
	}

	@Override
	public boolean canExtractItem(int slotID, ItemStack itemstack, int side)
	{
		if(slotID == 1)
		{
			return ChargeUtils.canBeOutputted(itemstack, true);
		}
		else if(slotID == 0)
		{
			return (itemstack.getItem() instanceof IGasItem && ((IGasItem)itemstack.getItem()).getGas(itemstack) == null);
		}

		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemstack)
	{
		if(slotID == 0)
		{
			return itemstack.getItem() instanceof IGasItem && ((IGasItem)itemstack.getItem()).getGas(itemstack) != null &&
					FuelHandler.getFuel((((IGasItem)itemstack.getItem()).getGas(itemstack).getGas())) != null;
		}
		else if(slotID == 1)
		{
			return ChargeUtils.canBeCharged(itemstack);
		}

		return true;
	}

	@Override
	public int[] getSlotsForFace(int side)
	{
		return EnumFacing.getFront(side) == MekanismUtils.getRight(facing) ? new int[] {1} : new int[] {0};
	}

	@Override
	public boolean canOperate()
	{
		return getEnergy() < getMaxEnergy() && fuelTank.getStored() > 0 && MekanismUtils.canFunction(this);
	}

	/**
	 * Gets the scaled gas level for the GUI.
	 * @param i - multiplier
	 * @return
	 */
	public int getScaledGasLevel(int i)
	{
		return fuelTank.getStored()*i / MAX_GAS;
	}

    private static final String[] methods = new String[] {"getStored", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getGas", "getGasNeeded"};

	@Override
	public String[] getMethods()
	{
		return methods;
	}

	@Override
	public Object[] invoke(int method, Object[] arguments) throws Exception
	{
		switch(method)
		{
			case 0:
				return new Object[] {getEnergy()};
			case 1:
				return new Object[] {output};
			case 2:
				return new Object[] {getMaxEnergy()};
			case 3:
				return new Object[] {getMaxEnergy()-getEnergy()};
			case 4:
				return new Object[] {fuelTank.getStored()};
			case 5:
				return new Object[] {fuelTank.getNeeded()};
			default:
				throw new NoSuchMethodException();
		}
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);
		
		if(dataStream.readBoolean())
		{
			fuelTank.setGas(new GasStack(GasRegistry.getGas(dataStream.readInt()), dataStream.readInt()));
		}
		else {
			fuelTank.setGas(null);
		}
		
		generationRate = dataStream.readDouble();
		output = dataStream.readDouble();
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);
		
		if(fuelTank.getGas() != null)
		{
			data.add(true);
			data.add(fuelTank.getGas().getGas().getID());
			data.add(fuelTank.getStored());
		}
		else {
			data.add(false);
		}
		
		data.add(generationRate);
		data.add(output);
		
		return data;
	}

	@Override
	public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer)
	{
		boolean isTankEmpty = (fuelTank.getGas() == null);
		
		if(canReceiveGas(side, stack.getGas()) && (isTankEmpty || fuelTank.getGas().isGasEqual(stack)))
		{
			int fuelReceived = fuelTank.receive(stack, doTransfer);
			
			if(doTransfer && isTankEmpty && fuelReceived > 0) 
			{
				output = FuelHandler.getFuel(fuelTank.getGas().getGas()).energyPerTick*2;
			}
			
			return fuelReceived;
		}

		return 0;
	}

	@Override
	public int receiveGas(EnumFacing side, GasStack stack)
	{
		return receiveGas(side, stack, true);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		fuelTank.read(nbtTags.getCompoundTag("fuelTank"));
		
		boolean isTankEmpty = (fuelTank.getGas() == null);
		FuelGas fuel = (isTankEmpty) ? null : FuelHandler.getFuel(fuelTank.getGas().getGas());
		
		if(fuel != null) 
		{
			output = fuel.energyPerTick * 2;
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
	}

	@Override
	public boolean canReceiveGas(EnumFacing side, Gas type)
	{
		return FuelHandler.getFuel(type) != null && side != EnumFacing.getFront(facing);
	}

	@Override
	public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer)
	{
		return null;
	}

	@Override
	public GasStack drawGas(EnumFacing side, int amount)
	{
		return drawGas(side, amount, true);
	}

	@Override
	public boolean canDrawGas(EnumFacing side, Gas type)
	{
		return false;
	}

	@Override
	public boolean canTubeConnect(EnumFacing side)
	{
		return side != EnumFacing.getFront(facing);
	}

	@Override
	public void writeSustainedData(ItemStack itemStack)
	{
		if(fuelTank != null)
		{
			itemStack.stackTagCompound.setTag("fuelTank", fuelTank.write(new NBTTagCompound()));
		}
	}

	@Override
	public void readSustainedData(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound.hasKey("fuelTank"))
		{
			fuelTank.read(itemStack.stackTagCompound.getCompoundTag("fuelTank"));
			
			boolean isTankEmpty = (fuelTank.getGas() == null);
			//Update energy output based on any existing fuel in tank
			FuelGas fuel = (isTankEmpty) ? null : FuelHandler.getFuel(fuelTank.getGas().getGas());
			
			if(fuel != null) 
			{
				output = fuel.energyPerTick * 2;
			}
		}
	}
}
