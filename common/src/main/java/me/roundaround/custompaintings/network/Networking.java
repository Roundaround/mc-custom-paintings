package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.trove.network.TroveNetworking;
import me.roundaround.trove.network.TrovePacketCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Networking {
  private Networking() {
  }

  public static final Identifier SUMMARY_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "summary_s2c");
  public static final Identifier IMAGE_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_s2c");
  public static final Identifier IMAGE_IDS_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_ids_s2c");
  public static final Identifier DOWNLOAD_SUMMARY_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "download_summary_s2c"
  );
  public static final Identifier IMAGE_HEADER_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "image_header_s2c"
  );
  public static final Identifier IMAGE_CHUNK_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_chunk_s2c");
  public static final Identifier EDIT_PAINTING_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "edit_painting_s2c"
  );
  public static final Identifier SET_PAINTING_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "set_painting_s2c"
  );
  public static final Identifier SYNC_ALL_DATA_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "sync_all_data_s2c"
  );
  public static final Identifier MIGRATION_FINISH_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "migration_finish_s2c"
  );
  public static final Identifier OPEN_MENU_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "open_menu_s2c");
  public static final Identifier LIST_UNKNOWN_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "list_unknown_s2c"
  );

  public static final Identifier HASHES_C2S = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hashes_c2s");
  public static final Identifier RELOAD_C2S = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "reload_c2s");
  public static final Identifier SET_PAINTING_C2S = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "set_painting_c2s"
  );
  public static final Identifier RUN_MIGRATION_C2S = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "run_migration_c2s"
  );

  public static void register() {
    // Server-bound (C2S) registrations: handler runs on the logical server.
    TroveNetworking.registerC2S(HashesC2S.ID, HashesC2S.CODEC, Networking::handleHashes);
    TroveNetworking.registerC2S(ReloadC2S.ID, ReloadC2S.CODEC, Networking::handleReload);
    TroveNetworking.registerC2S(SetPaintingC2S.ID, SetPaintingC2S.CODEC, Networking::handleSetPainting);
    TroveNetworking.registerC2S(RunMigrationC2S.ID, RunMigrationC2S.CODEC, Networking::handleRunMigration);

    // S2C bodies live in client-only ClientNetworking; explicit lambdas (not method refs) keep it off
    // the server, which runs register() only to send these.
    TroveNetworking.registerS2C(SummaryS2C.ID, SummaryS2C.CODEC, (payload) -> ClientNetworking.handleSummary(payload));
    TroveNetworking.registerS2C(DownloadSummaryS2C.ID, DownloadSummaryS2C.CODEC,
        (payload) -> ClientNetworking.handleDownloadSummary(payload));
    TroveNetworking.registerS2C(ImageS2C.ID, ImageS2C.CODEC, (payload) -> ClientNetworking.handleImage(payload));
    TroveNetworking.registerS2C(ImageHeaderS2C.ID, ImageHeaderS2C.CODEC,
        (payload) -> ClientNetworking.handleImageHeader(payload));
    TroveNetworking.registerS2C(ImageChunkS2C.ID, ImageChunkS2C.CODEC,
        (payload) -> ClientNetworking.handleImageChunk(payload));
    TroveNetworking.registerS2C(EditPaintingS2C.ID, EditPaintingS2C.CODEC,
        (payload) -> ClientNetworking.handleEditPainting(payload));
    TroveNetworking.registerS2C(SetPaintingS2C.ID, SetPaintingS2C.CODEC,
        (payload) -> ClientNetworking.handleSetPainting(payload));
    TroveNetworking.registerS2C(SyncAllDataS2C.ID, SyncAllDataS2C.CODEC,
        (payload) -> ClientNetworking.handleSyncAllData(payload));
    TroveNetworking.registerS2C(MigrationFinishS2C.ID, MigrationFinishS2C.CODEC,
        (payload) -> ClientNetworking.handleMigrationFinish(payload));
    TroveNetworking.registerS2C(OpenMenuS2C.ID, OpenMenuS2C.CODEC, (payload) -> ClientNetworking.handleOpenMenu(payload));
    // Note: ImageIdsS2C and ListUnknownS2C have no registered receiver (parity with the legacy
    // Fabric-only build, which registered the payload type but never a client handler).
  }

  // ---------------------------------------------------------------------------
  // C2S handlers (server side). The legacy Fabric receivers wrapped these in
  // context.server().execute(...); here player.level().getServer() is the MinecraftServer.
  // ---------------------------------------------------------------------------

  private static void handleHashes(HashesC2S payload, ServerPlayer player) {
    player.level().getServer().execute(() -> ServerPaintingRegistry.getInstance().checkPlayerHashes(player, payload.hashes()));
  }

  private static void handleReload(ReloadC2S payload, ServerPlayer player) {
    player.level().getServer().execute(() -> {
      if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
        return;
      }
      ServerInfo serverInfo = ServerInfo.getInstance();
      for (String packFileUid : payload.toActivate()) {
        serverInfo.markPackEnabled(packFileUid);
      }
      for (String packFileUid : payload.toDeactivate()) {
        serverInfo.markPackDisabled(packFileUid);
      }
      ServerPaintingRegistry.getInstance().reloadPaintingPacks(ServerPaintingManager::syncAllDataForAllPlayers);
    });
  }

  private static void handleSetPainting(SetPaintingC2S payload, ServerPlayer player) {
    player.level().getServer().execute(() -> {
      ServerLevel world = player.level();
      Entity entity = world.getEntity(payload.paintingId());
      if (!(entity instanceof Painting painting)) {
        return;
      }

      if (painting.custompaintings$getEditor() == null ||
          !painting.custompaintings$getEditor().equals(player.getUUID())) {
        return;
      }

      PaintingData paintingData = ServerPaintingRegistry.getInstance().get(payload.dataId());
      if (paintingData == null || paintingData.isEmpty()) {
        painting.custompaintings$setEditor(null);
        painting.hurtServer(world, player.damageSources().playerAttack(player), 0f);
        return;
      }

      if (paintingData.vanilla()) {
        painting.custompaintings$setVariant(paintingData.id());
      }
      painting.custompaintings$setData(paintingData);

      if (!painting.survives()) {
        painting.custompaintings$setEditor(null);
        painting.hurtServer(world, player.damageSources().playerAttack(player), 0f);
        return;
      }

      world.custompaintings$getPaintingManager().setPaintingData(painting, paintingData);
      painting.custompaintings$setEditor(null);
    });
  }

  private static void handleRunMigration(RunMigrationC2S payload, ServerPlayer player) {
    player.level().getServer().execute(() -> ServerPaintingManager.runMigration(player, payload.id()));
  }

  public record SummaryS2C(UUID serverId,
                           List<PackData> packs,
                           String combinedImageHash,
                           Map<CustomId, Boolean> finishedMigrations,
                           boolean skipped,
                           int loadErrorOrSkipCount) implements CustomPacketPayload {
    public static final Type<SummaryS2C> ID = new Type<>(SUMMARY_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, SummaryS2C> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        SummaryS2C::serverId,
        TrovePacketCodecs.forList(PackData.PACKET_CODEC),
        SummaryS2C::packs,
        ByteBufCodecs.STRING_UTF8,
        SummaryS2C::combinedImageHash,
        TrovePacketCodecs.forMap(CustomId.PACKET_CODEC, ByteBufCodecs.BOOL),
        SummaryS2C::finishedMigrations,
        ByteBufCodecs.BOOL,
        SummaryS2C::skipped,
        ByteBufCodecs.INT,
        SummaryS2C::loadErrorOrSkipCount,
        SummaryS2C::new
    );

    @Override
    public Type<SummaryS2C> type() {
      return ID;
    }
  }

  public record ImageS2C(CustomId id, Image image) implements CustomPacketPayload {
    public static final Type<ImageS2C> ID = new Type<>(IMAGE_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        ImageS2C::id,
        Image.PACKET_CODEC,
        ImageS2C::image,
        ImageS2C::new
    );

    @Override
    public Type<ImageS2C> type() {
      return ID;
    }
  }

  public record ImageIdsS2C(List<CustomId> ids) implements CustomPacketPayload {
    public static final Type<ImageIdsS2C> ID = new Type<>(IMAGE_IDS_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageIdsS2C> CODEC =
        StreamCodec.composite(TrovePacketCodecs.forList(CustomId.PACKET_CODEC),
        ImageIdsS2C::ids,
        ImageIdsS2C::new
    );

    @Override
    public Type<ImageIdsS2C> type() {
      return ID;
    }
  }

  public record DownloadSummaryS2C(List<CustomId> ids, int imageCount, int byteCount) implements CustomPacketPayload {
    public static final Type<DownloadSummaryS2C> ID = new Type<>(DOWNLOAD_SUMMARY_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadSummaryS2C> CODEC = StreamCodec.composite(
        TrovePacketCodecs.forList(CustomId.PACKET_CODEC),
        DownloadSummaryS2C::ids,
        ByteBufCodecs.INT,
        DownloadSummaryS2C::imageCount,
        ByteBufCodecs.INT,
        DownloadSummaryS2C::byteCount,
        DownloadSummaryS2C::new
    );

    @Override
    public Type<DownloadSummaryS2C> type() {
      return ID;
    }
  }

  public record ImageHeaderS2C(CustomId id, int width, int height, int totalChunks) implements CustomPacketPayload {
    public static final Type<ImageHeaderS2C> ID = new Type<>(IMAGE_HEADER_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageHeaderS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        ImageHeaderS2C::id,
        ByteBufCodecs.INT,
        ImageHeaderS2C::width,
        ByteBufCodecs.INT,
        ImageHeaderS2C::height,
        ByteBufCodecs.INT,
        ImageHeaderS2C::totalChunks,
        ImageHeaderS2C::new
    );

    @Override
    public Type<ImageHeaderS2C> type() {
      return ID;
    }
  }

  public record ImageChunkS2C(CustomId id, int index, byte[] bytes) implements CustomPacketPayload {
    public static final Type<ImageChunkS2C> ID = new Type<>(IMAGE_CHUNK_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageChunkS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        ImageChunkS2C::id,
        ByteBufCodecs.INT,
        ImageChunkS2C::index,
        ByteBufCodecs.BYTE_ARRAY,
        ImageChunkS2C::bytes,
        ImageChunkS2C::new
    );

    @Override
    public Type<ImageChunkS2C> type() {
      return ID;
    }
  }

  public record EditPaintingS2C(UUID paintingUuid, int paintingId, BlockPos pos, Direction facing) implements
      CustomPacketPayload {
    public static final Type<EditPaintingS2C> ID = new Type<>(EDIT_PAINTING_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, EditPaintingS2C> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        EditPaintingS2C::paintingUuid,
        ByteBufCodecs.INT,
        EditPaintingS2C::paintingId,
        BlockPos.STREAM_CODEC,
        EditPaintingS2C::pos,
        Direction.STREAM_CODEC,
        EditPaintingS2C::facing,
        EditPaintingS2C::new
    );

    @Override
    public Type<EditPaintingS2C> type() {
      return ID;
    }
  }

  public record SetPaintingS2C(PaintingAssignment assignment) implements CustomPacketPayload {
    public static final Type<SetPaintingS2C> ID = new Type<>(SET_PAINTING_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPaintingS2C> CODEC =
        StreamCodec.composite(PaintingAssignment.PACKET_CODEC,
        SetPaintingS2C::assignment,
        SetPaintingS2C::new
    );

    @Override
    public Type<SetPaintingS2C> type() {
      return ID;
    }
  }

  public record SyncAllDataS2C(List<PaintingAssignment> assignments) implements CustomPacketPayload {
    public static final Type<SyncAllDataS2C> ID = new Type<>(SYNC_ALL_DATA_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAllDataS2C> CODEC = StreamCodec.composite(
        TrovePacketCodecs.forList(PaintingAssignment.PACKET_CODEC),
        SyncAllDataS2C::assignments,
        SyncAllDataS2C::new
    );

    @Override
    public Type<SyncAllDataS2C> type() {
      return ID;
    }
  }

  public record MigrationFinishS2C(CustomId id, boolean succeeded) implements CustomPacketPayload {
    public static final Type<MigrationFinishS2C> ID = new Type<>(MIGRATION_FINISH_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, MigrationFinishS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        MigrationFinishS2C::id,
        ByteBufCodecs.BOOL,
        MigrationFinishS2C::succeeded,
        MigrationFinishS2C::new
    );

    @Override
    public Type<MigrationFinishS2C> type() {
      return ID;
    }
  }

  public record OpenMenuS2C() implements CustomPacketPayload {
    public static final Type<OpenMenuS2C> ID = new Type<>(OPEN_MENU_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuS2C> CODEC = TrovePacketCodecs.empty(
        OpenMenuS2C::new);

    @Override
    public Type<OpenMenuS2C> type() {
      return ID;
    }
  }

  public record ListUnknownS2C(Map<CustomId, Integer> counts) implements CustomPacketPayload {
    public static final Type<ListUnknownS2C> ID = new Type<>(LIST_UNKNOWN_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ListUnknownS2C> CODEC = StreamCodec.composite(
        TrovePacketCodecs.forMap(CustomId.PACKET_CODEC, ByteBufCodecs.INT),
        ListUnknownS2C::counts,
        ListUnknownS2C::new
    );

    @Override
    public Type<ListUnknownS2C> type() {
      return ID;
    }
  }

  public record HashesC2S(Map<CustomId, String> hashes) implements CustomPacketPayload {
    public static final Type<HashesC2S> ID = new Type<>(HASHES_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, HashesC2S> CODEC = StreamCodec.composite(
        TrovePacketCodecs.forMap(CustomId.PACKET_CODEC, ByteBufCodecs.STRING_UTF8),
        HashesC2S::hashes,
        HashesC2S::new
    );

    @Override
    public Type<HashesC2S> type() {
      return ID;
    }
  }

  public record ReloadC2S(List<String> toActivate, List<String> toDeactivate) implements CustomPacketPayload {
    public static final Type<ReloadC2S> ID = new Type<>(RELOAD_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadC2S> CODEC = StreamCodec.composite(
        TrovePacketCodecs.forList(ByteBufCodecs.STRING_UTF8),
        ReloadC2S::toActivate,
        TrovePacketCodecs.forList(ByteBufCodecs.STRING_UTF8),
        ReloadC2S::toDeactivate,
        ReloadC2S::new
    );

    @Override
    public Type<ReloadC2S> type() {
      return ID;
    }
  }

  public record SetPaintingC2S(int paintingId, CustomId dataId) implements CustomPacketPayload {
    public static final Type<SetPaintingC2S> ID = new Type<>(SET_PAINTING_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPaintingC2S> CODEC = StreamCodec.ofMember(
        SetPaintingC2S::write,
        SetPaintingC2S::read
    );

    private static SetPaintingC2S read(FriendlyByteBuf buf) {
      int paintingId = buf.readInt();
      CustomId dataId = null;
      if (buf.readBoolean()) {
        dataId = CustomId.read(buf);
      }
      return new SetPaintingC2S(paintingId, dataId);
    }

    private void write(FriendlyByteBuf buf) {
      buf.writeInt(this.paintingId);
      if (this.dataId == null) {
        buf.writeBoolean(false);
      } else {
        buf.writeBoolean(true);
        this.dataId.write(buf);
      }
    }

    @Override
    public Type<SetPaintingC2S> type() {
      return ID;
    }
  }

  public record RunMigrationC2S(CustomId id) implements CustomPacketPayload {
    public static final Type<RunMigrationC2S> ID = new Type<>(RUN_MIGRATION_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, RunMigrationC2S> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        RunMigrationC2S::id,
        RunMigrationC2S::new
    );

    @Override
    public Type<RunMigrationC2S> type() {
      return ID;
    }
  }
}
