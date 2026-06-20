package me.roundaround.custompaintings.gametest;

import java.util.concurrent.CompletableFuture;

import me.roundaround.allay.api.gametest.ClientGameTest;
import me.roundaround.trove.gametest.ClientTest;
import me.roundaround.trove.gametest.ClientTestContext;
import me.roundaround.trove.gametest.ClientWorld;
import net.minecraft.client.Minecraft;

/**
 * Joins a world, then forces a client resource reload — the path 26.2 broke.
 * {@code ItemManager#bakeModels} baked a missing model against the mod's own item
 * atlas, which 26.2 now rejects (sprites outside the block atlas). On world join
 * that threw silently (swallowed by the event loop → painting items showed the
 * missing texture); on a manual reload it surfaced through
 * {@code DownloadedPackSource#onReloadSuccess}, failing the reload and kicking the
 * player. Asserts the client stays in the world across a reload.
 */
@ClientGameTest
public class CustomPaintingsInWorldClientTest implements ClientTest {
  @Override
  public void runTest(ClientTestContext context) {
    try (ClientWorld world = context.worldBuilder().creative().create()) {
      context.waitTicks(5);

      CompletableFuture<Void> reload = context.computeOnClient(Minecraft::reloadResourcePacks);
      context.waitFor((mc) -> reload.isDone(), 600);
      context.waitTicks(20);
      context.runOnClient((mc) -> {
        if (reload.isCompletedExceptionally()) {
          throw new AssertionError("client resource reload failed — item-model bake regression");
        }
        if (mc.level == null) {
          throw new AssertionError("kicked from world after resource reload — item-model bake regression");
        }
      });
    }
  }
}
