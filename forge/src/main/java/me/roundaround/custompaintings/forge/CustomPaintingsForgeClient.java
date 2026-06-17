package me.roundaround.custompaintings.forge;

import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.custompaintings.client.option.KeyMappings;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge wiring, invoked from the @Mod ctor only behind a dist.isClient() gate. */
public final class CustomPaintingsForgeClient {
  private CustomPaintingsForgeClient() {
  }

  public static void init(FMLJavaModLoadingContext context) {
    EntityJoinLevelEvent.BUS.addListener(event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel().isClientSide()) {
        ClientPaintingManager.onEntityLoad(painting);
      }
    });
    EntityLeaveLevelEvent.BUS.addListener(event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel().isClientSide()) {
        ClientPaintingManager.onEntityUnload(painting);
      }
    });

    ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> ClientPaintingManager.init());
    ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
      ClientPaintingRegistry.getInstance().clear();
      ClientPaintingManager.getInstance().clear();
    });

    FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> {
      KeyMappings.register();
      CacheManager.runBackgroundClean();
      ItemManager.runBackgroundClean();
    });

    context.getContainer().registerExtensionPoint(
        ConfigScreenHandler.ConfigScreenFactory.class,
        () -> new ConfigScreenHandler.ConfigScreenFactory(
            (mc, parent) -> new MainMenuScreen(parent)));
  }
}
