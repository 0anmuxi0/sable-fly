package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flags the AO cache while a sub-level section is being compiled so
 * ModelBlockRendererCacheExtension marks the compiled geometry with the sub-level shading flag.
 *
 * <p>26.2 moved the compile task out of SectionRenderer into SectionCompiler.compile(...)
 * and relocated the AO cache ThreadLocal from ModelBlockRenderer (field_4179/class_4303 in
 * 1.21.1) to BlockModelLighter.CACHE. The cache lives in a different class than this mixin's
 * target, so it is reached reflectively (two lookups per compiled section, negligible).</p>
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @Unique
    private static ThreadLocal<?> sable$cacheField;

    @Unique
    private static Object sable$cacheValue() {
        try {
            if (sable$cacheField == null) {
                java.lang.reflect.Field f = Class.forName("net.minecraft.client.renderer.block.BlockModelLighter")
                        .getDeclaredField("CACHE");
                f.setAccessible(true);
                sable$cacheField = (ThreadLocal<?>) f.get(null);
            }
            return sable$cacheField.get();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Inject(method = "compile", at = @At("HEAD"))
    private void sable$preCompile(SectionPos sectionPos, net.minecraft.client.renderer.chunk.RenderSectionRegion region, com.mojang.blaze3d.vertex.VertexSorting vertexSorting, SectionBufferBuilderPack pack,
                                  CallbackInfoReturnable<SectionCompiler.Results> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        LevelPlot plot = container.getPlot(new ChunkPos(sectionPos.getX() >> 4, sectionPos.getZ() >> 4));
        Object cache = sable$cacheValue();
        if (cache instanceof dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension ext) {
            ext.sable$setOnSubLevel(plot != null);
        }
    }

    @Inject(method = "compile", at = @At("TAIL"))
    private void sable$postCompile(SectionPos sectionPos, net.minecraft.client.renderer.chunk.RenderSectionRegion region, com.mojang.blaze3d.vertex.VertexSorting vertexSorting, SectionBufferBuilderPack pack,
                                   CallbackInfoReturnable<SectionCompiler.Results> cir) {
        Object cache = sable$cacheValue();
        if (cache instanceof dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension ext) {
            ext.sable$setOnSubLevel(false);
        }
    }
}