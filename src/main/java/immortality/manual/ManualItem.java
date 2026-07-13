package immortality.manual;

import immortality.cultivation.CultivationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ManualItem extends Item {
	private final String manualId;

	public ManualItem(String manualId, Properties properties) {
		super(properties.stacksTo(1));
		this.manualId = manualId;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			CultivationManager.useManual(serverPlayer, this.manualId);
			CultivationManager.openManualScreen(serverPlayer, this.manualId);
		}
		return InteractionResult.SUCCESS;
	}
}
