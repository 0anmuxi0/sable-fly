package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F3+B physics-body block outlines.
 *
 * The upstream ImGui gizmo screen (which offered sub-level structure inspection) was stripped by
 * the 3.0.0 build ("Gizmo mode is not available in this build"), so 26.2/26.3 ship no debug view
 * for the blocks that make up a physics body. This restores a lightweight equivalent on top of
 * 26.2's vanilla gizmo system: while showHitboxes is active (F3+B), every non-air block of every
 * loaded sub-level section is drawn as an 8-corner gizmo box transformed into world space by the
 * sub-level's render pose - the outlines follow the moving/rotating physics body exactly.
 */
@Mixin(value = {EntityHitboxDebugRenderer.class})
public abstract class PhysicsBlockOutlineMixin {

    @Unique
    private static final int SABLE$MAX_BOXES = 4096;

    @Unique
    private static final double SABLE$MAX_DISTANCE_SQR = 128.0 * 128.0;

    @Inject(
        method = {"showHitboxes"},
        at = {@At("TAIL")}
    )
    private void sable$drawPhysicsBlockOutlines(net.minecraft.world.entity.Entity entity, float partialTicks, boolean force, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        dev.ryanhcode.sable.api.sublevel.SubLevelContainer container = ((SubLevelContainerHolder)level).sable$getPlotContainer();
        if (container == null) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.mainCamera().position();
        int budget = SABLE$MAX_BOXES;
        List<dev.ryanhcode.sable.sublevel.SubLevel> subLevels = new ArrayList<>();
        container.getAllSubLevels().forEach(subLevels::add);
        for (dev.ryanhcode.sable.sublevel.SubLevel subLevelBase : subLevels) {
            if (budget <= 0) {
                break;
            }
            if (!(subLevelBase instanceof ClientSubLevel)) {
                continue;
            }
            ClientSubLevel subLevel = (ClientSubLevel)subLevelBase;
            ClientLevelPlot plot = subLevel.getPlot();
            if (plot == null) {
                continue;
            }
            Pose3dc pose = subLevel.renderPose(partialTicks);
            for (PlotChunkHolder holder : plot.getLoadedChunks()) {
                if (budget <= 0) {
                    break;
                }
                LevelChunk chunk = holder.getChunk();
                if (chunk == null) {
                    continue;
                }
                ChunkPos chunkPos = chunk.getPos();
                LevelChunkSection[] sections = chunk.getSections();
                for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    if (budget <= 0) {
                        break;
                    }
                    LevelChunkSection section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }
                    int sectionBaseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
                    AABB sectionBox = sable$transformedSectionBounds(pose, chunkPos, sectionBaseY);
                    if (sectionBox.distanceToSqr(camera) > SABLE$MAX_DISTANCE_SQR) {
                        continue;
                    }
                    for (int y = 0; y < 16 && budget > 0; y++) {
                        for (int z = 0; z < 16 && budget > 0; z++) {
                            for (int x = 0; x < 16 && budget > 0; x++) {
                                if (section.getBlockState(x, y, z).isAir()) {
                                    continue;
                                }
                                int wx = (chunkPos.x() << 4) + x;
                                int wy = sectionBaseY + y;
                                int wz = (chunkPos.z() << 4) + z;
                                Vec3[] corners = new Vec3[8];
                                int index = 0;
                                for (int cx = 0; cx <= 1; cx++) {
                                    for (int cy = 0; cy <= 1; cy++) {
                                        for (int cz = 0; cz <= 1; cz++) {
                                            Vector3d corner = new Vector3d(wx + cx, wy + cy, wz + cz);
                                            pose.transformPosition(corner);
                                            corners[index++] = new Vec3(corner.x, corner.y, corner.z);
                                        }
                                    }
                                }
                                Gizmos.addGizmo((Gizmo)new Sable$PhysicsBox(corners, GizmoStyle.stroke(0xFF00FF00)));
                                budget--;
                            }
                        }
                    }
                }
            }
        }
    }

    @Unique
    private static AABB sable$transformedSectionBounds(Pose3dc pose, ChunkPos chunkPos, int sectionMinY) {
        int minX = chunkPos.x() << 4;
        int minZ = chunkPos.z() << 4;
        double x0 = Double.POSITIVE_INFINITY, y0 = Double.POSITIVE_INFINITY, z0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY, y1 = Double.NEGATIVE_INFINITY, z1 = Double.NEGATIVE_INFINITY;
        for (int dx = 0; dx <= 16; dx += 16) {
            for (int dy = 0; dy <= 16; dy += 16) {
                for (int dz = 0; dz <= 16; dz += 16) {
                    Vector3d corner = pose.transformPosition(new Vector3d(minX + dx, sectionMinY + dy, minZ + dz));
                    x0 = Math.min(x0, corner.x); y0 = Math.min(y0, corner.y); z0 = Math.min(z0, corner.z);
                    x1 = Math.max(x1, corner.x); y1 = Math.max(y1, corner.y); z1 = Math.max(z1, corner.z);
                }
            }
        }
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    /**
     * 8-corner wireframe box gizmo, mirroring the private BoxGizmo record inside
     * EntityHitboxDebugRendererMixin (same emit layout: 12 stroke lines, fill when the style asks).
     */
    @Unique
    private record Sable$PhysicsBox(Vec3[] corners, GizmoStyle style) implements Gizmo {
        @Override
        public void emit(GizmoPrimitives primitives, float partialTick) {
            if (this.style.hasFill()) {
                int fill = this.style.multipliedFill(partialTick);
                primitives.addQuad(this.corners[4], this.corners[6], this.corners[7], this.corners[5], fill);
                primitives.addQuad(this.corners[0], this.corners[1], this.corners[3], this.corners[2], fill);
                primitives.addQuad(this.corners[0], this.corners[2], this.corners[6], this.corners[4], fill);
                primitives.addQuad(this.corners[1], this.corners[5], this.corners[7], this.corners[3], fill);
                primitives.addQuad(this.corners[0], this.corners[4], this.corners[5], this.corners[1], fill);
                primitives.addQuad(this.corners[2], this.corners[3], this.corners[7], this.corners[6], fill);
            }
            if (this.style.hasStroke()) {
                int stroke = this.style.multipliedStroke(partialTick);
                float w = this.style.strokeWidth();
                primitives.addLine(this.corners[0], this.corners[4], stroke, w);
                primitives.addLine(this.corners[0], this.corners[2], stroke, w);
                primitives.addLine(this.corners[0], this.corners[1], stroke, w);
                primitives.addLine(this.corners[4], this.corners[6], stroke, w);
                primitives.addLine(this.corners[6], this.corners[2], stroke, w);
                primitives.addLine(this.corners[2], this.corners[3], stroke, w);
                primitives.addLine(this.corners[3], this.corners[1], stroke, w);
                primitives.addLine(this.corners[1], this.corners[5], stroke, w);
                primitives.addLine(this.corners[5], this.corners[4], stroke, w);
                primitives.addLine(this.corners[3], this.corners[7], stroke, w);
                primitives.addLine(this.corners[5], this.corners[7], stroke, w);
                primitives.addLine(this.corners[6], this.corners[7], stroke, w);
            }
        }
    }
}
