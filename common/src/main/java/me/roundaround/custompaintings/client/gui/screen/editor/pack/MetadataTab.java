package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.trove.client.gui.icon.BuiltinIcon;
import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.client.gui.widget.FlowListWidget;
import me.roundaround.trove.client.gui.widget.IconButtonWidget;
import me.roundaround.trove.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.trove.client.gui.widget.drawable.LabelWidget;
import me.roundaround.trove.observable.Observable;
import me.roundaround.trove.observable.Subject;
import me.roundaround.trove.observable.Subscription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class MetadataTab extends PackTab {
  private static final int MIN_WIDTH = 240;

  public MetadataTab(
      @NotNull Minecraft client,
      @NotNull State state,
      @NotNull PackScreen screen) {
    super(client,
        state,
        screen,
        Component.translatable("custompaintings.editor.editor.metadata.title"));

    MetadataList list = this.layout.add(new MetadataList(
        this.client,
        this.getWidth(this.layout),
        this.layout.getHeight()),
        (parent, self) -> {
          self.setRectangle(
              this.getWidth(parent),
              parent.getHeight(),
              parent.getX(),
              parent.getY());
        });

    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.font,
        "id",
        this.state.id,
        this.state.idDirty,
        () -> this.state.getLastSaved().id(),
        this.subscriptions::add));
    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.font,
        "name",
        this.state.name,
        this.state.nameDirty,
        () -> this.state.getLastSaved().name(),
        this.subscriptions::add));
    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.font,
        "description",
        this.state.description,
        this.state.descriptionDirty,
        () -> this.state.getLastSaved().description(),
        this.subscriptions::add,
        255));

    this.layout.arrangeElements();
  }

  private int getWidth(LinearLayoutWidget layout) {
    return Math.max(MIN_WIDTH, Math.round(layout.getWidth() * 0.8f));
  }

  static class MetadataList extends ParentElementEntryListWidget<MetadataList.Entry> {
    public MetadataList(Minecraft client, int width, int height) {
      super(client, 0, 0, width, height);
      this.setContentPadding(GuiUtil.PADDING);
    }

    @Override
    protected void renderListBackground(GuiGraphicsExtractor context) {
      // Disable background
    }

    @Override
    protected void renderListBorders(GuiGraphicsExtractor context) {
      // Disable borders
    }

    static class Entry extends ParentElementEntryListWidget.Entry {
      protected static final int HEIGHT = 20;
      protected static final int CONTROL_MIN_WIDTH = 140;

      protected final Font textRenderer;

      public Entry(Font textRenderer, int index, int x, int y, int width, int contentHeight) {
        super(index, x, y, width, contentHeight);
        this.textRenderer = textRenderer;
      }
    }

    static class TextFieldEntry extends Entry {
      private final EditBox field;

      public TextFieldEntry(
          Font textRenderer,
          int index,
          int x,
          int y,
          int width,
          String id,
          Subject<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          Consumer<Subscription> addSubscription,
          int maxLength) {
        super(textRenderer, index, x, y, width, HEIGHT);

        Component label = Component.translatable("custompaintings.editor.editor.metadata." + id);

        LinearLayoutWidget layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter();

        layout.add(
            LabelWidget.builder(this.textRenderer, label)
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(),
            (parent, self) -> {
              self.setSize(parent.getUnusedSpace(self), this.getContentHeight());
            });

        this.field = layout.add(
            new EditBox(
                this.textRenderer,
                this.getControlWidth(layout),
                HEIGHT,
                label),
            (parent, self) -> {
              self.setWidth(this.getControlWidth(parent));
            });
        this.field.setMaxLength(maxLength);
        this.field.setValue(valueObservable.get());

        // TODO: If the initial value is too long show a warning tooltip

        this.field.setResponder(valueObservable::set);
        addSubscription.accept(valueObservable.subscribe((value) -> {
          String text = this.field.getValue();
          if (!text.equals(value)) {
            this.field.setValue(value);
            this.field.moveCursorToEnd(false);
            this.field.setCursorPosition(0);
            this.field.setHighlightPos(0);
          }
        }));

        IconButtonWidget resetButton = layout.add(IconButtonWidget.builder(BuiltinIcon.UNDO_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Component.translatable("custompaintings.editor.editor.revert"))
            .onPress((button) -> {
              String value = getLastSaved.get();
              if (value.length() > maxLength) {
                value = value.substring(0, maxLength);
              }
              this.field.setValue(value);
            })
            .build());
        addSubscription.accept(dirtyObservable.subscribe((dirty) -> resetButton.active = dirty));

        this.addLayout(layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        layout.visitWidgets(this::addDrawableChild);
      }

      public EditBox getField() {
        return this.field;
      }

      private int getControlWidth(LinearLayoutWidget layout) {
        return Math.max(CONTROL_MIN_WIDTH, Math.round(layout.getWidth() * 0.6f));
      }

      public static FlowListWidget.EntryFactory<TextFieldEntry> factory(
          Font textRenderer,
          String id,
          Subject<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          Consumer<Subscription> addSubscription) {
        return factory(textRenderer, id, valueObservable, dirtyObservable, getLastSaved, addSubscription, 32);
      }

      public static FlowListWidget.EntryFactory<TextFieldEntry> factory(
          Font textRenderer,
          String id,
          Subject<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          Consumer<Subscription> addSubscription,
          int maxLength) {
        return (index, left, top, width) -> new TextFieldEntry(
            textRenderer,
            index,
            left,
            top,
            width,
            id,
            valueObservable,
            dirtyObservable,
            getLastSaved,
            addSubscription,
            maxLength);
      }
    }
  }
}
