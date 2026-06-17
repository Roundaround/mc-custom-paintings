package me.roundaround.custompaintings.forge;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.trove.forge.TroveForge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("custompaintings")
public final class CustomPaintingsForgeMod {
  public CustomPaintingsForgeMod(FMLJavaModLoadingContext context) {
    TroveForge.bootstrap(context);
    CustomPaintingsMod.bootstrapCommon();

    // This build uses eventbus-7 typed buses: game events via <Event>.BUS.addListener(...) and
    // mod-setup events via <Event>.getBus(context.getModBusGroup()).addListener(...). Cancellable
    // events take a Predicate listener and cancel by returning true.

    // -- Commands ------------------------------------------------------------
    RegisterCommandsEvent.BUS.addListener(event -> {
      CustomPaintingsCommand.register(event.getDispatcher());
    });

    // -- World load ----------------------------------------------------------
    LevelEvent.Load.BUS.addListener(event -> {
      if (event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onServerLevelLoad(world.getServer(), world);
      }
    });

    // -- Entity join/leave (server painting manager) -------------------------
    EntityJoinLevelEvent.BUS.addListener(event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onPaintingLoad(world, painting);
      }
    });
    EntityLeaveLevelEvent.BUS.addListener(event -> {
      if (event.getEntity() instanceof Painting painting && event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onPaintingUnload(world, painting);
      }
    });

    // -- Player join / dimension change --------------------------------------
    PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
      if (event.getEntity() instanceof ServerPlayer player) {
        CustomPaintingsMod.onPlayerJoin(player);
      }
    });

    PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
      if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel destination) {
        CustomPaintingsMod.onPlayerChangeLevel(player, destination);
      }
    });

    // -- Sneak name-toggle on painting (Predicate listener: return true to cancel) -----------
    // Forge in this build exposes EntityInteractSpecific (not a plain EntityInteract); it is the
    // entity-right-click hook. TODO(build pass): confirm this is the correct interaction event
    // for the sneak name-toggle parity with Fabric's UseEntityCallback.
    PlayerInteractEvent.EntityInteractSpecific.BUS.addListener((PlayerInteractEvent.EntityInteractSpecific event) -> {
      if (event.getTarget() instanceof Painting painting
          && CustomPaintingsMod.onUsePainting(event.getEntity(), painting)) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
      }
      return false;
    });

    // -- Server lifecycle ----------------------------------------------------
    ServerStartedEvent.BUS.addListener(event -> CustomPaintingsMod.onServerStarted(event.getServer()));
    ServerStoppingEvent.BUS.addListener(event -> CustomPaintingsMod.onServerStopping());
    ServerStoppedEvent.BUS.addListener(event -> {
      ServerInfo.onServerStopped();
      ServerPaintingRegistry.onServerStopped(event.getServer());
    });

    // -- ServerInfo persistence (save hook) ----------------------------------
    LevelEvent.Save.BUS.addListener(event -> ServerInfo.onBeforeSave());

    // -- Image-packet throttle tick (parity with Fabric's START_SERVER_TICK) -
    TickEvent.ServerTickEvent.Pre.BUS.addListener(event -> ImagePacketQueue.getInstance().tick());

    // -- Client setup: gated so the dedicated server never loads client classes.
    if (FMLEnvironment.dist.isClient()) {
      CustomPaintingsForgeClient.init(context);
    }
  }
}
