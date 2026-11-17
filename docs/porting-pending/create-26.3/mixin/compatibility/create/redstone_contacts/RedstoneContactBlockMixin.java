/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.AllBlockEntityTypes
 *  com.zurrtum.create.content.redstone.contact.RedstoneContactBlock
 *  com.zurrtum.create.foundation.block.IBE
 *  com.zurrtum.create.foundation.block.WrenchableDirectionalBlock
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 */
package dev.ryanhcode.sable.mixin.compatibility.create.redstone_contacts;

import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.content.redstone.contact.RedstoneContactBlock;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.block.WrenchableDirectionalBlock;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.redstone_contact.RedstoneContactBlockEntity;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.redstone_contact.RedstoneContactBlockEntityTypeGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={RedstoneContactBlock.class})
public class RedstoneContactBlockMixin
extends WrenchableDirectionalBlock
implements IBE<RedstoneContactBlockEntity> {
    @Unique
    private static final AllBlockEntityTypes sable$cursed = new AllBlockEntityTypes();

    public RedstoneContactBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public Class<RedstoneContactBlockEntity> getBlockEntityClass() {
        return RedstoneContactBlockEntity.class;
    }

    public BlockEntityType<? extends RedstoneContactBlockEntity> getBlockEntityType() {
        return (BlockEntityType)((RedstoneContactBlockEntityTypeGetter)sable$cursed).sable$getRedstoneContactType().get();
    }

    public <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level level, BlockState p_153213_, BlockEntityType<S> p_153214_) {
        if (!level.isClientSide) {
            return super.getTicker(level, p_153213_, p_153214_);
        }
        return null;
    }
}

