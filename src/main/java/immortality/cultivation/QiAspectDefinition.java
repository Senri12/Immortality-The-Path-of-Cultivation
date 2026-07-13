package immortality.cultivation;

import java.util.List;
import net.minecraft.network.chat.Component;

public record QiAspectDefinition(
	String id,
	List<String> connections
) {
	public Component titleComponent() {
		return Component.translatable("aspect.immortality." + this.id);
	}

	public boolean connectsTo(String otherId) {
		return this.id.equals(otherId) || this.connections.contains(otherId);
	}
}
