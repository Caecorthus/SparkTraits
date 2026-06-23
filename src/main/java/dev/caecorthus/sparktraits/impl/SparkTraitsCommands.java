package dev.caecorthus.sparktraits.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SparkTraitsCommands {
    private static final DynamicCommandExceptionType UNKNOWN_TRAIT = new DynamicCommandExceptionType(id -> Text.literal("Unknown trait: " + id));
    private static final DynamicCommandExceptionType INVALID_TRAIT_ID = new DynamicCommandExceptionType(id -> Text.literal("Invalid trait id: " + id));

    private SparkTraitsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(listTraitsCommand("sparktraits:listTraits"));
            dispatcher.register(enableTraitCommand("sparktraits:enableTrait", true));
            dispatcher.register(enableTraitCommand("sparktraits:disableTrait", false));
            dispatcher.register(playerTraitCommand("sparktraits:addTrait", PlayerTraitAction.ADD));
            dispatcher.register(playerTraitCommand("sparktraits:removeTrait", PlayerTraitAction.REMOVE));
            dispatcher.register(clearTraitsCommand("sparktraits:clearTraits"));
            dispatcher.register(listPlayerTraitsCommand("sparktraits:listPlayerTraits"));
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> listTraitsCommand(String literalName) {
        return literal(literalName)
                .executes(context -> {
                    TraitWorldComponent worldTraits = TraitWorldComponent.KEY.get(context.getSource().getWorld());
                    if (TraitRegistry.values().isEmpty()) {
                        context.getSource().sendFeedback(() -> Text.literal("No traits registered."), false);
                        return 0;
                    }
                    for (Trait trait : TraitRegistry.values()) {
                        boolean enabled = worldTraits.isTraitEnabled(trait.id());
                        context.getSource().sendFeedback(() -> Text.literal((enabled ? "[enabled] " : "[disabled] ") + trait.id()), false);
                    }
                    return TraitRegistry.values().size();
                });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> enableTraitCommand(String literalName, boolean enabled) {
        return literal(literalName)
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("trait", IdentifierArgumentType.identifier())
                        .suggests(SparkTraitsCommands::suggestTraits)
                        .executes(context -> {
                            Identifier traitId = getTraitId(context);
                            requireTrait(traitId);
                            TraitWorldComponent.KEY.get(context.getSource().getWorld()).setTraitEnabled(traitId, enabled);
                            context.getSource().sendFeedback(() -> Text.literal((enabled ? "Enabled " : "Disabled ") + traitId), true);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> playerTraitCommand(String literalName, PlayerTraitAction action) {
        return literal(literalName)
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("trait", IdentifierArgumentType.identifier())
                        .suggests(SparkTraitsCommands::suggestTraits)
                        .then(argument("players", EntityArgumentType.players())
                                .executes(context -> executePlayerTraitAction(
                                        context.getSource(),
                                        requireTrait(getTraitId(context)),
                                        EntityArgumentType.getPlayers(context, "players"),
                                        action
                                ))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> clearTraitsCommand(String literalName) {
        return literal(literalName)
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("players", EntityArgumentType.players())
                        .executes(context -> {
                            int changed = 0;
                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
                                TraitPlayerComponent component = TraitPlayerComponent.KEY.get(player);
                                if (!component.getPendingTraitIds().isEmpty()) {
                                    component.clearPendingTraits();
                                    changed++;
                                }
                            }
                            int finalChanged = changed;
                            context.getSource().sendFeedback(() -> Text.literal("Cleared pending traits for " + finalChanged + " player(s)."), true);
                            return finalChanged;
                        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> listPlayerTraitsCommand(String literalName) {
        return literal(literalName)
                .then(argument("players", EntityArgumentType.players())
                        .executes(context -> {
                            int count = 0;
                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
                                TraitPlayerComponent component = TraitPlayerComponent.KEY.get(player);
                                context.getSource().sendFeedback(() -> Text.literal(
                                        player.getName().getString()
                                                + " active=" + component.getActiveTraitIds()
                                                + " pending=" + component.getPendingTraitIds()
                                ), false);
                                count++;
                            }
                            return count;
                        }));
    }

    private static int executePlayerTraitAction(ServerCommandSource source, Trait trait, Collection<ServerPlayerEntity> players, PlayerTraitAction action) {
        int changed = 0;
        String singleChangedPlayerName = null;
        for (ServerPlayerEntity player : players) {
            TraitPlayerComponent component = TraitPlayerComponent.KEY.get(player);
            if (action == PlayerTraitAction.ADD) {
                AddResult result = canAddPending(source, player, component, trait);
                if (!result.ok()) {
                    source.sendFeedback(() -> Text.literal("Skipped " + player.getGameProfile().getName() + ": " + result.message()), false);
                    continue;
                }
                if (component.addPendingTrait(trait.id())) {
                    changed++;
                    singleChangedPlayerName = player.getGameProfile().getName();
                }
            } else if (component.removePendingTrait(trait.id())) {
                changed++;
                singleChangedPlayerName = player.getGameProfile().getName();
            }
        }
        int finalChanged = changed;
        String finalSingleChangedPlayerName = finalChanged == 1 ? singleChangedPlayerName : null;
        source.sendFeedback(() -> formatPlayerTraitActionFeedback(trait, action, finalChanged, finalSingleChangedPlayerName), true);
        return finalChanged;
    }

    static Text formatPlayerTraitActionFeedback(Trait trait, PlayerTraitAction action, int changed, String singleChangedPlayerName) {
        MutableText message = Text.literal(action.feedbackVerb + " a pending trait ")
                .append(Text.literal(formatTraitIdForCommand(trait.id())).withColor(trait.color()))
                .append(Text.literal(" " + action.targetPreposition + " "));
        if (changed == 1 && singleChangedPlayerName != null) {
            return message.append(Text.literal(singleChangedPlayerName + "."));
        }
        return message.append(Text.literal(changed + " player(s)."));
    }

    static String formatTraitIdForCommand(Identifier traitId) {
        if (traitId.getNamespace().equals(SparkTraits.MOD_ID)) {
            return traitId.getPath();
        }
        return traitId.toString();
    }

    private static AddResult canAddPending(ServerCommandSource source, ServerPlayerEntity player, TraitPlayerComponent component, Trait trait) {
        Collection<Identifier> pending = component.getPendingTraitIds();
        if (pending.contains(trait.id())) {
            return AddResult.failure("trait is already pending");
        }
        if (pending.size() >= TraitPlayerComponent.MAX_TRAITS) {
            return AddResult.failure("pending trait slots are full");
        }
        if (!TraitRules.isCompatibleWithAll(trait, pending)) {
            return AddResult.failure("trait is incompatible with another pending trait");
        }
        TraitLockValidationService.RoleConflict conflict = TraitLockValidationService.findAudienceConflict(
                trait,
                TraitLockValidationService.forcedRoleFor(source, player)
        );
        if (conflict != null) {
            return AddResult.failure(TraitLockValidationService.addTraitRoleConflictMessage(trait, conflict.role()));
        }
        ServerPlayerEntity uniqueOwner = TraitLockValidationService.findOtherPendingUniqueTraitOwner(source.getServer(), player, trait);
        if (uniqueOwner != null) {
            return AddResult.failure(TraitLockValidationService.lockUniqueTraitConflictMessage(uniqueOwner, trait));
        }
        return AddResult.OK;
    }

    private static Identifier getTraitId(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier parsed = IdentifierArgumentType.getIdentifier(context, "trait");
        if (parsed.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return SparkTraits.id(parsed.getPath());
        }
        return parsed;
    }

    private static Trait requireTrait(Identifier traitId) throws CommandSyntaxException {
        Trait trait = TraitRegistry.get(traitId);
        if (trait == null) {
            throw UNKNOWN_TRAIT.create(traitId);
        }
        return trait;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTraits(CommandContext<ServerCommandSource> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (Trait trait : TraitRegistry.values()) {
            builder.suggest(formatTraitIdForCommand(trait.id()));
        }
        return builder.buildFuture();
    }

    enum PlayerTraitAction {
        ADD("Added", "to"),
        REMOVE("Removed", "from");

        private final String feedbackVerb;
        private final String targetPreposition;

        PlayerTraitAction(String feedbackVerb, String targetPreposition) {
            this.feedbackVerb = feedbackVerb;
            this.targetPreposition = targetPreposition;
        }
    }

    private record AddResult(boolean ok, String message) {
        private static final AddResult OK = new AddResult(true, "ok");

        private static AddResult failure(String message) {
            return new AddResult(false, message);
        }
    }
}
