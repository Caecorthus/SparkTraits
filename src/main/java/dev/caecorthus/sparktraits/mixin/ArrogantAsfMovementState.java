package dev.caecorthus.sparktraits.mixin;

/**
 * Stores the current movement tick's Arrogant ASF lateral-input state on the player.
 * 在玩家身上暂存当前移动 tick 的展示豪度横移输入状态。
 */
public interface ArrogantAsfMovementState {
    void sparktraits$setArrogantAsfPureSidewaysInput(boolean pureSidewaysInput);

    boolean sparktraits$isArrogantAsfPureSidewaysInput();
}
