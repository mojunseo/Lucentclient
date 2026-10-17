package io.github.mojunseo.lucentclient.client.cosmetic;

import io.github.mojunseo.lucentclient.LucentClient;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * A cosmetic bundled with the mod. {@code model} names a model in CosmeticModels (capes use the
 * cloth renderer instead). {@code texture} is an asset id, e.g. {@code lucentclient:cosmetic/cape/flame}
 * for {@code textures/cosmetic/cape/flame.png}. {@code tint} is an ARGB color multiplied into the texture.
 */
//? if >=26.1 {
public record Cosmetic(String id, CosmeticType type, String model, Identifier texture, int tint) {
	//?} else {
	/*public record Cosmetic(String id, CosmeticType type, String model, ResourceLocation texture, int tint) {
	*///?}
	static Cosmetic of(CosmeticType type, String id, String model, String texture, int tint) {
		return new Cosmetic(id, type, model, LucentClient.id("cosmetic/" + type.id() + "/" + texture), tint);
	}

	//? if >=26.1 {
	public Identifier texturePath() {
		return texture.withPath(path -> "textures/" + path + ".png");
	}
	//?} else {
	/*public ResourceLocation texturePath() {
		return texture.withPath(path -> "textures/" + path + ".png");
	}
	*///?}

	public Component displayName() {
		return Component.translatable("cosmetic.lucentclient." + type.id() + "." + id);
	}
}
