package mekanism.common.multipart;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import mcmultipart.microblock.BlockMicroMaterial;

public class PlasticMicroMaterial extends BlockMicroMaterial
{
	public PlasticMicroMaterial(IBlockState state, int hardness)
	{
		super(state, hardness);
	}

/*
	@Override
	public int getColour(int pass)
	{
		return block().getRenderColor(meta()) << 8 | 0xFF;
	}
*/
}
