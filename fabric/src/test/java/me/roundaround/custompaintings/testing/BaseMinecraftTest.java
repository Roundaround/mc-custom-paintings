package me.roundaround.custompaintings.testing;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for tests that touch Minecraft types. Several of the classes under test
 * ({@code CustomId}, {@code Identifier}, the {@code *Data} records, anything reached through
 * {@code CustomPaintingsMod.GSON}) statically initialize Mojang/Minecraft machinery
 * (codecs, registries, {@code Identifier} validation). Calling
 * {@link Bootstrap#bootStrap()} once per JVM makes those references safe to load.
 */
public class BaseMinecraftTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }
}
