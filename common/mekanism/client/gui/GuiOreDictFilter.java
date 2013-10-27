package mekanism.client.gui;

import java.util.ArrayList;
import java.util.List;

import mekanism.api.EnumColor;
import mekanism.api.Object3D;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.inventory.container.ContainerFilter;
import mekanism.common.network.PacketLogisticalSorterGui;
import mekanism.common.network.PacketNewFilter;
import mekanism.common.tileentity.TileEntityLogisticalSorter;
import mekanism.common.transporter.OreDictFilter;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.TransporterUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiOreDictFilter extends GuiMekanism
{
	public TileEntityLogisticalSorter tileEntity;
	
	public boolean isNew = false;
	
	public OreDictFilter filter = new OreDictFilter();
	
	private GuiTextField oreDictText;
	
	public ItemStack renderStack;
	
	public int ticker = 0;
	
	public int stackSwitch = 0;
	
	public int stackIndex = 0;
	
	public List<ItemStack> iterStacks;
	
	public String status = EnumColor.DARK_GREEN + "All OK";
	
	public GuiOreDictFilter(EntityPlayer player, TileEntityLogisticalSorter tentity, int index)
	{
		super(new ContainerFilter(player.inventory));
		tileEntity = tentity;
		
		filter = (OreDictFilter)tentity.filters.get(index);
	}
	
	public GuiOreDictFilter(EntityPlayer player, TileEntityLogisticalSorter tentity)
	{
		super(new ContainerFilter(player.inventory));
		tileEntity = tentity;
		
		isNew = true;
		
		filter.color = TransporterUtils.colors.get(0);
	}
	
	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		
        int guiWidth = (width - xSize) / 2;
        int guiHeight = (height - ySize) / 2;
		
		buttonList.clear();
		buttonList.add(new GuiButton(0, guiWidth + 58, guiHeight + 63, 60, 18, "Save"));
		
		oreDictText = new GuiTextField(fontRenderer, guiWidth + 35, guiHeight + 48, 95, 12);
		oreDictText.setMaxStringLength(12);
		oreDictText.setFocused(true);
	}
	
	@Override
	public void keyTyped(char c, int i)
	{
		if(i == Keyboard.KEY_E)
		{
			oreDictText.textboxKeyTyped(c, i);
			return;
		}
		
		super.keyTyped(c, i);
		
		if(oreDictText.isFocused() && i == Keyboard.KEY_RETURN)
		{
			setOreDictKey();
			return;
		}
		
		if(Character.isLetter(c) || Character.isDigit(c) || c == '*' || i == Keyboard.KEY_BACK || i == Keyboard.KEY_DELETE)
		{
			oreDictText.textboxKeyTyped(c, i);
		}
	}
	
	@Override
	protected void actionPerformed(GuiButton guibutton)
	{
		super.actionPerformed(guibutton);
		
		if(guibutton.id == 0)
		{
			if(filter.oreDictName != null && !filter.oreDictName.isEmpty())
			{
				PacketHandler.sendPacket(Transmission.SERVER, new PacketNewFilter().setParams(Object3D.get(tileEntity), filter));
				PacketHandler.sendPacket(Transmission.SERVER, new PacketLogisticalSorterGui().setParams(Object3D.get(tileEntity), 0));
				mc.thePlayer.openGui(Mekanism.instance, 26, mc.theWorld, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
			}
			else {
				status = EnumColor.DARK_RED + "No key";
				ticker = 20;
			}
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {	
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);
		
		fontRenderer.drawString((isNew ? "New" : "Edit") + " OreDict Filter", 43, 6, 0x404040);
		fontRenderer.drawString("Status: " + status, 35, 20, 0x00CD00);
		fontRenderer.drawString("Key: " + filter.oreDictName, 35, 32, 0x00CD00);
		
		if(renderStack != null)
		{
			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_LIGHTING);
			itemRenderer.renderItemIntoGUI(fontRenderer, mc.getTextureManager(), renderStack, 12, 19);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glPopMatrix();
		}
		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        
        mc.getTextureManager().bindTexture(MekanismRenderer.getColorResource(filter.color));
		itemRenderer.renderIcon(12, 45, MekanismRenderer.getColorIcon(filter.color), 16, 16);
		
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
		
		if(xAxis >= 12 && xAxis <= 28 && yAxis >= 45 && yAxis <= 61)
		{
			drawCreativeTabHoveringText(filter.color.getName(), xAxis, yAxis);
		}
		
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

	@Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
    {
		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
		
		mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiOreDictFilter.png"));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int guiWidth = (width - xSize) / 2;
        int guiHeight = (height - ySize) / 2;
        drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);
        
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);
		
		if(xAxis >= 5 && xAxis <= 16 && yAxis >= 5 && yAxis <= 16)
		{
			drawTexturedModalRect(guiWidth + 5, guiHeight + 5, 176, 0, 11, 11);
		}
		else {
			drawTexturedModalRect(guiWidth + 5, guiHeight + 5, 176, 11, 11, 11);
		}
		
		if(xAxis >= 131 && xAxis <= 143 && yAxis >= 48 && yAxis <= 60)
		{
			drawTexturedModalRect(guiWidth + 131, guiHeight + 48, 176 + 11, 0, 12, 12);
		}
		else {
			drawTexturedModalRect(guiWidth + 131, guiHeight + 48, 176 + 11, 12, 12, 12);
		}
		
        oreDictText.drawTextBox();
    }
	
	@Override
	public void updateScreen()
	{
		super.updateScreen();
		
		oreDictText.updateCursorCounter();
		
		if(ticker > 0)
		{
			ticker--;
		}
		else {
			status = EnumColor.DARK_GREEN + "All OK";
		}
		
		if(stackSwitch > 0)
		{
			stackSwitch--;
		}
		
		if(stackSwitch == 0 && iterStacks != null && iterStacks.size() > 0)
		{
			stackSwitch = 20;
			
			if(stackIndex == -1 || stackIndex == iterStacks.size()-1)
			{
				stackIndex = 0;
			}
			else if(stackIndex < iterStacks.size()-1)
			{
				stackIndex++;
			}
			
			renderStack = iterStacks.get(stackIndex);
		}
		else if(iterStacks != null && iterStacks.size() == 0)
		{
			renderStack = null;
		}
	}
	
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button)
    {
        super.mouseClicked(mouseX, mouseY, button);
        
        oreDictText.mouseClicked(mouseX, mouseY, button);
        
    	if(button == 0)
		{
			int xAxis = (mouseX - (width - xSize) / 2);
			int yAxis = (mouseY - (height - ySize) / 2);
			
			if(xAxis >= 5 && xAxis <= 16 && yAxis >= 5 && yAxis <= 16)
			{
				mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
				PacketHandler.sendPacket(Transmission.SERVER, new PacketLogisticalSorterGui().setParams(Object3D.get(tileEntity), 0));
				mc.thePlayer.openGui(Mekanism.instance, 26, mc.theWorld, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
			}
			
			if(xAxis >= 12 && xAxis <= 28 && yAxis >= 45 && yAxis <= 61)
			{
				filter.color = TransporterUtils.increment(filter.color);
			}
		}
    }
    
    private void setOreDictKey()
    {
    	String oreName = oreDictText.getText();
    	
    	if(oreName == null || oreName.isEmpty())
    	{
    		status = EnumColor.DARK_RED + "No key entered";
    		return;
    	}
    	else if(oreName.equals(filter.oreDictName))
    	{
    		status = EnumColor.DARK_RED + "Same key";
    		return;
    	}
    	
    	if(iterStacks == null)
    	{
    		iterStacks = new ArrayList<ItemStack>();
    	}
    	else {
    		iterStacks.clear();
    	}
    	
    	List<String> keys = new ArrayList<String>();
    	
    	for(String s : OreDictionary.getOreNames())
    	{
    		if(oreName.equals(s))
    		{
    			keys.add(s);
    		}
    		else if(oreName.endsWith("*") && !oreName.startsWith("*"))
    		{
    			if(s.startsWith(oreName.substring(0, oreName.length()-1)))
    			{
    				keys.add(s);
    			}
    		}
    		else if(oreName.startsWith("*") && !oreName.endsWith("*"))
    		{
    			if(s.endsWith(oreName.substring(1)))
    			{
    				keys.add(s);
    			}
    		}
    		else if(oreName.startsWith("*") && oreName.endsWith("*"))
    		{
    			if(s.contains(oreName.substring(1, oreName.length()-1)))
    			{
    				keys.add(s);
    			}
    		}
    	}
    	
    	for(String key : keys)
    	{
    		for(ItemStack stack : OreDictionary.getOres(key))
    		{
    			ItemStack toAdd = stack.copy();
    			
    			if(!iterStacks.contains(stack))
    			{
    				iterStacks.add(stack.copy());
    			}
    		}
    	}
    	
    	filter.oreDictName = oreName;
    	oreDictText.setText("");
    	
    	stackSwitch = 0;
    	stackIndex = -1;
    }
}
