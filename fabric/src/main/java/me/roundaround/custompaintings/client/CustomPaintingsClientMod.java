package me.roundaround.custompaintings.client;

import me.roundaround.allay.api.Entrypoint;
import me.roundaround.custompaintings.client.option.KeyMappings;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

@Entrypoint(Entrypoint.CLIENT)
public class CustomPaintingsClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // Client S2C receivers are registered loader-agnostically through Networking.register()
    // (TroveNetworking dispatches them on the client side); only the client-only key bindings,
    // background cache cleanup, and client lifecycle/entity hooks live here.
    KeyMappings.register();

    CacheManager.runBackgroundClean();
    ItemManager.runBackgroundClean();

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      ClientPaintingManager.init();
    });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      ClientPaintingRegistry.getInstance().clear();
      ClientPaintingManager.getInstance().clear();
    });

    ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> ClientPaintingManager.onEntityLoad(entity));
    ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> ClientPaintingManager.onEntityUnload(entity));
  }
}
