package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.trove.client.gui.util.GuiUtil;
import me.roundaround.trove.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.trove.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class VersionStamp {
  private VersionStamp() {
  }

  public static LabelWidget create(Font textRenderer, ThreeSectionLayoutWidget layout) {
    Component version = Component.nullToEmpty("v" + Constants.VERSION);
    return layout.addNonPositioned(LabelWidget.builder(textRenderer, version)
        .hideBackground()
        .showShadow()
        .alignSelfRight()
        .alignSelfTop()
        .alignTextRight()
        .build(), (parent, self) -> self.setPosition(layout.getWidth() - GuiUtil.PADDING, GuiUtil.PADDING));
  }
}
