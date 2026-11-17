/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.flywheel.backend.engine.DrawManager
 *  com.zurrtum.create.client.flywheel.backend.engine.EngineImpl
 *  com.zurrtum.create.client.flywheel.backend.engine.LightStorage
 *  net.minecraft.world.level.LevelAccessor
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.ryanhcode.sable.mixin.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.backend.engine.DrawManager;
import com.zurrtum.create.client.flywheel.backend.engine.EngineImpl;
import com.zurrtum.create.client.flywheel.backend.engine.LightStorage;
import dev.ryanhcode.sable.compatibility.flywheel.SableFlywheelLightStorage;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EngineImpl.class})
public class EngineImplMixin {
    @Shadow
    @Final
    @Mutable
    private LightStorage lightStorage;

    @Inject(require = 0, method={"<init>"}, at={@At(value="TAIL")})
    private void sable$replaceLightStorage(LevelAccessor level, DrawManager drawManager, int maxOriginDistance, CallbackInfo ci) {
        this.lightStorage.delete();
        this.lightStorage = new SableFlywheelLightStorage(level);
    }
}

