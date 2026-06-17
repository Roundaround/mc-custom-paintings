package me.roundaround.custompaintings.neoforge;

import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.custompaintings.client.option.KeyMappings;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Client-only NeoForge wiring, invoked from the @Mod ctor only behind a Dist.CLIENT gate. */
public final class CustomPaintingsNeoForgeClient {
  private CustomPaintingsNeoForgeClient() {
  }

  public static void init(IEventBus modBus, ModContainer container) {
    NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel().isClientSide()) {
        ClientPaintingManager.onEntityLoad(painting);
      }
    });
    NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel().isClientSide()) {
        ClientPaintingManager.onEntityUnload(painting);
      }
    });

    NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> {
      ClientPaintingManager.init();
    });
    NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
      ClientPaintingRegistry.getInstance().clear();
      ClientPaintingManager.getInstance().clear();
    });

    modBus.addListener(FMLClientSetupEvent.class, event -> {
      KeyMappings.register();
      CacheManager.runBackgroundClean();
      ItemManager.runBackgroundClean();
    });

    container.registerExtensionPoint(IConfigScreenFactory.class,
        (modContainer, parent) -> new MainMenuScreen(parent));
  }
}
