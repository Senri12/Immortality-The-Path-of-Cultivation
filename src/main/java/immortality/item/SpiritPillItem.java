package immortality.item;

import immortality.cultivation.CultivationData;
import immortality.cultivation.CultivationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

public class SpiritPillItem extends Item {
	private final int qiBonus;
	private final double purityBonus;
	private final double stabilityBonus;

	public SpiritPillItem(int qiBonus, double purityBonus, double stabilityBonus, Properties properties) {
		super(properties);
		this.qiBonus = qiBonus;
		this.purityBonus = purityBonus;
		this.stabilityBonus = stabilityBonus;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
		ItemStack result = super.finishUsingItem(stack, level, livingEntity);
		if (!level.isClientSide() && livingEntity instanceof ServerPlayer serverPlayer) {
			CultivationData data = CultivationManager.get(serverPlayer);
			data.addQi(this.qiBonus);
			data.addPurity(this.purityBonus);
			data.addStability(this.stabilityBonus);
			CultivationManager.sync(serverPlayer);
		}
		return result;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 32;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}
}
