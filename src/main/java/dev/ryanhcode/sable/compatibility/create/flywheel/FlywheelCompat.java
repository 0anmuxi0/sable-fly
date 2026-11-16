package dev.ryanhcode.sable.compatibility.create.flywheel;

import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualizerRegistryImpl;
import com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Static manager for the Sable sublevel x Create Flywheel (com.zurrtum packages)
 * embedding adapter (ported from Aeronautics for 26.1.2).
 *
 * <p>Flywheel registers visuals for block entities inside sublevel plot chunks
 * (virtual coordinates near +/-20.48M), but instance positions are computed as
 * plotVirtualPos - parentWorldRenderOrigin and the sublevel render pose is never applied,
 * so instances land ~20M off-screen. The fix mirrors official Sable 1.21.1: create each
 * sublevel BE visual through {@code VisualizationContext.createEmbedding(origin)} and
 * write the sublevel render pose into the embedding transform every frame (see the
 * mixins under mixin/compatibility/create).</p>
 *
 * <p>Whitelist: only BE types listed in {@link FlywheelWhitelist} are injected into the
 * parent-world manager; everything else keeps using Sable's own rendering pipeline.
 * Taking over every visual type at once made some physics bodies render less content
 * than before, because the embedding pipeline does not cover every visual shape yet.</p>
 */
public final class FlywheelCompat {
    private FlywheelCompat() {
    }

    /** {@code -Dsable.flywheelCompat=false} fully disables the adapter (default true). */
    public static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("sable.flywheelCompat", "true"));

    /** Retry interval for pending queueAdds: 20 frames ~ 1 second. */
    private static final int RETRY_INTERVAL_FRAMES = 20;

    private static int frameCounter;
    private static final Map<ClientSubLevel, FlywheelEmbeddingState> EMBEDDINGS =
            new Object2ObjectOpenHashMap<>();

    /** BEs queued while no embedding state existed yet; transferred into the state's
     * pending set by the storage mixin when the first visual is created. */
    private static final Map<BlockEntity, Boolean> PENDING_BEFORE_EMBEDDING =
            new Object2ObjectOpenHashMap<>();

    /**
     * Returns the ClientSubLevel whose plot chunks contain the given BE position.
     *
     * <p>{@code SubLevelContainer.getSubLevel(x, z)} takes chunk coordinates and bounds-checks
     * them ({@code x < 0 || x >= 1 << logSideLength -> null}); the plot grid origin sits at
     * chunk 160000+, so any parent-world position is out of bounds and yields null.
     * This method therefore never misclassifies parent-world BEs.</p>
     */
    public static ClientSubLevel getContainingSubLevel(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ClientLevel clientLevel)) {
            return null;
        }
        return getContainingSubLevel(clientLevel, blockEntity.getBlockPos());
    }

    /**
     * Returns the sublevel owning the given position (plot-virtual coordinates).
     * Works for any position, not just BE positions.
     *
     * <p>IMPORTANT: {@code SubLevelContainer.getSubLevel(x, z)} takes <b>local plot-grid
     * indices</b> ({@code 0..(1<<logSideLength)}), not world chunk coordinates. Passing
     * world chunk coordinates here misclassifies parent-world BEs near the world origin
     * (chunk 0..1023) as sublevel BEs. Use {@code getPlot(chunkX, chunkZ)} instead - it
     * takes world chunk coordinates and applies the plot-grid origin offset internally.</p>
     */
    public static ClientSubLevel getContainingSubLevel(ClientLevel clientLevel, BlockPos pos) {
        SubLevelContainer container = SubLevelContainer.getContainer(clientLevel);
        if (container == null) {
            return null;
        }
        LevelPlot plot = container.getPlot(pos.getX() >> 4, pos.getZ() >> 4);
        if (plot == null) {
            return null;
        }
        return plot.getSubLevel() instanceof ClientSubLevel subLevel ? subLevel : null;
    }

    /**
     * Returns the ClientSubLevel whose plot chunks contain the given <b>entity</b>
     * (e.g. a Create Contraption entity at the plot-virtual coordinate, ~+/-20.48M).
     */
    public static ClientSubLevel getContainingSubLevel(net.minecraft.world.entity.Entity entity) {
        if (!(entity.level() instanceof ClientLevel clientLevel)) {
            return null;
        }
        return getContainingSubLevel(clientLevel, entity.blockPosition());
    }

    /**
     * Best-effort helper to create a Flywheel visual for a BE outside the mixin paths.
     */
    public static boolean tryAddFlywheelVisual(BlockEntity blockEntity) {
        return VisualizationHelper.tryAddBlockEntity(blockEntity);
    }

    /**
     * Returns (or lazily creates) the embedding state for a sublevel.
     * The embedding origin is the sublevel plot's center block.
     */
    public static FlywheelEmbeddingState getOrCreateState(VisualizationContext ctx, ClientSubLevel subLevel) {
        FlywheelEmbeddingState existing = EMBEDDINGS.get(subLevel);
        if (existing != null) {
            return existing;
        }
        FlywheelEmbeddingState created = new FlywheelEmbeddingState(
                new SubLevelEmbedding(ctx.createEmbedding(SubLevelEmbedding.embeddingOrigin(subLevel))));
        created.updateTransforms(subLevel.renderPose(), ctx.renderOrigin());
        EMBEDDINGS.put(subLevel, created);
        return created;
    }

    public static Map<ClientSubLevel, FlywheelEmbeddingState> embeddings() {
        return EMBEDDINGS;
    }

    /**
     * Clears all states and deletes their embeddings; for use before Flywheel recreates
     * everything (engine rebuild / context change).
     */
    public static void clearAll() {
        for (FlywheelEmbeddingState state : EMBEDDINGS.values()) {
            state.embedding().delete();
        }
        EMBEDDINGS.clear();
        PENDING_BEFORE_EMBEDDING.clear();
    }

    /**
     * Returns (or lazily creates) the embedding state used for a sublevel's <b>entity</b>
     * visuals (e.g. Create's {@code ContraptionVisual}).
     *
     * <p>Entity visuals go through {@code EntityStorage.createRaw -> EntityVisualizer.createVisual}
     * exactly like block-entity visuals go through {@code BlockEntityStorage}; the same
     * per-sublevel embedding is reused, so entity instances land in the sublevel's local
     * space and receive the sublevel render pose every frame.</p>
     */
    public static FlywheelEmbeddingState getOrCreateEntityEmbedding(VisualizationContext ctx, ClientSubLevel subLevel) {
        return getOrCreateState(ctx, subLevel);
    }

    /** True when any active sublevel exists; render-frame work can short-circuit otherwise. */
    public static boolean hasSubLevels() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return false;
        }
        for (var obj : container.getAllSubLevels()) {
            if (obj instanceof ClientSubLevel subLevel && !subLevel.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Bridges sublevel BEs into the parent-world manager's queue.
     *
     * <p>Sublevels are separate ClientLevels whose BE join events never reach the parent
     * world's flywheel storage. This walks the loaded plot chunks of every sublevel,
     * collects the currently alive whitelisted BEs, and diffs them against the last
     * registered set: new BEs are queueAdd'ed on the parent manager (the storage mixin
     * swaps in this sublevel's embedding context when creating the visual), gone BEs are
     * queueRemove'ed. Invoked every frame from onStartLevelRender by the render dispatcher
     * mixin.</p>
     *
     * <p>Timing caveat: {@code BlockEntityStorage.willAccept} calls
     * {@code getChunkForCollisions}, which rejects (without retry) while the plot chunk
     * has not been injected yet. As a safety net, BEs whose queueAdd has not produced a
     * visual enter the pending set and are retried every
     * {@value #RETRY_INTERVAL_FRAMES} frames (idempotent; willAccept filters anyway).</p>
     */
    public static void syncSubLevelBlockEntities(ClientLevel parentLevel, VisualizationManager manager) {
        if (!ENABLED) {
            return;
        }
        SubLevelContainer container = SubLevelContainer.getContainer(parentLevel);
        if (container == null) {
            return;
        }
        frameCounter++;
        boolean retryTick = frameCounter % RETRY_INTERVAL_FRAMES == 0;

        List<ClientSubLevel> subLevels = new ArrayList<>();
        for (var obj : container.getAllSubLevels()) {
            if (obj instanceof ClientSubLevel subLevel && !subLevel.isRemoved()) {
                subLevels.add(subLevel);
            }
        }

        // Drop states for sublevels no longer in the container (belt-and-braces; the
        // render dispatcher mixin also removes removed sublevels every frame).
        EMBEDDINGS.keySet().removeIf(subLevel -> !subLevels.contains(subLevel));

        for (ClientSubLevel subLevel : subLevels) {
            syncOneSubLevel(subLevel, manager, retryTick);
        }
    }

    private static void syncOneSubLevel(ClientSubLevel subLevel, VisualizationManager manager,
                                        boolean retryTick) {
        FlywheelEmbeddingState state = EMBEDDINGS.get(subLevel);
        Map<BlockEntity, Boolean> registered =
                state != null ? state.registeredView() : null;

        // Collect the currently alive whitelisted BEs across the plot's loaded chunks.
        List<BlockEntity> current = new ArrayList<>();
        LevelPlot plot = subLevel.getPlot();
        if (plot != null) {
            for (PlotChunkHolder holder : plot.getLoadedChunks()) {
                LevelChunk chunk = holder.getChunk();
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be == null || be.isRemoved() || !isWhitelisted(be.getType())) {
                        continue;
                    }
                    current.add(be);
                }
            }
        }

        boolean emptyState = state == null;
        if (emptyState && current.isEmpty()) {
            return; // No whitelisted BEs yet; do not create an embedding.
        }

        // Diff only: queueAdd new BEs, queueRemove gone BEs.
        // The embedding itself is created lazily (the storage mixin builds it when the
        // first visual is created).
        for (BlockEntity be : current) {
            boolean known = registered != null && registered.containsKey(be);
            boolean pending = state != null && state.isPending(be);
            if (!known && (!pending || retryTick)) {
                manager.blockEntities().queueAdd(be);
                if (state != null) {
                    state.markPending(be);
                } else {
                    // No embedding state yet; hold it until the storage mixin creates one.
                    PENDING_BEFORE_EMBEDDING.put(be, Boolean.TRUE);
                }
            }
        }
        if (registered != null && state != null) {
            for (BlockEntity be : new ArrayList<>(registered.keySet())) {
                if (!current.contains(be)) {
                    manager.blockEntities().queueRemove(be);
                    state.removeBlockEntity(be);
                    state.clearPending(be);
                }
            }
            // Drop pending entries for BEs that no longer exist.
            state.retainPending(current);
        }
    }

    /**
     * Whitelist check. The whitelist compares registered BlockEntityType instances;
     * canVisualize (flywheel visualizer presence) is checked again inside willAccept,
     * pre-filtering here just keeps the diff set small.
     */
    private static boolean isWhitelisted(BlockEntityType<?> type) {
        return FlywheelWhitelist.contains(type)
                && VisualizerRegistryImpl.getVisualizer(type) != null;
    }

    /** Called by the storage mixin when a visual is actually created: moves the BE
     * from pending into the registered set. */
    public static void onVisualCreated(BlockEntity be, FlywheelEmbeddingState state) {
        state.addBlockEntity(be);
        state.clearPending(be);
        PENDING_BEFORE_EMBEDDING.remove(be);
    }

    /** Per-sublevel embedding plus the set of BEs attached to it. */
    public static final class FlywheelEmbeddingState {
        private final SubLevelEmbedding embedding;
        private final Map<BlockEntity, Boolean> blockEntities = new Object2ObjectOpenHashMap<>();
        /** queueAdd sent but no visual created yet (rejected by willAccept or timing). */
        private final Map<BlockEntity, Boolean> pendingAdd = new Object2ObjectOpenHashMap<>();

        FlywheelEmbeddingState(SubLevelEmbedding embedding) {
            this.embedding = embedding;
        }

        public SubLevelEmbedding embedding() {
            return this.embedding;
        }

        public com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding visualEmbedding() {
            return this.embedding.embedding();
        }

        public void addBlockEntity(BlockEntity be) {
            this.blockEntities.put(be, Boolean.TRUE);
        }

        public void removeBlockEntity(BlockEntity be) {
            this.blockEntities.remove(be);
        }

        public Iterable<BlockEntity> blockEntities() {
            return this.blockEntities.keySet();
        }

        Map<BlockEntity, Boolean> registeredView() {
            return this.blockEntities;
        }

        void markPending(BlockEntity be) {
            this.pendingAdd.putIfAbsent(be, Boolean.TRUE);
        }

        void clearPending(BlockEntity be) {
            this.pendingAdd.remove(be);
        }

        void retainPending(List<BlockEntity> current) {
            this.pendingAdd.keySet().retainAll(current);
        }

        public boolean isPending(BlockEntity be) {
            return this.pendingAdd.containsKey(be);
        }

        public boolean hasVisuals() {
            return !this.blockEntities.isEmpty();
        }

        public void updateTransforms(dev.ryanhcode.sable.companion.math.Pose3dc renderPose,
                                     net.minecraft.core.Vec3i parentOrigin) {
            this.embedding.updateTransforms(renderPose, parentOrigin);
        }
    }
}
