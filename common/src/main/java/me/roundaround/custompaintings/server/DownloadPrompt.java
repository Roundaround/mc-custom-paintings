package me.roundaround.custompaintings.server;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.net.URI;

/**
 * Loader-agnostic holder for the "this server uses Custom Paintings" download prompt shown to
 * clients without the mod installed.
 *
 * <p>The legacy Fabric build read the homepage from {@code FabricLoader}'s mod-metadata contact
 * map at runtime (in {@code CustomPaintingsServerMod}). There is no cross-loader runtime
 * metadata API, so the homepage is inlined here as the constant it always resolved to. This
 * class lives in {@code common/} because {@code ServerNetworking} (also common) calls it — the
 * loader-only {@code @Mod} / {@code @Entrypoint} classes must not be referenced from common.
 */
public final class DownloadPrompt {
  private static final String HOMEPAGE = "https://modrinth.com/mod/custom-paintings-mod";

  private DownloadPrompt() {
  }

  public static Component getDownloadPrompt() {
    return Component.literal(
            "This server uses the Custom Paintings mod. Click here to download it and get the full experience!")
        .withStyle((style) -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(HOMEPAGE))));
  }
}
