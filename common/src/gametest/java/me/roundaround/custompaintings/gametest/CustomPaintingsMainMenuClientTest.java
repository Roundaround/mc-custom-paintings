package me.roundaround.custompaintings.gametest;

import me.roundaround.allay.api.gametest.ClientGameTest;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.trove.gametest.ClientTest;
import me.roundaround.trove.gametest.ClientTestContext;

/**
 * Opens the mod's primary menu straight from the title screen and asserts it
 * renders. Its {@code init()} handles the not-in-world case, so it stands alone
 * without a world or server connection — and exercises the 26.2 GUI-render path.
 */
@ClientGameTest
public class CustomPaintingsMainMenuClientTest implements ClientTest {
  @Override
  public void runTest(ClientTestContext context) {
    context.setScreen(() -> new MainMenuScreen(null));
    context.assertScreen(MainMenuScreen.class);
    context.waitTicks(2);
    context.returnToTitle();
  }
}
