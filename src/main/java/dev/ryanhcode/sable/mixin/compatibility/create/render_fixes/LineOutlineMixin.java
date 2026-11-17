package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.outliner.LineOutline;
import com.zurrtum.create.client.catnip.outliner.Outline;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={LineOutline.class})
public abstract class LineOutlineMixin
extends Outline {
    @Shadow
    @Final
    protected Vector3f diffPosTemp;

    public void sable$bufferSublevelLine(PoseStack poseStack, VertexConsumer consumer, Vec3 camera, Vector3d start, Vector3d end, float width, int color, int lightmap, boolean disableNormals) {
        ActiveSableCompanion helper = Sable.HELPER;
        ClientSubLevel startSubLevel = helper.getContainingClient((Vector3dc)start);
        ClientSubLevel endSubLevel = helper.getContainingClient((Vector3dc)end);
        if (startSubLevel != null) {
            startSubLevel.renderPose().transformPosition(start);
        }
        if (endSubLevel != null) {
            endSubLevel.renderPose().transformPosition(end);
        }
        Vector3f diff = this.diffPosTemp;
        diff.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        float length = Mth.sqrt((float)(diff.x() * diff.x() + diff.y() * diff.y() + diff.z() * diff.z()));
        float hAngle = AngleHelper.deg((double)Mth.atan2((double)diff.x(), (double)diff.z()));
        float hDistance = Mth.sqrt((float)(diff.x() * diff.x() + diff.z() * diff.z()));
        float vAngle = AngleHelper.deg((double)Mth.atan2((double)hDistance, (double)diff.y())) - 90.0f;
        poseStack.pushPose();
        ((PoseTransformStack)((PoseTransformStack)TransformStack.of((PoseStack)poseStack).translate(start.x - camera.x, start.y - camera.y, start.z - camera.z)).rotateYDegrees(hAngle)).rotateXDegrees(vAngle);
        this.bufferCuboidLine(poseStack.last(), consumer, new Vector3f(), Direction.SOUTH, length, width, color, lightmap, disableNormals);
        poseStack.popPose();
    }
}
