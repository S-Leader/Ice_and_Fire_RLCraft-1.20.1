package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.api.event.DragonFireEvent;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.message.MessageDragonSyncFire;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import com.github.alexthe666.iceandfire.misc.IafTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * 金龙实体 — 第四种龙类型
 * 使用魔法（附魔）吐息，不破坏方块
 */
public class EntityGoldDragon extends EntityDragonBase {

    public static final ResourceLocation FEMALE_LOOT = new ResourceLocation("iceandfire",
            "entities/dragon/gold_dragon_female");
    public static final ResourceLocation MALE_LOOT = new ResourceLocation("iceandfire",
            "entities/dragon/gold_dragon_male");
    public static final ResourceLocation SKELETON_LOOT = new ResourceLocation("iceandfire",
            "entities/dragon/gold_dragon_skeleton");

    public EntityGoldDragon(Level worldIn) {
        this(IafEntityRegistry.GOLD_DRAGON.get(), worldIn);
    }

    public EntityGoldDragon(EntityType<?> t, Level worldIn) {
        super(t, worldIn, DragonType.GOLD,
                1, 1 + IafConfig.dragonAttackDamage,
                IafConfig.dragonHealth * 0.04, IafConfig.dragonHealth,
                0.15F, 0.4F);
        ANIMATION_SPEAK = Animation.create(20);
        ANIMATION_BITE = Animation.create(35);
        ANIMATION_SHAKEPREY = Animation.create(65);
        ANIMATION_TAILWHACK = Animation.create(40);
        ANIMATION_FIRECHARGE = Animation.create(30);
        ANIMATION_WINGBLAST = Animation.create(50);
        ANIMATION_ROAR = Animation.create(40);
        ANIMATION_EPIC_ROAR = Animation.create(60);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected boolean shouldTarget(Entity entity) {
        if (entity instanceof EntityDragonBase && !this.isTame()) {
            return entity.getType() != this.getType()
                    && this.getBbWidth() >= entity.getBbWidth()
                    && !((EntityDragonBase) entity).isMobDead();
        }
        return entity instanceof Player
                || DragonUtils.isDragonTargetable(entity, IafTagRegistry.FIRE_DRAGON_TARGETS)
                || !this.isTame() && DragonUtils.isVillager(entity);
    }

    // 金龙只有一种变体
    @Override
    public String getVariantName(int variant) {
        return "gold_";
    }

    @Override
    public Item getVariantScale(int variant) {
        return IafItemRegistry.DRAGONSCALES_GOLD.get();
    }

    @Override
    public Item getVariantEgg(int variant) {
        return IafItemRegistry.DRAGONEGG_GOLD.get();
    }

    @Override
    public Item getSummoningCrystal() {
        return IafItemRegistry.SUMMONING_CRYSTAL_GOLD.get();
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity entityIn) {
        this.getLookControl().setLookAt(entityIn, 30.0F, 30.0F);
        if (!this.isPlayingAttackAnimation()) {
            switch (groundAttack) {
                case BITE:
                    this.setAnimation(ANIMATION_BITE);
                    break;
                case TAIL_WHIP:
                    this.setAnimation(ANIMATION_TAILWHACK);
                    break;
                case SHAKE_PREY:
                    boolean flag = false;
                    if (new Random().nextInt(2) == 0
                            && isDirectPathBetweenPoints(this, this.position().add(0, this.getBbHeight() / 2, 0),
                                    entityIn.position().add(0, entityIn.getBbHeight() / 2, 0))
                            && entityIn.getBbWidth() < this.getBbWidth() * 0.5F
                            && this.getControllingPassenger() == null
                            && this.getDragonStage() > 1
                            && !(entityIn instanceof EntityDragonBase)
                            && !DragonUtils.isAnimaniaMob(entityIn)) {
                        this.setAnimation(ANIMATION_SHAKEPREY);
                        flag = true;
                        entityIn.startRiding(this);
                    }
                    if (!flag) {
                        groundAttack = IafDragonAttacks.Ground.BITE;
                        this.setAnimation(ANIMATION_BITE);
                    }
                    break;
                case WING_BLAST:
                    this.setAnimation(ANIMATION_WINGBLAST);
                    break;
            }
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity attackTarget = this.getTarget();
        if (!level().isClientSide && attackTarget != null) {
            if (this.getBoundingBox().inflate(2.5F + this.getRenderSize() * 0.33F, 2.5F + this.getRenderSize() * 0.33F,
                    2.5F + this.getRenderSize() * 0.33F).intersects(attackTarget.getBoundingBox())) {
                doHurtTarget(attackTarget);
            }
            if (this.groundAttack == IafDragonAttacks.Ground.FIRE && (usingGroundAttack || this.onGround())) {
                shootFireAtMob(attackTarget);
            }
            if (this.airAttack == IafDragonAttacks.Air.TACKLE && !usingGroundAttack
                    && this.distanceToSqr(attackTarget) < 100) {
                double difX = attackTarget.getX() - this.getX();
                double difY = attackTarget.getY() + attackTarget.getBbHeight() - this.getY();
                double difZ = attackTarget.getZ() - this.getZ();
                this.setDeltaMovement(this.getDeltaMovement().add(difX * 0.1D, difY * 0.1D, difZ * 0.1D));
                if (this.getBoundingBox().inflate(1 + this.getRenderSize() * 0.5F, 1 + this.getRenderSize() * 0.5F,
                        1 + this.getRenderSize() * 0.5F).intersects(attackTarget.getBoundingBox())) {
                    doHurtTarget(attackTarget);
                    usingGroundAttack = true;
                    randomizeAttacks();
                    setFlying(false);
                    setHovering(false);
                }
            }
        }
    }

    @Override
    protected void breathFireAtPos(BlockPos burningTarget) {
        if (this.isBreathingFire()) {
            if (this.isActuallyBreathingFire()) {
                setYRot(yBodyRot);
                if (this.tickCount % 5 == 0) {
                    this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                }
                stimulateFire(burningTarget.getX() + 0.5F, burningTarget.getY() + 0.5F, burningTarget.getZ() + 0.5F, 1);
            }
        } else {
            this.setBreathingFire(true);
        }
    }

    @Override
    public void riderShootFire(Entity controller) {
        if (this.getRandom().nextInt(5) == 0 && !this.isBaby()) {
            if (this.getAnimation() != ANIMATION_FIRECHARGE) {
                this.setAnimation(ANIMATION_FIRECHARGE);
            } else if (this.getAnimationTick() == 20) {
                setYRot(yBodyRot);
                Vec3 headVec = this.getHeadPosition();
                this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                double d2 = controller.getLookAngle().x;
                double d3 = controller.getLookAngle().y;
                double d4 = controller.getLookAngle().z;
                float inaccuracy = 1.0F;
                d2 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d3 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d4 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                EntityDragonGoldCharge charge = new EntityDragonGoldCharge(
                        IafEntityRegistry.GOLD_DRAGON_CHARGE.get(), level(), this, d2, d3, d4);
                charge.setPos(headVec.x, headVec.y, headVec.z);
                if (!level().isClientSide) {
                    level().addFreshEntity(charge);
                }
            }
        } else {
            if (this.isBreathingFire()) {
                if (this.isActuallyBreathingFire()) {
                    setYRot(yBodyRot);
                    if (this.tickCount % 5 == 0) {
                        this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                    }
                    HitResult mop = rayTraceRider(controller, 10 * this.getDragonStage(), 1.0F);
                    if (mop != null) {
                        stimulateFire(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z, 1);
                    }
                }
            } else {
                this.setBreathingFire(true);
            }
        }
    }

    private void shootFireAtMob(LivingEntity entity) {
        if (this.usingGroundAttack && this.groundAttack == IafDragonAttacks.Ground.FIRE
                || !this.usingGroundAttack && (this.airAttack == IafDragonAttacks.Air.SCORCH_STREAM
                        || this.airAttack == IafDragonAttacks.Air.HOVER_BLAST)) {
            if (this.usingGroundAttack && this.getRandom().nextInt(5) == 0
                    || !this.usingGroundAttack && this.airAttack == IafDragonAttacks.Air.HOVER_BLAST) {
                if (this.getAnimation() != ANIMATION_FIRECHARGE) {
                    this.setAnimation(ANIMATION_FIRECHARGE);
                } else if (this.getAnimationTick() == 20) {
                    setYRot(yBodyRot);
                    Vec3 headVec = this.getHeadPosition();
                    double d2 = entity.getX() - headVec.x;
                    double d3 = entity.getY() - headVec.y;
                    double d4 = entity.getZ() - headVec.z;
                    float inaccuracy = 1.0F;
                    d2 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                    d3 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                    d4 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                    this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                    EntityDragonGoldCharge charge = new EntityDragonGoldCharge(
                            IafEntityRegistry.GOLD_DRAGON_CHARGE.get(), level(), this, d2, d3, d4);
                    charge.setPos(headVec.x, headVec.y, headVec.z);
                    if (!level().isClientSide) {
                        level().addFreshEntity(charge);
                    }
                    if (!entity.isAlive()) {
                        this.setBreathingFire(false);
                    }
                    this.randomizeAttacks();
                }
            } else {
                if (this.isBreathingFire()) {
                    if (this.isActuallyBreathingFire()) {
                        setYRot(yBodyRot);
                        if (this.tickCount % 5 == 0) {
                            this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                        }
                        stimulateFire(entity.getX(), entity.getY(), entity.getZ(), 1);
                        if (!entity.isAlive()) {
                            this.setBreathingFire(false);
                            this.randomizeAttacks();
                        }
                    }
                } else {
                    this.setBreathingFire(true);
                }
            }
        }
        this.lookAt(entity, 360, 360);
    }

    /**
     * 金龙吐息：附魔粒子流，不破坏方块
     * 对路径上生物造成魔法伤害
     */
    @Override
    public void stimulateFire(double burnX, double burnY, double burnZ, int syncType) {
        if (MinecraftForge.EVENT_BUS.post(new DragonFireEvent(this, burnX, burnY, burnZ)))
            return;
        if (syncType == 1 && !level().isClientSide) {
            IceAndFire.sendMSGToAll(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 0));
        }
        if (syncType == 2 && level().isClientSide) {
            IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 0));
        }
        if (syncType == 3 && !level().isClientSide) {
            IceAndFire.sendMSGToAll(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 5));
        }
        if (syncType == 4 && level().isClientSide) {
            IceAndFire.NETWORK_WRAPPER.sendToServer(new MessageDragonSyncFire(this.getId(), burnX, burnY, burnZ, 5));
        }
        // 弹射物模式
        if (syncType > 2 && syncType < 6) {
            if (this.getAnimation() != ANIMATION_FIRECHARGE) {
                this.setAnimation(ANIMATION_FIRECHARGE);
            } else if (this.getAnimationTick() == 20) {
                setYRot(yBodyRot);
                Vec3 headVec = this.getHeadPosition();
                double d2 = burnX - headVec.x;
                double d3 = burnY - headVec.y;
                double d4 = burnZ - headVec.z;
                float inaccuracy = 1.0F;
                d2 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d3 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                d4 += this.random.nextGaussian() * 0.007499999832361937D * inaccuracy;
                this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                EntityDragonGoldCharge charge = new EntityDragonGoldCharge(
                        IafEntityRegistry.GOLD_DRAGON_CHARGE.get(), level(), this, d2, d3, d4);
                charge.setPos(headVec.x, headVec.y, headVec.z);
                if (!level().isClientSide) {
                    level().addFreshEntity(charge);
                }
                this.randomizeAttacks();
            }
            return;
        }
        // 持续吐息模式 — 只生成附魔粒子，不破坏方块
        this.getNavigation().stop();
        this.burnParticleX = burnX;
        this.burnParticleY = burnY;
        this.burnParticleZ = burnZ;
        Vec3 headPos = getHeadPosition();
        double d2 = burnX - headPos.x;
        double d3 = burnY - headPos.y;
        double d4 = burnZ - headPos.z;
        double distance = Math.max(2.5F * Math.sqrt(this.distanceToSqr(burnX, burnY, burnZ)), 0);
        double conqueredDistance = burnProgress / 40D * distance;
        int increment = (int) Math.ceil(conqueredDistance / 100);
        for (int i = 0; i < conqueredDistance; i += increment) {
            double progressX = headPos.x + d2 * (i / (float) distance);
            double progressY = headPos.y + d3 * (i / (float) distance);
            double progressZ = headPos.z + d4 * (i / (float) distance);
            if (canPositionBeSeen(progressX, progressY, progressZ)) {
                // 金龙使用附魔粒子
                if (level().isClientSide && random.nextInt(3) == 0) {
                    double vx = (random.nextDouble() - 0.5) * 0.5;
                    double vy = (random.nextDouble() - 0.5) * 0.5;
                    double vz = (random.nextDouble() - 0.5) * 0.5;
                    level().addParticle(ParticleTypes.ENCHANT, progressX, progressY, progressZ, vx, vy, vz);
                }
            } else {
                // 金龙不破坏方块，只造成魔法伤害
                if (!level().isClientSide) {
                    HitResult result = this.level().clip(new ClipContext(
                            new Vec3(this.getX(), this.getY() + this.getEyeHeight(), this.getZ()),
                            new Vec3(progressX, progressY, progressZ),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                    // 不调用destroyArea，金龙吐息不破坏
                }
            }
        }
        // 对目标区域的实体造成魔法伤害
        if (burnProgress >= 40D && !level().isClientSide) {
            double range = 2.0;
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(burnX - range, burnY - range, burnZ - range,
                            burnX + range, burnY + range, burnZ + range))) {
                if (target != this && !target.isAlliedTo(this) && !((Object) target instanceof EntityDragonPart)) {
                    target.hurt(level().damageSources().magic(), (float) (this.getDragonStage() * 2));
                }
            }
            // 附魔粒子爆发
            if (level().isClientSide) {
                for (int k = 0; k < 8; k++) {
                    double vx = (random.nextDouble() - 0.5) * 1.5;
                    double vy = random.nextDouble() * 0.5;
                    double vz = (random.nextDouble() - 0.5) * 1.5;
                    level().addParticle(ParticleTypes.ENCHANT,
                            burnX + random.nextFloat() * 3 - 1.5,
                            burnY + random.nextFloat() * 2,
                            burnZ + random.nextFloat() * 3 - 1.5,
                            vx, vy, vz);
                }
            }
        }
    }

    // 复用火龙音效
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTeen() ? IafSoundRegistry.FIREDRAGON_TEEN_IDLE
                : this.shouldDropLoot() ? IafSoundRegistry.FIREDRAGON_ADULT_IDLE
                        : IafSoundRegistry.FIREDRAGON_CHILD_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource src) {
        return this.isTeen() ? IafSoundRegistry.FIREDRAGON_TEEN_HURT
                : this.shouldDropLoot() ? IafSoundRegistry.FIREDRAGON_ADULT_HURT
                        : IafSoundRegistry.FIREDRAGON_CHILD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isTeen() ? IafSoundRegistry.FIREDRAGON_TEEN_DEATH
                : this.shouldDropLoot() ? IafSoundRegistry.FIREDRAGON_ADULT_DEATH
                        : IafSoundRegistry.FIREDRAGON_CHILD_DEATH;
    }

    @Override
    public SoundEvent getRoarSound() {
        return this.isTeen() ? IafSoundRegistry.FIREDRAGON_TEEN_ROAR
                : this.shouldDropLoot() ? IafSoundRegistry.FIREDRAGON_ADULT_ROAR
                        : IafSoundRegistry.FIREDRAGON_CHILD_ROAR;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[] {
                IAnimatedEntity.NO_ANIMATION, ANIMATION_EAT, ANIMATION_SPEAK, ANIMATION_BITE,
                ANIMATION_SHAKEPREY, ANIMATION_TAILWHACK, ANIMATION_FIRECHARGE,
                ANIMATION_WINGBLAST, ANIMATION_ROAR, ANIMATION_EPIC_ROAR
        };
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == IafItemRegistry.FIRE_STEW.get();
    }

    @Override
    protected void spawnDeathParticles() {
        for (int k = 0; k < 3; ++k) {
            double d2 = this.random.nextGaussian() * 0.02D;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            if (level().isClientSide) {
                this.level().addParticle(ParticleTypes.ENCHANT,
                        this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                        this.getY() + this.random.nextFloat() * this.getBbHeight(),
                        this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                        d2, d0, d1);
            }
        }
    }

    @Override
    protected void spawnBabyParticles() {
        for (int i = 0; i < 5; i++) {
            float radiusAdd = i * 0.15F;
            float headPosX = (float) (this.getX() + 1.8F * getRenderSize() * (0.3F + radiusAdd)
                    * net.minecraft.util.Mth.cos((float) ((getYRot() + 90) * Math.PI / 180)));
            float headPosZ = (float) (this.getY() + 1.8F * getRenderSize() * (0.3F + radiusAdd)
                    * net.minecraft.util.Mth.sin((float) ((getYRot() + 90) * Math.PI / 180)));
            float headPosY = (float) (this.getZ() + 0.5 * getRenderSize() * 0.3F);
            level().addParticle(ParticleTypes.ENCHANT, headPosX, headPosY, headPosZ, 0, 0, 0);
        }
    }

    @Override
    public ItemStack getSkull() {
        return new ItemStack(IafItemRegistry.DRAGON_SKULL_GOLD.get());
    }

    @Override
    public Item getBloodItem() {
        return IafItemRegistry.GOLD_DRAGON_BLOOD.get();
    }

    @Override
    public Item getFleshItem() {
        return IafItemRegistry.GOLD_DRAGON_FLESH.get();
    }

    @Override
    public ItemLike getHeartItem() {
        return IafItemRegistry.GOLD_DRAGON_HEART.get();
    }

    @Override
    public ResourceLocation getDeadLootTable() {
        if (this.getDeathStage() >= (this.getAgeInDays() / 5) / 2) {
            return SKELETON_LOOT;
        } else {
            return isMale() ? MALE_LOOT : FEMALE_LOOT;
        }
    }
}
