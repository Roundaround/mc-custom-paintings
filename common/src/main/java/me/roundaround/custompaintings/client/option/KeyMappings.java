package me.roundaround.custompaintings.client.option;

import com.mojang.blaze3d.platform.InputConstants;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.trove.client.KeyBindings;
import me.roundaround.trove.event.ClientLifecycle;
import me.roundaround.trove.event.ScreenInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {
  public static KeyMapping openMenu;

  private KeyMappings() {
  }

  public static void register() {
    openMenu = KeyBindings.register(new KeyMapping(
        "custompaintings.key.openMainMenu",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        KeyMapping.Category.MISC
    ));

    ClientLifecycle.onTick(() -> {
      Minecraft client = Minecraft.getInstance();
      while (openMenu.consumeClick()) {
        client.gui.setScreen(new MainMenuScreen(client.gui.screen()));
      }
    });

    ScreenInput.subscribe((screen, input) -> {
      Minecraft client = Minecraft.getInstance();
      if (client.gui.screen() != null && !(client.gui.screen() instanceof TitleScreen)) {
        return false;
      }
      if (openMenu.matches(input)) {
        client.gui.setScreen(new MainMenuScreen(client.gui.screen()));
        return true;
      }
      return false;
    });
  }
}
