package dev.caecorthus.sparktraits.impl.traits.killer.combat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CloseQuartersSelectionRulesTest {
    // Registry identities stand in for Minecraft items; two KnifeItems need not be the same item.
    private record KnifeItem(String registryId) { }
    private static final KnifeItem KNIFE = new KnifeItem("wathe:knife");
    private static final KnifeItem OTHER_KNIFE = new KnifeItem("other:knife");
    private record Player(boolean eligibleRole, List<KnifeItem> inventory, List<KnifeItem> shop) {
        boolean canSelect() {
            return CloseQuartersSelectionRules.canSelect(eligibleRole, KNIFE, shop::stream);
        }
    }

    @Test
    void inventoryKnifeCannotReplaceMissingShopEntry() {
        Player player = new Player(true, List.of(KNIFE), List.of());
        assertTrue(player.inventory().contains(KNIFE));
        assertFalse(player.canSelect());
    }

    @Test
    void anotherKnifeItemInShopDoesNotQualifyEvenWithInventoryKnife() {
        assertEquals(KNIFE.getClass(), OTHER_KNIFE.getClass());
        assertFalse(new Player(true, List.of(KNIFE), List.of(OTHER_KNIFE)).canSelect());
    }

    @Test
    void exactRegisteredShopKnifeAllowsEligiblePlayerWithoutInventoryKnife() {
        assertTrue(new Player(true, List.of(), List.of(OTHER_KNIFE, KNIFE)).canSelect());
    }

    @Test
    void matchingValueIsNotRegisteredItemIdentity() {
        assertFalse(new Player(true, List.of(), List.of(new KnifeItem("wathe:knife"))).canSelect());
    }

    @Test
    void shopKnifeCannotOverrideRoleExclusionAndExcludedPlayersDoNotQueryShop() {
        assertFalse(new Player(false, List.of(KNIFE), List.of(KNIFE)).canSelect());
        assertFalse(CloseQuartersSelectionRules.canSelect(false, KNIFE, () -> {
            fail("Role rejection must short-circuit the shop lookup, including null players");
            return List.<KnifeItem>of().stream();
        }));
    }

    @Test
    void serviceSuppliesOnlyNonemptyShopItemsAndPreservesExistingRoleGates() throws Exception {
        String service = source("impl/traits/killer/combat/CloseQuartersService.java");
        String selection = service.substring(service.indexOf("public static boolean canSelect("),
                service.indexOf("public static boolean hasRaisedKnifeTrait("));
        assertTrue(selection.contains("CloseQuartersSelectionRules.canSelect(eligibleRole(player), WatheItems.KNIFE,"));
        assertTrue(selection.contains("ShopUtils.getShopEntriesForPlayer(player).stream()"));
        assertTrue(selection.contains(".map(entry -> entry.stack())"));
        assertTrue(selection.contains(".filter(stack -> !stack.isEmpty())"));
        assertTrue(selection.contains(".map(ItemStack::getItem)"));
        assertFalse(selection.contains("getInventory"));
        assertFalse(selection.contains("isRaisedKnife"));
        assertTrue(service.contains("if (player == null) return false;"));
        assertTrue(service.contains("role != null && !NO_RAISE_ROLES.contains(role.identifier().toString())\n"
                + "                && !instantCoroner(player)\n"
                + "                && KillerTraitService.canSelectKillerTrait(role, TraitPlayerComponent.KEY.get(player).getActiveTraitIds())"));
        assertTrue(service.contains("Set.of(\"wathe:veteran\", \"noellesroles:scavenger\",\n"
                + "            \"sparkwitch:vendetta\", \"sparkwitch:ninja\", \"noellesroles:ninja\")"));
        assertTrue(source("impl/traits/killer/KillerTraits.java").contains(
                ".predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())\n"
                        + "                        && CloseQuartersService.canSelect(context.player()))"));
    }

    @Test
    void randomAndPendingAssignmentRevalidateTraitPredicate() throws Exception {
        assertTrue(source("api/TraitDefinition.java").contains("audience.canApply(context) && predicate.test(context)"));
        assertTrue(source("impl/selection/TraitSelector.java").contains("if (!trait.canApply(context)) {\n                continue;"));
        String assignment = source("impl/assignment/TraitAssignmentService.java");
        assertTrue(assignment.contains("if (!trait.canApply(new TraitSelectionContext(world, gameComponent, player, role, accepted))) {\n"
                + "                continue;"));
        assertTrue(assignment.contains("applyPendingLocks(world, gameComponent, player, pendingTraits)"));
    }

    private static String source(String path) throws Exception {
        return Files.readString(Path.of(System.getProperty("user.dir"),
                "src/main/java/dev/caecorthus/sparktraits", path));
    }
}
