package com.simibubi.create;

import java.util.Map;
import java.util.function.Function;

import com.simibubi.create.foundation.block.CTModel;
import com.simibubi.create.foundation.block.IHaveConnectedTextures;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.SuperByteBufferCache;
import com.simibubi.create.modules.contraptions.WrenchModel;
import com.simibubi.create.modules.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.modules.contraptions.receivers.EncasedFanParticleHandler;
import com.simibubi.create.modules.contraptions.receivers.constructs.ContraptionRenderer;
import com.simibubi.create.modules.curiosities.deforester.DeforesterModel;
import com.simibubi.create.modules.curiosities.partialWindows.WindowInABlockModel;
import com.simibubi.create.modules.curiosities.placementHandgun.BuilderGunModel;
import com.simibubi.create.modules.curiosities.symmetry.client.SymmetryWandModel;
import com.simibubi.create.modules.schematics.ClientSchematicLoader;
import com.simibubi.create.modules.schematics.client.SchematicAndQuillHandler;
import com.simibubi.create.modules.schematics.client.SchematicHandler;
import com.simibubi.create.modules.schematics.client.SchematicHologram;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class CreateClient {

	public static ClientSchematicLoader schematicSender;
	public static SchematicHandler schematicHandler;
	public static SchematicHologram schematicHologram;
	public static SchematicAndQuillHandler schematicAndQuillHandler;
	public static EncasedFanParticleHandler fanParticles;
	public static SuperByteBufferCache bufferCache;
	public static int renderTicks;

	public static ModConfig config;

	public static void addListeners(IEventBus modEventBus) {
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			modEventBus.addListener(CreateClient::clientInit);
			modEventBus.addListener(CreateClient::createConfigs);
			modEventBus.addListener(CreateClient::onModelBake);
			modEventBus.addListener(CreateClient::onModelRegistry);
			modEventBus.addListener(CreateClient::onTextureStitch);
		});
	}

	public static void clientInit(FMLClientSetupEvent event) {
		schematicSender = new ClientSchematicLoader();
		schematicHandler = new SchematicHandler();
		schematicHologram = new SchematicHologram();
		schematicAndQuillHandler = new SchematicAndQuillHandler();
		fanParticles = new EncasedFanParticleHandler();

		bufferCache = new SuperByteBufferCache();
		bufferCache.registerCompartment(KineticTileEntityRenderer.KINETIC_TILE);
		bufferCache.registerCompartment(ContraptionRenderer.CONTRAPTION, 20);

		AllKeys.register();
		AllContainers.registerScreenFactories();
		AllTileEntities.registerRenderers();
		AllItems.registerColorHandlers();
		AllBlocks.registerColorHandlers();
		AllEntities.registerRenderers();

		IResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		if (resourceManager instanceof IReloadableResourceManager)
			((IReloadableResourceManager) resourceManager).addReloadListener(new ResourceReloadHandler());
	}

	public static void createConfigs(ModConfig.ModConfigEvent event) {
		if (event.getConfig().getSpec() == CreateConfig.specification)
			return;

		config = event.getConfig();
	}

	public static void gameTick() {
		schematicSender.tick();
		schematicAndQuillHandler.tick();
		schematicHandler.tick();
		schematicHologram.tick();
	}

	@OnlyIn(Dist.CLIENT)
	public static void onTextureStitch(TextureStitchEvent.Pre event) {
		if (!event.getMap().getBasePath().equals("textures"))
			return;
		for (AllBlocks allBlocks : AllBlocks.values()) {
			if (!(allBlocks.get() instanceof IHaveConnectedTextures))
				continue;
			event.addSprite(new ResourceLocation(Create.ID, "block/connected/" + Lang.asId(allBlocks.name())));
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void onModelBake(ModelBakeEvent event) {
		Map<ResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();

		for (AllBlocks allBlocks : AllBlocks.values()) {
			if (!(allBlocks.get() instanceof IHaveConnectedTextures))
				continue;
			swapModels(modelRegistry, getBlockModelLocation(allBlocks, ""),
					t -> new CTModel(t, Lang.asId(allBlocks.name())));
		}

		swapModels(modelRegistry, getItemModelLocation(AllItems.SYMMETRY_WAND),
				t -> new SymmetryWandModel(t).loadPartials(event));
		swapModels(modelRegistry, getItemModelLocation(AllItems.PLACEMENT_HANDGUN),
				t -> new BuilderGunModel(t).loadPartials(event));
		swapModels(modelRegistry, getItemModelLocation(AllItems.WRENCH), t -> new WrenchModel(t).loadPartials(event));
		swapModels(modelRegistry, getItemModelLocation(AllItems.DEFORESTER),
				t -> new DeforesterModel(t).loadPartials(event));
		swapModels(modelRegistry,
				getBlockModelLocation(AllBlocks.WINDOW_IN_A_BLOCK,
						BlockModelShapes
								.getPropertyMapString(AllBlocks.WINDOW_IN_A_BLOCK.get().getDefaultState().getValues())),
				WindowInABlockModel::new);
		swapModels(modelRegistry,
				getBlockModelLocation(AllBlocks.WINDOW_IN_A_BLOCK,
						BlockModelShapes.getPropertyMapString(AllBlocks.WINDOW_IN_A_BLOCK.get().getDefaultState()
								.with(BlockStateProperties.WATERLOGGED, true).getValues())),
				WindowInABlockModel::new);
	}

	@OnlyIn(Dist.CLIENT)
	public static void onModelRegistry(ModelRegistryEvent event) {
		for (String location : SymmetryWandModel.getCustomModelLocations())
			ModelLoader.addSpecialModel(new ResourceLocation(Create.ID, "item/" + location));
		for (String location : BuilderGunModel.getCustomModelLocations())
			ModelLoader.addSpecialModel(new ResourceLocation(Create.ID, "item/" + location));
		for (String location : WrenchModel.getCustomModelLocations())
			ModelLoader.addSpecialModel(new ResourceLocation(Create.ID, "item/" + location));
		for (String location : DeforesterModel.getCustomModelLocations())
			ModelLoader.addSpecialModel(new ResourceLocation(Create.ID, "item/" + location));
	}

	protected static ModelResourceLocation getItemModelLocation(AllItems item) {
		return new ModelResourceLocation(item.item.getRegistryName(), "inventory");
	}

	protected static ModelResourceLocation getBlockModelLocation(AllBlocks block, String suffix) {
		return new ModelResourceLocation(block.block.getRegistryName(), suffix);
	}

	@OnlyIn(Dist.CLIENT)
	protected static <T extends IBakedModel> void swapModels(Map<ResourceLocation, IBakedModel> modelRegistry,
			ModelResourceLocation location, Function<IBakedModel, T> factory) {
		modelRegistry.put(location, factory.apply(modelRegistry.get(location)));
	}

}