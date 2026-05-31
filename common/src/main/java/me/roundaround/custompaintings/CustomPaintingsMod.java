package me.roundaround.custompaintings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.file.json.CustomPaintingsJson;
import me.roundaround.custompaintings.resource.file.json.LegacyCustomPaintingsJson;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.trove.observable.Observer;
import me.roundaround.trove.observable.Subscription;
import me.roundaround.trove.util.PathAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Loader-agnostic core of Custom Paintings. Holds the shared statics
 * ({@link #LOGGER}, {@link #GSON}, {@link #EMPTY_HASH}, {@link #reloadDataPacks}) referenced
 * throughout {@code common/}, plus the server-side game-event handler bodies. The actual
 * event registration lives in the per-loader entrypoints
 * ({@code fabric/CustomPaintingsMod}, {@code neoforge/CustomPaintingsNeoForgeMod},
 * {@code forge/CustomPaintingsForgeMod}); each calls {@link #bootstrapCommon()} once and then
 * wires its loader's events to the {@code on*} methods here.
 */
public final class CustomPaintingsMod {
  public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
      .registerTypeAdapter(CustomPaintingsJson.class, new CustomPaintingsJson.TypeAdapter())
      .registerTypeAdapter(LegacyCustomPaintingsJson.class, new LegacyCustomPaintingsJson.TypeAdapter())
      .registerTypeAdapter(PackResource.class, new PackResource.TypeAdapter())
      .create();
  public static final String EMPTY_HASH = "$$";

  private static Subscription stonecutterSub = null;
  private static Subscription vanillaStonecutterSub = null;

  private CustomPaintingsMod() {
  }

  public static CompletableFuture<Void> reloadDataPacks(MinecraftServer server) {
    if (server == null) {
      return CompletableFuture.completedFuture(null);
    }
    List<String> enabledPacks = server.getPackRepository().getSelectedPacks().stream().map(Pack::getId).toList();
    return server.reloadResources(enabledPacks).exceptionally((throwable) -> {
      LOGGER.warn("Failed to reload data packs", throwable);
      return null;
    });
  }

  /**
   * Registers the loader-agnostic pieces: config init, networking, and the world-attach hook
   * that loads {@link ServerInfo}. Each loader entrypoint calls this exactly once (after the
   * Trove bootstrap on NeoForge/Forge; Fabric self-bootstraps Trove).
   */
  public static void bootstrapCommon() {
    CustomPaintingsConfig.getInstance().init();
    CustomPaintingsPerWorldConfig.getInstance().init();

    Networking.register();

    // Trove's PathAccessor populates the world directory across all three loaders at world-session
    // creation (its own ServerPacksSource hook) and fires onWorldDirAttached then. This replaces the
    // legacy RoundaLib ResourceManagerEvents.CREATING / ServerPacksSourceMixin pair the mod relied on.
    PathAccessor.onWorldDirAttached(ServerInfo::init);
  }

  // --- Server game-event handler bodies (wired per loader) -------------------

  public static void onServerLevelLoad(MinecraftServer server, ServerLevel world) {
    server.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT);
    ServerPaintingRegistry.init(server);
    world.custompaintings$getPaintingManager();
  }

  public static void onPaintingLoad(ServerLevel world, Painting painting) {
    world.custompaintings$getPaintingManager().onEntityLoad(painting);
  }

  public static void onPaintingUnload(ServerLevel world, Painting painting) {
    world.custompaintings$getPaintingManager().onEntityUnload(painting);
  }

  public static void onPlayerJoin(ServerPlayer player) {
    ServerPaintingRegistry.getInstance().sendSummaryToPlayer(player);
    player.level().custompaintings$getPaintingManager().syncAllDataForPlayer(player);
  }

  public static void onPlayerChangeLevel(ServerPlayer player, ServerLevel destination) {
    destination.custompaintings$getPaintingManager().syncAllDataForPlayer(player);
  }

  /**
   * Sneak-toggles a painting's custom-name visibility. Returns {@code true} if the interaction
   * was consumed (the legacy Fabric build returned {@code InteractionResult.SUCCESS}); the loader
   * glue maps that back to its own interaction-result type.
   */
  public static boolean onUsePainting(Player player, Painting painting) {
    if (player.isSpectator() || !player.isShiftKeyDown()) {
      return false;
    }

    painting.setCustomNameVisible(!painting.isCustomNameVisible());
    return true;
  }

  public static void onServerStarted(MinecraftServer server) {
    if (stonecutterSub != null) {
      stonecutterSub.close();
    }
    if (vanillaStonecutterSub != null) {
      vanillaStonecutterSub.close();
    }
    Observer.P0 onChange = () -> reloadDataPacks(server);
    stonecutterSub = CustomPaintingsPerWorldConfig.getInstance().pickPaintingWithStoneCutter.savedValue.cold()
        .subscribe(onChange);
    vanillaStonecutterSub =
        CustomPaintingsPerWorldConfig.getInstance().pickVanillaPaintingWithStoneCutter.savedValue.cold()
            .subscribe(onChange);
  }

  public static void onServerStopping() {
    if (stonecutterSub != null) {
      stonecutterSub.close();
      stonecutterSub = null;
    }
    if (vanillaStonecutterSub != null) {
      vanillaStonecutterSub.close();
      vanillaStonecutterSub = null;
    }
  }
}
