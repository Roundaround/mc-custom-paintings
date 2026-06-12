package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.trove.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.trove.client.gui.scaffold.BaseScreen;
import me.roundaround.trove.client.gui.scaffold.ScreenParent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class PackScreen extends BaseScreen {
  private static final Identifier TAB_HEADER_BACKGROUND_TEXTURE = Identifier
      .withDefaultNamespace("textures/gui/tab_header_background.png");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      this::addRenderableWidget,
      this::removeWidget);

  private TabNavigationBar tabNavigation;
  private State state;

  public PackScreen(
      @NotNull ScreenParent parent,
      @NotNull Minecraft client,
      @NotNull PackData pack) {
    this(parent, client, new State(pack));
  }

  public PackScreen(
      @NotNull ScreenParent parent,
      @NotNull Minecraft client,
      @NotNull State state) {
    super(Component.translatable("custompaintings.editor.editor.title"), parent, client);
    this.state = state;
  }

  @Override
  protected void init() {
    this.tabNavigation = TabNavigationBar.builder(this.tabManager, this.width)
        .addTabs(
            new MetadataTab(this.minecraft, this.state, this),
            new PaintingsTab(this.minecraft, this.state, this),
            new MigrationsTab(this.minecraft, this.state, this))
        .build();
    this.addRenderableWidget(this.tabNavigation);

    Button doneButton = this.layout.addFooter(Button.builder(
        this.getDoneButtonMessage(this.state.dirty.get()),
        (b) -> this.onClose())
        .width(Button.BIG_WIDTH)
        .build());
    this.state.dirty.subscribe((dirty) -> doneButton.setMessage(this.getDoneButtonMessage(dirty)));

    VersionStamp.create(this.font, this.layout);

    this.layout.visitWidgets((child) -> {
      child.setTabOrderGroup(1);
      this.addRenderableWidget(child);
    });
    this.tabNavigation.selectTab(0, false);
    this.repositionElements();
  }

  @Override
  protected void repositionElements() {
    if (this.tabNavigation == null) {
      return;
    }

    this.tabNavigation.updateWidth(this.width);
    this.tabNavigation.arrangeElements();

    int headerFooterHeight = this.tabNavigation.getRectangle().bottom();
    this.layout.setHeaderHeight(headerFooterHeight);

    this.layout.arrangeElements();
    ScreenRectangle tabArea = new ScreenRectangle(
        0,
        headerFooterHeight,
        this.width,
        this.height - this.layout.getFooterHeight() - headerFooterHeight);
    this.tabManager.setTabArea(tabArea);
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
    super.extractRenderState(context, mouseX, mouseY, deltaTicks);
    context.blit(
        RenderPipelines.GUI_TEXTURED,
        FOOTER_SEPARATOR,
        0,
        this.height - this.layout.getFooterHeight(),
        0.0f,
        0.0f,
        this.width,
        2,
        32,
        2);
  }

  @Override
  protected void extractMenuBackground(GuiGraphicsExtractor context) {
    context.blit(
        RenderPipelines.GUI_TEXTURED,
        TAB_HEADER_BACKGROUND_TEXTURE,
        0,
        0,
        0.0f,
        0.0f,
        this.width,
        this.layout.getHeaderHeight(),
        16,
        16);
    this.extractMenuBackground(context, 0, this.layout.getHeaderHeight(), this.width, this.height);
  }

  @Override
  public void onClose() {
    this.state.close();
    super.onClose();
  }

  public ScreenParent getParent() {
    return this.parent;
  }

  private Component getDoneButtonMessage(boolean dirty) {
    MutableComponent text = CommonComponents.GUI_DONE.copy();
    if (dirty) {
      text.append(" *");
    }
    return text;
  }
}
