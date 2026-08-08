package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.ai.*;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.github.alexthe666.iceandfire.enums.EnumParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shivaxi Dragon from the RLCraft Ice & Fire branch, ported to 1.20.1.
 * It deliberately keeps the old special AI and projectile behavior rather than behaving as a normal lightning dragon.
 */
public class EntityShivaxiDragon extends EntityLightningDragon {
    private static final UUID SHIVAXI = UUID.fromString("cdfccefb-1a2e-4fb8-a3b5-041da27fde61");

    public EntityShivaxiDragon(Level level) {
        this(IafEntityRegistry.SHIVAXI_DRAGON.get(), level);
    }

    public EntityShivaxiDragon(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new DragonAIAttackMelee(this, 1.5D, false));
        this.goalSelector.addGoal(5, new DragonAIAirTarget(this));
        this.goalSelector.addGoal(6, new DragonAIWander(this, 1.0D));
        this.goalSelector.addGoal(7, new DragonAIWatchClosest(this, LivingEntity.class, 6.0F));
        this.goalSelector.addGoal(7, new DragonAILookIdle(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> target != this && target.isAlive() && this.getControllingPassenger() != target));
        this.targetSelector.addGoal(5, new DragonAITargetItems<>(this, false));
    }

    @Override
    protected boolean shouldTarget(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && this.getControllingPassenger() != entity;
    }

    /**
     * Shivaxi has two ranged attacks in this port:
     *  - normal breath: the three-colour elemental stream (fire + ice + lightning)
     *  - charge attack: the RLC Shivaxi lightning orb
     *
     * The previous port replaced the entire breath with a projectile every 3 ticks, which is why
     * the dragon appeared to only shoot electric balls.
     */
    @Override
    public void riderShootFire(Entity controller) {
        // Once a charge has started, finish it deterministically instead of rolling a new random
        // choice every tick.
        if (this.getAnimation() == ANIMATION_FIRECHARGE) {
            if (this.getAnimationTick() == 20) {
                this.setYRot(this.yBodyRot);
                spawnShivaxiProjectile(controller.getLookAngle());
            }
            return;
        }

        // Keep the special electric orb as the occasional heavy attack.
        if (!this.isBaby() && this.getRandom().nextInt(5) == 0) {
            this.setAnimation(ANIMATION_FIRECHARGE);
            return;
        }

        // The normal attack is the three-colour continuous breath.
        if (this.isBreathingFire()) {
            if (this.isActuallyBreathingFire()) {
                this.setYRot(this.yBodyRot);
                if (this.fireTicks % 7 == 0) {
                    this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH, 4.0F, 1.0F);
                }
                HitResult hit = this.rayTraceRider(controller, 10.0D * this.getDragonStage(), 1.0F);
                if (hit != null) {
                    this.stimulateFire(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 1);
                }
            }
        } else {
            this.setBreathingFire(true);
        }
    }

    @Override
    protected void shootFireAtMob(LivingEntity entity) {
        if (!this.attackDecision) {
            if (this.getAnimation() == ANIMATION_FIRECHARGE) {
                if (this.getAnimationTick() == 20) {
                    this.setYRot(this.yBodyRot);
                    Vec3 head = this.getHeadPosition();
                    spawnShivaxiProjectile(new Vec3(entity.getX() - head.x, entity.getY() - head.y, entity.getZ() - head.z));
                    this.randomizeAttacks();
                }
            } else if (!this.isBaby() && this.getRandom().nextInt(5) == 0) {
                this.setAnimation(ANIMATION_FIRECHARGE);
            } else {
                if (this.isBreathingFire()) {
                    if (this.isActuallyBreathingFire()) {
                        this.setYRot(this.yBodyRot);
                        if (this.fireTicks % 7 == 0) {
                            this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH, 4.0F, 1.0F);
                        }
                        this.stimulateFire(entity.getX(), entity.getY(), entity.getZ(), 1);
                        if (!entity.isAlive()) {
                            this.setBreathingFire(false);
                            this.randomizeAttacks();
                        }
                    }
                } else {
                    this.setBreathingFire(true);
                }
            }
            this.lookAt(entity, 360.0F, 360.0F);
        }
    }

    /**
     * Keep the 1.20.1 lightning breath's server-side hit/destruction/synchronisation, then layer
     * the fire and ice breath particles over the lightning beam on the client.  This gives the
     * Shivaxi breath its intended red + blue + electric-purple appearance without tripling damage.
     */
    @Override
    public void stimulateFire(double burnX, double burnY, double burnZ, int syncType) {
        super.stimulateFire(burnX, burnY, burnZ, syncType);

        // Bomb sync types are handled as projectiles; only decorate the continuous breath.
        if (!this.level().isClientSide() || syncType > 2 || !this.isActuallyBreathingFire()) {
            return;
        }

        Vec3 head = this.getHeadPosition();
        double distance = Math.max(2.5D * Math.sqrt(this.distanceToSqr(burnX, burnY, burnZ)), 0.0D);
        double conqueredDistance = this.burnProgress / 40.0D * distance;
        int particleSpacing = this.getDragonStage() <= 3 ? 6 : 3;
        int attempts = Math.max(1, Math.min(12, (int) Math.ceil(conqueredDistance / 2.5D)));

        // DragonFire and DragonIce particles use burnParticleX/Y/Z from stimulateFire() as their
        // destination. The inherited lightning renderer supplies the third colour/element.
        for (int i = 0; i < attempts; i++) {
            if (this.getRandom().nextInt(particleSpacing) == 0) {
                IceAndFire.PROXY.spawnDragonParticle(EnumParticles.DragonFire,
                        head.x, head.y, head.z, 0.0D, 0.0D, 0.0D, this);
                IceAndFire.PROXY.spawnDragonParticle(EnumParticles.DragonIce,
                        head.x, head.y, head.z, 0.0D, 0.0D, 0.0D, this);
            }
        }
    }

    private void spawnShivaxiProjectile(Vec3 direction) {
        Vec3 head = this.getHeadPosition();
        double dx = direction.x + this.random.nextGaussian() * 0.007499999832361937D;
        double dy = direction.y + this.random.nextGaussian() * 0.007499999832361937D;
        double dz = direction.z + this.random.nextGaussian() * 0.007499999832361937D;
        this.playSound(IafSoundRegistry.LIGHTNINGDRAGON_BREATH_CRACKLE, 4.0F, 1.0F);
        EntityShivaxiDragonLightning projectile = new EntityShivaxiDragonLightning(
                IafEntityRegistry.SHIVAXI_DRAGON_LIGHTNING.get(), this.level(), this, dx, dy, dz);
        projectile.setPos(head.x, head.y, head.z);
        if (!this.level().isClientSide()) {
            this.level().addFreshEntity(projectile);
        }
    }

    @Override
    public String getVariantName(int variant) {
        return "electric_";
    }

    @Override
    public ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    @Override
    public Item getBloodItem() {
        return Items.GLASS_BOTTLE;
    }

    @Override
    public ItemLike getHeartItem() {
        return Items.AIR;
    }

    @Override
    public Item getFleshItem() {
        return Items.AIR;
    }

    @Override
    public Item getVariantScale(int variant) {
        return Items.AIR;
    }

    @Override
    public Item getVariantEgg(int variant) {
        return Items.AIR;
    }

    @Override
    public ResourceLocation getDeadLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(@NotNull net.minecraft.world.entity.animal.Animal otherAnimal) {
        return false;
    }

    @Override
    public boolean isMale() {
        return true;
    }

    @Override
    public boolean isTimeToWake() {
        return true;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty,
                                        @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.setGender(true);
        this.setVariant(0);
        this.setInSittingPose(false);
        this.growDragon(125);
        this.updateAttributes();
        this.setHealth(this.getMaxHealth());
        this.attackDecision = true;
        this.setHunger(50);
        return data;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Old RLC special rule: even the owner cannot mount the Shivaxi dragon unless UUID-authorized.
        if (this.isOwnedBy(player) && stack.isEmpty() && !player.isShiftKeyDown() && !isAuthorized(player)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public static boolean isAuthorized(Player player) {
        return SHIVAXI.equals(player.getUUID());
    }
}
