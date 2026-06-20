package me.roundaround.custompaintings.client.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class CustomSystemToasts {
  public static final SystemToast.SystemToastId PACK_DROP_FAILURE = new SystemToast.SystemToastId();
  public static final SystemToast.SystemToastId PACK_LOAD_SKIPPED = new SystemToast.SystemToastId();
  public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
  public static final SystemToast.SystemToastId LEGACY_PACKS_FOUND = new SystemToast.SystemToastId();

  public static void addPackCopyFailure(Minecraft client, String directory) {
    SystemToast.add(
        client.gui.toastManager(), PACK_DROP_FAILURE,
        Component.translatable("custompaintings.toasts.copy.failure.title"), Component.nullToEmpty(directory));
  }

  public static void addPackLoadSkipped(Minecraft client) {
    SystemToast.add(client.gui.toastManager(), PACK_LOAD_SKIPPED,
        Component.translatable("custompaintings.toasts.load.skipped.title"),
        Component.translatable("custompaintings.toasts.load.skipped.body")
    );
  }

  public static void addPackLoadFailure(Minecraft client) {
    SystemToast.add(client.gui.toastManager(), PACK_LOAD_FAILURE,
        Component.translatable("custompaintings.toasts.load.failure.title"),
        Component.translatable("custompaintings.toasts.load.failure.body")
    );
  }

  public static void addLegacyPacksFound(Minecraft client, int count) {
    SystemToast.add(client.gui.toastManager(), LEGACY_PACKS_FOUND,
        Component.translatable("custompaintings.toasts.legacy.title"),
        Component.translatable("custompaintings.toasts.legacy.body", count)
    );
  }
}
