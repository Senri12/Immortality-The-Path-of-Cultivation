package immortality.cultivation;

import net.minecraft.network.chat.Component;

public record BreakthroughResult(BreakthroughOutcome outcome, double chance, Component message) {
}
