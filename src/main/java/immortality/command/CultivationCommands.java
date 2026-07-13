package immortality.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import immortality.cultivation.BodyRegistry;
import immortality.cultivation.CultivationStage;
import immortality.cultivation.CultivationManager;
import immortality.cultivation.QiFocus;
import immortality.cultivation.ResearchDefinition;
import immortality.cultivation.ResearchRegistry;
import immortality.manual.ManualDefinition;
import immortality.manual.ManualRegistry;
import immortality.technique.TechniqueDefinition;
import immortality.technique.TechniqueRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CultivationCommands {
	private CultivationCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("cultivation")
			.executes(context -> showStatus(context.getSource().getPlayerOrException()))
			.then(Commands.literal("status")
				.executes(context -> showStatus(context.getSource().getPlayerOrException())))
			.then(Commands.literal("techniques")
				.executes(context -> {
					CultivationManager.openTechniqueScreen(context.getSource().getPlayerOrException());
					return 1;
				}))
			.then(Commands.literal("effects")
				.executes(context -> {
					CultivationManager.openEffectsScreen(context.getSource().getPlayerOrException());
					return 1;
				}))
			.then(Commands.literal("breakthrough")
				.executes(context -> {
					CultivationManager.tryBreakthrough(context.getSource().getPlayerOrException());
					return 1;
				}))
			.then(Commands.literal("manual")
				.then(Commands.argument("id", StringArgumentType.word())
					.executes(context -> {
						ServerPlayer player = context.getSource().getPlayerOrException();
						String id = StringArgumentType.getString(context, "id");
						ManualDefinition definition = ManualRegistry.get(id);
						if (ManualRegistry.NONE_ID.equals(definition.id()) && !ManualRegistry.NONE_ID.equals(id)) {
							player.sendSystemMessage(Component.literal("Unknown manual: " + id));
							return 0;
						}
						CultivationManager.useManual(player, definition.id());
						return 1;
					})))
			.then(Commands.literal("technique")
				.then(Commands.literal("next")
					.executes(context -> {
						CultivationManager.handleTechniqueAction(context.getSource().getPlayerOrException(), new immortality.network.TechniqueActionPayload(immortality.network.TechniqueActionPayload.ACTION_CYCLE_NEXT, ""));
						return 1;
					}))
				.then(Commands.literal("clear")
					.executes(context -> {
						CultivationManager.handleTechniqueAction(context.getSource().getPlayerOrException(), new immortality.network.TechniqueActionPayload(immortality.network.TechniqueActionPayload.ACTION_CLEAR, ""));
						return 1;
					}))
				.then(Commands.literal("invoke")
					.executes(context -> {
						CultivationManager.handleTechniqueAction(context.getSource().getPlayerOrException(), new immortality.network.TechniqueActionPayload(immortality.network.TechniqueActionPayload.ACTION_INVOKE, ""));
						return 1;
					}))
				.then(Commands.argument("id", StringArgumentType.word())
					.executes(context -> {
						ServerPlayer player = context.getSource().getPlayerOrException();
						String id = StringArgumentType.getString(context, "id");
						TechniqueDefinition definition = TechniqueRegistry.get(id);
						if (TechniqueRegistry.NONE_ID.equals(definition.id()) && !TechniqueRegistry.NONE_ID.equals(id)) {
							player.sendSystemMessage(Component.literal("Unknown technique: " + id));
							return 0;
						}
						CultivationManager.handleTechniqueAction(player, new immortality.network.TechniqueActionPayload(immortality.network.TechniqueActionPayload.ACTION_SET, id));
						return 1;
					})))
			.then(Commands.literal("research")
				.then(Commands.literal("next")
					.executes(context -> {
						CultivationManager.completeNextResearch(context.getSource().getPlayerOrException());
						return 1;
					}))
				.then(Commands.argument("id", StringArgumentType.word())
					.executes(context -> {
						ServerPlayer player = context.getSource().getPlayerOrException();
						String id = StringArgumentType.getString(context, "id");
						ResearchDefinition definition = ResearchRegistry.get(id);
						if (definition == null) {
							player.sendSystemMessage(Component.translatable("message.immortality.research_unknown", id));
							return 0;
						}
						CultivationManager.completeResearch(player, definition);
						return 1;
					})))
			.then(Commands.literal("debug")
				.then(Commands.literal("stage")
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							String id = StringArgumentType.getString(context, "id").toUpperCase();
							try {
								CultivationManager.debugSetStage(player, CultivationStage.valueOf(id));
							} catch (IllegalArgumentException exception) {
								player.sendSystemMessage(Component.literal("Unknown stage: " + id));
								return 0;
							}
							return 1;
						})))
				.then(Commands.literal("next")
					.executes(context -> {
						CultivationManager.debugAdvanceStage(context.getSource().getPlayerOrException(), 1);
						return 1;
					}))
				.then(Commands.literal("prev")
					.executes(context -> {
						CultivationManager.debugAdvanceStage(context.getSource().getPlayerOrException(), -1);
						return 1;
					}))
				.then(Commands.literal("qi")
					.then(Commands.literal("set")
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
							.executes(context -> {
								CultivationManager.debugSetQi(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"));
								return 1;
							})))
					.then(Commands.literal("add")
						.then(Commands.argument("amount", IntegerArgumentType.integer())
							.executes(context -> {
								CultivationManager.debugAddQi(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"));
								return 1;
							})))
					.then(Commands.literal("fill")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							CultivationManager.debugSetQi(player, CultivationManager.get(player).maxQi());
							return 1;
						})))
				.then(Commands.literal("manual")
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							String id = StringArgumentType.getString(context, "id");
							ManualDefinition definition = ManualRegistry.get(id);
							if (ManualRegistry.NONE_ID.equals(definition.id()) && !ManualRegistry.NONE_ID.equals(id)) {
								player.sendSystemMessage(Component.literal("Unknown manual: " + id));
								return 0;
							}
							CultivationManager.debugSetManual(player, definition.id());
							return 1;
						})))
				.then(Commands.literal("body")
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							String id = StringArgumentType.getString(context, "id");
							if (BodyRegistry.NONE_ID.equals(BodyRegistry.get(id).id()) && !BodyRegistry.NONE_ID.equals(id)) {
								player.sendSystemMessage(Component.literal("Unknown body: " + id));
								return 0;
							}
							CultivationManager.debugSetBody(player, id);
							return 1;
						})))
				.then(Commands.literal("focus")
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							QiFocus focus = QiFocus.byId(StringArgumentType.getString(context, "id"));
							if (focus == QiFocus.NONE && !"none".equalsIgnoreCase(StringArgumentType.getString(context, "id"))) {
								player.sendSystemMessage(Component.literal("Unknown focus: " + StringArgumentType.getString(context, "id")));
								return 0;
							}
							CultivationManager.debugSetQiFocus(player, focus);
							return 1;
						})))
				.then(Commands.literal("technique")
					.then(Commands.literal("all")
						.executes(context -> {
							CultivationManager.debugGrantAllTechniques(context.getSource().getPlayerOrException());
							return 1;
						}))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							String id = StringArgumentType.getString(context, "id");
							TechniqueDefinition definition = TechniqueRegistry.get(id);
							if (TechniqueRegistry.NONE_ID.equals(definition.id()) && !TechniqueRegistry.NONE_ID.equals(id)) {
								player.sendSystemMessage(Component.literal("Unknown technique: " + id));
								return 0;
							}
							CultivationManager.debugGrantTechnique(player, id);
							return 1;
						})))
				.then(Commands.literal("insight")
					.then(Commands.literal("all")
						.executes(context -> {
							CultivationManager.debugGrantAllInsights(context.getSource().getPlayerOrException());
							return 1;
						}))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							CultivationManager.debugGrantInsight(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id"));
							return 1;
						})))
				.then(Commands.literal("research")
					.then(Commands.literal("all")
						.executes(context -> {
							CultivationManager.debugUnlockAllResearches(context.getSource().getPlayerOrException());
							return 1;
						}))
					.then(Commands.literal("prepare")
						.then(Commands.argument("id", StringArgumentType.word())
							.executes(context -> {
								ServerPlayer player = context.getSource().getPlayerOrException();
								String id = StringArgumentType.getString(context, "id");
								if (ResearchRegistry.get(id) == null) {
									player.sendSystemMessage(Component.translatable("message.immortality.research_unknown", id));
									return 0;
								}
								CultivationManager.debugPrepareResearch(player, id);
								return 1;
							})))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							String id = StringArgumentType.getString(context, "id");
							ResearchDefinition definition = ResearchRegistry.get(id);
							if (definition == null) {
								player.sendSystemMessage(Component.translatable("message.immortality.research_unknown", id));
								return 0;
							}
							CultivationManager.debugUnlockResearch(player, definition);
							return 1;
						})))));
	}

	private static int showStatus(ServerPlayer player) {
		player.sendSystemMessage(CultivationManager.buildStatusText(player));
		return 1;
	}
}
