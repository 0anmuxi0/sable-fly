package dev.ryanhcode.sable.api;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.platform.SableAssemblyPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelAssemblyHelper {
   public static ServerSubLevel assembleBlocks(ServerLevel level, BlockPos anchor, Iterable<BlockPos> blocks, BoundingBox3ic bounds) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
      SubLevel containingSubLevel = Sable.HELPER.getContaining(level, anchor);
      Pose3d pose = new Pose3d();
      pose.position().set(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5);
      Vector3d containingAngularVelocity = new Vector3d();
      Vector3d containingLinearVelocity = new Vector3d();
      Pose3d containingPose;
      if (containingSubLevel != null) {
         if (containingSubLevel.isRemoved()) {
            throw new RuntimeException("Sub-level assembly attempted inside plot of already removed sub-level");
         }

         containingPose = new Pose3d(containingSubLevel.logicalPose());
         containingPose.transformPosition(pose.position());
         pose.orientation().set(containingPose.orientation());
         RigidBodyHandle containingHandle = physicsSystem.getPhysicsHandle((ServerSubLevel)containingSubLevel);
         containingHandle.getLinearVelocity(containingLinearVelocity);
         containingHandle.getAngularVelocity(containingAngularVelocity);
      } else {
         containingPose = null;
      }

      ServerSubLevel subLevel = (ServerSubLevel)container.allocateNewSubLevel(pose);
      LevelPlot plot = subLevel.getPlot();
      plot.newEmptyChunk(plot.getCenterChunk());
      BlockPos plotAnchor = plot.getCenterBlock();
      SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(anchor, plotAnchor, 0, Rotation.NONE, level);
      moveOtherStuff(level, transform, blocks, bounds);
      moveBlocks(level, transform, blocks);
      Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
      Vec3 subLevelCenter = Vec3.atLowerCornerOf(anchor);
      if (centerOfMass != null) {
         subLevelCenter = subLevelCenter.subtract(Vec3.atLowerCornerOf(plotAnchor)).add(centerOfMass.x(), centerOfMass.y(), centerOfMass.z());
      } else {
         subLevel.logicalPose().rotationPoint().set(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
      }

      subLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);
      PhysicsPipeline pipeline = physicsSystem.getPipeline();
      if (containingSubLevel != null) {
         kickFromContainingSubLevel(
            pipeline,
            subLevel,
            containingLinearVelocity,
            containingAngularVelocity,
            containingPose,
            !containingSubLevel.isRemoved() ? containingSubLevel : null
         );
      }

      pipeline.teleport(subLevel, subLevel.logicalPose().position(), subLevel.logicalPose().orientation());
      subLevel.updateLastPose();
      subLevel.updateBoundingBox();
      moveTrackingPoints(level, bounds, subLevel, transform);
      return subLevel;
   }

   @Internal
   public static void kickFromContainingSubLevel(
      ServerLevel level, SubLevelPhysicsSystem physicsSystem, PhysicsPipeline pipeline, ServerSubLevel subLevel, SubLevel containingSubLevel
   ) {
      RigidBodyHandle containingHandle = physicsSystem.getPhysicsHandle((ServerSubLevel)containingSubLevel);
      Vector3d linearVelocity = containingHandle.getLinearVelocity(new Vector3d());
      Vector3d angularVelocity = containingHandle.getAngularVelocity(new Vector3d());
      Pose3d containingPose = containingSubLevel.logicalPose();
      kickFromContainingSubLevel(pipeline, subLevel, linearVelocity, angularVelocity, containingPose, containingSubLevel);
   }

   @Internal
   private static void kickFromContainingSubLevel(
      PhysicsPipeline pipeline,
      ServerSubLevel subLevel,
      Vector3d containingLinearVelocity,
      Vector3d containingAngularVelocity,
      Pose3d containingPose,
      @Nullable SubLevel containingSubLevel
   ) {
      Pose3d originalPose = new Pose3d(subLevel.logicalPose());
      containingPose.transformPosition(subLevel.logicalPose().position());
      Vector3d localPos = subLevel.logicalPose().position().sub(containingPose.position(), new Vector3d());
      pipeline.addLinearAndAngularVelocity(
         subLevel, containingAngularVelocity.cross(localPos, localPos).add(containingLinearVelocity), containingAngularVelocity
      );
      if (containingSubLevel != null) {
         subLevel.setSplitFrom((ServerSubLevel)containingSubLevel, originalPose);
      }
   }

   @NotNull
   public static SubLevelAssemblyHelper.GatherResult gatherConnectedBlocks(
      BlockPos gatherOrigin, ServerLevel level, int maximumBlocksToAssemble, @Nullable SubLevelAssemblyHelper.FrontierPredicate frontierPredicate
   ) {
      LinkedHashSet<Pair<BlockPos, BlockState>> frontier = new LinkedHashSet<>(4096);
      Set<BlockPos> blocks = new ObjectOpenHashSet(1024);
      LevelAccelerator accelerator = new LevelAccelerator(level);
      BlockState gatherOriginState = accelerator.getBlockState(gatherOrigin);
      if (gatherOriginState.isAir()) {
         return new SubLevelAssemblyHelper.GatherResult(null, 0, null, SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS);
      } else {
         frontier.add(Pair.of(gatherOrigin, gatherOriginState));
         int minX = gatherOrigin.getX();
         int minY = gatherOrigin.getY();
         int minZ = gatherOrigin.getZ();
         int maxX = gatherOrigin.getX();
         int maxY = gatherOrigin.getY();
         int maxZ = gatherOrigin.getZ();
         int blockCount = 0;
         MutableBlockPos mutablePos = new MutableBlockPos();

         while (!frontier.isEmpty()) {
            Pair<BlockPos, BlockState> pair = frontier.removeFirst();
            BlockPos pos = (BlockPos)pair.key();
            if (++blockCount > maximumBlocksToAssemble) {
               return new SubLevelAssemblyHelper.GatherResult(null, blockCount, null, SubLevelAssemblyHelper.GatherResult.State.TOO_MANY_BLOCKS);
            }

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            blocks.add(pos);

            for (int x = -1; x <= 1; x++) {
               for (int y = -1; y <= 1; y++) {
                  for (int z = -1; z <= 1; z++) {
                     if (x != 0 || y != 0 || z != 0) {
                        int absTotal = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (absTotal != 3) {
                           BlockPos candidate = mutablePos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                           if (!frontier.contains(candidate)) {
                              Direction direction = absTotal == 1 ? Direction.getNearest(x, y, z, null) : null;
                              BlockState candidateState = accelerator.getBlockState(candidate);
                              if (!candidateState.isAir()
                                 && (
                                    frontierPredicate == null
                                       || frontierPredicate.isValidConnection(pos, (BlockState)pair.second(), candidate, candidateState, direction)
                                 )
                                 && !blocks.contains(candidate)) {
                                 frontier.add(Pair.of(candidate.immutable(), candidateState));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         BoundingBox3i bounds = new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);
         return blocks.isEmpty()
            ? new SubLevelAssemblyHelper.GatherResult(null, blockCount, null, SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS)
            : new SubLevelAssemblyHelper.GatherResult(blocks, blockCount, bounds, SubLevelAssemblyHelper.GatherResult.State.SUCCESS);
      }
   }

   public static void moveTrackingPoints(ServerLevel level, BoundingBox3ic bounds, ServerSubLevel subLevel, SubLevelAssemblyHelper.AssemblyTransform transform) {
      SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(level);

      for (Pair<UUID, TrackingPoint> entry : data.getAllTrackingPoints(bounds)) {
         UUID key = (UUID)entry.key();
         TrackingPoint point = new TrackingPoint(
            subLevel != null,
            subLevel != null ? subLevel.getUniqueId() : null,
            subLevel != null ? subLevel.getLastSerializationPointer() : null,
            JOMLConversion.toJOML(transform.apply(JOMLConversion.toMojang(((TrackingPoint)entry.value()).point()))),
            ((TrackingPoint)entry.value()).globalPlaceholderPosition()
         );
         data.setTrackingPoint(key, point);
      }
   }

   public static void moveOtherStuff(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, BoundingBox3ic bounds) {
      List<Entity> entities = level.getEntitiesOfClass(Entity.class, bounds.toAABB().inflate(2.0));
      boolean needsBitSet = needsBitSet(level, bounds, entities);
      if (needsBitSet) {
         BoundedBitVolume3i volume = BoundedBitVolume3i.fromBlocks(blocks);

         assert volume != null;

         for (Entity entity : entities) {
            boolean moveEntity = false;
            if (entity instanceof HangingEntity hangingEntity) {
               moveEntity = BlockPos.betweenClosedStream(hangingEntity.calculateSupportBox())
                  .anyMatch(blockPos -> volume.getOccupied(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            }

            if (moveEntity) {
               entity.setPos(transform.apply(entity.position()));
            }
         }
      }
   }

   private static boolean needsBitSet(ServerLevel level, BoundingBox3ic bounds, List<Entity> entities) {
      return !entities.isEmpty();
   }

   public static void moveBlocks(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks) {
      ServerLevel resultingLevel = transform.resultingLevel;
      LevelAccelerator accelerator = new LevelAccelerator(level);
      LevelAccelerator resultingAccelerator = new LevelAccelerator(resultingLevel);
      BlockState airState = Blocks.AIR.defaultBlockState();
      List<BlockState> states = new ArrayList<>();
      BlockPos firstBlock = null;
      Vector2i chunkBoundsMin = null;
      Vector2i chunkBoundsMax = null;

      for (BlockPos block : blocks) {
         if (firstBlock == null) {
            firstBlock = block;
         }

         BlockPos blockPos = transform.apply(block);
         ChunkPos chunk = new ChunkPos(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()));
         Vector2i jomlChunkPos = new Vector2i(chunk.x, chunk.z);
         if (chunkBoundsMin == null) {
            chunkBoundsMin = new Vector2i(jomlChunkPos);
            chunkBoundsMax = new Vector2i(jomlChunkPos);
         }

         chunkBoundsMin.min(jomlChunkPos);
         chunkBoundsMax.max(jomlChunkPos);
      }

      SubLevel subLevel = Sable.HELPER.getContaining(level, transform.apply(firstBlock));
      if (subLevel != null) {
         LevelPlot plot = subLevel.getPlot();

         for (int chunkX = chunkBoundsMin.x; chunkX <= chunkBoundsMax.x; chunkX++) {
            for (int chunkZ = chunkBoundsMin.y; chunkZ <= chunkBoundsMax.y; chunkZ++) {
               if (plot.getChunkHolder(plot.toLocal(new ChunkPos(chunkX, chunkZ))) == null) {
                  plot.newEmptyChunk(new ChunkPos(chunkX, chunkZ));
               }
            }
         }
      }

      SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(resultingLevel, true);

      for (BlockPos block : blocks) {
         BlockState state = accelerator.getBlockState(block);
         BlockPos newPos = transform.apply(block);

         try {
            BlockState subLevelState = transform.apply(state);
            if (state.getBlock() instanceof BlockSubLevelAssemblyListener listener) {
               listener.beforeMove(level, resultingLevel, state, block, newPos);
            }

            BlockEntity blockEntity = level.getBlockEntity(block);
            CompoundTag tag = null;
            if (blockEntity != null) {
               tag = blockEntity.saveWithFullMetadata(level.registryAccess());
               tag.putInt("x", newPos.getX());
               tag.putInt("y", newPos.getY());
               tag.putInt("z", newPos.getZ());
            }

            if (state.is(SableTags.SILENT_ASSEMBLY_REMOVAL)) {
               level.removeBlockEntity(block);
            } else {
               if (blockEntity instanceof RandomizableContainer container) {
                  container.setLootTable(null);
               }

               if (blockEntity instanceof Clearable clearable) {
                  clearable.clearContent();
               }
            }

            LevelChunk chunk = resultingAccelerator.getChunk(SectionPos.blockToSectionCoord(newPos.getX()), SectionPos.blockToSectionCoord(newPos.getZ()));
            chunk.setBlockState(newPos, subLevelState, 64);
            states.add(subLevelState);
            BlockEntity newBlockEntity = resultingLevel.getBlockEntity(newPos);
            if (newBlockEntity != null && tag != null) {
               newBlockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            }

            if (state.getBlock() instanceof BlockSubLevelAssemblyListener listener) {
               listener.afterMove(level, resultingLevel, state, block, newPos);
            }

            level.updatePOIOnBlockStateChange(newPos, airState, state);
         } catch (Exception e) {
            Sable.LOGGER.error("Failed to move block {} at {} to {}", new Object[]{state, block, newPos, e});
         }
      }

      SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(resultingLevel, false);
      int i = 0;

      for (BlockPos untransformed : blocks) {
         BlockPos pos = transform.apply(untransformed);

         try {
            LevelChunk levelchunk = resultingAccelerator.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            BlockState subLevelStatex = states.get(i);
            markAndNotifyBlock(resultingLevel, pos, levelchunk, airState, subLevelStatex, 19, 512);
         } catch (Exception e) {
            Sable.LOGGER.error("Failed to mark & notify block {} (untransformed = {})", new Object[]{pos, untransformed, e});
         }

         i++;
      }

      SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(resultingLevel, true);

      for (BlockPos block : blocks) {
         try {
            LevelChunk chunkx = accelerator.getChunk(SectionPos.blockToSectionCoord(block.getX()), SectionPos.blockToSectionCoord(block.getZ()));
            movePendingTicks(chunkx, level, resultingLevel, block, transform.apply(block));
            level.updatePOIOnBlockStateChange(block, chunkx.getBlockState(block), airState);
            chunkx.setBlockState(block, airState, 64);
         } catch (Exception e) {
            Sable.LOGGER.error("Failed to destroy old block during assembly {}", block, e);
         }
      }

      SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(resultingLevel, false);

      for (BlockPos block : blocks) {
         resultingLevel.sendBlockUpdated(block, Blocks.STONE.defaultBlockState(), airState, 3);
      }
   }

   private static void movePendingTicks(LevelChunk sourceChunk, Level sourceLevel, Level targetLevel, BlockPos from, BlockPos to) {
      if (sourceLevel != targetLevel || !from.equals(to)) {
         long sourceGameTime = sourceLevel.getGameTime();
         long targetGameTime = targetLevel.getGameTime();
         movePendingTicks(sourceChunk.getBlockTicks(), targetLevel.getBlockTicks(), from, to, sourceGameTime, targetGameTime);
         movePendingTicks(sourceChunk.getFluidTicks(), targetLevel.getFluidTicks(), from, to, sourceGameTime, targetGameTime);
      }
   }

   private static <T> void movePendingTicks(
      TickContainerAccess<T> sourceTicks, LevelTickAccess<T> targetTicks, BlockPos from, BlockPos to, long sourceGameTime, long targetGameTime
   ) {
      if (sourceTicks instanceof LevelChunkTicks<?> container) {
         List<ScheduledTick<T>> pending = container.getAll().filter(tickx -> tickx.pos().equals(from)).map(tickx -> (ScheduledTick<T>) tickx).toList();
         if (!pending.isEmpty()) {
            for (ScheduledTick<T> tick : pending) {
               long delay = Math.max(1L, tick.triggerTick() - sourceGameTime);
               targetTicks.schedule(new ScheduledTick<>(tick.type(), to, targetGameTime + delay, tick.priority(), tick.subTickOrder()));
            }

            boolean accepted = pending.stream().allMatch(tickx -> targetTicks.hasScheduledTick(to, (T)tickx.type()));
            if (accepted) {
               container.removeIf(tickx -> tickx.pos().equals(from));
            } else {
               Sable.LOGGER
                  .warn("Could not migrate {} pending tick(s) from {} to {}: no tick container registered there", new Object[]{pending.size(), from, to});
            }
         }
      }
   }

   public static void markAndNotifyBlock(
      Level level, BlockPos pPos, @Nullable LevelChunk levelchunk, BlockState oldState, BlockState newState, int pFlags, int pRecursionLeft
   ) {
      Block block = newState.getBlock();
      BlockState worldState = level.getBlockState(pPos);
      if (worldState == newState) {
         if (oldState != worldState) {
            level.setBlocksDirty(pPos, oldState, worldState);
         }

         if ((pFlags & 2) != 0 && levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
            level.sendBlockUpdated(pPos, oldState, newState, pFlags);
         }

         if ((pFlags & 1) != 0) {
            if (!level.isClientSide()) {
               level.updateNeighborsAt(pPos, oldState.getBlock());
            }

            if (newState.hasAnalogOutputSignal()) {
               level.updateNeighbourForOutputSignal(pPos, block);
            }
         }

         if ((pFlags & 16) == 0 && pRecursionLeft > 0) {
            int i = pFlags & -34;
            oldState.updateIndirectNeighbourShapes(level, pPos, i, pRecursionLeft - 1);
            newState.updateNeighbourShapes(level, pPos, i, pRecursionLeft - 1);
            newState.updateIndirectNeighbourShapes(level, pPos, i, pRecursionLeft - 1);
         }

         level.updatePOIOnBlockStateChange(pPos, oldState, worldState);
      }
   }

   public static class AssemblyTransform {
      private final BlockPos anchorPos;
      private final BlockPos resultingAnchorPos;
      private final int angle;
      private final Rotation rotation;
      private final ServerLevel resultingLevel;

      public AssemblyTransform(BlockPos anchorPos, BlockPos resultingAnchorPos, int angle, Rotation rotation, ServerLevel resultingLevel) {
         this.anchorPos = anchorPos;
         this.resultingAnchorPos = resultingAnchorPos;
         this.angle = angle;
         this.rotation = rotation;
         this.resultingLevel = resultingLevel;
      }

      public Vec3 apply(Vec3 pos) {
         return pos.subtract(Vec3.atCenterOf(this.anchorPos)).yRot((float)(this.angle * Math.PI / 2.0)).add(Vec3.atCenterOf(this.resultingAnchorPos));
      }

      public BlockPos apply(BlockPos pos) {
         return BlockPos.containing(this.apply(Vec3.atCenterOf(pos)));
      }

      public BlockState apply(BlockState state) {
         Block block = state.getBlock();
         if (block instanceof BellBlock) {
            if (state.getValue(BlockStateProperties.BELL_ATTACHMENT) == BellAttachType.DOUBLE_WALL) {
               state = state.setValue(BlockStateProperties.BELL_ATTACHMENT, BellAttachType.SINGLE_WALL);
            }

            return state.setValue(BellBlock.FACING, this.rotation.rotate(state.getValue(BellBlock.FACING)));
         } else {
            return state.rotate(this.rotation);
         }
      }

      public ServerLevel getLevel() {
         return this.resultingLevel;
      }

      public Rotation getRotation() {
         return this.rotation;
      }
   }

   @FunctionalInterface
   public interface FrontierPredicate {
      boolean isValidConnection(BlockPos pos, BlockState state, BlockPos candidate, BlockState candidateState, @Nullable Direction direction);
   }

   public record GatherResult(
      @Nullable Set<BlockPos> blocks, int checkedBlocks, @Nullable BoundingBox3i boundingBox, SubLevelAssemblyHelper.GatherResult.State assemblyState
   ) {
      public static enum State {
         SUCCESS("commands.sable.sub_level.assemble.connected.success"),
         TOO_MANY_BLOCKS("commands.sable.sub_level.assemble.connected.too_many_blocks"),
         NO_BLOCKS("commands.sable.sub_level.assemble.no_blocks");

         public final String errorKey;

         private State(final String errorKey) {
            this.errorKey = errorKey;
         }
      }
   }
}
