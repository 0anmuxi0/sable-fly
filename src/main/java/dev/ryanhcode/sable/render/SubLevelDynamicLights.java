package dev.ryanhcode.sable.render;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.joml.Vector3d;

public final class SubLevelDynamicLights {
   private static final Map<LevelLightEngine, SubLevelDynamicLights.DynamicLightState> LIGHTS_BY_ENGINE = new IdentityHashMap<>();
   private static final SubLevelDynamicLights.DynamicLightState EMPTY_STATE = new SubLevelDynamicLights.DynamicLightState(
      new Long2IntOpenHashMap(), new Long2ObjectOpenHashMap(), new LongOpenHashSet(), new IdentityHashMap<>()
   );
   private static final ThreadLocal<ArrayDeque<SubLevelDynamicLights.DynamicLightState>> ACTIVE_STATES = ThreadLocal.withInitial(ArrayDeque::new);

   private SubLevelDynamicLights() {
   }

   public static void tick(Level level) {
      LevelLightEngine lightEngine = level.getLightEngine();
      SubLevelDynamicLights.DynamicLightState nextState = collectState(level);
      SubLevelDynamicLights.DynamicLightState previousState;
      synchronized (LIGHTS_BY_ENGINE) {
         previousState = LIGHTS_BY_ENGINE.put(lightEngine, nextState);
      }

      LongIterator var9 = nextState.sections.iterator();

      while (var9.hasNext()) {
         long packedSection = (Long)var9.next();
         lightEngine.updateSectionStatus(SectionPos.of(packedSection), false);
      }

      if (previousState != null) {
         var9 = previousState.sections.iterator();

         while (var9.hasNext()) {
            long packedSection = (Long)var9.next();
            if (!nextState.sections.contains(packedSection)) {
               restoreSectionStatus(level, lightEngine, packedSection);
            }
         }
      }

      LongOpenHashSet positionsToCheck = new LongOpenHashSet(nextState.lights.keySet());
      if (previousState != null) {
         positionsToCheck.addAll(previousState.lights.keySet());
      }

      ObjectIterator var13 = nextState.blocks.long2ObjectEntrySet().iterator();

      while (var13.hasNext()) {
         Entry<BlockState> entry = (Entry<BlockState>)var13.next();
         if (previousState == null || previousState.blocks.get(entry.getLongKey()) != entry.getValue()) {
            positionsToCheck.add(entry.getLongKey());
         }
      }

      if (previousState != null) {
         LongIterator var14 = previousState.blocks.keySet().iterator();

         while (var14.hasNext()) {
            long packedPos = (Long)var14.next();
            if (!nextState.blocks.containsKey(packedPos)) {
               positionsToCheck.add(packedPos);
            }
         }
      }

      if (!positionsToCheck.isEmpty()) {
         LongIterator var15 = positionsToCheck.iterator();

         while (var15.hasNext()) {
            long packedPos = (Long)var15.next();
            lightEngine.checkBlock(BlockPos.of(packedPos));
         }

         if (level.isClientSide()) {
            lightEngine.runLightUpdates();
         }
      }
   }

   public static void clear(Level level) {
      synchronized (LIGHTS_BY_ENGINE) {
         LIGHTS_BY_ENGINE.remove(level.getLightEngine());
      }
   }

   public static void beginLightUpdates(LevelLightEngine engine) {
      SubLevelDynamicLights.DynamicLightState state;
      synchronized (LIGHTS_BY_ENGINE) {
         state = LIGHTS_BY_ENGINE.get(engine);
      }

      ACTIVE_STATES.get().push(state == null ? EMPTY_STATE : state);
   }

   public static void endLightUpdates(LevelLightEngine engine) {
      ArrayDeque<SubLevelDynamicLights.DynamicLightState> activeStates = ACTIVE_STATES.get();
      if (!activeStates.isEmpty()) {
         activeStates.pop();
      }

      if (activeStates.isEmpty()) {
         ACTIVE_STATES.remove();
      }
   }

   public static int getLightEmission(long packedPos) {
      ArrayDeque<SubLevelDynamicLights.DynamicLightState> activeStates = ACTIVE_STATES.get();
      return activeStates.isEmpty() ? 0 : activeStates.peek().lights.get(packedPos);
   }

   public static BlockState getOccludingBlockState(long packedPos) {
      ArrayDeque<SubLevelDynamicLights.DynamicLightState> activeStates = ACTIVE_STATES.get();
      return activeStates.isEmpty() ? null : (BlockState)activeStates.peek().blocks.get(packedPos);
   }

   public static SubLevelDynamicLights.OwnLightMask getOwnLightMask(LevelLightEngine engine, SubLevel subLevel) {
      synchronized (LIGHTS_BY_ENGINE) {
         SubLevelDynamicLights.DynamicLightState state = LIGHTS_BY_ENGINE.get(engine);
         return state == null ? SubLevelDynamicLights.OwnLightMask.EMPTY : state.ownLights.getOrDefault(subLevel, SubLevelDynamicLights.OwnLightMask.EMPTY);
      }
   }

   private static SubLevelDynamicLights.DynamicLightState collectState(Level level) {
      Long2IntOpenHashMap lights = new Long2IntOpenHashMap();
      Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap();
      LongOpenHashSet sections = new LongOpenHashSet();
      Map<SubLevel, SubLevelDynamicLights.OwnLightMask> ownLights = new IdentityHashMap<>();
      lights.defaultReturnValue(0);
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         return new SubLevelDynamicLights.DynamicLightState(lights, blocks, sections, ownLights);
      } else {
         for (SubLevel subLevel : container.getAllSubLevels()) {
            Pose3dc pose = subLevel.logicalPose();
            List<SubLevelDynamicLights.LocalLightSource> localSources = new ArrayList<>();
            subLevel.getPlot().getLoadedChunks().forEach(holder -> holder.getChunk().findBlockLightSources((sourcePos, state) -> {
               int emission = state.getLightEmission();
               if (emission > 0) {
                  localSources.add(new SubLevelDynamicLights.LocalLightSource(sourcePos.immutable(), emission));
               }
            }));
            if (!localSources.isEmpty()) {
               Vector3d physicalCenter = new Vector3d();
               MutableBlockPos physicalPos = new MutableBlockPos();
               MutableBlockPos localPos = new MutableBlockPos();
               LongOpenHashSet occlusionCandidates = new LongOpenHashSet();
               List<SubLevelDynamicLights.ProjectedLightSource> projectedSources = new ArrayList<>();

               for (SubLevelDynamicLights.LocalLightSource source : localSources) {
                  if (projectPosition(level, pose, source.pos, physicalCenter, physicalPos)) {
                     projectedSources.add(
                        new SubLevelDynamicLights.ProjectedLightSource(physicalPos.getX(), physicalPos.getY(), physicalPos.getZ(), source.emission)
                     );
                     long packedSourcePos = physicalPos.asLong();
                     if (source.emission > lights.get(packedSourcePos)) {
                        lights.put(packedSourcePos, source.emission);
                     }

                     sections.add(SectionPos.blockToSection(packedSourcePos));
                     int radius = Math.min(15, source.emission);
                     int radiusSquared = radius * radius;

                     for (int x = -radius; x <= radius; x++) {
                        for (int y = -radius; y <= radius; y++) {
                           for (int z = -radius; z <= radius; z++) {
                              if (x * x + y * y + z * z <= radiusSquared) {
                                 int candidateY = source.pos.getY() + y;
                                 if (!level.isOutsideBuildHeight(candidateY)) {
                                    occlusionCandidates.add(BlockPos.asLong(source.pos.getX() + x, candidateY, source.pos.getZ() + z));
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               LongIterator var25 = occlusionCandidates.iterator();

               while (var25.hasNext()) {
                  long packedLocalPos = (Long)var25.next();
                  localPos.set(packedLocalPos);
                  BlockState blockState = level.getBlockState(localPos);
                  if (!blockState.isAir()
                     && (blockState.getLightDampening() > 0 || blockState.useShapeForLightOcclusion())
                     && projectPosition(level, pose, localPos, physicalCenter, physicalPos)) {
                     long packedPhysicalPos = physicalPos.asLong();
                     BlockState existingState = (BlockState)blocks.get(packedPhysicalPos);
                     if (existingState == null
                        || blockState.getLightDampening() > existingState.getLightDampening()
                        || blockState.getLightDampening() == existingState.getLightDampening()
                           && blockState.useShapeForLightOcclusion()
                           && !existingState.useShapeForLightOcclusion()) {
                        blocks.put(packedPhysicalPos, blockState);
                     }

                     sections.add(SectionPos.blockToSection(packedPhysicalPos));
                  }
               }

               if (!projectedSources.isEmpty()) {
                  ownLights.put(subLevel, new SubLevelDynamicLights.OwnLightMask(List.copyOf(projectedSources)));
               }
            }
         }

         return new SubLevelDynamicLights.DynamicLightState(lights, blocks, sections, ownLights);
      }
   }

   private static boolean projectPosition(Level level, Pose3dc pose, BlockPos localPos, Vector3d physicalCenter, MutableBlockPos physicalPos) {
      pose.transformPosition(physicalCenter.set(localPos.getX() + 0.5, localPos.getY() + 0.5, localPos.getZ() + 0.5), physicalCenter);
      physicalPos.set(physicalCenter.x, physicalCenter.y, physicalCenter.z);
      return !level.isOutsideBuildHeight(physicalPos) && level.hasChunkAt(physicalPos);
   }

   private static void restoreSectionStatus(Level level, LevelLightEngine lightEngine, long packedSection) {
      SectionPos sectionPos = SectionPos.of(packedSection);
      if (sectionPos.y() >= level.getMinSectionY() && sectionPos.y() < level.getMaxSectionY()) {
         BlockPos origin = sectionPos.origin();
         if (level.hasChunkAt(origin)) {
            LevelChunk chunk = level.getChunkAt(origin);
            int sectionIndex = level.getSectionIndexFromSectionY(sectionPos.y());
            lightEngine.updateSectionStatus(sectionPos, chunk.getSection(sectionIndex).hasOnlyAir());
         }
      }
   }

   private record DynamicLightState(
      Long2IntOpenHashMap lights,
      Long2ObjectOpenHashMap<BlockState> blocks,
      LongOpenHashSet sections,
      Map<SubLevel, SubLevelDynamicLights.OwnLightMask> ownLights
   ) {
   }

   private record LocalLightSource(BlockPos pos, int emission) {
   }

   public static final class OwnLightMask {
      private static final SubLevelDynamicLights.OwnLightMask EMPTY = new SubLevelDynamicLights.OwnLightMask(List.of());
      private final List<SubLevelDynamicLights.ProjectedLightSource> sources;

      private OwnLightMask(List<SubLevelDynamicLights.ProjectedLightSource> sources) {
         this.sources = sources;
      }

      public int upperBound(long packedPos) {
         int x = BlockPos.getX(packedPos);
         int y = BlockPos.getY(packedPos);
         int z = BlockPos.getZ(packedPos);
         int upperBound = 0;

         for (SubLevelDynamicLights.ProjectedLightSource source : this.sources) {
            int distance = Math.abs(x - source.x) + Math.abs(y - source.y) + Math.abs(z - source.z);
            upperBound = Math.max(upperBound, source.emission - distance);
         }

         return upperBound;
      }
   }

   private record ProjectedLightSource(int x, int y, int z, int emission) {
   }
}
