/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.zurrtum.create.content.contraptions.Contraption
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.ryanhcode.sable.mixin.compatibility.create.sticker;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.contraptions.Contraption;
import dev.ryanhcode.sable.mixinterface.compatibility.create.StickerBlockEntityExtension;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={Contraption.class})
public class ContraptionMixin {
    @WrapOperation(require = 0, method={"getBlockEntityNBT"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/block/entity/BlockEntity;saveWithFullMetadata(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;")})
    private CompoundTag sable$saveStickerNBT(BlockEntity instance, HolderLookup.Provider registries, Operation<CompoundTag> original) {
        if (instance instanceof StickerBlockEntityExtension) {
            StickerBlockEntityExtension extension = (StickerBlockEntityExtension)instance;
            extension.sable$saveToContraption(registries);
        }
        return (CompoundTag)original.call(instance, registries);
    }
}

