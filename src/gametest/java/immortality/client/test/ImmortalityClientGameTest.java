package immortality.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.animal.sheep.Sheep;
import immortality.Immortality;
import immortality.cultivation.CultivationManager;
import immortality.cultivation.CultivationStage;
import immortality.client.AltarResearchScreen;
import immortality.client.BreakthroughScreen;
import immortality.client.ManualScreen;
import immortality.client.QiFocusScreen;
import immortality.client.ResearchStudyScreen;
import immortality.client.TechniqueScreen;

@SuppressWarnings("UnstableApiUsage")
public class ImmortalityClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientWorld().waitForChunksRender();
            
            // Fetch ServerPlayer instance using computeOnServer (safe across client/server threads)
            ServerPlayer player = singleplayer.getServer().computeOnServer(server -> 
                server.getPlayerList().getPlayers().get(0)
            );
            
            // Set up player cultivation state to populate the screens with data
            singleplayer.getServer().runOnServer(server -> {
                CultivationManager.debugSetStage(player, CultivationStage.CORE_FORMATION);
                CultivationManager.debugGrantAllInsights(player);
                CultivationManager.debugUnlockAllResearches(player);
                CultivationManager.debugGrantAllTechniques(player);
                CultivationManager.debugPrepareResearch(player, "qi_sense");
            });
            
            // Wait for server tick updates to sync player data
            context.waitTicks(20);
            context.takeScreenshot("hud_screen");
            
            // Test 1: Altar Research Screen
            context.runOnClient(client -> {
                CompoundTag data = CultivationManager.buildResearchScreenData(player);
                client.setScreen(new AltarResearchScreen(data));
            });
            context.waitTicks(30);
            context.takeScreenshot("altar_screen");
            
            // Test 2: Manual Screen
            context.runOnClient(client -> {
                CompoundTag data = CultivationManager.buildManualScreenData(player, "book_of_omniscience");
                client.setScreen(new ManualScreen(data));
            });
            context.waitTicks(30);
            context.takeScreenshot("manual_screen");
            
            // Test 3: Breakthrough Screen
            context.runOnClient(client -> {
                CompoundTag data = CultivationManager.buildBreakthroughScreenData(player);
                client.setScreen(new BreakthroughScreen(data));
            });
            context.waitTicks(30);
            context.takeScreenshot("breakthrough_screen");
            
            // Test 4: Technique Screen
            context.runOnClient(client -> {
                CompoundTag data = CultivationManager.buildTechniqueScreenData(player);
                client.setScreen(new TechniqueScreen(data));
            });
            context.waitTicks(30);
            context.takeScreenshot("techniques_screen");
            
            // Test 5: Qi Focus Screen
            context.runOnClient(client -> {
                CompoundTag data = CultivationManager.buildEffectsScreenData(player);
                client.setScreen(new QiFocusScreen(data));
            });
            context.waitTicks(30);
            context.takeScreenshot("qi_focus_screen");
            
            // Test 6: Research Study Screen
            context.runOnClient(client -> {
                CompoundTag data = CultivationManager.buildStudyScreenData(player);
                client.setScreen(new ResearchStudyScreen(data));
            });
            context.waitTicks(30);
            context.takeScreenshot("study_screen");

            // Test 7: Spiritual Items & Modifiers (Tempered, Vigor, Ignis)
            singleplayer.getServer().runOnServer(server -> {
                // 1. Create Tempered item
                ItemStack sword = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
                java.util.List<immortality.item.ModifierInstance> mods = new java.util.ArrayList<>();
                mods.add(new immortality.item.ModifierInstance("ignis", 2));
                mods.add(new immortality.item.ModifierInstance("vigor", 3));
                
                immortality.item.SpiritualBlueprintComponent blueprint = new immortality.item.SpiritualBlueprintComponent(
                    mods,
                    immortality.item.SpiritualBlueprintComponent.TEMPERED,
                    100, 100
                );
                
                sword.set(Immortality.SPIRITUAL_BLUEPRINT, blueprint);
                
                // Set damage to almost broken
                int maxDmg = sword.getMaxDamage();
                sword.setDamageValue(maxDmg - 1);
                
                // Damage it again (exceeding maxDamage)
                sword.setDamageValue(maxDmg + 10);
                if (sword.getDamageValue() != maxDmg - 1) {
                    throw new AssertionError("Tempered item broke! Damage was: " + sword.getDamageValue());
                }
                
                // 2. Test Vigor modifier (+12 extra max health / +6 hearts)
                sword.setDamageValue(0); // Repair the sword so vigor applies
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
                
                CultivationManager.tickPlayer(player);
                
                double maxHealth = player.getMaxHealth();
                if (maxHealth <= 20.0D) {
                    throw new AssertionError("Vigor health boost did not apply! Max health: " + maxHealth);
                }
                
                // Remove sword and tick again
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                CultivationManager.tickPlayer(player);
                
                if (player.getMaxHealth() > 20.0D) {
                    throw new AssertionError("Vigor health boost was not removed! Max health: " + player.getMaxHealth());
                }
                
                // 3. Test Ignis modifier
                Sheep dummy = new Sheep(net.minecraft.world.entity.EntityType.SHEEP, player.level());
                player.level().addFreshEntity(dummy);
                
                // Attack dummy
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
                player.attack(dummy);
                
                if (dummy.getRemainingFireTicks() <= 0) {
                    throw new AssertionError("Ignis did not set target on fire!");
                }
                
                
                dummy.discard();
                
                // 4. Test 8: Magic Arrays & Spiritual Flags
                net.minecraft.core.BlockPos corePos = player.blockPosition().east(5);
                net.minecraft.core.BlockPos flag1 = corePos.north(3).west(3);
                net.minecraft.core.BlockPos flag2 = corePos.north(3).east(3);
                net.minecraft.core.BlockPos flag3 = corePos.south(3);
                
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) player.level();
                
                // Move player to center of array zone
                player.setPos(corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5);
                
                // Place core and flags
                serverLevel.setBlockAndUpdate(corePos, Immortality.FORMATION_CORE.defaultBlockState());
                serverLevel.setBlockAndUpdate(flag1, Immortality.JADE_FLAG.defaultBlockState());
                serverLevel.setBlockAndUpdate(flag2, Immortality.JADE_FLAG.defaultBlockState());
                serverLevel.setBlockAndUpdate(flag3, Immortality.JADE_FLAG.defaultBlockState());
                
                // Charge flags
                ((immortality.block.entity.FormationFlagBlockEntity) serverLevel.getBlockEntity(flag1)).setCurrentQi(500);
                ((immortality.block.entity.FormationFlagBlockEntity) serverLevel.getBlockEntity(flag2)).setCurrentQi(500);
                ((immortality.block.entity.FormationFlagBlockEntity) serverLevel.getBlockEntity(flag3)).setCurrentQi(500);
                
                immortality.block.entity.FormationCoreBlockEntity coreBE = 
                    (immortality.block.entity.FormationCoreBlockEntity) serverLevel.getBlockEntity(corePos);
                
                // Bind flags via core
                java.util.List<net.minecraft.core.BlockPos> flagsList = java.util.List.of(flag1, flag2, flag3);
                boolean bound = coreBE.bindFlags(flagsList, player);
                if (!bound) {
                    throw new AssertionError("Failed to bind flags to core!");
                }
                
                // Insert Spirit Convergence Rune
                coreBE.setRune(new ItemStack(Immortality.SPIRIT_CONVERGENCE_RUNE));
                
                // Tick the core manually (bypass throttling)
                // First tick: verify Qi consumption
                int startQi1 = ((immortality.block.entity.FormationFlagBlockEntity) serverLevel.getBlockEntity(flag1)).getCurrentQi();
                coreBE.tick(serverLevel, corePos, serverLevel.getBlockState(corePos), coreBE);
                
                // Since coreBE.tick throttles by 20, let's call the serverTick directly to simulate 1 tick
                try {
                    java.lang.reflect.Method tickMethod = immortality.block.entity.FormationCoreBlockEntity.class.getDeclaredMethod("serverTick", net.minecraft.server.level.ServerLevel.class);
                    tickMethod.setAccessible(true);
                    tickMethod.invoke(coreBE, serverLevel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                
                int endQi1 = ((immortality.block.entity.FormationFlagBlockEntity) serverLevel.getBlockEntity(flag1)).getCurrentQi();
                if (endQi1 >= startQi1) {
                    throw new AssertionError("Core did not consume Qi from flags! Start: " + startQi1 + ", End: " + endQi1);
                }
                
                // Destroy one of the flags and tick to trigger Backlash
                serverLevel.destroyBlock(flag1, false);
                
                // Tick core again
                try {
                    java.lang.reflect.Method tickMethod = immortality.block.entity.FormationCoreBlockEntity.class.getDeclaredMethod("serverTick", net.minecraft.server.level.ServerLevel.class);
                    tickMethod.setAccessible(true);
                    tickMethod.invoke(coreBE, serverLevel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                
                // Verify Backlash effects (Player has Wither or Blindness effect applied)
                if (!player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
                    throw new AssertionError("Qi Backflash did not apply Wither to player!");
                }
                
                // Cleanup
                serverLevel.destroyBlock(corePos, false);
                serverLevel.destroyBlock(flag2, false);
                serverLevel.destroyBlock(flag3, false);
            });
        }
    }
}
