package dev.ryanhcode.sable.sublevel.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;

/**
 * 26.2 adaptation of 26.3's SubLevelBatchedTerrainRenderer.
 *
 * 26.3 built {@link net.minecraft.client.renderer.chunk.ChunkSectionsToRender} instances against the
 * renderpearl API ({@code com.mojang.renderpearl.api.*}) plus {@code DynamicGpuData} and the
 * {@code client.renderer.oit} package, none of which exist on 26.2. This port rebuilds the exact same
 * pipeline on 26.2's blaze3d API:
 * <ul>
 *   <li>per-section uniforms via {@link DynamicUniforms#writeChunkSections} (public constructor allows a
 *       private instance, mirroring 26.3's own {@code ownUniforms});</li>
 *   <li>{@code RenderPass.Draw} keeps the same shape (slot, vertex/index buffer, IndexType, firstIndex,
 *       indexCount, baseVertex, uniform uploader);</li>
 *   <li>26.2's {@code ChunkSectionsToRender.renderGroup(group, sampler)} handles the translucent
 *       draw-order reversal internally, so 26.3's {@code reverseTranslucent} flag is not needed;</li>
 *   <li>26.2 has no OIT stage ({@code OitStage}/{@code OitRenderPassProvider} are 26.3-only), so
 *       {@code renderOit} has no counterpart - translucent sub-level terrain is drawn through the
 *       regular translucency pipeline.</li>
 * </ul>
 */
public final class SubLevelBatchedTerrainRenderer {
    private static final List<ChunkSectionsToRender> FRAME_GROUPS = new ArrayList<ChunkSectionsToRender>();
    private static boolean insideRender;
    private static final long RESORT_INTERVAL_MILLIS = 500L;
    private static long lastResortMillis;
    private static final Map<LevelPlot, LongSet> FRAME_VISIBLE_SECTIONS;

    private SubLevelBatchedTerrainRenderer() {
    }

    public static void beginSubmitFrame() {
        FRAME_VISIBLE_SECTIONS.clear();
        FRAME_GROUPS.clear();
    }

    public static boolean isEnabled() {
        return (Boolean)SableClientConfig.SUB_LEVEL_BATCHED_SECTIONS.get();
    }

    public static boolean isSectionBatchable(SectionRenderDispatcher dispatcher, SectionRenderDispatcher.RenderSection renderSection) {
        if (dispatcher == null || renderSection == null) {
            return false;
        }
        SectionMesh mesh = renderSection.getSectionMesh();
        if (mesh == null || mesh == CompiledSectionMesh.UNCOMPILED) {
            return false;
        }
        dispatcher.lock();
        try {
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                if (mesh.getSectionDraw(layer) == null || dispatcher.getRenderSectionSlice(mesh, layer) == null) continue;
                return true;
            }
        }
        finally {
            dispatcher.unlock();
        }
        return false;
    }

    public static void build(Matrix4fc viewRotation, Vec3 cameraPos, SectionRenderDispatcher dispatcher, GpuTextureView atlasView) {
        FRAME_GROUPS.clear();
        if (!isEnabled() || viewRotation == null || cameraPos == null || dispatcher == null || atlasView == null) {
            return;
        }
        int atlasWidth = atlasView.getWidth(0);
        int atlasHeight = atlasView.getHeight(0);
        DynamicUniforms uniforms = ownUniforms();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        boolean occlusionCulling = (Boolean)SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
        double renderDistance = (Double)SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
        double renderDistanceSq = renderDistance <= 0.0 ? Double.POSITIVE_INFINITY : renderDistance * renderDistance;
        Frustum frustum = Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum();
        long now = Util.getMillis();
        boolean resortTranslucency = now - lastResortMillis >= RESORT_INTERVAL_MILLIS;
        if (resortTranslucency) {
            lastResortMillis = now;
        }
        dispatcher.lock();
        try {
            for (ClientSubLevel subLevel : container.getAllSubLevels()) {
                ChunkSectionsToRender group = sable$buildGroup(
                    subLevel, viewRotation, cameraPos, dispatcher, frustum, now,
                    occlusionCulling, renderDistanceSq, atlasView, atlasWidth, atlasHeight, resortTranslucency, uniforms
                );
                if (group != null) {
                    FRAME_GROUPS.add(group);
                }
            }
        }
        finally {
            dispatcher.unlock();
        }
    }

    private static ChunkSectionsToRender sable$buildGroup(
        ClientSubLevel subLevel,
        Matrix4fc viewRotation,
        Vec3 cameraPos,
        SectionRenderDispatcher dispatcher,
        Frustum frustum,
        long now,
        boolean occlusionCulling,
        double renderDistanceSq,
        GpuTextureView atlasView,
        int atlasWidth,
        int atlasHeight,
        boolean resortTranslucency,
        DynamicUniforms uniforms
    ) {
        ClientLevelPlot plot = subLevel.getPlot();
        BoundingBox3ic bounds = plot.getBoundingBox();
        if (bounds == null || bounds.volume() <= 0) {
            return null;
        }
        if (!(subLevel.getRenderData() instanceof VanillaChunkedSubLevelRenderData chunkedRenderData)) {
            return null;
        }
        Pose3dc pose = subLevel.renderPose();
        Vector3dc position = pose.position();
        Vector3dc rotationPoint = pose.rotationPoint();
        Vector3dc scale = pose.scale();
        Vec3 worldCenter = pose.transformPosition(
            new Vec3(
                (bounds.minX() + bounds.maxX() + 1) * 0.5,
                (bounds.minY() + bounds.maxY() + 1) * 0.5,
                (bounds.minZ() + bounds.maxZ() + 1) * 0.5
            )
        );
        if (worldCenter.distanceToSqr(cameraPos) > renderDistanceSq) {
            return null;
        }
        LongSet visibleSections = occlusionCulling ? computeVisibleSections(plot, pose, cameraPos) : null;
        // 26.2's terrain.vsh computes pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset and then
        // gl_Position = ProjMat * ModelViewMat * pos, where (CameraBlockPos, CameraOffset) is the camera split into
        // an integer block part and a fractional part, and fog runs on pos. Vanilla feeds ModelViewMat = pure view
        // rotation and ChunkPosition = world origin so pos collapses to the camera-relative world position.
        // A sub-level needs its full plot transform (rotate + scale + translate), which cannot ride on the integer
        // ChunkPosition, so we bake it into ModelViewMat and neutralise the shader's forced camera terms:
        //   ChunkPosition = subLevelOrigin + cameraBlock   ->  pos = plotLocalPos + cameraOffset (small, fog-safe)
        //   ModelViewMat  = viewRot * plotTransform * translate(-cameraOffset)
        //                 -> gl_Position = Proj * viewRot * plotTransform * plotLocalPos  (exactly 26.3's result)
        BlockPos cameraBlock = BlockPos.containing(cameraPos);
        double cameraOffX = cameraPos.x - cameraBlock.getX();
        double cameraOffY = cameraPos.y - cameraBlock.getY();
        double cameraOffZ = cameraPos.z - cameraBlock.getZ();
        Matrix4f plotTransform = new Matrix4f()
            .translate((float)(position.x() - cameraPos.x), (float)(position.y() - cameraPos.y), (float)(position.z() - cameraPos.z))
            .rotate((Quaternionfc)new Quaternionf().set(pose.orientation()))
            .scale((float)scale.x(), (float)scale.y(), (float)scale.z())
            .translate((float)(cameraPos.x - rotationPoint.x()), (float)(cameraPos.y - rotationPoint.y()), (float)(cameraPos.z - rotationPoint.z()));
        // LINK-TEST: raw view rotation, no plot transform - sub-level blocks should appear at their
        // plot-local coordinates (wrong place, but proves the draw path works).
        Matrix4f modelView = new Matrix4f(viewRotation);
        ArrayList<DynamicUniforms.ChunkSectionInfo> infos = new ArrayList<DynamicUniforms.ChunkSectionInfo>();
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawsPerLayer = new EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>>(
            ChunkSectionLayer.class
        );
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawsPerLayer.put(layer, new Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>());
        }
        int maxIndices = 0;
        LongArrayList drawOrder = sable$sectionDrawOrder(plot, visibleSections, pose, cameraPos, frustum);
        for (int orderIndex = 0; orderIndex < drawOrder.size(); orderIndex++) {
            long sectionNode = drawOrder.getLong(orderIndex);
            SectionRenderDispatcher.RenderSection renderSection = chunkedRenderData.getRenderSection(SectionPos.of(sectionNode));
            if (!isSectionBatchable(dispatcher, renderSection)) {
                continue;
            }
            SectionMesh mesh = renderSection.getSectionMesh();
            BlockPos origin = renderSection.getRenderOrigin();
            int infoIndex = -1;
            if (resortTranslucency && renderSection.hasTranslucentGeometry()) {
                renderSection.resortTransparency();
            }
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                int firstIndex;
                com.mojang.blaze3d.IndexType indexType;
                GpuBuffer indexBuffer;
                SectionRenderDispatcher.RenderSectionBufferSlice slice;
                SectionMesh.SectionDraw sectionDraw = mesh.getSectionDraw(layer);
                if (sectionDraw == null || (slice = dispatcher.getRenderSectionSlice(mesh, layer)) == null || sectionDraw.hasCustomIndexBuffer() && slice.indexBuffer() == null) {
                    continue;
                }
                if (infoIndex == -1) {
                    infoIndex = infos.size();
                    infos.add(
                        new DynamicUniforms.ChunkSectionInfo(
                            modelView,
                            origin.getX() + cameraBlock.getX(),
                            origin.getY() + cameraBlock.getY(),
                            origin.getZ() + cameraBlock.getZ(),
                            renderSection.getVisibility(now),
                            atlasWidth,
                            atlasHeight
                        )
                    );
                }
                VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                GpuBuffer vertexBuffer = slice.vertexBuffer();
                if (sectionDraw.hasCustomIndexBuffer()) {
                    indexBuffer = slice.indexBuffer();
                    indexType = sectionDraw.indexType();
                    firstIndex = (int)(slice.indexBufferOffset() / (long)indexType.bytes);
                } else {
                    indexBuffer = null;
                    indexType = null;
                    firstIndex = 0;
                    maxIndices = Math.max(maxIndices, sectionDraw.indexCount());
                }
                int baseVertex = (int)(slice.vertexBufferOffset() / (long)vertexFormat.getVertexSize());
                int uniformIndex = infoIndex;
                drawsPerLayer
                    .get(layer)
                    .computeIfAbsent(infoIndex, k -> new ArrayList<RenderPass.Draw<GpuBufferSlice[]>>())
                    .add(
                        new RenderPass.Draw<GpuBufferSlice[]>(
                            0,
                            vertexBuffer,
                            indexBuffer,
                            indexType,
                            firstIndex,
                            sectionDraw.indexCount(),
                            baseVertex,
                            (sectionInfos, uploader) -> {
                            uploader.upload("ChunkSection", sectionInfos[uniformIndex]);
                            }
                        )
                    );
            }
        }
        if (infos.isEmpty()) {
            return null;
        }
        if (maxIndices > 0) {
            // 26.3 had RenderSystem.getSequentialBuffer(...).requestIndexCount(count); 26.2's
            // AutoStorageIndexBuffer exposes getBuffer(count) instead, which pre-warms the storage too.
            RenderSystem.getSequentialBuffer((PrimitiveTopology)PrimitiveTopology.QUADS).getBuffer(maxIndices);
        }
        GpuBufferSlice[] chunkSectionInfos = uniforms.writeChunkSections(infos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(atlasView, drawsPerLayer, maxIndices, chunkSectionInfos);
    }

    private static LongArrayList sable$sectionDrawOrder(LevelPlot plot, LongSet visibleSections, Pose3dc pose, Vec3 cameraPos, Frustum frustum) {
        Vec3 cameraPlot = pose.transformPositionInverse(cameraPos);
        LongArrayList nodes = new LongArrayList();
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) continue;
            ChunkPos chunkPos = chunk.getPos();
            LevelChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                LevelChunkSection section = sections[sectionIndex];
                if (section == null || section.hasOnlyAir()) continue;
                int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                long sectionNode = SectionPos.asLong(chunkPos.x(), sectionY, chunkPos.z());
                if (visibleSections != null && !visibleSections.contains(sectionNode)
                    || frustum != null && !frustum.isVisible(sectionBounds(pose, chunkPos, sectionY << 4))) {
                    continue;
                }
                nodes.add(sectionNode);
            }
        }
        nodes.sort((a, b) -> sable$compareSectionDistance(a, b, cameraPlot));
        return nodes;
    }

    private static double sable$sectionDistanceSq(long sectionNode, Vec3 cameraPlot) {
        double dx = (double)SectionPos.x(sectionNode) + 8.0 - cameraPlot.x;
        double dy = (double)SectionPos.y(sectionNode) + 8.0 - cameraPlot.y;
        double dz = (double)SectionPos.z(sectionNode) + 8.0 - cameraPlot.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static int sable$compareSectionDistance(long a, long b, Vec3 cameraPlot) {
        return Double.compare(sable$sectionDistanceSq(a, cameraPlot), sable$sectionDistanceSq(b, cameraPlot));
    }
    /**
     * 26.2 has no private DynamicGpuData like 26.3; the per-section uniform slices must live in
     * the same global DynamicUniforms storage that vanilla feeds, so the draw-time
     * RenderPass.UniformUploader.upload("ChunkSection", slice) path resolves against buffers the
     * frame graph already uploads. We append AFTER vanilla writes (no reset here - that would
     * wipe vanilla slices); the shared ring buffer is rotated by vanilla endFrame.
     */
    private static DynamicUniforms ownUniforms() {
        return RenderSystem.getDynamicUniforms();
    }

    public static void renderGroup(ChunkSectionLayerGroup layerGroup, com.mojang.blaze3d.textures.GpuSampler sampler) {
        if (insideRender || FRAME_GROUPS.isEmpty()) {
            return;
        }
        insideRender = true;
        try {
            for (int i = 0; i < FRAME_GROUPS.size(); i++) {
                FRAME_GROUPS.get(i).renderGroup(layerGroup, sampler);
            }
        }
        finally {
            insideRender = false;
        }
    }

    public static LongSet computeVisibleSections(LevelPlot plot, Pose3dc pose, Vec3 cameraPos) {
        LongSet cached = FRAME_VISIBLE_SECTIONS.get(plot);
        if (cached != null) {
            return cached;
        }
        LongSet visible = sable$scanVisibleSections(plot, pose, cameraPos);
        FRAME_VISIBLE_SECTIONS.put(plot, visible);
        return visible;
    }

    private static LongSet sable$scanVisibleSections(LevelPlot plot, Pose3dc pose, Vec3 cameraPos) {
        Long2ObjectOpenHashMap<LevelChunkSection> sections = new Long2ObjectOpenHashMap<LevelChunkSection>();
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) continue;
            LevelChunkSection[] chunkSections = chunk.getSections();
            for (int i = 0; i < chunkSections.length; i++) {
                LevelChunkSection section = chunkSections[i];
                if (section == null || section.hasOnlyAir()) continue;
                sections.put(SectionPos.asLong(chunk.getPos().x(), chunk.getSectionYFromSectionIndex(i), chunk.getPos().z()), section);
            }
        }
        if (sections.isEmpty()) {
            return LongSets.EMPTY_SET;
        }
        Vec3 cameraPlot = pose.transformPositionInverse(cameraPos);
        long start = SectionPos.asLong(Mth.floor(cameraPlot.x) >> 4, Mth.floor(cameraPlot.y) >> 4, Mth.floor(cameraPlot.z) >> 4);
        if (!sections.containsKey(start)) {
            return new LongOpenHashSet((LongCollection)sections.keySet());
        }
        LongOpenHashSet visible = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        visible.add(start);
        queue.enqueue(start);
        while (!queue.isEmpty()) {
            long node = queue.dequeueLong();
            LevelChunkSection section = sections.get(node);
            int x = SectionPos.x(node);
            int y = SectionPos.y(node);
            int z = SectionPos.z(node);
            for (Direction direction : Direction.values()) {
                long next;
                if (faceOpaque(section, direction) || !sections.containsKey(next = SectionPos.asLong(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())) || !visible.add(next)) continue;
                queue.enqueue(next);
            }
        }
        return visible;
    }

    private static boolean faceOpaque(LevelChunkSection section, Direction direction) {
        switch (direction) {
            case DOWN: {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(x, 0, z).canOcclude()) continue;
                        return false;
                    }
                }
                break;
            }
            case UP: {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(x, 15, z).canOcclude()) continue;
                        return false;
                    }
                }
                break;
            }
            case NORTH: {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        if (section.getBlockState(x, y, 0).canOcclude()) continue;
                        return false;
                    }
                }
                break;
            }
            case SOUTH: {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        if (section.getBlockState(x, y, 15).canOcclude()) continue;
                        return false;
                    }
                }
                break;
            }
            case WEST: {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(0, y, z).canOcclude()) continue;
                        return false;
                    }
                }
                break;
            }
            case EAST: {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(15, y, z).canOcclude()) continue;
                        return false;
                    }
                }
                break;
            }
        }
        return true;
    }

    public static AABB sectionBounds(Pose3dc pose, ChunkPos chunkPos, int sectionMinY) {
        int minX = chunkPos.x() << 4;
        int minZ = chunkPos.z() << 4;
        double x0 = Double.POSITIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY;
        double z0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double y1 = Double.NEGATIVE_INFINITY;
        double z1 = Double.NEGATIVE_INFINITY;
        for (int dx = 0; dx <= 16; dx += 16) {
            for (int dy = 0; dy <= 16; dy += 16) {
                for (int dz = 0; dz <= 16; dz += 16) {
                    Vec3 corner = pose.transformPosition(new Vec3((double)(minX + dx), (double)(sectionMinY + dy), (double)(minZ + dz)));
                    x0 = Math.min(x0, corner.x);
                    y0 = Math.min(y0, corner.y);
                    z0 = Math.min(z0, corner.z);
                    x1 = Math.max(x1, corner.x);
                    y1 = Math.max(y1, corner.y);
                    z1 = Math.max(z1, corner.z);
                }
            }
        }
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    static {
        FRAME_VISIBLE_SECTIONS = new IdentityHashMap<LevelPlot, LongSet>();
    }
}