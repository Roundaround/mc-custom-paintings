package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.client.gui.widget.drawable.LabelWidget;
import me.roundaround.trove.observable.Subject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;

public class InfoTab extends PaintingTab {
  private final LabelWidget widthLabel;
  private final LabelWidget heightLabel;

  public InfoTab(
      Minecraft client,
      State state) {
    // TODO: i18n
    super(client, state, Component.nullToEmpty("Info"));

    this.widthLabel = this.layout.add(
        LabelWidget.builder(this.textRenderer, this.getWidthText())
            .hideBackground()
            .showShadow()
            .build());
    this.heightLabel = this.layout.add(
        LabelWidget.builder(this.textRenderer, this.getHeightText())
            .hideBackground()
            .showShadow()
            .build());

    // TODO: i18n
    this.addNumField(Component.nullToEmpty("Block width"), this.state.blockWidth);
    // TODO: i18n
    this.addNumField(Component.nullToEmpty("Block height"), this.state.blockHeight);

    this.state.image.subscribe(() -> {
      this.widthLabel.setText(this.getWidthText());
      this.heightLabel.setText(this.getHeightText());
      this.layout.arrangeElements();
    });
  }

  private Component getWidthText() {
    // TODO: i18n
    return Component.nullToEmpty("Width: " + this.state.image.get().width() + "px");
  }

  private Component getHeightText() {
    // TODO: i18n
    return Component.nullToEmpty("Height: " + this.state.image.get().height() + "px");
  }

  private void addNumField(
      Component label,
      Subject<Integer> obs) {
    LinearLayoutWidget row = LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING / 2);

    row.add(
        LabelWidget.builder(this.textRenderer, label)
            .hideBackground()
            .showShadow()
            .build(),
        (parent, self) -> {
          self.setWidth(parent.getUnusedSpace(self));
        });

    EditBox field = row.add(
        new EditBox(this.textRenderer, this.textRenderer.width("9999") + 10, 20, label) {
          @Override
          public boolean charTyped(CharacterEvent event) {
            if (!Character.isDigit(event.codepoint())) {
              return false;
            }
            return super.charTyped(event);
          }
        });
    field.setMaxLength(4);
    field.setValue(obs.get().toString());
    field.setResponder((text) -> {
      if (text == null || text.isEmpty()) {
        field.setTextColor(GuiUtil.ERROR_COLOR);
        return;
      }

      int value;
      try {
        value = Integer.parseInt(text);
      } catch (Exception e) {
        field.setTextColor(GuiUtil.ERROR_COLOR);
        return;
      }

      if (value > 0 && value <= 8) {
        obs.set(value);
        field.setTextColor(GuiUtil.LABEL_COLOR);
      } else {
        field.setTextColor(GuiUtil.ERROR_COLOR);
      }
    });
    obs.subscribe((i) -> {
      String text = field.getValue();
      String value = i.toString();
      if (!text.equals(value)) {
        field.setValue(value);
        field.moveCursorToEnd(false);
        field.setCursorPosition(text.length());
        field.setHighlightPos(text.length());
      }
    });

    // TODO: Step buttons

    this.layout.add(row, (parent, self) -> {
      self.setWidth(parent.getInnerWidth());
    });
  }
}
