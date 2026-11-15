package dev.ryanhcode.sable.sublevel.plot;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.compatibility.LightEngineCompat;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.mixinterface.plot.serialization.LevelChunkTicksExtension;
import dev.ryanhcode.sable.platform.SableChunkEventPlatform;
import dev.ryanhcode.sable.platform.SablePlotPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.ChunkAccess.PackedTicks;
import net.minecraft.world.level.chunk.storage.SerializableChunkData.ChunkReadException;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;

public class ServerLevelPlot extends LevelPlot {
   protected static final int DATA_VERSION = 1;
   private static final Strategy<BlockState> BLOCK_STATE_STRATEGY = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);
   private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
      BlockState.CODEC, BLOCK_STATE_STRATEGY, Blocks.AIR.defaultBlockState()
   );
   private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
   private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();
   protected final LevelLightEngine lightEngine;
   private final Set<KinematicContraption> contraptions = new ReferenceOpenHashSet();
   private final Long2ObjectMap<BlockSubLevelLiftProvider.LiftProviderContext> liftProviders = new Long2ObjectOpenHashMap();

   public ServerLevelPlot(SubLevelContainer plotContainer, int x, int z, int logSize, ServerSubLevel subLevel) {
      super(plotContainer, x, z, logSize, subLevel);
      Level level = subLevel.getLevel();
      LevelLightEngine parentLightEngine = level.getLightEngine();
      ChunkSource chunkSource = level.getChunkSource();
      this.lightEngine = new LevelLightEngine(chunkSource, LightEngineCompat.hasBlockLight(parentLightEngine), LightEngineCompat.hasSkyLight(parentLightEngine));
   }

   public void addContraption(KinematicContraption contraption) {
      this.contraptions.add(contraption);
   }

   public void removeContraption(KinematicContraption contraption) {
      this.contraptions.remove(contraption);
   }

   public Collection<KinematicContraption> getContraptions() {
      return this.contraptions;
   }

   private static void logLoadingErrors(ChunkPos chunkPos, int y, String errorText) {
      Sable.LOGGER.error("Recoverable errors when loading plot section [{}, {}, {}]: {}", new Object[]{chunkPos.x, y, chunkPos.z, errorText});
   }

   @Override
   public void tick() {
      do {
         this.lightEngine.runLightUpdates();
      } while (this.lightEngine.hasLightWork());

      this.contraptions.removeIf(contraption -> !contraption.sable$isValid());
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Override
   protected void onRemoveChunkHolder(LevelChunk levelChunk) {
      ChunkPos pos = levelChunk.getPos();
      ServerLevel serverLevel = this.getSubLevel().getLevel();
      ServerChunkCache var5 = serverLevel.getChunkSource();
      if (var5 instanceof ServerChunkCache) {
         var5.chunkMap.updatingChunkMap.remove(pos.pack());
         var5.chunkMap.modified = true;
      }

      levelChunk.setLoaded(false);
      serverLevel.unload(levelChunk);
      if (LightEngineCompat.usesScalableLux()) {
         LightEngineCompat.removeChunk(this.lightEngine, pos);
      } else {
         this.lightEngine.retainData(pos, false);
         this.lightEngine.setLightEnabled(pos, false);

         for (int idx = this.lightEngine.getMinLightSection(); idx < this.lightEngine.getMaxLightSection(); idx++) {
            this.lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, idx), null);
            this.lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(pos, idx), null);
         }

         for (int idx = serverLevel.getMinSectionY(); idx < serverLevel.getMaxSectionY(); idx++) {
            this.lightEngine.updateSectionStatus(SectionPos.of(pos, idx), true);
         }
      }

      serverLevel.entityManager.updateChunkStatus(pos, FullChunkStatus.INACCESSIBLE);
   }

   public void setBiome(ResourceKey<Biome> biome) {
      this.biome = biome;
   }

   private void initializeLight(LevelChunk chunk) {
      LevelChunkSection[] alevelchunksection = chunk.getSections();
      Level level = chunk.getLevel();
      ChunkPos pos = chunk.getPos();
      LevelLightEngine lightEngine = this.lightEngine;

      for (int i = 0; i < chunk.getSectionsCount(); i++) {
         LevelChunkSection levelchunksection = alevelchunksection[i];
         if (!levelchunksection.hasOnlyAir()) {
            this.lightEngine.updateSectionStatus(SectionPos.of(pos, level.getSectionYFromSectionIndex(i)), false);
         }
      }

      lightEngine.setLightEnabled(pos, true);
      lightEngine.retainData(pos, false);
   }

   private void correctLight(LevelChunk chunk) {
      if (!chunk.isLightCorrect()) {
         this.lightEngine.propagateLightSources(chunk.getPos());
         chunk.setLightCorrect(true);
      }
   }

   private void lightChunk(LevelChunk chunk) {
      if (LightEngineCompat.usesScalableLux()) {
         LightEngineCompat.lightChunk(this.lightEngine, chunk);
      } else {
         chunk.initializeLightSources();
         this.initializeLight(chunk);
         this.correctLight(chunk);
      }
   }

   @Override
   public void addChunkHolder(ChunkPos localChunkPos, PlotChunkHolder holder, boolean initializeLighting) {
      ServerLevel level = this.getSubLevel().getLevel();
      ChunkPos globalChunkPos = this.toGlobal(localChunkPos);
      LevelChunk chunk = holder.getChunk();
      ServerChunkCache cache = level.getChunkSource();
      cache.chunkMap.updatingChunkMap.put(globalChunkPos.pack(), holder);
      cache.chunkMap.modified = true;
      super.addChunkHolder(localChunkPos, holder, initializeLighting);
      chunk.setLightCorrect(false);
      if (initializeLighting) {
         this.lightChunk(chunk);
      }

      chunk.setFullStatus(holder::getFullStatus);
      chunk.runPostLoad();
      chunk.setLoaded(true);
      chunk.registerAllBlockEntitiesAfterLevelLoad();
      chunk.registerTickContainerInLevel(level);
      level.entityManager.updateChunkStatus(chunk.getPos(), FullChunkStatus.ENTITY_TICKING);
      level.getChunkSource().chunkMap.onFullChunkStatusChange(globalChunkPos, FullChunkStatus.ENTITY_TICKING);

      do {
         this.lightEngine.runLightUpdates();
      } while (this.lightEngine.hasLightWork());

      for (ServerPlayer player : this.container.getPlayersTracking(globalChunkPos)) {
         SubLevelPlayerChunkSender.sendChunk(player.connection::send, this.lightEngine, chunk);
         SubLevelPlayerChunkSender.sendChunkPoiData(level, chunk);
      }
   }

   public void kickAllEntities() {
      ServerSubLevel subLevel = this.getSubLevel();
      PersistentEntitySectionManager<Entity> manager = subLevel.getLevel().entityManager;

      for (PlotChunkHolder chunk : this.getLoadedChunks()) {
         Stream<EntitySection<Entity>> sections = manager.sectionStorage.getExistingSectionsInChunk(chunk.getPos().pack());

         for (EntitySection<Entity> section : sections.toList()) {
            for (Entity entity : section.getEntities().toList()) {
               if (entity.is(SableTags.DESTROY_WITH_SUB_LEVEL)) {
                  entity.remove(RemovalReason.KILLED);
                  section.remove(entity);
               } else {
                  EntitySubLevelUtil.kickEntity(subLevel, entity);
               }
            }
         }
      }
   }

   public void destroyAllBlocks() {
      if (this.localBounds != null && this.localBounds != BoundingBox3i.EMPTY) {
         Level level = this.getSubLevel().getLevel();
         BoundingBox3i bounds = this.localBounds;

         for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
               for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  level.destroyBlock(pos, true);
               }
            }
         }
      }
   }

   private void newNonLitChunk(ChunkPos pos) {
      Level level = this.container.getLevel();
      int sectionCount = level.getSectionsCount();
      LevelChunkSection[] sections = new LevelChunkSection[sectionCount];

      for (int i = 0; i < sectionCount; i++) {
         sections[i] = new LevelChunkSection(level.palettedContainerFactory());
      }

      LevelChunk chunk = new LevelChunk(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, sections, null, null);
      this.newChunk(pos, chunk, false);
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("plot_x", this.plotPos.x - this.container.getOrigin().x());
      tag.putInt("plot_z", this.plotPos.z - this.container.getOrigin().y);
      tag.putInt("log_size", this.logSize);
      tag.putString("biome", this.biome.identifier().toString());
      tag.putInt("data_version", 1);
      ServerLevel level = this.getSubLevel().getLevel();
      CompoundTag chunks = new CompoundTag();

      for (PlotChunkHolder chunkHolder : this.getLoadedChunks()) {
         ChunkPos global = chunkHolder.getPos();
         ChunkPos local = this.toLocal(global);
         LevelChunk chunk = chunkHolder.getChunk();
         CompoundTag chunkTag = new CompoundTag();
         CompoundTag sectionsTag = new CompoundTag();

         for (int idx = 0; idx < chunk.getSectionsCount(); idx++) {
            LevelChunkSection section = chunk.getSection(idx);
            if (!section.hasOnlyAir()) {
               CompoundTag sectionTag = new CompoundTag();
               sectionTag.put("block_states", (Tag)BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow());
               SectionPos sectionPos = SectionPos.of(global, level.getSectionYFromSectionIndex(idx));
               DataLayer blockLight = this.lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(sectionPos);
               DataLayer skyLight = this.lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(sectionPos);
               if (blockLight != null && !blockLight.isEmpty()) {
                  sectionTag.putByteArray("BlockLight", blockLight.getData());
               }

               if (skyLight != null && !skyLight.isEmpty()) {
                  sectionTag.putByteArray("SkyLight", skyLight.getData());
               }

               sectionsTag.put(String.valueOf(idx), sectionTag);
            }
         }

         chunkTag.put("sections", sectionsTag);
         tag.putBoolean("isLightOn", chunk.isLightCorrect());
         ListTag blockEntitiesTag = new ListTag();

         for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag blockEntityNBT = chunk.getBlockEntityNbtForSaving(blockPos, level.registryAccess());
            if (blockEntityNBT != null) {
               blockEntitiesTag.add(blockEntityNBT);
            }
         }

         chunkTag.put("block_entities", blockEntitiesTag);
         PackedTicks ticksToSave = chunk.getTicksForSerialization(level.getGameTime());
         chunkTag.store("block_ticks", BLOCK_TICKS_CODEC, ticksToSave.blocks());
         chunkTag.store("fluid_ticks", FLUID_TICKS_CODEC, ticksToSave.fluids());
         CompoundTag heightMapsTag = new CompoundTag();

         for (Entry<Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
               heightMapsTag.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
         }

         chunkTag.put("heightmaps", heightMapsTag);
         SablePlotPlatform.INSTANCE.writeLightData(tag, level.registryAccess(), chunk);
         SablePlotPlatform.INSTANCE.writeChunkAttachments(tag, level.registryAccess(), chunk);
         chunks.put(String.valueOf(ChunkPos.pack(local.x, local.z)), chunkTag);
      }

      tag.put("chunks", chunks);
      return tag;
   }

   public void load(CompoundTag tag) {
      int logSize = tag.getIntOr("log_size", 0);
      if (logSize != this.logSize) {
         throw new IllegalArgumentException("Log size mismatch");
      } else {
         int dataVersion = tag.getIntOr("data_version", 0);
         if (dataVersion >= 0 && dataVersion <= 1) {
            ServerSubLevel subLevel = this.getSubLevel();
            ServerLevel level = subLevel.getLevel();
            if (tag.contains("biome")) {
               Identifier location = Identifier.tryParse(tag.getStringOr("biome", ""));
               if (location != null) {
                  this.biome = ResourceKey.create(Registries.BIOME, location);
               }
            }

            CompoundTag chunks = tag.getCompoundOrEmpty("chunks");

            for (String key : chunks.keySet()) {
               long chunkPos = Long.parseLong(key);
               int x = ChunkPos.getX(chunkPos);
               int z = ChunkPos.getZ(chunkPos);
               ChunkPos local = new ChunkPos(x, z);
               ChunkPos global = this.toGlobal(local);
               CompoundTag chunkTag = chunks.getCompoundOrEmpty(key);
               CompoundTag sectionsTag = chunkTag.getCompoundOrEmpty("sections");
               this.newNonLitChunk(global);
               LevelChunk chunk = this.getChunk(local);
               boolean hasLit = false;

               for (String sectionKey : sectionsTag.keySet()) {
                  int yIndex = Integer.parseInt(sectionKey);
                  LevelChunkSection[] sections = chunk.getSections();
                  CompoundTag sectionTag = sectionsTag.getCompoundOrEmpty(sectionKey);
                  PalettedContainer<BlockState> palettedContainer = (PalettedContainer<BlockState>)BLOCK_STATE_CODEC.parse(
                        NbtOps.INSTANCE, sectionTag.getCompoundOrEmpty("block_states")
                     )
                     .promotePartial(
                        string -> logLoadingErrors(
                           new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos)), chunk.getSectionYFromSectionIndex(yIndex), string
                        )
                     )
                     .getOrThrow(ChunkReadException::new);
                  Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
                  PalettedContainer<Holder<Biome>> biomeContainer = new PalettedContainer<>(
                     biomeRegistry.getOrThrow(this.biome), level.palettedContainerFactory().biomeStrategy()
                  );
                  sections[yIndex] = new LevelChunkSection(palettedContainer, biomeContainer);
                  SectionPos sectionPos = SectionPos.of(global, level.getSectionYFromSectionIndex(yIndex));
                  boolean hasBlockLight = LightEngineCompat.hasBlockLight(this.lightEngine) && sectionTag.contains("BlockLight");
                  boolean hasSkyLight = LightEngineCompat.hasSkyLight(this.lightEngine)
                     && level.dimensionType().hasSkyLight()
                     && sectionTag.contains("SkyLight");
                  if (hasBlockLight || hasSkyLight) {
                     if (!hasLit) {
                        this.lightEngine.retainData(global, true);
                        hasLit = true;
                     }

                     if (hasBlockLight) {
                        this.lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, sectionTag.getByteArray("BlockLight").map(DataLayer::new).orElse(null));
                     }

                     if (hasSkyLight) {
                        this.lightEngine.queueSectionData(LightLayer.SKY, sectionPos, sectionTag.getByteArray("SkyLight").map(DataLayer::new).orElse(null));
                     }
                  }
               }

               if (dataVersion >= 0) {
                  LevelChunkTicks<Block> blockTicks = new LevelChunkTicks<>(
                     SavedTick.filterTickListForChunk(chunkTag.<List<SavedTick<Block>>>read("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()), global)
                  );
                  LevelChunkTicks<Fluid> fluidTicks = new LevelChunkTicks<>(
                     SavedTick.filterTickListForChunk(chunkTag.<List<SavedTick<Fluid>>>read("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()), global)
                  );
                  ((LevelChunkTicksExtension)chunk.getBlockTicks()).sable$copy(blockTicks);
                  ((LevelChunkTicksExtension)chunk.getFluidTicks()).sable$copy(fluidTicks);
                  CompoundTag heightMapsTag = chunkTag.getCompoundOrEmpty("heightmaps");
                  EnumSet<Types> enumset = EnumSet.noneOf(Types.class);

                  for (Types heightMapType : chunk.getPersistedStatus().heightmapsAfter()) {
                     String heightMapKey = heightMapType.getSerializationKey();
                     Optional<long[]> heightmapData = heightMapsTag.getLongArray(heightMapKey);
                     if (heightmapData.isPresent()) {
                        chunk.setHeightmap(heightMapType, heightmapData.get());
                     } else {
                        enumset.add(heightMapType);
                     }
                  }

                  Heightmap.primeHeightmaps(chunk, enumset);
                  SablePlotPlatform.INSTANCE.readLightData(chunkTag, level.registryAccess(), chunk);
                  chunk.setLightCorrect(chunkTag.getBooleanOr("isLightOn", false));
               }

               this.lightChunk(chunk);
               SablePlotPlatform.INSTANCE.readChunkAttachments(chunkTag, level.registryAccess(), chunk);
               ListTag blockEntitiesTag = chunkTag.getListOrEmpty("block_entities");

               for (int i = 0; i < blockEntitiesTag.size(); i++) {
                  CompoundTag blockEntityTag = blockEntitiesTag.getCompoundOrEmpty(i);
                  boolean keepBlockEntityPacked = blockEntityTag.getBooleanOr("keepPacked", false);
                  if (keepBlockEntityPacked) {
                     chunk.setBlockEntityNbt(blockEntityTag);
                  } else {
                     BlockPos blockPos = BlockEntity.getPosFromTag(chunk.getPos(), blockEntityTag);
                     BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, chunk.getBlockState(blockPos), blockEntityTag, level.registryAccess());
                     if (blockEntity != null) {
                        chunk.setBlockEntity(blockEntity);
                     }
                  }
               }

               chunk.registerAllBlockEntitiesAfterLevelLoad();
               level.startTickingChunk(chunk);
               SablePlotPlatform.INSTANCE.postLoad(chunkTag, chunk);
               SableChunkEventPlatform.INSTANCE.onPlotChunkLoaded(chunk);
            }

            do {
               this.lightEngine.runLightUpdates();
            } while (this.lightEngine.hasLightWork());

            SubLevelPhysicsSystem physicsSystem = ((ServerSubLevelContainer)this.container).physicsSystem();
            MutableBlockPos globalBlockPos = new MutableBlockPos();

            for (String key : chunks.keySet()) {
               long chunkPos = Long.parseLong(key);
               int x = ChunkPos.getX(chunkPos);
               int z = ChunkPos.getZ(chunkPos);
               ChunkPos local = new ChunkPos(x, z);
               ChunkPos global = this.toGlobal(local);
               PlotChunkHolder chunkHolder = this.getChunkHolder(local);
               LevelChunk chunk = this.getChunk(local);
               LevelChunkSection[] levelChunkSections = chunk.getSections();

               for (ServerPlayer player : this.container.getPlayersTracking(global)) {
                  SubLevelPlayerChunkSender.sendChunk(player.connection::send, this.lightEngine, chunk);
                  SubLevelPlayerChunkSender.sendChunkPoiData(level, chunk);
               }

               for (int ix = 0; ix < chunk.getSectionsCount(); ix++) {
                  LevelChunkSection section = levelChunkSections[ix];
                  if (!section.hasOnlyAir()) {
                     int sectionY = chunk.getSectionYFromSectionIndex(ix);
                     int chunkMinX = global.getMinBlockX();
                     int chunkMinY = sectionY << 4;
                     int chunkMinZ = global.getMinBlockZ();
                     boolean expandPlotBackup = this.expandPlotIfNecessary;
                     this.expandPlotIfNecessary = false;
                     BlockState airState = Blocks.AIR.defaultBlockState();

                     for (int xOff = 0; xOff < 16; xOff++) {
                        for (int yOff = 0; yOff < 16; yOff++) {
                           for (int zOff = 0; zOff < 16; zOff++) {
                              BlockState state = section.getBlockState(xOff, yOff, zOff);
                              if (!state.isAir()) {
                                 globalBlockPos.set(xOff + chunkMinX, yOff + chunkMinY, zOff + chunkMinZ);
                                 BlockPos immutable = globalBlockPos.immutable();
                                 chunkHolder.handleBlockChange(xOff, chunkMinY + yOff, zOff, airState, state);
                                 subLevel.getHeatMapManager().onSolidAdded(immutable);
                                 subLevel.getFloatingBlockController().queueAddFloatingBlock(state, immutable);
                                 physicsSystem.updateMassDataFromBlockChange(subLevel, globalBlockPos, airState, state, false);
                                 this.onBlockChange(immutable, state);
                              }
                           }
                        }
                     }

                     this.expandPlotIfNecessary = expandPlotBackup;
                  }
               }
            }

            this.updateBoundingBox();
            subLevel.updateMergedMassData(1.0F);
            physicsSystem.getPipeline().onStatsChanged(subLevel);

            for (String key : chunks.keySet()) {
               long chunkPos = Long.parseLong(key);
               int x = ChunkPos.getX(chunkPos);
               int z = ChunkPos.getZ(chunkPos);
               ChunkPos local = new ChunkPos(x, z);
               ChunkPos global = this.toGlobal(local);
               LevelChunk chunk = this.getChunk(local);
               LevelChunkSection[] levelChunkSections = chunk.getSections();

               for (int ixx = 0; ixx < chunk.getSectionsCount(); ixx++) {
                  LevelChunkSection section = levelChunkSections[ixx];
                  if (!section.hasOnlyAir()) {
                     int sectionY = chunk.getSectionYFromSectionIndex(ixx);
                     physicsSystem.getTicketManager().addTicketForSection(level, SectionPos.of(global.x, sectionY, global.z));
                     physicsSystem.getPipeline().handleChunkSectionAddition(section, global.x, sectionY, global.z, true);
                  }
               }
            }

            subLevel.updateMergedMassData(1.0F);
            physicsSystem.getPipeline().onStatsChanged(subLevel);
         } else {
            throw new IllegalArgumentException("Unsupported version: " + dataVersion);
         }
      }
   }

   @Override
   public void onBlockChange(BlockPos pos, BlockState state) {
      super.onBlockChange(pos, state);
      this.liftProviders.remove(pos.asLong());
      if (state.getBlock() instanceof BlockSubLevelLiftProvider prov) {
         this.liftProviders
            .put(pos.asLong(), new BlockSubLevelLiftProvider.LiftProviderContext(pos, state, Vec3.atLowerCornerOf(prov.sable$getNormal(state).getUnitVec3i())));
      }
   }

   public ObjectCollection<BlockSubLevelLiftProvider.LiftProviderContext> getLiftProviders() {
      return this.liftProviders.values();
   }

   public ServerSubLevel getSubLevel() {
      return (ServerSubLevel)super.getSubLevel();
   }
}
