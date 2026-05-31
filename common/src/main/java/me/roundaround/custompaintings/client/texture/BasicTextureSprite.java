package me.roundaround.custompaintings.client.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.Identifier;

import java.io.FileNotFoundException;
import java.util.Set;

public class BasicTextureSprite {
  public static SpriteContents fetch(Minecraft client, Identifier spriteId, Identifier textureId) {
    try {
      SpriteResourceLoader opener = SpriteResourceLoader.create(Set.of());
      return opener.loadSprite(spriteId, client.getResourceManager().getResourceOrThrow(textureId));
    } catch (FileNotFoundException e) {
      return MissingTextureAtlasSprite.create();
    }
  }
}
