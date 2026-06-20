package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.custompaintings.client.gui.screen.MigrationsScreen;
import me.roundaround.custompaintings.client.gui.screen.set.PackSelectScreen;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.toast.CustomSystemToasts;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.StringUtil;
import me.roundaround.trove.network.TroveNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

public final class ClientNetworking {
  private ClientNetworking() {
  }

  public static void sendHashesPacket(Map<CustomId, String> hashes) {
    TroveNetworking.sendToServer(new Networking.HashesC2S(hashes));
  }

  public static void sendReloadPacket() {
    sendReloadPacket(List.of(), List.of());
  }

  public static void sendReloadPacket(List<String> toActivate, List<String> toDeactivate) {
    TroveNetworking.sendToServer(new Networking.ReloadC2S(toActivate, toDeactivate));
  }

  public static void sendSetPaintingPacket(int paintingId, CustomId dataId) {
    TroveNetworking.sendToServer(new Networking.SetPaintingC2S(paintingId, dataId));
  }

  public static void sendRunMigrationPacket(CustomId id) {
    TroveNetworking.sendToServer(new Networking.RunMigrationC2S(id));
  }

  public static void handleSummary(Networking.SummaryS2C payload) {
    Minecraft client = Minecraft.getInstance();
    client.execute(() -> {
      if (client.isLocalServer() && payload.skipped()) {
        CustomSystemToasts.addPackLoadSkipped(client);
      }
      if (client.player != null &&
          (client.isLocalServer() || client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) &&
          payload.loadErrorOrSkipCount() > 0) {
        CustomSystemToasts.addPackLoadFailure(client);
      }
      ClientPaintingRegistry.getInstance()
          .processSummary(
              payload.packs(),
              payload.serverId(),
              payload.combinedImageHash(),
              payload.finishedMigrations()
          );
    });
  }

  public static void handleDownloadSummary(Networking.DownloadSummaryS2C payload) {
    Minecraft.getInstance().execute(() -> ClientPaintingRegistry.getInstance()
        .trackExpectedPackets(payload.ids(), payload.imageCount(), payload.byteCount()));
  }

  public static void handleImage(Networking.ImageS2C payload) {
    Minecraft.getInstance().execute(() -> {
      CustomPaintingsMod.LOGGER.info(
          "Received full image for {} ({}).",
          payload.id(),
          StringUtil.formatBytes(payload.image().getSize())
      );
      ClientPaintingRegistry.getInstance().setPaintingImage(payload.id(), payload.image());
    });
  }

  public static void handleImageHeader(Networking.ImageHeaderS2C payload) {
    Minecraft.getInstance().execute(() -> {
      CustomPaintingsMod.LOGGER.info("Received image header for {}.", payload.id());
      ClientPaintingRegistry.getInstance()
          .setPaintingHeader(payload.id(), payload.width(), payload.height(), payload.totalChunks());
    });
  }

  public static void handleImageChunk(Networking.ImageChunkS2C payload) {
    Minecraft.getInstance().execute(() -> {
      CustomPaintingsMod.LOGGER.info(
          "Received image chunk #{} for {} ({}).",
          payload.index(),
          payload.id(),
          StringUtil.formatBytes(payload.bytes().length)
      );
      ClientPaintingRegistry.getInstance().setPaintingChunk(payload.id(), payload.index(), payload.bytes());
    });
  }

  public static void handleEditPainting(Networking.EditPaintingS2C payload) {
    Minecraft client = Minecraft.getInstance();
    client.execute(() -> {
      PaintingEditState state = new PaintingEditState(
          client,
          payload.paintingId(),
          payload.pos(),
          payload.facing()
      );

      client.gui.setScreen(new PackSelectScreen(state));
    });
  }

  public static void handleSetPainting(Networking.SetPaintingS2C payload) {
    Minecraft client = Minecraft.getInstance();
    client.execute(() -> {
      if (client.player == null) {
        return;
      }
      ClientPaintingManager.getInstance().trySetPaintingData(client.player.level(), payload.assignment());
    });
  }

  public static void handleSyncAllData(Networking.SyncAllDataS2C payload) {
    Minecraft client = Minecraft.getInstance();
    client.execute(() -> {
      if (client.player == null) {
        return;
      }
      Level world = client.player.level();
      for (PaintingAssignment assignment : payload.assignments()) {
        ClientPaintingManager.getInstance().trySetPaintingData(world, assignment);
      }
    });
  }

  public static void handleMigrationFinish(Networking.MigrationFinishS2C payload) {
    Minecraft client = Minecraft.getInstance();
    client.execute(() -> {
      ClientPaintingRegistry.getInstance().markMigrationFinished(payload.id(), payload.succeeded());
      Screen currentScreen = client.gui.screen();
      if (!(currentScreen instanceof MigrationsScreen screen)) {
        return;
      }
      screen.onMigrationFinished(payload.id(), payload.succeeded());
    });
  }

  public static void handleOpenMenu(Networking.OpenMenuS2C payload) {
    Minecraft client = Minecraft.getInstance();
    client.execute(() -> {
      Screen screen = client.gui.screen();
      if (screen == null || screen instanceof ChatScreen) {
        client.gui.setScreen(new MainMenuScreen(null));
      }
    });
  }
}
