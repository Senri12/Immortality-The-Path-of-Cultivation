package immortality.mixin;

import immortality.cultivation.CultivationData;
import immortality.cultivation.CultivatorAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void immortality$copyCultivation(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		CultivationData oldData = ((CultivatorAccess) oldPlayer).immortality$getCultivationData();
		CultivationData newData = ((CultivatorAccess) this).immortality$getCultivationData();
		newData.copyFrom(oldData);
	}
}
