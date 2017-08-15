package eladkay.scanner.terrain;

import eladkay.scanner.ScannerMod;
import eladkay.scanner.misc.IsolatedBlock;
import eladkay.scanner.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class TileDimensionalRiftRenderer extends TileEntitySpecialRenderer<BlockDimensionalRift.TileDimensionalRift> {

	private IBakedModel modelFrom, modelTo, modelBlack;

	public TileDimensionalRiftRenderer() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void reload(ClientProxy.ResourceReloadEvent event) {
		modelFrom = null;
		modelTo = null;
		modelBlack = null;
	}

	private void getBakedModels() {
		IModel model = null;
		if (modelFrom == null) {
			try {
				model = ModelLoaderRegistry.getModel(new ResourceLocation(ScannerMod.MODID, "block/dimensionalRift"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			modelFrom = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM,
					location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString()));
		}
		if (modelTo == null) {
			try {
				model = ModelLoaderRegistry.getModel(new ResourceLocation(ScannerMod.MODID, "block/dimensionalCore_" + (getWorld().provider.getDimension() == 0 ? "overworld" : getWorld().provider.getDimension() == 1 ? "end" : "nether")));
			} catch (Exception e) {
				e.printStackTrace();
			}
			modelTo = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM,
					location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString()));
		}
		if (modelBlack == null) {
			try {
				model = ModelLoaderRegistry.getModel(new ResourceLocation(ScannerMod.MODID, "block/black"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			modelBlack = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM,
					location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString()));
		}
	}

	@Override
	public void renderTileEntityAt(BlockDimensionalRift.TileDimensionalRift te, double x, double y, double z, float partialTicks, int destroyStage) {
		super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage);

		GlStateManager.pushMatrix();
		GlStateManager.enableCull();
		GlStateManager.disableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableRescaleNormal();
		GlStateManager.color(1, 1, 1);
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		if (Minecraft.isAmbientOcclusionEnabled()) GlStateManager.shadeModel(GL11.GL_SMOOTH);
		else GlStateManager.shadeModel(GL11.GL_FLAT);

		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		getBakedModels();

		GlStateManager.translate(x, y, z);

		float s = (float) te.ticks / (float) BlockDimensionalRift.TileDimensionalRift.TICKS_TO_COMPLETION;

		Minecraft mc = Minecraft.getMinecraft();
		Tessellator tes = Tessellator.getInstance();
		VertexBuffer buffer = tes.getBuffer();
		BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();

		IsolatedBlock block = new IsolatedBlock(te.getWorld().getBlockState(te.getPos()), null);
		{
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			dispatcher.getBlockModelRenderer().renderModel(block, modelBlack, te.getWorld().getBlockState(te.getPos()), IsolatedBlock.POS, buffer, false, MathHelper.getPositionRandom(IsolatedBlock.POS));
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			tes.draw();
		}
		{
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			dispatcher.getBlockModelRenderer().renderModel(block, modelTo, te.getWorld().getBlockState(te.getPos()), IsolatedBlock.POS, buffer, false, MathHelper.getPositionRandom(IsolatedBlock.POS));
			for (int i = 0; i < buffer.getVertexCount(); i++) {
				int idx = buffer.getColorIndex(i + 1);
				buffer.putColorRGBA(idx, 255, 255, 255, (int) (s * 255));
			}
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			tes.draw();
		}
		{
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			dispatcher.getBlockModelRenderer().renderModel(block, modelFrom, te.getWorld().getBlockState(te.getPos()), IsolatedBlock.POS, buffer, false, MathHelper.getPositionRandom(IsolatedBlock.POS));
			for (int i = 0; i < buffer.getVertexCount(); i++) {
				int idx = buffer.getColorIndex(i + 1);
				buffer.putColorRGBA(idx, 255, 255, 255, (int) ((1 - s) * 255));
			}
			mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			tes.draw();
		}
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}
}
