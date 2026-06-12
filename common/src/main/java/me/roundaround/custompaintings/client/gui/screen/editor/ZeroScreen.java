package me.roundaround.custompaintings.client.gui.screen.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.editor.pack.PackScreen;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.file.PackReader;
import me.roundaround.custompaintings.resource.file.Painting;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.trove.client.gui.scaffold.BaseScreen;
import me.roundaround.trove.client.gui.scaffold.ScreenParent;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.client.gui.widget.FlowListWidget;
import me.roundaround.trove.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.trove.client.gui.widget.drawable.LabelWidget;
import me.roundaround.trove.util.PathAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ZeroScreen extends BaseScreen {
  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  public ZeroScreen(
      @NotNull ScreenParent parent,
      @NotNull Minecraft minecraft) {
    super(Component.translatable("custompaintings.editor.zero.title"), parent, minecraft);
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.font, this.title);

    ZeroList list = this.layout.addBody(new ZeroList(this.minecraft, this.layout));

    list.addEntry(ZeroList.BodyEntry.factory(
        this.font,
        LabelWidget.builder(
            this.font,
            List.of(
                Component.translatable("custompaintings.editor.zero.body.1"),
                Component.translatable("custompaintings.editor.zero.body.2"),
                Component.translatable("custompaintings.editor.zero.body.drag")))
            .hideBackground()
            .showShadow()
            .alignTextCenterX()
            .lineSpacing(GuiUtil.PADDING / 2)
            .build()));

    list.addEntry(ZeroList.ButtonEntry.factory(
        this.font,
        Component.translatable("custompaintings.editor.zero.new.label"),
        Button.builder(
            Component.translatable("custompaintings.editor.zero.new.button"),
            this::newPack)
            .width(Button.SMALL_WIDTH).build()));

    list.addEntry(ZeroList.ButtonEntry.factory(
        this.font,
        Component.literal("Open Famous Paintings from assets"),
        Button.builder(
            Component.translatable("custompaintings.editor.zero.open.button"),
            this::openSample)
            .width(Button.SMALL_WIDTH).build()));

    this.layout.addFooter(Button.builder(CommonComponents.GUI_DONE, this::done)
        .width(Button.BIG_WIDTH)
        .build());

    VersionStamp.create(this.font, this.layout);

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  @Override
  public void onFilesDrop(List<Path> paths) {
    Path path = paths.isEmpty() ? null : paths.getFirst();
    if (path == null) {
      return;
    }

    PackData packData = this.getPackData(path);
    if (packData == null) {
      return;
    }

    this.navigateToEditor(packData);
  }

  private void navigateToEditor(PackData packData) {
    this.minecraft.setScreen(new PackScreen(new ScreenParent(this), this.minecraft, packData));
  }

  private void newPack(Button button) {
    this.navigateToEditor(new PackData());
  }

  private void openSample(Button button) {
    // The sample lives in the repo's top-level assets/ dir. The runtime game dir varies by loader
    // (e.g. <project>/fabric/run), so walk up a few levels to find <some ancestor>/assets/<zip>.
    Path gameDir = PathAccessor.get().getGameDir();
    Path path = null;
    for (Path dir = gameDir; dir != null && path == null; dir = dir.getParent()) {
      Path candidate = dir.resolve("assets").resolve("FamousPaintings-1.0.0.zip");
      if (Files.isRegularFile(candidate)) {
        path = candidate;
      }
    }

    if (path == null) {
      CustomPaintingsMod.LOGGER.warn("Could not find sample pack assets/FamousPaintings-1.0.0.zip near {}", gameDir);
      return;
    }

    PackData packData = this.getPackData(path);
    if (packData == null) {
      return;
    }

    this.navigateToEditor(packData);
  }

  private boolean isDirectory(Path path) {
    return Files.isDirectory(path);
  }

  private boolean isZipFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip");
  }

  private PackData getPackData(Path path) {
    if (!this.isDirectory(path) && !this.isZipFile(path)) {
      return null;
    }

    try {
      Metadata meta = PackReader.readMetadata(path);
      HashMap<CustomId, Image> images = PackReader.readPaintingImages(meta);

      List<PackData.Painting> paintings = new ArrayList<>();
      for (Painting painting : meta.pack().paintings()) {
        CustomId id = new CustomId(meta.pack().id(), painting.id());
        Image image = images.remove(id);

        paintings.add(new PackData.Painting(
            painting.id(),
            painting.name(),
            painting.artist(),
            painting.width(),
            painting.height(),
            image));
      }

      for (Map.Entry<CustomId, Image> entry : images.entrySet()) {
        CustomId id = entry.getKey();
        Image image = entry.getValue();

        int pixelWidth = image.width();
        int pixelHeight = image.height();

        paintings.add(new PackData.Painting(
            id.resource(),
            id.resource(),
            "",
            pixelWidth,
            pixelHeight,
            image));
      }

      return new PackData(
          meta.pack().id(),
          meta.pack().name(),
          meta.pack().description(),
          meta.icon(),
          paintings);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn("Failed to read metadata for {}", path, e);
      return null;
    }
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  private static class ZeroList extends ParentElementEntryListWidget<ZeroList.Entry> {
    public ZeroList(
        @NotNull Minecraft client,
        @NotNull ThreeSectionLayoutWidget layout) {
      super(client, layout);
    }

    private static class Entry extends ParentElementEntryListWidget.Entry {
      protected final Font textRenderer;

      public Entry(Font textRenderer, int index, int x, int y, int width, int contentHeight) {
        super(index, x, y, width, contentHeight);
        this.textRenderer = textRenderer;
      }
    }

    private static class BodyEntry extends Entry {
      public BodyEntry(Font textRenderer, int index, int x, int y, int width, LabelWidget widget) {
        super(textRenderer, index, x, y, width, widget.getHeight() + 4 * GuiUtil.PADDING);

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal()
                .mainAxisContentAlignCenter()
                .defaultOffAxisContentAlignCenter(),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(),
                  this.getContentTop(),
                  this.getContentWidth(),
                  this.getContentHeight());
            });

        layout.add(widget);

        layout.visitWidgets(this::addDrawableChild);
      }

      public static FlowListWidget.EntryFactory<BodyEntry> factory(
          Font textRenderer,
          LabelWidget widget) {
        return (index, left, top, width) -> new BodyEntry(
            textRenderer,
            index,
            left,
            top,
            width,
            widget);
      }
    }

    private static class ButtonEntry extends Entry {
      public ButtonEntry(Font textRenderer, int index, int x, int y, int width, Component text,
          Button widget) {
        super(textRenderer, index, x, y, width, Button.DEFAULT_HEIGHT);

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal()
                .spacing(GuiUtil.PADDING)
                .defaultOffAxisContentAlignCenter(),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(),
                  this.getContentTop(),
                  this.getContentWidth(),
                  this.getContentHeight());
            });

        layout.add(LabelWidget.builder(this.textRenderer, text)
            .alignTextLeft()
            .alignTextCenterY()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
              self.setSize(this.getLabelWidth(parent, widget), this.getContentHeight());
            });

        layout.add(widget);

        layout.visitWidgets(this::addDrawableChild);
      }

      private int getLabelWidth(LinearLayoutWidget layout, Button widget) {
        return layout.getWidth() - 2 * layout.getSpacing() - widget.getWidth();
      }

      public static FlowListWidget.EntryFactory<ButtonEntry> factory(
          Font textRenderer,
          Component text,
          Button widget) {
        return (index, left, top, width) -> new ButtonEntry(
            textRenderer,
            index,
            left,
            top,
            width,
            text,
            widget);
      }
    }
  }
}
