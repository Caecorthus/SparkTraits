package dev.caecorthus.sparktraits.entity;

import dev.caecorthus.sparktraits.impl.ToxicologistCapsuleService;
import dev.caecorthus.sparktraits.item.SparkTraitsItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class CapsuleEntity extends ThrownItemEntity {
    public CapsuleEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected Item getDefaultItem() {
        return SparkTraitsItems.CAPSULE;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (!getWorld().isClient
                && entityHitResult.getEntity() instanceof ServerPlayerEntity target
                && getOwner() instanceof ServerPlayerEntity owner
                && ToxicologistCapsuleService.isToxicologist(owner)) {
            ToxicologistCapsuleService.forceFeedCapsule(target, getStack());
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!getWorld().isClient) {
            discard();
        }
    }
}
