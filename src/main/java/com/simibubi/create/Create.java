package com.simibubi.create;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.simibubi.create.modules.ModuleLoadedCondition;
import com.simibubi.create.modules.contraptions.TorquePropagator;
import com.simibubi.create.modules.contraptions.receivers.constructs.piston.MovingConstructHandler;
import com.simibubi.create.modules.logistics.FrequencyHandler;
import com.simibubi.create.modules.logistics.management.LogisticalNetworkHandler;
import com.simibubi.create.modules.logistics.transport.villager.LogisticianHandler;
import com.simibubi.create.modules.schematics.ServerSchematicLoader;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.particles.ParticleType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Create.ID)
public class Create {

	public static final String ID = "create";
	public static final String NAME = "Create";
	public static final String VERSION = "0.1.1a";

	public static Logger logger = LogManager.getLogger();
	public static ItemGroup creativeTab = new CreateItemGroup();
	public static ServerSchematicLoader schematicReceiver;
	public static FrequencyHandler frequencyHandler;
	public static MovingConstructHandler constructHandler;
	public static LogisticalNetworkHandler logisticalNetworkHandler;
	public static TorquePropagator torquePropagator;
	public static LogisticianHandler logisticianHandler;

	public static ModConfig config;

	public Create() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(Create::init);

		modEventBus.addGenericListener(Block.class, AllBlocks::register);
		modEventBus.addGenericListener(Item.class, AllItems::register);
		modEventBus.addGenericListener(IRecipeSerializer.class, AllRecipes::register);
		modEventBus.addGenericListener(TileEntityType.class, AllTileEntities::register);
		modEventBus.addGenericListener(ContainerType.class, AllContainers::register);
		modEventBus.addGenericListener(VillagerProfession.class, Create::registerVillagerProfessions);
		modEventBus.addGenericListener(PointOfInterestType.class, Create::registerPointsOfInterest);
		modEventBus.addGenericListener(EntityType.class, AllEntities::register);
		modEventBus.addGenericListener(ParticleType.class, AllParticles::register);

		modEventBus.addListener(Create::createConfigs);
		CreateClient.addListeners(modEventBus);

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CreateConfig.specification);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CreateClientConfig.specification);
	}

	public static void init(final FMLCommonSetupEvent event) {
		schematicReceiver = new ServerSchematicLoader();
		frequencyHandler = new FrequencyHandler();
		constructHandler = new MovingConstructHandler();
		logisticalNetworkHandler = new LogisticalNetworkHandler();
		torquePropagator = new TorquePropagator();

		CraftingHelper.register(new ModuleLoadedCondition.Serializer());
		AllPackets.registerPackets();
	}

	public static void registerVillagerProfessions(RegistryEvent.Register<VillagerProfession> event) {
		LogisticianHandler.registerVillagerProfessions(event);
	}

	@SubscribeEvent
	public static void registerPointsOfInterest(RegistryEvent.Register<PointOfInterestType> event) {
		LogisticianHandler.registerPointsOfInterest(event);
	}

	public static void createConfigs(ModConfig.ModConfigEvent event) {
		if (event.getConfig().getSpec() == CreateConfig.specification)
			config = event.getConfig();
	}

	public static void tick() {
		if (schematicReceiver == null)
			schematicReceiver = new ServerSchematicLoader();
		schematicReceiver.tick();
	}

	public static void shutdown() {
		schematicReceiver.shutdown();
	}

}
