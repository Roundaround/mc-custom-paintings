package me.roundaround.custompaintings.forge;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.custompaintings.client.option.KeyMappings;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import me.roundaround.custompaintings.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.trove.forge.TroveForge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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

    // -- Entity join/leave (server painting manager + client painting cache) -
    EntityJoinLevelEvent.BUS.addListener(event -> {
      if (!(event.getEntity() instanceof Painting painting)) {
        return;
      }
      if (event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onPaintingLoad(world, painting);
      } else if (event.getLevel().isClientSide()) {
        ClientPaintingManager.onEntityLoad(painting);
      }
    });

    EntityLeaveLevelEvent.BUS.addListener(event -> {
      if (!(event.getEntity() instanceof Painting painting)) {
        return;
      }
      if (event.getLevel() instanceof ServerLevel world) {
        CustomPaintingsMod.onPaintingUnload(world, painting);
      } else if (event.getLevel().isClientSide()) {
        ClientPaintingManager.onEntityUnload(painting);
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

    // -- Client lifecycle (game bus) -----------------------------------------
    ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> ClientPaintingManager.init());
    ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
      ClientPaintingRegistry.getInstance().clear();
      ClientPaintingManager.getInstance().clear();
    });

    // -- Client setup (mod bus) ----------------------------------------------
    FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> {
      KeyMappings.register();
      CacheManager.runBackgroundClean();
      ItemManager.runBackgroundClean();
    });

    // -- ModMenu analog: config-screen extension point -----------------------
    context.getContainer().registerExtensionPoint(
        ConfigScreenHandler.ConfigScreenFactory.class,
        () -> new ConfigScreenHandler.ConfigScreenFactory(
            (mc, parent) -> new MainMenuScreen(parent)));
  }
}
