package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static com.github.alexthe666.iceandfire.entity.tile.IafTileEntityRegistry.DRAGONFORGE_CORE;

/**
 * 龙锻炉核心 - 统一方块，通过BlockState属性区分类型和激活状态
 * ACTIVE: 组装完成且正在工作时为true
 * TYPE: 当前龙类型（龙吐息时动态设置）
 */
public class BlockDragonforgeCore extends BaseEntityBlock implements IDragonProof, INoTab {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<DragonForgeType> TYPE = EnumProperty.create("type", DragonForgeType.class);

    private static boolean keepInventory;

    public BlockDragonforgeCore() {
        super(
            Properties
                .of()
                .mapColor(MapColor.METAL)
                .dynamicShape()
                .strength(40, 500)
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(ACTIVE) ? 15 : 0)
        );
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(ACTIVE, Boolean.FALSE)
            .setValue(TYPE, DragonForgeType.NONE));
    }

    /**
     * 设置Core的类型和激活状态（通过BlockState属性切换，不替换方块）
     */
    public static void setState(DragonForgeType type, boolean active, Level worldIn, BlockPos pos) {
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        keepInventory = true;

        BlockState newState = IafBlockRegistry.DRAGONFORGE_CORE.get().defaultBlockState()
            .setValue(ACTIVE, active)
            .setValue(TYPE, type != null ? type : DragonForgeType.NONE);

        worldIn.setBlock(pos, newState, 3);

        keepInventory = false;

        if (tileentity != null) {
            tileentity.clearRemoved();
            worldIn.setBlockEntity(tileentity);
        }
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            if (worldIn.isClientSide) {
                IceAndFire.PROXY.setRefrencedTE(worldIn.getBlockEntity(pos));
            } else {
                MenuProvider provider = this.getMenuProvider(state, worldIn, pos);
                if (provider != null) {
                    player.openMenu(provider);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public ItemStack getItem(Level worldIn, BlockPos pos, BlockState state) {
        return new ItemStack(IafBlockRegistry.DRAGONFORGE_CORE.get().asItem());
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(@NotNull BlockState state, Level worldIn, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!keepInventory) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof TileEntityDragonforge) {
                Containers.dropContents(worldIn, pos, (TileEntityDragonforge) tileentity);
                worldIn.updateNeighbourForOutputSignal(pos, this);
                worldIn.removeBlockEntity(pos);
            }
        }
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState blockState, Level worldIn, @NotNull BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(worldIn.getBlockEntity(pos));
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public boolean shouldBeInTab() {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, TYPE);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> entityType) {
        return createTickerHelper(entityType, DRAGONFORGE_CORE.get(), TileEntityDragonforge::tick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TileEntityDragonforge(pos, state);
    }
}
