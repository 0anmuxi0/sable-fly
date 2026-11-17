/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  com.zurrtum.create.client.catnip.levelWrappers.SchematicLevel
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 */
package dev.ryanhcode.sable.mixin.compatibility.create.schematics;

import dev.ryanhcode.sable.mixinterface.compatibility.create.schematics.SchematicLevelExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import com.zurrtum.create.catnip.levelWrappers.SchematicLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={SchematicLevel.class})
public class SchematicLevelMixin
implements SchematicLevelExtension {
    @Unique
    private final List<SchematicLevelExtension.SchematicSubLevel> sable$subLevels = new ObjectArrayList();

    @Override
    public List<SchematicLevelExtension.SchematicSubLevel> sable$getSubLevels() {
        return this.sable$subLevels;
    }
}

