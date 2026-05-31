package me.roundaround.custompaintings.fabric;

import me.roundaround.allay.api.Entrypoint;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;

@Entrypoint(Entrypoint.MAIN)
public final class CustomPaintingsFabricMod implements ModInitializer {
  @Override
  public void onInitialize() {
    CustomPaintingsMod.bootstrapCommon();

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      CustomPaintingsCommand.register(dispatcher);
    });

    ServerLevelEvents.LOAD.register(CustomPaintingsMod::onServerLevelLoad);

    ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (entity instanceof Painting painting) {
        CustomPaintingsMod.onPaintingLoad(world, painting);
      }
    });

    ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
      if (entity instanceof Painting painting) {
        CustomPaintingsMod.onPaintingUnload(world, painting);
      }
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      CustomPaintingsMod.onPlayerJoin(handler.getPlayer());
    });

    ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
      CustomPaintingsMod.onPlayerChangeLevel(player, destination);
    });

    UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
      if (!(entity instanceof Painting painting)) {
        return InteractionResult.PASS;
      }
      return CustomPaintingsMod.onUsePainting(player, painting)
          ? InteractionResult.SUCCESS
          : InteractionResult.PASS;
    });

    ServerLifecycleEvents.SERVER_STARTED.register(CustomPaintingsMod::onServerStarted);
    ServerLifecycleEvents.SERVER_STOPPING.register((server) -> CustomPaintingsMod.onServerStopping());

    // ServerInfo persistence + registry teardown.
    ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> ServerInfo.onBeforeSave());
    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
      ServerInfo.onServerStopped();
      ServerPaintingRegistry.onServerStopped(server);
    });
  }
}
