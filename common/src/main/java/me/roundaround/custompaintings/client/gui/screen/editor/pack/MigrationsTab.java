package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import org.jetbrains.annotations.NotNull;

import me.roundaround.trove.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MigrationsTab extends PackTab {
  public MigrationsTab(
      @NotNull Minecraft client,
      @NotNull State state,
      @NotNull PackScreen screen) {
    super(client,
        state,
        screen,
        Component.translatable("custompaintings.editor.editor.migrations.title"));

    this.layout.add(
        LabelWidget.builder(this.client.font, Component.nullToEmpty("Migrations"))
            .hideBackground()
            .showShadow()
            .build(),
        (parent, self) -> self.setWidth(this.getContentWidth()));
  }
}
