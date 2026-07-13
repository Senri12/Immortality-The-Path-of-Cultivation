package immortality.cultivation;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum QiFocus {
	NONE,
	HEAD,
	HANDS,
	TORSO,
	LEGS;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "focus.immortality." + id();
	}

	public Component displayName() {
		return Component.translatable(translationKey());
	}

	public static QiFocus byId(String id) {
		if (id == null || id.isBlank()) {
			return NONE;
		}
		for (QiFocus value : values()) {
			if (value.id().equalsIgnoreCase(id)) {
				return value;
			}
		}
		return NONE;
	}
}
