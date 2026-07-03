package dev.caecorthus.sparktraits.yuusha;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.GoodTraitService;
import dev.caecorthus.sparktraits.yuusha.component.YuushaComponents;
import dev.caecorthus.sparktraits.yuusha.component.YuushaPlayerComponent;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.effect.WhiskeyShieldEffect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class YuushaTrait implements Trait {
    public static final YuushaTrait INSTANCE = new YuushaTrait();
    public static final int COLOR = 0xff5fa8;

    private static final int OPENING_COOLDOWN_TICKS = 20 * 60;
    private static final int AFTER_USE_COOLDOWN_TICKS = 20 * 180;
    private static final int BLOOM_TICKS = 20 * 60;
    private static final int ROUND_LONG_TICKS = 20 * 60 * 60;

    private static final String PHONE_NAME = "请假装这是个手机";
    private static final String TEMP_REVOLVER_NAME = "「滿開」临时左轮";
    private static final String TEMP_KNIFE_NAME = "「滿開」临时刀";

    private YuushaTrait() {}

    public static void register() {
        TraitRegistry.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return YuushaBootstrap.ID;
    }

    @Override
    public int color() {
        return COLOR;
    }

    @Override
    public int weight() {
        return 35;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public boolean canApply(TraitSelectionContext ctx) {
        // Pending/command locks use the short TraitSelectionContext constructor, where
        // enforceStartingPlayerCount is false. Treat locks as administrator-forced and
        // do not block them by the natural 18-player/good-side refresh rules.
        if (!ctx.enforceStartingPlayerCount()) {
            return true;
        }

        int count = ctx.startingPlayerCount() > 0 ? ctx.startingPlayerCount() : ctx.gameComponent().getAllPlayers().size();
        return count >= 18 && GoodTraitService.canSelectNonUndercoverGoodTrait(ctx.role(), ctx.selectedTraitIds());
    }

    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(player);
        yuusha.resetForRound();
        yuusha.setPhoneCooldownTicks(OPENING_COOLDOWN_TICKS);
        player.getInventory().insertStack(createPhone());
        player.getItemCooldownManager().set(Items.ECHO_SHARD, OPENING_COOLDOWN_TICKS);
    }

    @Override
    public void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
        endBloom(player, false);
        YuushaComponents.YUUSHA.get(player).resetForRound();
    }

    public static int heroCap(int playerCount) {
        if (playerCount < 18) return 0;
        return 1 + (playerCount - 18) / 6;
    }

    public static boolean hasYuusha(PlayerEntity player) {
        return TraitPlayerComponent.KEY.get(player).hasActiveTrait(YuushaBootstrap.ID);
    }

    public static Text t(String key) {
        return Text.translatable(key);
    }

    private static int secondsCeil(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    public static ItemStack createPhone() {
        ItemStack stack = new ItemStack(Items.ECHO_SHARD);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(PHONE_NAME).formatted(Formatting.LIGHT_PURPLE));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.translatable("item.sparktraits.hero_phone.tooltip").formatted(Formatting.GRAY)
        )));
        return stack;
    }

    public static boolean isYuushaPhone(ItemStack stack) {
        return stack.isOf(Items.ECHO_SHARD) && hasExactCustomName(stack, PHONE_NAME);
    }

    public static boolean isTemporaryYuushaKnife(ItemStack stack) {
        return stack.isOf(WatheItems.KNIFE) && hasExactCustomName(stack, TEMP_KNIFE_NAME);
    }

    public static boolean hasTemporaryYuushaKnife(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (isTemporaryYuushaKnife(player.getInventory().getStack(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTemporaryYuushaWeapon(ItemStack stack) {
        return (stack.isOf(WatheItems.REVOLVER) && hasExactCustomName(stack, TEMP_REVOLVER_NAME))
            || isTemporaryYuushaKnife(stack);
    }

    private static boolean hasExactCustomName(ItemStack stack, String name) {
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        return customName != null && name.equals(customName.getString());
    }

    public static TypedActionResult<ItemStack> tryUsePhone(PlayerEntity player, World world, Hand hand, ItemStack stack) {
        if (world.isClient) return TypedActionResult.pass(stack);
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(stack);
        if (!hasYuusha(serverPlayer)) return TypedActionResult.pass(stack);

        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(serverPlayer);
        if (yuusha.bloomActiveTicks() > 0) {
            serverPlayer.sendMessage(Text.translatable("message.sparktraits.hero.bloom_active", secondsCeil(yuusha.bloomActiveTicks())), true);
            return TypedActionResult.fail(stack);
        }
        if (yuusha.phoneCooldownTicks() > 0 || serverPlayer.getItemCooldownManager().isCoolingDown(Items.ECHO_SHARD)) {
            serverPlayer.sendMessage(Text.translatable("message.sparktraits.hero.cooldown", secondsCeil(yuusha.phoneCooldownTicks())), true);
            return TypedActionResult.fail(stack);
        }

        if (!fullBloom(serverPlayer)) {
            return TypedActionResult.fail(stack);
        }

        // The 180s phone cooldown starts only after 「滿開」 ends.
        // During 「滿開」 itself, bloomActiveTicks blocks reuse and shows the active-time reminder.
        return TypedActionResult.success(stack);
    }

    public static boolean fullBloom(ServerPlayerEntity player) {
        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(player);
        if (yuusha.bloomActiveTicks() > 0) {
            return false;
        }

        ItemStack weapon = createTemporaryWeapon(player, yuusha);
        boolean inserted = player.getInventory().insertStack(weapon);
        if (!inserted) {
            player.dropItem(weapon, false);
        }

        yuusha.setBloomCount(yuusha.bloomCount() + 1);
        yuusha.setBloomActiveTicks(BLOOM_TICKS);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, BLOOM_TICKS, 1, false, true, true));
        giveShield(player);
        player.sendMessage(t("message.sparktraits.hero.full_bloom").copy().formatted(Formatting.LIGHT_PURPLE), true);
        return true;
    }

    private static ItemStack createTemporaryWeapon(ServerPlayerEntity player, YuushaPlayerComponent yuusha) {
        int weaponType = yuusha.bloomWeaponType();
        if (weaponType == 0) {
            weaponType = player.getRandom().nextBoolean() ? 1 : 2;
            yuusha.setBloomWeaponType(weaponType);
        }

        boolean giveRevolver = weaponType == 1;
        ItemStack weapon = new ItemStack(giveRevolver ? WatheItems.REVOLVER : WatheItems.KNIFE);
        weapon.set(DataComponentTypes.CUSTOM_NAME, Text.literal(giveRevolver ? TEMP_REVOLVER_NAME : TEMP_KNIFE_NAME).formatted(Formatting.LIGHT_PURPLE));
        weapon.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("「滿開」结束后消失").formatted(Formatting.GRAY),
            Text.literal(giveRevolver ? "临时左轮手枪" : "临时刀：冷却20秒").formatted(Formatting.GRAY)
        )));
        return weapon;
    }

    private static void giveShield(ServerPlayerEntity player) {
        WhiskeyShieldEffect.addShieldLayer(player, BLOOM_TICKS);
        YuushaComponents.YUUSHA.get(player).setBloomShieldGiven(true);
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        if (!hasYuusha(player)) return;
        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(player);

        if (yuusha.phoneCooldownTicks() > 0) {
            yuusha.setPhoneCooldownTicks(yuusha.phoneCooldownTicks() - 1);
        }
        if (yuusha.bloomActiveTicks() <= 0) return;

        yuusha.setBloomActiveTicks(yuusha.bloomActiveTicks() - 1);
        if (yuusha.bloomActiveTicks() == 0) {
            endBloom(player, true);
        }
    }

    private static void endBloom(ServerPlayerEntity player, boolean applyCost) {
        removeTemporaryWeapons(player);
        removeBloomShield(player);
        if (applyCost) {
            applyBloomCost(player);
            startPhoneCooldown(player);
        }
    }

    private static void startPhoneCooldown(ServerPlayerEntity player) {
        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(player);
        yuusha.setPhoneCooldownTicks(AFTER_USE_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(Items.ECHO_SHARD, AFTER_USE_COOLDOWN_TICKS);
    }

    private static void removeTemporaryWeapons(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isTemporaryYuushaWeapon(stack)) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static void removeBloomShield(ServerPlayerEntity player) {
        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(player);
        if (!yuusha.bloomShieldGiven()) return;

        removeOneWhiskeyShieldLayer(player);
        yuusha.setBloomShieldGiven(false);
    }

    private static void removeOneWhiskeyShieldLayer(ServerPlayerEntity player) {
        StatusEffectInstance current = player.getStatusEffect(ModEffects.WHISKEY_SHIELD);
        if (current == null) return;

        int remainingLayersAfterRemovingYuushaLayer = current.getAmplifier();
        int duration = current.getDuration();
        player.removeStatusEffect(ModEffects.WHISKEY_SHIELD);

        if (remainingLayersAfterRemovingYuushaLayer > 0) {
            player.addStatusEffect(new StatusEffectInstance(
                ModEffects.WHISKEY_SHIELD,
                duration,
                remainingLayersAfterRemovingYuushaLayer - 1,
                false,
                false,
                true
            ));
        }
    }

    private static void applyBloomCost(ServerPlayerEntity player) {
        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(player);
        List<BloomCost> pool = new ArrayList<>();

        if (!yuusha.blindnessCost()) pool.add(BloomCost.BLINDNESS);
        if (yuusha.slownessCostLevel() < 4) pool.add(BloomCost.SLOWNESS);
        if (!yuusha.tasteLossCost()) pool.add(BloomCost.TASTE_LOSS);

        // All possible costs have already been paid. Later 「滿開」 activations
        // still work, but they no longer add any new round-long drawback.
        if (pool.isEmpty()) return;

        BloomCost cost = pool.get(player.getRandom().nextInt(pool.size()));
        switch (cost) {
            case BLINDNESS -> {
                yuusha.setBlindnessCost(true);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, ROUND_LONG_TICKS, 0, false, true, true));
                player.sendMessage(t("message.sparktraits.hero.cost.blindness").copy().formatted(Formatting.DARK_PURPLE), false);
            }
            case SLOWNESS -> {
                int newLevel = yuusha.slownessCostLevel() >= 2 ? 4 : 2;
                yuusha.setSlownessCostLevel(newLevel);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ROUND_LONG_TICKS, newLevel - 1, false, true, true));
                player.sendMessage(t("message.sparktraits.hero.cost.slowness").copy().formatted(Formatting.DARK_PURPLE), false);
            }
            case TASTE_LOSS -> {
                yuusha.setTasteLossCost(true);
                player.sendMessage(t("message.sparktraits.hero.cost.taste_loss").copy().formatted(Formatting.DARK_PURPLE), false);
            }
        }
    }

    public static boolean shouldBlockFoodOrDrink(PlayerEntity player, ItemStack stack) {
        if (!YuushaComponents.YUUSHA.get(player).tasteLossCost()) return false;
        if (stack.isEmpty()) return false;
        if (stack.get(DataComponentTypes.FOOD) != null) return true;

        Item item = stack.getItem();
        return item == WatheItems.OLD_FASHIONED
            || item == WatheItems.MOJITO
            || item == WatheItems.MARTINI
            || item == WatheItems.COSMOPOLITAN
            || item == WatheItems.CHAMPAGNE;
    }

    public static boolean isYuushaShooter(PlayerEntity shooter) {
        return shooter != null && hasYuusha(shooter);
    }

    public static void scheduleGunSanityRestoreIfNeeded(PlayerEntity shooter, PlayerEntity target) {
        if (!(shooter instanceof ServerPlayerEntity serverShooter)) return;
        if (target == null || !hasYuusha(serverShooter)) return;

        YuushaPlayerComponent yuusha = YuushaComponents.YUUSHA.get(serverShooter);
        if (yuusha.bloomActiveTicks() <= 0) return;

        GameWorldComponent game = GameWorldComponent.KEY.get(serverShooter.getWorld());
        if (!game.isInnocent(target)) return;
        if (!game.isInnocent(serverShooter)) return;
        if (game.isRole(serverShooter, WatheRoles.VIGILANTE)) return;
        if (game.isRole(serverShooter, WatheRoles.VETERAN)) return;

        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(serverShooter);
        float moodBeforeShot = mood.getMood();

        // Let Wathe's normal shoot-innocent / 小脑 logic run first.
        // After it applies the -0.35 sanity loss, restore only that loss for Yuusha.
        Scheduler.schedule(() -> {
            if (serverShooter.isRemoved()) return;

            PlayerMoodComponent currentMoodComponent = PlayerMoodComponent.KEY.get(serverShooter);
            float currentMood = currentMoodComponent.getMood();
            if (currentMood <= moodBeforeShot - 0.34f) {
                currentMoodComponent.setMood(Math.min(moodBeforeShot, currentMood + 0.35f));
            }
        }, 5);
    }

    public static boolean isPriorityRole(dev.doctor4t.wathe.api.Role role) {
        if (role == null) return false;
        Identifier id = role.identifier();
        return id.equals(Noellesroles.DETECTIVE_ID)
            || id.equals(Noellesroles.TIMEKEEPER_ID)
            || id.equals(Noellesroles.TOXICOLOGIST_ID)
            || id.equals(Noellesroles.BODYGUARD_ID)
            || id.equals(Noellesroles.SURVIVAL_MASTER_ID)
            || id.equals(Noellesroles.CONDUCTOR_ID);
    }

    public static void enforceHeroCapOnPlans(List<?> plans) {
        int cap = heroCap(plans.size());

        List<Object> randomHeroes = new ArrayList<>();
        for (Object plan : plans) {
            if (planHasRandomYuusha(plan)) randomHeroes.add(plan);
        }

        if (cap <= 0) {
            for (Object plan : randomHeroes) removeRandomYuushaAndReplace(plan, plans.size());
            return;
        }
        if (randomHeroes.size() <= cap) return;

        Set<Object> keep = pickWeightedHeroPlans(randomHeroes, cap);
        for (Object plan : randomHeroes) {
            if (!keep.contains(plan)) removeRandomYuushaAndReplace(plan, plans.size());
        }
    }

    private static Set<Object> pickWeightedHeroPlans(List<Object> heroes, int cap) {
        Set<Object> keep = new HashSet<>();
        List<Object> pool = new ArrayList<>(heroes);

        while (keep.size() < cap && !pool.isEmpty()) {
            int totalWeight = 0;
            for (Object plan : pool) totalWeight += heroPlanWeight(plan);

            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            Object chosen = pool.get(0);
            for (Object plan : pool) {
                roll -= heroPlanWeight(plan);
                if (roll < 0) {
                    chosen = plan;
                    break;
                }
            }

            keep.add(chosen);
            pool.remove(chosen);
        }
        return keep;
    }

    private static int heroPlanWeight(Object plan) {
        ServerPlayerEntity player = planPlayer(plan);
        if (player == null) return 1;

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (game == null) return 1;

        return isPriorityRole(game.getRole(player)) ? 4 : 1;
    }

    private static boolean planHasRandomYuusha(Object playerPlan) {
        return mutableTraitField(playerPlan, "randomTraits").contains(YuushaBootstrap.ID);
    }

    private static void removeRandomYuushaAndReplace(Object playerPlan, int startingPlayerCount) {
        List<Identifier> randomTraits = mutableTraitField(playerPlan, "randomTraits");
        boolean removed = randomTraits.remove(YuushaBootstrap.ID);
        if (!removed) return;

        Identifier replacement = pickReplacementTrait(playerPlan, startingPlayerCount);
        if (replacement != null && !randomTraits.contains(replacement)) {
            randomTraits.add(replacement);
        }
    }

    @SuppressWarnings("unchecked")
    private static Identifier pickReplacementTrait(Object playerPlan, int startingPlayerCount) {
        try {
            ServerPlayerEntity player = planPlayer(playerPlan);
            if (player == null) return null;

            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(player.getWorld());
            LinkedHashSet<Identifier> selected = new LinkedHashSet<>(planTraits(playerPlan));
            selected.remove(YuushaBootstrap.ID);

            Method collectCandidates = Class.forName("dev.caecorthus.sparktraits.impl.TraitSelector")
                .getDeclaredMethod(
                    "collectCandidates",
                    net.minecraft.server.world.ServerWorld.class,
                    GameWorldComponent.class,
                    TraitWorldComponent.class,
                    ServerPlayerEntity.class,
                    dev.doctor4t.wathe.api.Role.class,
                    LinkedHashSet.class,
                    int.class
                );
            collectCandidates.setAccessible(true);

            List<Trait> candidates = new ArrayList<>((List<Trait>) collectCandidates.invoke(
                null,
                player.getServerWorld(),
                game,
                traitWorld,
                player,
                game.getRole(player),
                selected,
                startingPlayerCount
            ));
            candidates.removeIf(trait -> trait.id().equals(YuushaBootstrap.ID));
            if (candidates.isEmpty()) return null;

            Method pickWeighted = Class.forName("dev.caecorthus.sparktraits.impl.TraitSelector")
                .getDeclaredMethod("pickWeighted", List.class, java.util.random.RandomGenerator.class);
            pickWeighted.setAccessible(true);
            Trait chosen = (Trait) pickWeighted.invoke(null, candidates, ThreadLocalRandom.current());
            return chosen.id();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot replace removed random yuusha trait; SparkTraits internals changed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Identifier> mutableTraitField(Object playerPlan, String fieldName) {
        try {
            Field field = playerPlan.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (List<Identifier>) field.get(playerPlan);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot access SparkTraits PlayerPlan." + fieldName + "; update Yuusha for this SparkTraits version", e);
        }
    }

    private static List<Identifier> planTraits(Object playerPlan) {
        try {
            Method method = playerPlan.getClass().getDeclaredMethod("traits");
            method.setAccessible(true);
            return (List<Identifier>) method.invoke(playerPlan);
        } catch (ReflectiveOperationException e) {
            List<Identifier> combined = new ArrayList<>(mutableTraitField(playerPlan, "lockedTraits"));
            combined.addAll(mutableTraitField(playerPlan, "randomTraits"));
            return combined;
        }
    }

    private static ServerPlayerEntity planPlayer(Object playerPlan) {
        try {
            Method method = playerPlan.getClass().getDeclaredMethod("player");
            method.setAccessible(true);
            return (ServerPlayerEntity) method.invoke(playerPlan);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private enum BloomCost {
        BLINDNESS,
        SLOWNESS,
        TASTE_LOSS
    }
}
