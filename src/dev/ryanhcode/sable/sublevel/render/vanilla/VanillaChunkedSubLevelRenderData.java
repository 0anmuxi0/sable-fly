package dev.ryanhcode.sable.sublevel.render.vanilla;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla.RenderSectionExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public class VanillaChunkedSubLevelRenderData
implements SubLevelRenderData {
    private final Vector3d origin = new Vector3d();
    private final Vector3i chunkOrigin = new Vector3i();
    private final ClientSubLevel subLevel;
    private final Vector3i size = new Vector3i();
    private final ObjectList<SectionRenderDispatcher.RenderSection> allRenderSections = new ObjectArrayList();
    private final ObjectList<SectionRenderDispatcher.RenderSection> dirtyRenderSections = new ObjectArrayList();
    private final LongSet playerChangedSections = new LongOpenHashSet();
    private SectionRenderDispatcher.RenderSection[] renderSections = null;
    private final SectionRenderDispatcher sectionRenderDispatcher;
    private final Long2ObjectMap<VisibleBlocks> sable$visibleBlocks = new Long2ObjectOpenHashMap();

    public VanillaChunkedSubLevelRenderData(ClientSubLevel subLevel, SectionRenderDispatcher sectionRenderDispatcher) {
        this.subLevel = subLevel;
        this.sectionRenderDispatcher = sectionRenderDispatcher;
        this.resize();
    }

    private static SectionRenderDispatcher.RenderSection getSection(SectionRenderDispatcher.RenderSection[] sections, Vector3i size, Vector3i origin, int x, int y, int z) {
        int relX = x - origin.x();
        int relY = y - origin.y();
        int relZ = z - origin.z();
        if (relX < 0 || relY < 0 || relZ < 0) {
            return null;
        }
        if (relX >= size.x() || relY >= size.y() || relZ >= size.z()) {
            return null;
        }
        return sections[relX + relY * size.x() + relZ * size.x() * size.y()];
    }

    private int getIndex(int x, int y, int z) {
        return x - this.chunkOrigin.x() + (y - this.chunkOrigin.y()) * this.size.x() + (z - this.chunkOrigin.z()) * this.size.x() * this.size.y();
    }

    private boolean inBounds(int x, int y, int z) {
        int localX = x - this.chunkOrigin.x();
        int localY = y - this.chunkOrigin.y();
        int localZ = z - this.chunkOrigin.z();
        return localX >= 0 && localY >= 0 && localZ >= 0 && localX < this.size.x() && localY < this.size.y() && localZ < this.size.z();
    }

    public void resize() {
        SectionRenderDispatcher.RenderSection[] oldRenderSections = this.renderSections;
        ObjectArrayList oldRenderSectionsList = new ObjectArrayList(this.allRenderSections);
        this.renderSections = null;
        this.allRenderSections.clear();
        this.dirtyRenderSections.clear();
        this.sable$visibleBlocks.clear();
        BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();
        if (bounds != null && !bounds.equals(BoundingBox3i.EMPTY) && (double)bounds.volume() > 0.0) {
            Vector3i minChunkPos = new Vector3i(bounds.minX() >> 4, bounds.minY() >> 4, bounds.minZ() >> 4);
            Vector3i maxChunkPos = new Vector3i(bounds.maxX() >> 4, bounds.maxY() >> 4, bounds.maxZ() >> 4);
            Vector3i oldSize = new Vector3i((Vector3ic)this.size);
            Vector3i oldOrigin = new Vector3i((Vector3ic)this.chunkOrigin);
            this.size.set(maxChunkPos.x() - minChunkPos.x() + 1, maxChunkPos.y() - minChunkPos.y() + 1, maxChunkPos.z() - minChunkPos.z() + 1);
            this.chunkOrigin.set((Vector3ic)minChunkPos);
            this.origin.set((double)(minChunkPos.x() << 4), (double)(minChunkPos.y() << 4), (double)(minChunkPos.z() << 4));
            this.renderSections = new SectionRenderDispatcher.RenderSection[this.size.x() * this.size.y() * this.size.z()];
            for (int x = minChunkPos.x(); x <= maxChunkPos.x(); ++x) {
                for (int y = minChunkPos.y(); y <= maxChunkPos.y(); ++y) {
                    for (int z = minChunkPos.z(); z <= maxChunkPos.z(); ++z) {
                        SectionRenderDispatcher.RenderSection newSection;
                        SectionRenderDispatcher.RenderSection oldSection = VanillaChunkedSubLevelRenderData.getSection(oldRenderSections, oldSize, oldOrigin, x, y, z);
                        if (oldRenderSections != null && oldSection != null) {
                            newSection = oldSection;
                        } else {
                            SectionRenderDispatcher sectionRenderDispatcher = this.sectionRenderDispatcher;
                            Objects.requireNonNull(sectionRenderDispatcher);
                            newSection = new SectionRenderDispatcher.RenderSection(sectionRenderDispatcher, -1, SectionPos.asLong((int)x, (int)y, (int)z));
                            ((RenderSectionExtension)newSection).sable$addDirtyListener(arg_0 -> this.dirtyRenderSections.add(arg_0));
                        }
                        if (newSection.getSectionMesh() == CompiledSectionMesh.UNCOMPILED) {
                            this.dirtyRenderSections.add(newSection);
                        }
                        this.renderSections[this.getIndex((int)x, (int)y, (int)z)] = newSection;
                        this.allRenderSections.add(newSection);
                    }
                }
            }
            if (oldRenderSections != null) {
                for (SectionRenderDispatcher.RenderSection oldSection : oldRenderSectionsList) {
                    SectionPos oldSectionPos = SectionPos.of((BlockPos)oldSection.getRenderOrigin());
                    if (oldSectionPos.getX() >= minChunkPos.x() && oldSectionPos.getX() <= maxChunkPos.x() && oldSectionPos.getY() >= minChunkPos.y() && oldSectionPos.getY() <= maxChunkPos.y() && oldSectionPos.getZ() >= minChunkPos.z() && oldSectionPos.getZ() <= maxChunkPos.z()) continue;
                    oldSection.reset();
                }
            }
        }
    }

    @Override
    public void rebuild() {
        for (SectionRenderDispatcher.RenderSection renderSection : this.allRenderSections) {
            this.sable$markDirty(renderSection, true);
        }
    }

    private void sable$markDirty(SectionRenderDispatcher.RenderSection renderSection, boolean playerChanged) {
        if (playerChanged) {
            this.playerChangedSections.add(renderSection.getSectionNode());
        }
        if (!this.dirtyRenderSections.contains(renderSection)) {
            this.dirtyRenderSections.add(renderSection);
        }
    }

    @Override
    public void compileSections(PrioritizeChunkUpdates chunkUpdates, RenderRegionCache renderRegionCache, Camera camera) {
        if (this.dirtyRenderSections.isEmpty()) {
            return;
        }
        ProfilerFiller profiler = Profiler.get();
        Vector3d cameraPos = JOMLConversion.atCenterOf((Vec3i)camera.blockPosition()).sub(8.0, 8.0, 8.0);
        this.subLevel.logicalPose().transformPositionInverse(cameraPos);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            this.dirtyRenderSections.clear();
        // 26.2: LevelRenderer constructs its SectionRenderDispatcher with a null SectionCompiler
        // when model data is not ready yet and injects it later via setCompiler. A sub-level
        // bounds packet can arrive before that; compiling then NPEs inside CompileTask.doTask.
        // Skip this pass and KEEP the dirty queue so compilation retries next frame.
        try {
            java.lang.reflect.Field compilerField =
                SectionRenderDispatcher.class.getDeclaredField("sectionCompiler");
            compilerField.setAccessible(true);
            if (compilerField.get(Minecraft.getInstance().levelRenderer.sectionRenderDispatcher()) == null) {
                return;
            }
        } catch (ReflectiveOperationException ignored) {
            // reflective probe failed - assume ready and let vanilla behave
        }
            return;
        }
        for (SectionRenderDispatcher.RenderSection renderSection : this.dirtyRenderSections) {
            ((RenderSectionExtension)renderSection).sable$setListening(false);
            boolean playerChanged = this.playerChangedSections.contains(renderSection.getSectionNode());
            boolean buildSync = false;
            if (chunkUpdates == PrioritizeChunkUpdates.NEARBY) {
                BlockPos origin = renderSection.getRenderOrigin();
                buildSync = cameraPos.distanceSquared((double)origin.getX(), (double)origin.getY(), (double)origin.getZ()) < 768.0 || playerChanged;
            } else if (chunkUpdates == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                buildSync = playerChanged;
            }
            RenderSectionRegion region = renderRegionCache.createRegion(level, renderSection.getSectionNode());
            if (buildSync) {
                profiler.push("sublevel_build_near_sync");
                renderSection.compileSync(region);
                profiler.pop();
            } else {
                profiler.push("sublevel_schedule_async_compile");
                renderSection.compileAsync(region);
                profiler.pop();
            }
            ((RenderSectionExtension)renderSection).sable$setListening(true);
        }
        this.dirtyRenderSections.clear();
        this.playerChangedSections.clear();
    }

    @Override
    public int getVisibleSectionCount() {
        return this.allRenderSections.size();
    }

    @Override
    public ClientSubLevel getSubLevel() {
        return this.subLevel;
    }

    @Override
    public boolean isSectionCompiled(int x, int y, int z) {
        if (this.renderSections == null) {
            return false;
        }
        if (!this.inBounds(x, y, z)) {
            return true;
        }
        int index = this.getIndex(x, y, z);
        return index >= 0 && index < this.renderSections.length && this.renderSections[index].getSectionMesh() != CompiledSectionMesh.UNCOMPILED;
    }

    @Override
    public void setDirty(int x, int y, int z, boolean playerChanged) {
        if (this.renderSections == null) {
            return;
        }
        if (!this.inBounds(x, y, z)) {
            return;
        }
        int index = this.getIndex(x, y, z);
        if (index >= 0 && index < this.renderSections.length) {
            this.sable$markDirty(this.renderSections[index], playerChanged);
        }
    }

    public ObjectList<SectionRenderDispatcher.RenderSection> allRenderSections() {
        return this.allRenderSections;
    }

    @Override
    public void close() {
        for (SectionRenderDispatcher.RenderSection section : this.allRenderSections) {
            section.reset();
        }
        this.allRenderSections.clear();
        this.renderSections = null;
        this.sable$visibleBlocks.clear();
    }

    public VisibleBlocks sable$getVisibleBlocks(long sectionNode, LevelChunkSection section, int sectionMinY, int baseX, int baseZ, boolean cullEnclosedEnabled) {
        int revision = this.subLevel.getPlot().sable$getRenderRevision(sectionNode);
        VisibleBlocks cached = (VisibleBlocks)this.sable$visibleBlocks.get(sectionNode);
        if (cached != null && cached.revision == (long)revision && cached.cullEnclosed == cullEnclosedEnabled) {
            return cached;
        }
        ClientLevel level = this.subLevel.getLevel();
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        ArrayList<BlockState> states = new ArrayList<BlockState>();
        ArrayList<Holder<Biome>> biomes = new ArrayList<Holder<Biome>>();
        for (int localX = 0; localX < 16; ++localX) {
            for (int localY = 0; localY < 16; ++localY) {
                for (int localZ = 0; localZ < 16; ++localZ) {
                    BlockState blockState = section.getBlockState(localX, localY, localZ);
                    if (blockState.isAir()) continue;
                    BlockPos blockPos = new BlockPos(baseX + localX, sectionMinY + localY, baseZ + localZ);
                    if (cullEnclosedEnabled && blockState.isSolidRender() && VanillaChunkedSubLevelRenderData.sable$isFullyEnclosed((Level)level, blockPos)) continue;
                    positions.add(blockPos);
                    states.add(blockState);
                    biomes.add((Holder<Biome>)level.getBiome(blockPos));
                }
            }
        }
        VisibleBlocks built = new VisibleBlocks(revision, cullEnclosedEnabled, positions.toArray(new BlockPos[0]), states.toArray(new BlockState[0]), VanillaChunkedSubLevelRenderData.sable$biomeArray(biomes));
        this.sable$visibleBlocks.put(sectionNode, built);
        return built;
    }

    private static Holder<Biome>[] sable$biomeArray(List<Holder<Biome>> biomes) {
        return biomes.toArray(new Holder[0]);
    }

    private static boolean sable$isFullyEnclosed(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).canOcclude()) continue;
            return false;
        }
        return true;
    }

    public SectionRenderDispatcher.RenderSection getRenderSection(SectionPos sectionPos) {
        if (this.renderSections == null) {
            return null;
        }
        if (!this.inBounds(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ())) {
            return null;
        }
        int index = this.getIndex(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
        if (index < 0 || index >= this.renderSections.length) {
            return null;
        }
        return this.renderSections[index];
    }

    public static final class VisibleBlocks {
        public final long revision;
        public final boolean cullEnclosed;
        public final BlockPos[] positions;
        public final BlockState[] states;
        public final Holder<Biome>[] biomes;

        VisibleBlocks(long revision, boolean cullEnclosed, BlockPos[] positions, BlockState[] states, Holder<Biome>[] biomes) {
            this.revision = revision;
            this.cullEnclosed = cullEnclosed;
            this.positions = positions;
            this.states = states;
            this.biomes = biomes;
        }
    }
}

