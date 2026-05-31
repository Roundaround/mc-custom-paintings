package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.trove.event.ClientLifecycle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClientPaintingManager {
  private static final long TTL = 1000 * 60 * 15; // 15 minutes

  private static ClientPaintingManager instance = null;

  private final HashMap<Integer, PaintingData> cachedData = new HashMap<>();
  private final HashMap<Integer, Long> expiryTimes = new HashMap<>();

  private ClientPaintingManager() {
    // The client tick has a loader-agnostic Trove facade, so it registers here. The client
    // entity load/unload events have no Trove equivalent, so the per-loader client setup wires
    // them to onEntityLoad / onEntityUnload below.
    ClientLifecycle.onTick(() -> {
      long now = Util.getEpochMillis();
      List<Integer> expiredIds = this.expiryTimes.entrySet()
          .stream()
          .filter((entry) -> now >= entry.getValue())
          .map(Map.Entry::getKey)
          .toList();
      expiredIds.forEach(this::remove);
    });
  }

  public static void init() {
    ClientPaintingManager instance = getInstance();
    // In case clear somehow didn't get called before, clear it now
    instance.clear();
  }

  public static ClientPaintingManager getInstance() {
    if (instance == null) {
      instance = new ClientPaintingManager();
    }
    return instance;
  }

  // Called from each loader's client-side entity-load hook (Fabric ClientEntityEvents.ENTITY_LOAD;
  // NeoForge/Forge EntityJoinLevelEvent filtered to the client level).
  public static void onEntityLoad(Entity entity) {
    if (!(entity instanceof Painting painting)) {
      return;
    }

    ClientPaintingManager manager = getInstance();
    int id = painting.getId();
    PaintingData data = manager.cachedData.get(id);
    if (data != null && !data.isEmpty()) {
      manager.setPaintingData(painting, data);
    }

    manager.remove(id);
  }

  // Called from each loader's client-side entity-unload hook (Fabric ClientEntityEvents.ENTITY_UNLOAD;
  // NeoForge/Forge EntityLeaveLevelEvent filtered to the client level).
  public static void onEntityUnload(Entity entity) {
    if (!(entity instanceof Painting painting)) {
      return;
    }

    ClientPaintingManager manager = getInstance();
    Entity.RemovalReason removalReason = painting.getRemovalReason();
    // On client side, unloaded paintings are always "discarded"
    if (removalReason == Entity.RemovalReason.DISCARDED) {
      manager.cacheData(painting);
    } else {
      manager.remove(painting.getId());
    }
  }

  public void trySetPaintingData(Level world, PaintingAssignment assignment) {
    int id = assignment.getPaintingId();
    CompletableFuture<PaintingData> future = assignment.isKnown() ?
        ClientPaintingRegistry.getInstance().safeGet(assignment.getDataId()) :
        CompletableFuture.completedFuture(assignment.getData());
    future.thenAccept((data) -> {
      if (data == null || data.isEmpty()) {
        return;
      }

      Entity entity = world.getEntity(id);
      if (!(entity instanceof Painting painting)) {
        this.cachedData.put(id, data);
        return;
      }

      this.setPaintingData(painting, data);
    });
  }

  public void clear() {
    this.cachedData.clear();
    this.expiryTimes.clear();
  }

  private void setPaintingData(Painting painting, PaintingData data) {
    if (data.vanilla()) {
      painting.custompaintings$setVariant(data.id());
    }
    painting.custompaintings$setData(data);
  }

  private void remove(int id) {
    this.cachedData.remove(id);
    this.expiryTimes.remove(id);
  }

  private void cacheData(Painting painting) {
    int id = painting.getId();
    PaintingData data = painting.custompaintings$getData();
    if (data != null && !data.isEmpty()) {
      this.cachedData.put(id, data);
      this.expiryTimes.put(id, Util.getEpochMillis() + TTL);
    }
  }
}
