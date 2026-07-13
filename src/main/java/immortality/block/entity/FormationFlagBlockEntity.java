package immortality.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FormationFlagBlockEntity extends BlockEntity {
	private int currentQi = 0;

	public FormationFlagBlockEntity(BlockPos pos, BlockState state) {
		super(immortality.Immortality.FORMATION_FLAG_ENTITY, pos, state);
	}

	public int getMaxQi() {
		if (this.getBlockState().is(immortality.Immortality.JADE_FLAG)) {
			return 500;
		}
		return 100;
	}

	public int getRange() {
		if (this.getBlockState().is(immortality.Immortality.JADE_FLAG)) {
			return 24;
		}
		return 12;
	}

	public int getCurrentQi() {
		return this.currentQi;
	}

	public void setCurrentQi(int currentQi) {
		this.currentQi = Math.max(0, Math.min(getMaxQi(), currentQi));
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public int addQi(int amount) {
		int before = this.currentQi;
		setCurrentQi(this.currentQi + amount);
		return this.currentQi - before;
	}

	@Override
	protected void loadAdditional(ValueInput tag) {
		super.loadAdditional(tag);
		this.currentQi = tag.getIntOr("Qi", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput tag) {
		super.saveAdditional(tag);
		tag.putInt("Qi", this.currentQi);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
