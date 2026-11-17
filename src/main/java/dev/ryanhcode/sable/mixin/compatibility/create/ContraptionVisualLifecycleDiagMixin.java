package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import dev.ryanhcode.sable.Sable;
import java.util.concurrent.atomic.AtomicLong;
import com.zurrtum.create.client.content.contraptions.render.ClientContraption;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 诊断：确认 ContraptionVisual 是否真的被创建、setupStructure 是否被执行。
 * 若这两条日志都不出现，说明结构并不是通过 Flywheel entity visual 渲染的。
 */
@Mixin(value = ContraptionVisual.class, remap = false)
public abstract class ContraptionVisualLifecycleDiagMixin {

    private static final AtomicLong CTOR = new AtomicLong();
    private static final AtomicLong SETUP = new AtomicLong();

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void sable$diagCtor(VisualizationContext ctx, AbstractContraptionEntity entity, float partialTick, CallbackInfo ci) {
        long n = CTOR.incrementAndGet();
        if (n <= 3 || n % 300 == 0) {
            Sable.LOGGER.info("[vispath-diag] ContraptionVisual constructed #{}, entity={} pos={}",
                    n, entity == null ? "null" : entity.getName().getString(), entity == null ? "?" : entity.position());
        }
    }

    @Inject(method = "setupStructure", at = @At("HEAD"), remap = false)
    private void sable$diagSetup(ClientContraption clientContraption, CallbackInfo ci) {
        long n = SETUP.incrementAndGet();
        if (n <= 3 || n % 300 == 0) {
            Sable.LOGGER.info("[vispath-diag] setupStructure called #{}", n);
        }
    }
}
