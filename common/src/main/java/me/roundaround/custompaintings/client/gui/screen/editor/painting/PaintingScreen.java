package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.trove.client.gui.icon.BuiltinIcon;
import me.roundaround.trove.client.gui.layout.FillerWidget;
import me.roundaround.trove.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.trove.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.trove.client.gui.scaffold.BaseScreen;
import me.roundaround.trove.client.gui.scaffold.ScreenParent;
import me.roundaround.trove.client.gui.util.Axis;
import me.roundaround.trove.client.gui.util.FloatRect;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.client.gui.util.IntRect;
import me.roundaround.trove.client.gui.widget.IconButtonWidget;
import me.roundaround.trove.client.gui.widget.drawable.DrawableWidget;
import me.roundaround.trove.observable.Observable;
import me.roundaround.trove.observable.Subject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

public class PaintingScreen extends BaseScreen {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final Identifier IMAGE_TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_editor");
  private static final Identifier SHADOW_TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shadow_8px");
  // TODO: i18n
  private static final Component SHOW_BACKGROUND_TEXT = Component.nullToEmpty("Show background");
  // TODO: i18n
  private static final Component HIDE_BACKGROUND_TEXT = Component.nullToEmpty("Hide background");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      (element) -> this.addRenderableWidget(element),
      (child) -> this.removeWidget(child));
  private final Consumer<PackData.Painting> saveCallback;
  private final State state;
  private final DynamicTexture texture;
  private final Subject<IntRect> imageRegionBounds = Subject.of(null);
  private final Subject<Boolean> showBackground = Subject.of(true);

  private TabNavigationBar tabNavigation;
  private InfoTab infoTab;
  private ImageTab imageTab;
  private FillerWidget tabRegion;
  private IntRect frameBounds;
  private float pixelsPerBlock;
  private FloatRect imageBounds;
  private Background background = Background.DARK_OAK;

  public PaintingScreen(
      @NotNull Component title,
      @NotNull ScreenParent parent,
      @NotNull Minecraft client,
      PackData.Painting painting,
      Consumer<PackData.Painting> saveCallback) {
    super(title, parent, client);
    this.saveCallback = saveCallback;

    this.state = new State(painting);
    this.texture = new DynamicTexture(() -> "Image Editor", getNativeImage(painting.image()));
    this.minecraft.getTextureManager().register(IMAGE_TEXTURE, this.texture);
  }

  @Override
  public void init() {
    this.infoTab = new InfoTab(this.minecraft, this.state);
    this.imageTab = new ImageTab(this.minecraft, this.state);
    this.tabNavigation = TabNavigationBar.builder(this.tabManager, this.width)
        .addTabs(this.infoTab, this.imageTab)
        .build();
    this.addRenderableWidget(this.tabNavigation);

    this.layout.getBody()
        .flowAxis(Axis.HORIZONTAL)
        .spacing(GuiUtil.PADDING)
        .padding(GuiUtil.PADDING);

    this.tabRegion = this.layout.addBody(FillerWidget.empty(), (parent, self) -> {
      self.setDimensions(this.getPanelWidth(parent), parent.getInnerHeight());
    });

    LinearLayoutWidget imageRegion = this.layout.addBody(
        LinearLayoutWidget.vertical()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter(),
        (parent, self) -> {
          self.setDimensions(parent.getUnusedSpace(self), parent.getInnerHeight());
        });

    imageRegion.add(new ImagePreviewWidget(), (parent, self) -> {
      self.setSize(parent.getInnerWidth(), parent.getUnusedSpace(self));
      this.imageRegionBounds.set(IntRect.fromWidget(self));
    });

    LinearLayoutWidget buttonRow = LinearLayoutWidget.horizontal()
        .spacing(GuiUtil.PADDING);

    IconButtonWidget showBackgroundButton = buttonRow.add(
        IconButtonWidget.builder(BuiltinIcon.SHOW_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(HIDE_BACKGROUND_TEXT)
            .onPress((button) -> {
              this.showBackground.update((showBackground) -> !showBackground);
            })
            .build());
    // TODO: i18n
    IconButtonWidget changeBackgroundButton = buttonRow.add(
        IconButtonWidget.builder(BuiltinIcon.BRUSH_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Component.nullToEmpty("Change background texture"))
            .onPress((button) -> {
              this.background = this.background.next();
            })
            .build());

    this.showBackground.subscribe((showBackground) -> {
      showBackgroundButton.setTexture(showBackground ? BuiltinIcon.HIDE_18 : BuiltinIcon.SHOW_18, Constants.MOD_ID);
      Component message = showBackground ? HIDE_BACKGROUND_TEXT : SHOW_BACKGROUND_TEXT;
      showBackgroundButton.setMessage(message);
      showBackgroundButton.setTooltip(Tooltip.create(message));

      changeBackgroundButton.active = showBackground;
    });

    imageRegion.add(buttonRow);

    this.layout.addFooter(Button.builder(
        CommonComponents.GUI_DONE,
        (b) -> {
          this.saveCallback.accept(this.state.getPainting());
          this.onClose();
        })
        .build());
    this.layout.addFooter(Button.builder(
        CommonComponents.GUI_CANCEL,
        (b) -> this.onClose())
        .build());

    VersionStamp.create(this.font, this.layout);

    this.layout.visitWidgets((child) -> {
      child.setTabOrderGroup(1);
      this.addRenderableWidget(child);
    });
    this.tabNavigation.selectTab(0, false);
    this.repositionElements();

    this.state.image.subscribe((image) -> {
      this.texture.setPixels(getNativeImage(image));
      this.texture.upload();
      this.repositionElements();
    });

    Observable.subscribeAll(
        this.imageRegionBounds,
        this.state.blockWidth,
        this.state.blockHeight,
        this.showBackground,
        (region, width, height, showBackground) -> {
          if (region == null) {
            return;
          }

          int regionWidth = region.getWidth();
          int regionHeight = region.getHeight();
          int blockWidth = width + (showBackground ? 2 : 0);
          int blockHeight = height + (showBackground ? 2 : 0);

          float scale = Math.min(
              (float) regionWidth / blockWidth,
              (float) regionHeight / blockHeight);
          int scaledWidth = Math.round(scale * blockWidth);
          int scaledHeight = Math.round(scale * blockHeight);

          this.pixelsPerBlock = (float) scaledWidth / blockWidth;
          this.frameBounds = IntRect.byDimensions(
              region.left() + (regionWidth - scaledWidth) / 2,
              region.top() + (regionHeight - scaledHeight) / 2,
              scaledWidth,
              scaledHeight);
          this.imageBounds = this.frameBounds
              .toFloatRect()
              .reduce(showBackground ? this.pixelsPerBlock : 0);

          this.layout.arrangeElements();
        });
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
        this.tabRegion.getX(),
        this.tabRegion.getY(),
        this.tabRegion.getWidth(),
        this.tabRegion.getHeight());
    this.tabManager.setTabArea(tabArea);
  }

  @Override
  public void onClose() {
    this.state.close();
    super.onClose();
  }

  @Override
  public void removed() {
    this.minecraft.getTextureManager().release(IMAGE_TEXTURE);
    this.texture.close();
  }

  private int getPanelWidth(LinearLayoutWidget layout) {
    return Math.max(PANEL_MIN_WIDTH, Math.round(layout.getInnerWidth() * 0.3f));
  }

  private static NativeImage getNativeImage(Image image) {
    if (image == null) {
      return Image.empty().toNativeImage();
    }
    return image.toNativeImage();
  }

  private class ImagePreviewWidget extends DrawableWidget {
    private ImagePreviewWidget() {
      super(0, 0, 0, 0);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      if (PaintingScreen.this.imageBounds == null) {
        return;
      }

      IntRect imageBounds = PaintingScreen.this.imageBounds.round();

      if (!PaintingScreen.this.showBackground.get()) {
        GuiUtil.drawStrokedRectangle(context, imageBounds, CommonColors.BLACK, true);
        drawImage(context, imageBounds);
        return;
      }

      GuiUtil.drawStrokedRectangle(context, PaintingScreen.this.frameBounds, CommonColors.BLACK, true);

      float pixelsPerBlock = PaintingScreen.this.pixelsPerBlock;
      for (int x = 0; x < PaintingScreen.this.state.blockWidth.get() + 2; x++) {
        for (int y = 0; y < PaintingScreen.this.state.blockHeight.get() + 2; y++) {
          float posX = PaintingScreen.this.frameBounds.left() + (x * pixelsPerBlock);
          float posY = PaintingScreen.this.frameBounds.top() + (y * pixelsPerBlock);
          GuiUtil.drawTexturedQuad(
              context,
              PaintingScreen.this.background.get(),
              Math.round(posX),
              Math.round(posX + pixelsPerBlock),
              Math.round(posY),
              Math.round(posY + pixelsPerBlock));
        }
      }

      float shadow1Size = 2f * pixelsPerBlock / 16; // 2 "block pixels"
      GuiUtil.drawSpriteNineSliced(
          context,
          RenderPipelines.GUI_TEXTURED,
          SHADOW_TEXTURE,
          Math.round(PaintingScreen.this.imageBounds.left() - shadow1Size),
          Math.round(PaintingScreen.this.imageBounds.top() - shadow1Size),
          Math.round(PaintingScreen.this.imageBounds.getWidth() + shadow1Size * 2),
          Math.round(PaintingScreen.this.imageBounds.getHeight() + shadow1Size * 2),
          32,
          32,
          GuiUtil.genColorInt(1f, 1f, 1f, 0.15f),
          8);

      float shadow2Size = 1.25f * pixelsPerBlock / 16; // 1.25 "block pixels"
      GuiUtil.drawSpriteNineSliced(
          context,
          RenderPipelines.GUI_TEXTURED,
          SHADOW_TEXTURE,
          Math.round(PaintingScreen.this.imageBounds.left() - shadow2Size),
          Math.round(PaintingScreen.this.imageBounds.top() - shadow2Size),
          Math.round(PaintingScreen.this.imageBounds.getWidth() + shadow2Size * 2),
          Math.round(PaintingScreen.this.imageBounds.getHeight() + shadow2Size * 2),
          32,
          32,
          GuiUtil.genColorInt(1f, 1f, 1f, 0.15f),
          8);

      drawImage(context, imageBounds);
    }

    private void drawImage(GuiGraphicsExtractor context, IntRect imageBounds) {
      GuiUtil.drawTexturedQuad(
          context,
          IMAGE_TEXTURE,
          imageBounds.left(),
          imageBounds.right(),
          imageBounds.top(),
          imageBounds.bottom());
    }
  }

  private enum Background {
    DARK_OAK(Identifier.withDefaultNamespace("textures/block/dark_oak_planks.png")),
    DEEPSLATE(Identifier.withDefaultNamespace("textures/block/deepslate_bricks.png")),
    QUARTZ(Identifier.withDefaultNamespace("textures/block/quartz_block_side.png"));

    private Identifier id;

    Background(Identifier id) {
      this.id = id;
    }

    public Identifier get() {
      return this.id;
    }

    public Background next() {
      return values()[(this.ordinal() + 1) % values().length];
    }
  }
}
