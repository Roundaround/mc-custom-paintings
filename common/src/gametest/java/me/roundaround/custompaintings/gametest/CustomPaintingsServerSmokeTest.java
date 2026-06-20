package me.roundaround.custompaintings.gametest;

import me.roundaround.allay.api.gametest.ServerGameTest;
import me.roundaround.trove.gametest.ServerSmokeTest;

/**
 * Asserts Custom Paintings boots a dedicated server cleanly and loads no client code.
 * The body lives in {@link ServerSmokeTest}; this opt-in subclass just carries the
 * {@code @ServerGameTest} marker the build-time scan discovers.
 */
@ServerGameTest
public class CustomPaintingsServerSmokeTest extends ServerSmokeTest {
}
