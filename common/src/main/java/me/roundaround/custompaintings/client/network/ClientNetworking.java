package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.trove.network.TroveNetworking;

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
}
