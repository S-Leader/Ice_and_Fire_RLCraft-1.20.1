package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforge;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforgeBrick;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * 龙锻炉外壳砖 - 统一方块，通过BlockState属性区分类型
 * GRILL: 是否处于组装态（显示通风口纹理）
 * TYPE: 当前关联的龙类型（由Core动态设置）
 */
public class BlockDragonforgeBricks extends BaseEntityBlock implements IDragonProof {

    public static final BooleanProperty GRILL = BooleanProperty.create("grill");
    public static final EnumProperty<DragonForgeType> TYPE = EnumProperty.create("type", DragonForgeType.class);

    public BlockDragonforgeBricks() {
        super(
            Properties
                .of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .dynamicShape()
                .strength(40, 500)
                .sound(SoundType.METAL)
        );
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(GRILL, Boolean.FALSE)
            .setValue(TYPE, DragonForgeType.NONE));
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, BlockHitResult resultIn) {
        TileEntityDragonforge forge = getConnectedTileEntity(worldIn, resultIn.getBlockPos());
        if (forge != null && forge.assembled()) {
            if (worldIn.isClientSide) {
                IceAndFire.PROXY.setRefrencedTE(worldIn.getBlockEntity(forge.getBlockPos()));
            } else {
                MenuProvider provider = this.getMenuProvider(forge.getBlockState(), worldIn, forge.getBlockPos());
                if (provider != null) {
                    player.openMenu(provider);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private TileEntityDragonforge getConnectedTileEntity(Level worldIn, BlockPos pos) {
        for (Direction facing : Direction.values()) {
            if (worldIn.getBlockEntity(pos.relative(facing)) instanceof TileEntityDragonforge forge) {
                if (forge.assembled()) {
                    return forge;
                }
            }
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRILL, TYPE);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TileEntityDragonforgeBrick(pos, state);
    }
}
