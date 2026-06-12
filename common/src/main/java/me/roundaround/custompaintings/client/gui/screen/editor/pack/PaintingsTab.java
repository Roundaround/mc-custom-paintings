package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.client.gui.screen.editor.painting.PaintingScreen;
import me.roundaround.custompaintings.client.gui.widget.ImageButtonWidget;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.trove.client.gui.icon.BuiltinIcon;
import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.scaffold.ScreenParent;
import me.roundaround.trove.client.gui.util.Axis;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.client.gui.widget.EmptyWidget;
import me.roundaround.trove.client.gui.widget.FlowListWidget;
import me.roundaround.trove.client.gui.widget.IconButtonWidget;
import me.roundaround.trove.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.trove.client.gui.widget.drawable.LabelWidget;
import me.roundaround.trove.observable.Observable;
import me.roundaround.trove.observable.Subject;
import me.roundaround.trove.observable.Subscription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class PaintingsTab extends PackTab {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final int BUTTON_HEIGHT = 20;

  private final LabelWidget countLabel;
  private final EditBox searchBox;
  private final PaintingList paintingList;

  public PaintingsTab(
      @NotNull Minecraft client,
      @NotNull State state,
      @NotNull PackScreen screen) {
    super(
        client,
        state,
        screen,
        Component.translatable("custompaintings.editor.editor.paintings.title"));

    this.layout.flowAxis(Axis.HORIZONTAL)
        .spacing(GuiUtil.PADDING)
        .padding(GuiUtil.PADDING);

    LinearLayoutWidget sidePanel = LinearLayoutWidget.vertical()
        .spacing(GuiUtil.PADDING);

    this.countLabel = sidePanel.add(LabelWidget.builder(this.client.font, Component.nullToEmpty("0"))
        .hideBackground()
        .showShadow()
        .build());
    this.subscriptions.add(this.state.paintings.subscribe((paintings) -> {
      this.countLabel.setText(Component.nullToEmpty(String.format("%d", paintings.size())));
    }));

    this.layout.add(sidePanel, (parent, self) -> {
      self.setDimensions(this.getPanelWidth(this.layout), parent.getInnerHeight());
    });

    LinearLayoutWidget listColumn = LinearLayoutWidget.vertical()
        .defaultOffAxisContentAlignCenter()
        .spacing(GuiUtil.PADDING);

    LinearLayoutWidget searchRow = listColumn.add(
        LinearLayoutWidget
            .horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter(),
        (parent, self) -> {
          self.setDimensions(listColumn.getWidth(), BUTTON_HEIGHT + GuiUtil.PADDING);
        });

    this.searchBox = searchRow.add(
        new EditBox(
            this.client.font,
            0,
            BUTTON_HEIGHT,
            Component.translatable("custompaintings.editor.editor.paintings.search")),
        (parent, self) -> {
          self.setWidth(parent.getUnusedSpace(self));
        });
    this.searchBox.setResponder(this::onSearchBoxChanged);

    searchRow.add(IconButtonWidget.builder(BuiltinIcon.CLOSE_13, Constants.MOD_ID)
        .medium()
        .messageAndTooltip(Component.translatable("custompaintings.editor.editor.paintings.clear"))
        .onPress(this::clearSearch)
        .build());

    this.paintingList = listColumn.add(new PaintingList(
        this.client,
        listColumn.getInnerWidth(),
        listColumn.getInnerHeight(),
        this.state.paintings,
        this::editInfo,
        this::editImage,
        this.state::movePaintingUp,
        this.state::movePaintingDown,
        this.subscriptions::add),
        (parent, self) -> {
          self.setSize(parent.getInnerWidth(), parent.getUnusedSpace(self));
        });

    this.layout.add(listColumn, (parent, self) -> {
      self.setDimensions(
          parent.getUnusedSpace(self),
          parent.getInnerHeight());
    });

    this.layout.arrangeElements();
  }

  private int getPanelWidth(LinearLayoutWidget layout) {
    return Math.max(PANEL_MIN_WIDTH, Math.round(layout.getInnerWidth() * 0.3f));
  }

  private void onSearchBoxChanged(String text) {
    this.paintingList.setSearch(text);
  }

  private void clearSearch(Button button) {
    this.searchBox.setValue("");
  }

  private void editInfo(int paintingIndex) {
    CustomPaintingsMod.LOGGER.info("Editing info for painting {}", paintingIndex);
  }

  private void editImage(int paintingIndex) {
    List<PackData.Painting> paintings = this.state.paintings.get();
    if (paintingIndex < 0 || paintingIndex >= paintings.size()) {
      return;
    }

    this.client.setScreen(new PaintingScreen(
        Component.nullToEmpty("Edit image"),
        new ScreenParent(() -> new PackScreen(
            this.screen.getParent(),
            this.client,
            this.state)),
        this.client,
        paintings.get(paintingIndex),
        (painting) -> {
          this.state.setPainting(paintingIndex, painting);
        }));
  }

  record FilteredState(
      String search,
      int totalCount,
      List<PackData.Painting> filtered,
      Map<Integer, Integer> indexMap) {
  }

  static class PaintingList extends ParentElementEntryListWidget<PaintingList.Entry> {
    private final Subject<List<PackData.Painting>> paintings;
    private final Subject<String> search = Subject.of("");

    public PaintingList(
        Minecraft client,
        int width,
        int height,
        Subject<List<PackData.Painting>> observable,
        Consumer<Integer> editCallback,
        Consumer<Integer> imageCallback,
        Consumer<Integer> moveUpCallback,
        Consumer<Integer> moveDownCallback,
        Consumer<Subscription> addSubscription) {
      super(client, 0, 0, width, height);
      this.setContentPadding(GuiUtil.PADDING);
      this.setRowSpacing(GuiUtil.PADDING / 2);

      this.paintings = observable;

      addSubscription.accept(Observable.subscribeAll(this.search, this.paintings, (search, paintings) -> {
        int totalCount = paintings.size();
        List<PackData.Painting> filtered = new ArrayList<>();
        Map<Integer, Integer> indexMap = new HashMap<>();

        for (int paintingIdx = 0; paintingIdx < paintings.size(); paintingIdx++) {
          PackData.Painting painting = paintings.get(paintingIdx);
          if (this.matches(search, painting)) {
            indexMap.put(filtered.size(), paintingIdx);
            filtered.add(painting);
          }
        }

        int filteredCount = filtered.size();
        if (filteredCount > this.getEntryCount()) {
          for (int i = this.getEntryCount(); i < filteredCount; i++) {
            this.addEntry(Entry.factory(
                this.client.font,
                imageCallback,
                editCallback,
                moveUpCallback,
                moveDownCallback,
                filtered.get(i),
                indexMap.get(i),
                totalCount));
          }
        } else {
          for (int i = this.getEntryCount(); i > filteredCount; i--) {
            this.removeEntry();
          }
        }

        // TODO: Handle filtered count == 0

        int maxIndexWidth = IntStream.range(1, totalCount)
            .map((i) -> this.client.font.width(Component.nullToEmpty(String.format("%d", i))))
            .max()
            .orElse(1);
        for (int i = 0; i < this.getEntryCount(); i++) {
          this.getEntry(i).setData(
              filtered.get(i),
              indexMap.get(i),
              maxIndexWidth,
              totalCount);
        }
      }));
    }

    public void setSearch(String search) {
      this.search.set(search);
    }

    private boolean matches(String search, PackData.Painting painting) {
      String query = this.sanitize(search);
      if (query.isBlank()) {
        return true;
      }

      return Stream.of(painting.id(), painting.name(), painting.artist())
          .map(this::sanitize)
          .anyMatch((value) -> value.contains(query));
    }

    private String sanitize(String text) {
      return text.toLowerCase().replace(" ", "");
    }

    static class Entry extends ParentElementEntryListWidget.Entry {
      private static final Component LINE_ID = Component.translatable("custompaintings.editor.editor.paintings.id");
      private static final Component LINE_NAME = Component.translatable("custompaintings.editor.editor.paintings.name");
      private static final Component LINE_ARTIST = Component.translatable(
          "custompaintings.editor.editor.paintings.artist");
      private static final Component LINE_BLOCKS = Component.translatable(
          "custompaintings.editor.editor.paintings.blocks");

      private final Font textRenderer;
      private final Consumer<Integer> imageCallback;
      private final Consumer<Integer> editCallback;
      private final Consumer<Integer> moveUpCallback;
      private final Consumer<Integer> moveDownCallback;
      private final LinearLayoutWidget layout;
      private final LabelWidget idLabel;
      private final LabelWidget nameLabel;
      private final LabelWidget artistLabel;
      private final LabelWidget blocksLabel;
      private final ImageButtonWidget imageButton;
      private final IconButtonWidget moveUpButton;
      private final IconButtonWidget moveDownButton;
      private final LabelWidget indexLabel;

      private PackData.Painting painting;
      private int paintingIndex;

      public Entry(
          Font textRenderer,
          int index,
          int left,
          int top,
          int width,
          Consumer<Integer> imageCallback,
          Consumer<Integer> editCallback,
          Consumer<Integer> moveUpCallback,
          Consumer<Integer> moveDownCallback,
          PackData.Painting painting,
          int paintingIndex,
          int totalCount) {
        super(index, left, top, width, textRenderer.lineHeight * 4 + 3);
        this.textRenderer = textRenderer;
        this.imageCallback = imageCallback;
        this.editCallback = editCallback;
        this.moveUpCallback = moveUpCallback;
        this.moveDownCallback = moveDownCallback;
        this.painting = painting;
        this.paintingIndex = paintingIndex;

        this.layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.imageButton = this.layout.add(
            new ImageButtonWidget(
                Component.translatable("custompaintings.editor.editor.paintings.image"),
                (button) -> this.imageCallback.accept(this.paintingIndex),
                (image) -> State.getImageTextureId(image),
                this.painting.image()),
            (parent, self) -> {
              self.setSize(this.getContentHeight(), this.getContentHeight());
            });

        this.layout.add(new EmptyWidget());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical()
            .spacing(1)
            .defaultOffAxisContentAlignStart();
        int headerWidth = Stream.of(LINE_ID, LINE_NAME, LINE_ARTIST, LINE_BLOCKS)
            .mapToInt(this.textRenderer::width)
            .max()
            .orElse(1);
        this.idLabel = this.textLine(textSection, headerWidth, LINE_ID, this.painting.id());
        this.nameLabel = this.textLine(textSection, headerWidth, LINE_NAME, this.painting.name());
        this.artistLabel = this.textLine(textSection, headerWidth, LINE_ARTIST, this.painting.artist());
        this.blocksLabel = this.textLine(textSection, headerWidth, LINE_BLOCKS, this.getBlocksText());
        this.layout.add(textSection, (parent, self) -> {
          self.setWidth(parent.getUnusedSpace(self));
        });

        this.layout.add(IconButtonWidget.builder(BuiltinIcon.SLIDERS_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Component.translatable("custompaintings.editor.editor.paintings.edit"))
            .onPress((button) -> this.editCallback.accept(this.paintingIndex))
            .build());

        this.layout.add(new EmptyWidget());

        this.indexLabel = this.layout
            .add(LabelWidget.builder(this.textRenderer, Component.nullToEmpty(String.format("%d", this.index + 1)))
                .hideBackground()
                .showShadow()
                .alignTextRight()
                .build());

        LinearLayoutWidget moveControls = LinearLayoutWidget.vertical()
            .spacing(GuiUtil.PADDING / 2);
        this.moveUpButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.UP_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Component.translatable("custompaintings.editor.editor.paintings.up"))
            .onPress((button) -> this.moveUpCallback.accept(this.paintingIndex))
            .build());
        this.moveUpButton.active = this.paintingIndex > 0;
        this.moveDownButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.DOWN_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Component.translatable("custompaintings.editor.editor.paintings.down"))
            .onPress((button) -> this.moveDownCallback.accept(this.paintingIndex))
            .build());
        this.moveDownButton.active = this.paintingIndex < totalCount - 1;
        this.layout.add(moveControls);

        this.addLayout(this.layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        this.layout.visitWidgets(this::addDrawableChild);
      }

      private LabelWidget textLine(
          LinearLayoutWidget textSection,
          int headerWidth,
          Component header,
          String value) {
        return this.textLine(textSection, headerWidth, header, Component.nullToEmpty(value));
      }

      private LabelWidget textLine(
          LinearLayoutWidget textSection,
          int headerWidth,
          Component header,
          Component value) {
        LinearLayoutWidget line = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING / 2);
        line.add(LabelWidget.builder(this.textRenderer, header)
            .hideBackground()
            .showShadow()
            .width(headerWidth)
            .color(CommonColors.LIGHT_GRAY)
            .build());
        LabelWidget valueLabel = line.add(
            LabelWidget.builder(this.textRenderer, value)
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(),
            (parent, self) -> {
              self.setWidth(parent.getWidth() - parent.getSpacing() - headerWidth);
            });
        textSection.add(line, (parent, self) -> {
          self.setWidth(parent.getWidth());
        });
        return valueLabel;
      }

      private Component getBlocksText() {
        return Component.translatable(
            "custompaintings.editor.editor.paintings.blocks.value",
            this.painting.blockWidth(),
            this.painting.blockHeight());
      }

      public void setData(
          PackData.Painting painting,
          int paintingIndex,
          int indexWidth,
          int totalCount) {
        this.painting = painting;
        this.paintingIndex = paintingIndex;

        this.idLabel.setText(Component.nullToEmpty(this.painting.id()));
        this.nameLabel.setText(Component.nullToEmpty(this.painting.name()));
        this.artistLabel.setText(Component.nullToEmpty(this.painting.artist()));
        this.blocksLabel.setText(this.getBlocksText());
        this.imageButton.setImage(this.painting.image());
        this.moveUpButton.active = this.paintingIndex > 0;
        this.moveDownButton.active = this.paintingIndex < totalCount - 1;
        this.indexLabel.batchUpdates(() -> {
          this.indexLabel.setText(Component.nullToEmpty(String.format("%d", this.paintingIndex + 1)));
          this.indexLabel.setWidth(indexWidth);
        });

        this.arrangeElements();
      }

      public PackData.Painting getPainting() {
        return this.painting;
      }

      public static FlowListWidget.EntryFactory<Entry> factory(
          Font textRenderer,
          Consumer<Integer> imageCallback,
          Consumer<Integer> editCallback,
          Consumer<Integer> moveUpCallback,
          Consumer<Integer> moveDownCallback,
          PackData.Painting painting,
          int paintingIndex,
          int totalCount) {
        return (index, left, top, width) -> new Entry(
            textRenderer,
            index,
            left,
            top,
            width,
            imageCallback,
            editCallback,
            moveUpCallback,
            moveDownCallback,
            painting,
            paintingIndex,
            totalCount);
      }
    }
  }
}
