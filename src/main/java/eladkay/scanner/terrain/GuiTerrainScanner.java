package eladkay.scanner.terrain;

import eladkay.scanner.Config;
import eladkay.scanner.init.ModBlocks;
import eladkay.scanner.misc.MessageUpdateEnergyServer;
import eladkay.scanner.misc.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Things this should do:
 * Rotate chunk (i hate my life)
 * Map (check and i'm really proud of myself tbh)
 * Speedup (check!)
 * Stop/start (check!)
 */
// todo Fix gui size in relation to JEI
public class GuiTerrainScanner extends GuiContainer {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("scanner:textures/gui/standard_background.png");
    private final TileEntityTerrainScanner scanner;
    private GuiButton rotate; // todo
    private GuiButton toggleMode;
    private GuiSlider sliderSpeedup;
    private GuiButton showMap;

    public GuiTerrainScanner(TileEntityTerrainScanner scanner) {
        super(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        });
        this.scanner = scanner;
    }

    @Override
    public void initGui() {
        super.initGui();
        toggleMode = new GuiButton(0, (this.width / 2) - 75, this.height / 2 + 40, 150, 20, "");
        rotate = new GuiButton(1, (this.width / 2) - 75, this.height / 2, 150, 20, "");
        //noinspection Convert2Lambda
        sliderSpeedup = new GuiSlider(new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int id, boolean value) {

            }

            @Override
            public void setEntryValue(int id, float value) {
                scanner.speedup = value < 1 ? 1 : (int) value;
                scanner.markDirty();
                MessageUpdateScanner.send(scanner);
            }

            @Override
            public void setEntryValue(int id, String value) {

            }
        }, 2, (this.width / 2) - 75, this.height / 2 + 10, "Speed (Blocks/t)", 1f, Float.parseFloat(Config.maxSpeedup + ""), scanner.speedup, new GuiSlider.FormatHelper() {
            @Override
            public String getText(int id, String name, float value) {
                return name + ": " + (int) value;
            }
        });
        showMap = new GuiButton(3, (this.width / 2) - 75, this.height / 2 - 15, 150, 20, "Show map"); //Build elsewhere (Requires adjacent ultimate biome scanner)
        buttonList.add(toggleMode);
        buttonList.add(showMap);
        //buttonList.add(rotate); todo
        if (Config.maxSpeedup > 0)
            buttonList.add(sliderSpeedup);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == toggleMode) {
            if (scanner.on) scanner.deactivate();
            else scanner.activate();
            NetworkHelper.instance.sendToServer(new MessageUpdateEnergyServer(scanner.getPos().getX(), scanner.getPos().getY(), scanner.getPos().getZ()));
            MessageUpdateScanner.send(scanner);
        } else if (button == rotate) {
            scanner.rotation = scanner.rotation.getNext();
            if (scanner.on) scanner.deactivate();
            scanner.current.setPos(scanner.getPos().getX(), 0, scanner.getPos().getZ());
            MessageUpdateScanner.send(scanner);
        } else if (button == showMap) new GuiBuildRemotely(scanner).openGui();
    }

    private static final ResourceLocation ENERGY_BAR = new ResourceLocation("scanner:textures/gui/bar.png");

    public void drawMultiEnergyBar(int x, int y, int mouseX, int mouseY) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ENERGY_BAR);
        int energyStored = scanner.getEnergyStored(null);
        int maxEnergyStored = scanner.getMaxEnergyStored(null);

        drawTexturedModalRect(x, y, -15, -1, 14, 50);

        int draw = (int) ((double) energyStored / (double) maxEnergyStored * (48));
        drawTexturedModalRect(x + 1, y + 49 - draw, 0, 48 - draw, 12, draw);

        if (isInRect(x + 1, y + 1, 11, 48, mouseX, mouseY)) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);
            GuiUtils.drawGradientRect(0, x + 1, y + 1, x + 13, y + 49, 0x80FFFFFF, 0x80FFFFFF);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableDepth();

            List<String> list = new ArrayList<>();
            list.add(TextFormatting.GOLD + "" + energyStored + "/" + maxEnergyStored + " RF");
            if (isShiftKeyDown()) {
                int percentage = (energyStored / maxEnergyStored) * 100;
                TextFormatting color;
                if (percentage <= 10) {
                    color = TextFormatting.RED;
                } else if (percentage >= 75) {
                    color = TextFormatting.GREEN;
                } else {
                    color = TextFormatting.YELLOW;
                }
                list.add(color + "" + percentage + "%" + TextFormatting.GRAY + " Charged");
            }
            GuiUtils.drawHoveringText(list, mouseX, mouseY, width, height, -1, mc.fontRenderer);
            GlStateManager.disableLighting();
        }
    }

    private static boolean isInRect(int x, int y, int xSize, int ySize, int mouseX, int mouseY) {
        return ((mouseX >= x && mouseX <= x + xSize) && (mouseY >= y && mouseY <= y + ySize));
    }

    public int getKx() {
        return 0;
        //return mc.displayWidth / 5 + 5;
    }

    public int getKy() {
        return 0;
        // return mc.displayHeight / 5 - 30;
    }


    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (scanner == null || toggleMode == null || showMap == null) return;
        toggleMode.displayString = scanner.on ? "Turn off" : "Turn on";
        boolean flag = false;
        for (EnumFacing facing : EnumFacing.values())
            if (mc.world.getBlockState(scanner.getPos().offset(facing)).getBlock() == ModBlocks.biomeScannerUltimate) {

                flag = true;

            }
        showMap.visible = flag;
        switch (scanner.rotation) {
            case POSX_POSZ:
                rotate.displayString = "Build on +x, +z";
                break;
            case POSX_NEGZ:
                rotate.displayString = "Build on +x, -z";
                break;
            case NEGX_POSZ:
                rotate.displayString = "Build on -x, +z";
                break;
            case NEGX_NEGZ:
                rotate.displayString = "Build on -x, -z";
                break;
        }
        /*
        if (scanner.biomeScanner != null){
                flag = true;
        }
        showMap.visible = flag;
        */
        //drawMultiEnergyBar((this.width / 2) - 112, this.height / 2 - 15, mouseX, mouseY);
        /*drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        drawGuiContainerForegroundLayer(mouseX, mouseY);*/
        super.drawScreen(mouseX, mouseY, partialTicks);
        /*for (GuiButton aButtonList : this.buttonList) {
            aButtonList.drawButton(this.mc, mouseX, mouseY);
        }

        for (GuiLabel aLabelList : this.labelList) {
            aLabelList.drawLabel(this.mc, mouseX, mouseY);
        }*/
    }

    public void drawCenteredString(String text, int x, int y, int color) {
        fontRenderer.drawString(text, x - fontRenderer.getStringWidth(text) / 2, y, color);
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        NetworkHelper.instance.sendToServer(new MessageUpdateEnergyServer(scanner.getPos().getX(), scanner.getPos().getY(), scanner.getPos().getZ()));
        if (mc.isSingleplayer()) drawCenteredString("Terrain Scanner", 90 + getKx(), 6 + getKy(), 4210752); //
        else {
            drawCenteredString("Terrain Scanner", 90 + getKx(), 6 + getKy(), 4210752);
            drawCenteredString("(Warning: May appear broken with", 90 + getKx(), 13 + getKy(), 4210752);
            drawCenteredString("EnderIO conduits and capacitor banks)", 90 + getKx(), 20 + getKy(), 4210752);
        }
        //this.fontRendererObj.drawString("Terrain Scanner", 45, 6, 4210752);
        if (!"(0, -1, 0)".equals("(" + scanner.current.getX() + ", " + scanner.current.getY() + ", " + scanner.current.getZ() + ")"))
            drawCenteredString("Current: (" + scanner.current.getX() + ", " + scanner.current.getY() + ", " + scanner.current.getZ() + ")", 90 + getKx(), 35 + getKy(), 4210752);
        else drawCenteredString("Current block: Off", 90 + getKx(), 35 + getKy(), 4210752);
        //this.fontRendererObj.drawString("Current: (" + scanner.current.getX() + ", " + scanner.current.getY() + ", " + scanner.current.getZ() + ")", 40, 20, 4210752);
        drawCenteredString("End block: (" + scanner.getEnd().getX() + ", " + scanner.maxY + ", " + scanner.getEnd().getZ() + ")", 90 + getKx(), 45 + getKy(), 4210752);
        //this.fontRendererObj.drawString("End: (" + scanner.getEnd().getX() + ", 256, " + scanner.getEnd().getZ() + ")", 40, 35, 4210752);
        if (scanner.posStart != null)
            drawCenteredString("Remote start: (" + scanner.posStart.getX() + ", " + scanner.posStart.getZ() + ")", 90 + getKx(), 55 + getKy(), 4210752);
        boolean flag = false;
        for (EnumFacing facing : EnumFacing.values())
            if (mc.world.getBlockState(scanner.getPos().offset(facing)).getBlock() == ModBlocks.biomeScannerUltimate)
                flag = true;
        if (!flag) {
            this.fontRenderer.drawString("Place ultimate biome scanner", 15 + getKx(), 65 + getKy(), 4210752);
            this.fontRenderer.drawString("adjacent to show map", 30 + getKx(), 75 + getKy(), 4210752);
        }

        /*
        if (scanner.biomeScanner == null){
                this.fontRenderer.drawString("Place ultimate biome scanner", 15 + getKx(), 65 + getKy(), 4210752);
                this.fontRenderer.drawString("adjacent to show map", 30 + getKx(), 75 + getKy(), 4210752);
        }
        */

        if (scanner.queue != null)
            this.fontRenderer.drawString("Scanner Queue attached", 30 + getKx(), 150 + getKy(), 4210752);
       /* boolean flag0 = false;
        if (mc.player.getName().matches("(?:Player\\d{1,3})|(?:Eladk[ae]y)") && flag0)
            this.fontRendererObj.drawString("Debug: (" + scanner.getPos().east().add(15, 255, 15).getX() + ", " + scanner.getPos().east().getY() + ", " + scanner.getPos().east().getZ() + ")", 20, 60, 4210752);*/
        /*if (Config.maxSpeedup > 0)
            this.fontRendererObj.drawString("Speedup (blocks per tick): " + scanner.speedup, 20, 120, 4210752);*/
    }

    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        //GlStateManager.scale(2,2,1);
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        //drawScaledCustomSizeModalRect(i, j, 6, 0, 159, 110, xSize, ySize, xSize, ySize);
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        /*GlStateManager.pushMatrix();
        GlStateManager.translate(i, j, 0);
        GlStateManager.scale(2,2,1);
        this.drawTexturedModalRect(-i, -j,  6, 0, 159, 110);
        GlStateManager.popMatrix();*/


    }
}