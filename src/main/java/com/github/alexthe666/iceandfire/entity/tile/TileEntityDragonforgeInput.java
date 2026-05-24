package com.github.alexthe666.iceandfire.entity.tile;

import com.github.alexthe666.iceandfire.block.BlockDragonforgeInput;
import com.github.alexthe666.iceandfire.block.DragonForgeType;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * 龙锻炉输入口方块实体 - 吸引龙并接收龙吐息
 * 统一方块，通过BlockState的TYPE属性区分当前龙类型
 */
public class TileEntityDragonforgeInput extends BlockEntity {
    private static final int LURE_DISTANCE = 50;
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private int ticksSinceDragonFire;
    private TileEntityDragonforge core = null;

    public TileEntityDragonforgeInput(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.DRAGONFORGE_INPUT.get(), pos, state);
    }

    /** 被龙吐息击中时调用，传递能量和龙类型 */
    public void onHitWithFlame(DragonForgeType type) {
        if (core != null) {
            core.transferPower(1, type);
            updateInputType(type);
        }
    }

    /** 兼容旧调用 */
    public void onHitWithFlame() {
        onHitWithFlame(getDragonForgeType());
    }

    /** 更新输入口BlockState的TYPE属性 */
    private void updateInputType(DragonForgeType type) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof BlockDragonforgeInput && type != null) {
            BlockState newState = state
                .setValue(BlockDragonforgeInput.TYPE, type)
                .setValue(BlockDragonforgeInput.ACTIVE, true);
            if (state != newState) {
                level.setBlockAndUpdate(worldPosition, newState);
            }
        }
    }

    /** 从BlockState获取当前龙类型 */
    private DragonForgeType getDragonForgeType() {
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(BlockDragonforgeInput.TYPE)) {
            return state.getValue(BlockDragonforgeInput.TYPE);
        }
        return DragonForgeType.NONE;
    }

    /** 获取旧式int类型ID（用于龙类型匹配） */
    private int getDragonType() {
        DragonForgeType type = getDragonForgeType();
        if (type.isActive()) {
            return type.getLegacyId();
        }
        if (core != null) {
            return core.fireType;
        }
        return 0;
    }

    public static void tick(final Level level, final BlockPos position, final BlockState state, final TileEntityDragonforgeInput forgeInput) {
        if (forgeInput.core == null) {
            forgeInput.core = forgeInput.getConnectedTileEntity(position);
        }

        if (forgeInput.ticksSinceDragonFire > 0) {
            forgeInput.ticksSinceDragonFire--;
        }

        if ((forgeInput.ticksSinceDragonFire == 0 || forgeInput.core == null) && forgeInput.isActive()) {
            BlockEntity tileentity = level.getBlockEntity(position);
            BlockState deactivated = state.setValue(BlockDragonforgeInput.ACTIVE, false);
            level.setBlockAndUpdate(position, deactivated);
            if (tileentity != null) {
                tileentity.clearRemoved();
                level.setBlockEntity(tileentity);
            }
        }

        if (forgeInput.isAssembled()) {
            forgeInput.lureDragons();
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    /** 吸引附近符合条件的龙（统一方块，吸引所有类型的龙） */
    protected void lureDragons() {
        Vec3 targetPosition = new Vec3(
            this.getBlockPos().getX() + 0.5F,
            this.getBlockPos().getY() + 0.5F,
            this.getBlockPos().getZ() + 0.5F
        );

        AABB searchArea = new AABB(
            (double) worldPosition.getX() - LURE_DISTANCE,
            (double) worldPosition.getY() - LURE_DISTANCE,
            (double) worldPosition.getZ() - LURE_DISTANCE,
            (double) worldPosition.getX() + LURE_DISTANCE,
            (double) worldPosition.getY() + LURE_DISTANCE,
            (double) worldPosition.getZ() + LURE_DISTANCE
        );

        boolean dragonSelected = false;

        for (EntityDragonBase dragon : level.getEntitiesOfClass(EntityDragonBase.class, searchArea)) {
            if (!dragonSelected && (dragon.isChained() || dragon.isTame()) && canSeeInput(dragon, targetPosition)) {
                dragon.burningTarget = this.worldPosition;
                dragonSelected = true;
            } else if (dragon.burningTarget == this.worldPosition) {
                dragon.burningTarget = null;
                dragon.setBreathingFire(false);
            }
        }
    }

    public boolean isAssembled() {
        return (core != null && core.assembled() && core.canSmelt());
    }

    public void resetCore() {
        core = null;
    }

    private boolean canSeeInput(EntityDragonBase dragon, Vec3 target) {
        if (target != null) {
            HitResult rayTrace = this.level.clip(new ClipContext(dragon.getHeadPosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
            double distance = dragon.getHeadPosition().distanceTo(rayTrace.getLocation());

            return distance < 10.0F + dragon.getBbWidth();
        }

        return false;
    }

    private boolean isActive() {
        BlockState state = level.getBlockState(worldPosition);
        return state.getBlock() instanceof BlockDragonforgeInput && state.getValue(BlockDragonforgeInput.ACTIVE);
    }

    private TileEntityDragonforge getConnectedTileEntity(final BlockPos position) {
        for (Direction facing : HORIZONTALS) {
            if (level.getBlockEntity(position.relative(facing)) instanceof TileEntityDragonforge forge) {
                return forge;
            }
        }

        return null;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull final Capability<T> capability, @Nullable final Direction facing) {
        if (core != null && capability == ForgeCapabilities.ITEM_HANDLER) {
            return core.getCapability(capability, facing);
        }

        return super.getCapability(capability, facing);
    }
}
