package me.roundaround.custompaintings.neoforge;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.trove.neoforge.TroveNeoForge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("custompaintings")
public final class CustomPaintingsNeoForgeMod {
  public CustomPaintingsNeoForgeMod(IEventBus modBus, ModContainer container) {
    TroveNeoForge.bootstrap(modBus, container);
    CustomPaintingsMod.bootstrapCommon();

    // -- Commands ------------------------------------------------------------
    NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
      CustomPaintingsCommand.register(event.getDispatcher());
    });

    // -- World load ----------------------------------------------------------
    NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
      if (event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onServerLevelLoad(world.getServer(), world);
      }
    });

    // -- Entity join/leave (server painting manager) -------------------------
    NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onPaintingLoad(world, painting);
      }
    });
    NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onPaintingUnload(world, painting);
      }
    });

    // -- Player join / dimension change --------------------------------------
    NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
      if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
        CustomPaintingsMod.onPlayerJoin(player);
      }
    });

    NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
      if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
          && player.level() instanceof ServerLevel destination) {
        CustomPaintingsMod.onPlayerChangeLevel(player, destination);
      }
    });

    // -- Sneak name-toggle on painting ---------------------------------------
    NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, event -> {
      if (event.getTarget() instanceof Painting painting
          && CustomPaintingsMod.onUsePainting(event.getEntity(), painting)) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
      }
    });

    // -- Server lifecycle ----------------------------------------------------
    NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
      CustomPaintingsMod.onServerStarted(event.getServer());
    });
    NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> {
      CustomPaintingsMod.onServerStopping();
    });
    NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
      ServerInfo.onServerStopped();
      ServerPaintingRegistry.onServerStopped(event.getServer());
    });

    // -- ServerInfo persistence (save hook) ----------------------------------
    NeoForge.EVENT_BUS.addListener(LevelEvent.Save.class, event -> {
      ServerInfo.onBeforeSave();
    });

    // -- Image-packet throttle tick (parity with Fabric's START_SERVER_TICK) -
    NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, event -> {
      ImagePacketQueue.getInstance().tick();
    });

    // -- Client setup: gated so the dedicated server never loads client classes.
    if (FMLEnvironment.getDist() == Dist.CLIENT) {
      CustomPaintingsNeoForgeClient.init(modBus, container);
    }
  }
}
