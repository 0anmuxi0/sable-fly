/*
 * Decompiled with CFR 0.152.
 * 
 * Sable-Fly 26.3 移植说明：
 * 原 @Redirect 目标是 PoseTransformStack.translate(Vec3)Translate，
 * Create 26.3 的 TrackTargetingClient.render(Minecraft, PoseStack, SubmitNodeCollector, Vec3)
 * 内已不再使用 PoseTransformStack（改用 PoseStack），该类中不存在该调用点。
 * 目标方法彻底不存在，故移除注解但保留方法体。
 */
package dev.ryanhcode.sable.mixin.compatibility.create.tracks;

import com.zurrtum.create.client.content.trains.track.TrackTargetingClient;
import com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack;
import com.zurrtum.create.client.flywheel.lib.transform.Translate;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={TrackTargetingClient.class})
public class TrackTargetingClientMixin {
    private static Translate sable$manipulateMatrixStack(PoseTransformStack instance, Vec3 vec3, Minecraft minecraft, BlockPos pos, Vec3 camera) {
        ClientLevel level = minecraft.level;
        SubLevel subLevel = Sable.HELPER.getContaining((Level)level, (Vec3i)pos);
        if (subLevel instanceof ClientSubLevel) {
            ClientSubLevel clientSubLevel = (ClientSubLevel)subLevel;
            Pose3dc renderPose = clientSubLevel.renderPose();
            Vec3 renderPos = renderPose.transformPosition(Vec3.atLowerCornerOf((Vec3i)pos));
            Quaternionf renderOrientation = new Quaternionf(renderPose.orientation());
            return ((PoseTransformStack)instance.translate(renderPos.x() - camera.x(), renderPos.y() - camera.y(), renderPos.z() - camera.z())).rotate((Quaternionfc)renderOrientation);
        }
        return instance.translate(vec3);
    }
}
