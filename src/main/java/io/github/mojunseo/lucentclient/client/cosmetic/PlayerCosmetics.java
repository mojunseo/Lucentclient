package io.github.mojunseo.lucentclient.client.cosmetic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** What one player is wearing: at most one cosmetic id per type. Unknown ids are ignored when drawing. */
public final class PlayerCosmetics {
	public static final PlayerCosmetics NONE = new PlayerCosmetics(new EnumMap<>(CosmeticType.class));

	private final Map<CosmeticType, String> equipped;

	private PlayerCosmetics(Map<CosmeticType, String> equipped) {
		this.equipped = equipped;
	}

	public @Nullable String id(CosmeticType type) {
		return equipped.get(type);
	}

	public @Nullable Cosmetic get(CosmeticType type) {
		return Cosmetics.get(type, equipped.get(type));
	}

	public PlayerCosmetics with(CosmeticType type, @Nullable String id) {
		Map<CosmeticType, String> copy = new EnumMap<>(CosmeticType.class);
		copy.putAll(equipped);
		if (id == null) {
			copy.remove(type);
		} else {
			copy.put(type, id);
		}
		return new PlayerCosmetics(copy);
	}

	public boolean isEmpty() {
		return equipped.isEmpty();
	}

	public static PlayerCosmetics fromJson(JsonObject json) {
		Map<CosmeticType, String> equipped = new EnumMap<>(CosmeticType.class);
		for (CosmeticType type : CosmeticType.values()) {
			JsonElement element = json.get(type.id());
			if (element != null && element.isJsonPrimitive()) equipped.put(type, element.getAsString());
		}
		return new PlayerCosmetics(equipped);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		equipped.forEach((type, id) -> json.addProperty(type.id(), id));
		return json;
	}
}
