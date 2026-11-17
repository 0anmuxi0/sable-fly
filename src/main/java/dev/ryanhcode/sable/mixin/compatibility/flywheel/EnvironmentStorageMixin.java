/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.flywheel.backend.engine.embed.EnvironmentStorage
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 */
package dev.ryanhcode.sable.mixin.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.backend.engine.embed.EnvironmentStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value={EnvironmentStorage.class})
public class EnvironmentStorageMixin {
    @ModifyArg(require = 0, method={"<init>"}, at=@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/backend/engine/CpuArena;<init>(JI)V"), index=0)
    private long sable$overrideMatrixSize(long elementSizeBytes) {
        return 192L;
    }
}

