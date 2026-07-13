package immortality.cultivation;

import immortality.beast.BeastCoreDefinition;
import net.minecraft.world.item.ItemStack;

public record BreakthroughCoreSelection(
	BeastCoreDefinition definition,
	ItemStack stack,
	int slot,
	boolean required
) {
	public boolean present() {
		return this.definition != null && !this.stack.isEmpty() && this.slot >= 0;
	}
}
