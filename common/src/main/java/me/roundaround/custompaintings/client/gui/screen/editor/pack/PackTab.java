package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.util.Axis;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.observable.Subscription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public abstract class PackTab implements Tab {
  protected static final int PREFERRED_WIDTH = 300;

  protected final @NotNull Minecraft client;
  protected final @NotNull State state;
  protected final @NotNull PackScreen screen;
  protected final @NotNull Component title;
  protected final LinearLayoutWidget layout = new LinearLayoutWidget(Axis.VERTICAL)
      .mainAxisContentAlignStart()
      .defaultOffAxisContentAlignCenter()
      .spacing(0);
  protected final ArrayList<Subscription> subscriptions = new ArrayList<>();

  protected PackTab(
      @NotNull Minecraft client,
      @NotNull State state,
      @NotNull PackScreen screen,
      @NotNull Component title) {
    this.client = client;
    this.state = state;
    this.screen = screen;
    this.title = title;
  }

  @Override
  public Component getTabTitle() {
    return this.title;
  }

  @Override
  public Component getTabExtraNarration() {
    return Component.empty();
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

  protected int getContentWidth() {
    return Math.min(PREFERRED_WIDTH, this.layout.getWidth() - 2 * GuiUtil.PADDING);
  }

  public void clearSubscriptions() {
    this.subscriptions.forEach(Subscription::close);
    this.subscriptions.clear();
  }

  public void beforeLoad() {
  }

  public void beforeUnload() {
    this.clearSubscriptions();
  }
}
