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
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class VanillaChunkedSubLevelRenderData implements SubLevelRenderData {
   private final Vector3d origin = new Vector3d();
   private final Vector3i chunkOrigin = new Vector3i();
   private final ClientSubLevel subLevel;
   private final Vector3i size = new Vector3i();
   private final ObjectList<RenderSection> allRenderSections = new ObjectArrayList();
   private final ObjectList<RenderSection> dirtyRenderSections = new ObjectArrayList();
   private final LongSet playerChangedSections = new LongOpenHashSet();
   private RenderSection[] renderSections = null;
   private final SectionRenderDispatcher sectionRenderDispatcher;
   private final Long2ObjectMap<VanillaChunkedSubLevelRenderData.VisibleBlocks> sable$visibleBlocks = new Long2ObjectOpenHashMap();

   public VanillaChunkedSubLevelRenderData(ClientSubLevel subLevel, SectionRenderDispatcher sectionRenderDispatcher) {
      this.subLevel = subLevel;
      this.sectionRenderDispatcher = sectionRenderDispatcher;
      this.resize();
   }

   private static RenderSection getSection(RenderSection[] sections, Vector3i size, Vector3i origin, int x, int y, int z) {
      int relX = x - origin.x();
      int relY = y - origin.y();
      int relZ = z - origin.z();
      if (relX < 0 || relY < 0 || relZ < 0) {
         return null;
      } else {
         return relX < size.x() && relY < size.y() && relZ < size.z() ? sections[relX + relY * size.x() + relZ * size.x() * size.y()] : null;
      }
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
      RenderSection[] oldRenderSections = this.renderSections;
      Collection<RenderSection> oldRenderSectionsList = new ObjectArrayList(this.allRenderSections);
      this.renderSections = null;
      this.allRenderSections.clear();
      this.dirtyRenderSections.clear();
      this.sable$visibleBlocks.clear();
      BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();
      if (bounds != null && !bounds.equals(BoundingBox3i.EMPTY) && bounds.volume() > 0.0) {
         Vector3i minChunkPos = new Vector3i(bounds.minX() >> 4, bounds.minY() >> 4, bounds.minZ() >> 4);
         Vector3i maxChunkPos = new Vector3i(bounds.maxX() >> 4, bounds.maxY() >> 4, bounds.maxZ() >> 4);
         Vector3i oldSize = new Vector3i(this.size);
         Vector3i oldOrigin = new Vector3i(this.chunkOrigin);
         this.size.set(maxChunkPos.x() - minChunkPos.x() + 1, maxChunkPos.y() - minChunkPos.y() + 1, maxChunkPos.z() - minChunkPos.z() + 1);
         this.chunkOrigin.set(minChunkPos);
         this.origin.set(minChunkPos.x() << 4, minChunkPos.y() << 4, minChunkPos.z() << 4);
         this.renderSections = new RenderSection[this.size.x() * this.size.y() * this.size.z()];

         for (int x = minChunkPos.x(); x <= maxChunkPos.x(); x++) {
            for (int y = minChunkPos.y(); y <= maxChunkPos.y(); y++) {
               for (int z = minChunkPos.z(); z <= maxChunkPos.z(); z++) {
                  RenderSection oldSection = getSection(oldRenderSections, oldSize, oldOrigin, x, y, z);
                  RenderSection newSection;
                  if (oldRenderSections != null && oldSection != null) {
                     newSection = oldSection;
                  } else {
                     newSection = this.sectionRenderDispatcher.new RenderSection(-1, SectionPos.asLong(x, y, z));
                     ((RenderSectionExtension)newSection).sable$addDirtyListener(this.dirtyRenderSections::add);
                  }

                  if (newSection.getSectionMesh() == CompiledSectionMesh.UNCOMPILED) {
                     this.dirtyRenderSections.add(newSection);
                  }

                  this.renderSections[this.getIndex(x, y, z)] = newSection;
                  this.allRenderSections.add(newSection);
               }
            }
         }

         if (oldRenderSections != null) {
            for (RenderSection oldSectionx : oldRenderSectionsList) {
               SectionPos oldSectionPos = SectionPos.of(oldSectionx.getRenderOrigin());
               if (oldSectionPos.getX() < minChunkPos.x()
                  || oldSectionPos.getX() > maxChunkPos.x()
                  || oldSectionPos.getY() < minChunkPos.y()
                  || oldSectionPos.getY() > maxChunkPos.y()
                  || oldSectionPos.getZ() < minChunkPos.z()
                  || oldSectionPos.getZ() > maxChunkPos.z()) {
                  oldSectionx.reset();
               }
            }
         }
      }
   }

   @Override
   public void rebuild() {
      ObjectListIterator iterator = this.allRenderSections.iterator();

      while (iterator.hasNext()) {
         RenderSection renderSection = (RenderSection)iterator.next();
         this.sable$markDirty(renderSection, true);
      }
   }

   private void sable$markDirty(RenderSection renderSection, boolean playerChanged) {
      if (playerChanged) {
         this.playerChangedSections.add(renderSection.getSectionNode());
      }

      if (!this.dirtyRenderSections.contains(renderSection)) {
         this.dirtyRenderSections.add(renderSection);
      }
   }

   @Override
   public void compileSections(PrioritizeChunkUpdates chunkUpdates, RenderRegionCache renderRegionCache, Camera camera) {
      if (!this.dirtyRenderSections.isEmpty()) {
         ProfilerFiller profiler = Profiler.get();
         Vector3d cameraPos = JOMLConversion.atCenterOf(camera.blockPosition()).sub(8.0, 8.0, 8.0);
         this.subLevel.logicalPose().transformPositionInverse(cameraPos);
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            this.dirtyRenderSections.clear();
            this.playerChangedSections.clear();
         } else {
            ObjectListIterator iterator = this.dirtyRenderSections.iterator();

            while (iterator.hasNext()) {
               RenderSection renderSection = (RenderSection)iterator.next();
               ((RenderSectionExtension)renderSection).sable$setListening(false);
               boolean playerChanged = this.playerChangedSections.contains(renderSection.getSectionNode());
                boolean buildSync = false;
                if (chunkUpdates == PrioritizeChunkUpdates.NEARBY) {
                   // 抽搐/一顿一顿的元凶：原来这里用 distanceSquared < 768.0（约 27 格）判定，
                   // 只要玩家在 27 格内，该 section 就会在【渲染线程同步编译】（compileSync），
                   // 每帧重复导致明显卡顿 —— 站在结构上和站在旁边地面都会触发。
                   //
                   // 现在只在"玩家自己刚改过方块"时同步编译（保证挖掘/放置立刻可见），
                   // 其余一律异步，避免渲染线程被阻塞。
                   buildSync = playerChanged;
                } else if (chunkUpdates == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                   buildSync = playerChanged;
                }

               RenderRegionCache region = renderRegionCache;
               if (buildSync) {
                  profiler.push("sublevel_build_near_sync");
                  renderSection.compileSync(renderRegionCache);
                  profiler.pop();
               } else {
                  profiler.push("sublevel_schedule_async_compile");
                  renderSection.rebuildSectionAsync(region);
                  profiler.pop();
               }

               ((RenderSectionExtension)renderSection).sable$setListening(true);
            }

            this.dirtyRenderSections.clear();
            this.playerChangedSections.clear();
         }
      }
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
      } else if (!this.inBounds(x, y, z)) {
         return true;
      } else {
         int index = this.getIndex(x, y, z);
         return index >= 0 && index < this.renderSections.length && this.renderSections[index].getSectionMesh() != CompiledSectionMesh.UNCOMPILED;
      }
   }

   @Override
   public void setDirty(int x, int y, int z, boolean playerChanged) {
      if (this.renderSections != null) {
         if (this.inBounds(x, y, z)) {
            int index = this.getIndex(x, y, z);
            if (index >= 0 && index < this.renderSections.length) {
               this.sable$markDirty(this.renderSections[index], playerChanged);
            }
         }
      }
   }

   public ObjectList<RenderSection> allRenderSections() {
      return this.allRenderSections;
   }

   @Override
   public void close() {
      ObjectListIterator iterator = this.allRenderSections.iterator();

      while (iterator.hasNext()) {
         RenderSection section = (RenderSection)iterator.next();
         section.reset();
      }

      this.allRenderSections.clear();
      this.renderSections = null;
      this.sable$visibleBlocks.clear();
   }

   public VanillaChunkedSubLevelRenderData.VisibleBlocks sable$getVisibleBlocks(
      long sectionNode, LevelChunkSection section, int sectionMinY, int baseX, int baseZ, boolean cullEnclosedEnabled
   ) {
      int revision = this.subLevel.getPlot().sable$getRenderRevision(sectionNode);
      VanillaChunkedSubLevelRenderData.VisibleBlocks cached = (VanillaChunkedSubLevelRenderData.VisibleBlocks)this.sable$visibleBlocks.get(sectionNode);
      if (cached != null && cached.revision == revision && cached.cullEnclosed == cullEnclosedEnabled) {
         return cached;
      } else {
         Level level = this.subLevel.getLevel();
         List<BlockPos> positions = new ArrayList<>();
         List<BlockState> states = new ArrayList<>();
         List<Holder<Biome>> biomes = new ArrayList<>();

         for (int localX = 0; localX < 16; localX++) {
            for (int localY = 0; localY < 16; localY++) {
               for (int localZ = 0; localZ < 16; localZ++) {
                  BlockState blockState = section.getBlockState(localX, localY, localZ);
                  if (!blockState.isAir()) {
                     BlockPos blockPos = new BlockPos(baseX + localX, sectionMinY + localY, baseZ + localZ);
                     if (!cullEnclosedEnabled || !blockState.isSolidRender() || !sable$isFullyEnclosed(level, blockPos)) {
                        positions.add(blockPos);
                        states.add(blockState);
                        biomes.add(level.getBiome(blockPos));
                     }
                  }
               }
            }
         }

         VanillaChunkedSubLevelRenderData.VisibleBlocks built = new VanillaChunkedSubLevelRenderData.VisibleBlocks(
            revision, cullEnclosedEnabled, positions.toArray(new BlockPos[0]), states.toArray(new BlockState[0]), sable$biomeArray(biomes)
         );
         this.sable$visibleBlocks.put(sectionNode, built);
         return built;
      }
   }

   private static Holder<Biome>[] sable$biomeArray(List<Holder<Biome>> biomes) {
      return biomes.toArray(new Holder[0]);
   }

   private static boolean sable$isFullyEnclosed(Level level, BlockPos pos) {
      for (Direction direction : Direction.values()) {
         if (!level.getBlockState(pos.relative(direction)).canOcclude()) {
            return false;
         }
      }

      return true;
   }

   public RenderSection getRenderSection(SectionPos sectionPos) {
      if (this.renderSections == null) {
         return null;
      } else if (!this.inBounds(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ())) {
         return null;
      } else {
         int index = this.getIndex(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
         return index >= 0 && index < this.renderSections.length ? this.renderSections[index] : null;
      }
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
