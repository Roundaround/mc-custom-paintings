package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.server.DownloadPrompt;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.trove.network.TroveNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerNetworking {
  private ServerNetworking() {
  }

  public static void sendSummaryPacketToAll(
      MinecraftServer server,
      List<PackData> packs,
      String combinedImageHash,
      Map<CustomId, Boolean> finishedMigrations,
      boolean skipped,
      int loadErrorOrSkipCount
  ) {
    server.getPlayerList()
        .getPlayers()
        .forEach((player) -> sendSummaryPacket(
            player,
            packs,
            combinedImageHash,
            finishedMigrations,
            skipped,
            loadErrorOrSkipCount
        ));
  }

  public static void sendSummaryPacket(
      ServerPlayer player,
      List<PackData> packs,
      String combinedImageHash,
      Map<CustomId, Boolean> finishedMigrations,
      boolean skipped,
      int loadErrorOrSkipCount
  ) {
    if (!TroveNetworking.canSend(player, Networking.SummaryS2C.ID)) {
      player.sendSystemMessage(DownloadPrompt.getDownloadPrompt());
      return;
    }
    UUID serverId = ServerInfo.getInstance().getServerId();
    TroveNetworking.sendToClient(
        player,
        new Networking.SummaryS2C(serverId, packs, combinedImageHash, finishedMigrations, skipped, loadErrorOrSkipCount)
    );
  }

  public static void sendDownloadSummaryPacket(
      ServerPlayer player,
      Collection<CustomId> ids,
      int imageCount,
      int byteCount
  ) {
    TroveNetworking.sendToClient(
        player, new Networking.DownloadSummaryS2C(ids.stream().toList(), imageCount, byteCount));
  }

  public static void sendEditPaintingPacket(
      ServerPlayer player,
      UUID paintingUuid,
      int paintingId,
      BlockPos pos,
      Direction facing
  ) {
    TroveNetworking.sendToClient(player, new Networking.EditPaintingS2C(paintingUuid, paintingId, pos, facing));
  }

  public static void sendSetPaintingPacketToAll(ServerLevel world, PaintingAssignment assignment) {
    Networking.SetPaintingS2C payload = new Networking.SetPaintingS2C(assignment);
    world.players().forEach((player) -> {
      sendSetPaintingPacket(player, payload);
    });
  }

  public static void sendSetPaintingPacket(ServerPlayer player, Networking.SetPaintingS2C payload) {
    if (TroveNetworking.canSend(player, payload.type())) {
      TroveNetworking.sendToClient(player, payload);
    }
  }

  public static void sendSyncAllDataPacket(ServerPlayer player, List<PaintingAssignment> assignments) {
    if (TroveNetworking.canSend(player, Networking.SyncAllDataS2C.ID)) {
      TroveNetworking.sendToClient(player, new Networking.SyncAllDataS2C(assignments));
    }
  }

  public static void sendMigrationFinishPacketToAll(MinecraftServer server, CustomId id, boolean succeeded) {
    Networking.MigrationFinishS2C payload = new Networking.MigrationFinishS2C(id, succeeded);
    server.getPlayerList().getPlayers().forEach((player) -> {
      sendMigrationFinishPacket(player, payload);
    });
  }

  public static void sendMigrationFinishPacket(ServerPlayer player, Networking.MigrationFinishS2C payload) {
    if (TroveNetworking.canSend(player, Networking.MigrationFinishS2C.ID)) {
      TroveNetworking.sendToClient(player, payload);
    }
  }

  public static void sendOpenMenuPacket(ServerPlayer player) {
    if (TroveNetworking.canSend(player, Networking.OpenMenuS2C.ID)) {
      TroveNetworking.sendToClient(player, new Networking.OpenMenuS2C());
    }
  }
}
