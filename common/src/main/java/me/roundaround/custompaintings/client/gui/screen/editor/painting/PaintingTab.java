package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import java.util.function.Consumer;

import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class PaintingTab implements Tab {
  protected final Minecraft client;
  protected final Font textRenderer;
  protected final State state;
  protected final Component title;
  protected final LinearLayoutWidget layout = LinearLayoutWidget.vertical()
      .spacing(GuiUtil.PADDING)
      .padding(GuiUtil.PADDING);

  public PaintingTab(Minecraft client, State state, Component title) {
    this.client = client;
    this.textRenderer = client.font;
    this.state = state;
    this.title = title;
  }

  @Override
  public Component getTabTitle() {
    return this.title;
  }

  @Override
  public Component getTabExtraNarration() {
    return CommonComponents.EMPTY;
  }

  @Override
  public void visitChildren(Consumer<AbstractWidget> consumer) {
    this.layout.visitWidgets(consumer);
  }

  @Override
  public void doLayout(ScreenRectangle tabArea) {
    this.layout.setPositionAndDimensions(
        tabArea.left(),
        tabArea.top(),
        tabArea.width(),
        tabArea.height());
    this.layout.arrangeElements();
  }
}
