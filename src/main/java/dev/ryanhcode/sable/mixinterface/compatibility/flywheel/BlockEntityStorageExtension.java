/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext
 */
package dev.ryanhcode.sable.mixinterface.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import dev.ryanhcode.sable.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.SubLevel;

public interface BlockEntityStorageExtension {
    public void sable$setPlanVisualizationContext(VisualizationContext var1);

    public SubLevelEmbedding sable$getEmbeddingInfo(SubLevel var1);

    public void sable$preFlywheelFrame();
}

