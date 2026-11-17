/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorShape
 *  net.minecraft.core.BlockPos
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package dev.ryanhcode.sable.mixin.compatibility.create.frogports;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.kinetics.chainConveyor.ChainConveyorShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={ChainConveyorShape.class})
public interface ChainConveyorShapeAccessor {
    @Invoker
    public void invokeSubmitOutline(Minecraft var1, BlockPos var2, PoseStack var3, SubmitNodeCollector var4, float var5);
}

