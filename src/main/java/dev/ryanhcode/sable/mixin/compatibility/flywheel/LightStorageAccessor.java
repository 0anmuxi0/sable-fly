/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.flywheel.backend.engine.LightDataCollector
 *  com.zurrtum.create.client.flywheel.backend.engine.LightStorage
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package dev.ryanhcode.sable.mixin.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.backend.engine.LightDataCollector;
import com.zurrtum.create.client.flywheel.backend.engine.LightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={LightStorage.class})
public interface LightStorageAccessor {
    @Accessor
    public LightDataCollector getCollector();

    @Accessor
    public void setNeedsLutRebuild(boolean var1);
}

