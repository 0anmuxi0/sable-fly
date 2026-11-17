/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.JOMLConversion
 *  com.zurrtum.create.client.catnip.placement.PlacementClient
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3dc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import com.zurrtum.create.client.catnip.placement.PlacementClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={PlacementClient.class})
public class PlacementClientMixin {
    @Redirect(require = 0, method={"drawDirectionIndicator"}, at=@At(value="INVOKE", target="Lcom/zurrtum/create/catnip/math/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"), remap=false)
    private static Vec3 sable$projectLastTargetedPos(Vec3i pos) {
        return JOMLConversion.toMojang((Vector3dc)Sable.HELPER.projectOutOfSubLevel((Level)Minecraft.getInstance().level, JOMLConversion.atCenterOf((Vec3i)pos)));
    }
}

