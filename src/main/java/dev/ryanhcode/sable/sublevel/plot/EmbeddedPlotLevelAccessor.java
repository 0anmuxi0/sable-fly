package dev.ryanhcode.sable.sublevel.plot;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

public class EmbeddedPlotLevelAccessor implements CommonLevelAccessor, ServerLevelAccessor {
   private final LevelPlot plot;
   private final BlockPos center;
   private final ChunkPos centerChunk;
   private final Level level;

   public EmbeddedPlotLevelAccessor(LevelPlot plot) {
      this.plot = plot;
      this.level = plot.getSubLevel().getLevel();
      this.center = plot.getCenterBlock();
      this.centerChunk = plot.getCenterChunk();
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.level.getWorldBorder();
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return this.level.getBlockEntity(blockPos.offset(this.center));
   }

   @Override
   public BlockState getBlockState(BlockPos blockPos) {
      return this.level.getBlockState(blockPos.offset(this.center));
   }

   @Override
   public FluidState getFluidState(BlockPos blockPos) {
      return this.level.getFluidState(blockPos.offset(this.center));
   }

   @Override
   public List<Entity> getEntities(@Nullable Entity entity, AABB aABB, Predicate<? super Entity> predicate) {
      return this.level.getEntities(entity, aABB.move(this.center.getX(), this.center.getY(), this.center.getZ()), predicate);
   }

   @Override
   public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate) {
      return this.level.getEntities(entityTypeTest, aABB.move(this.center.getX(), this.center.getY(), this.center.getZ()), predicate);
   }

   @Override
   public List<? extends Player> players() {
      return this.level.players();
   }

   @Nullable
   @Override
   public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
      return this.level.getChunk(i + this.centerChunk.x, j + this.centerChunk.z, chunkStatus, bl);
   }

   @Override
   public long nextSubTickCount() {
      return this.level.nextSubTickCount();
   }

   @Override
   public LevelTickAccess<Block> getBlockTicks() {
      return this.level.getBlockTicks();
   }

   @Override
   public LevelTickAccess<Fluid> getFluidTicks() {
      return this.level.getFluidTicks();
   }

   @Override
   public LevelData getLevelData() {
      return this.level.getLevelData();
   }

   @Override
   public DifficultyInstance getCurrentDifficultyAt(BlockPos blockPos) {
      return this.getLevel().getCurrentDifficultyAt(blockPos.offset(this.center));
   }

   @Nullable
   @Override
   public MinecraftServer getServer() {
      return this.level.getServer();
   }

   @Override
   public ChunkSource getChunkSource() {
      return this.level.getChunkSource();
   }

   @Override
   public boolean hasChunk(int i, int j) {
      return this.level.hasChunk(i + this.centerChunk.x, j + this.centerChunk.z);
   }

   @Override
   public RandomSource getRandom() {
      return this.level.getRandom();
   }

   @Override
   public void playSound(@Nullable Entity entity, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
      this.level.playSound(entity, blockPos.offset(this.center), soundEvent, soundSource, f, g);
   }

   @Override
   public void addParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
      this.level.addParticle(particleOptions, d + this.center.getX(), e + this.center.getY(), f + this.center.getZ(), g, h, i);
   }

   @Override
   public void levelEvent(@Nullable Entity entity, int i, BlockPos blockPos, int j) {
      this.level.levelEvent(entity, i, blockPos.offset(this.center), j);
   }

   @Override
   public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) {
      this.level.gameEvent(holder, vec3, context);
   }

   @Override
   public int getHeight(Types types, int i, int j) {
      return this.level.getHeight(types, i + this.center.getX(), j + this.center.getZ());
   }

   @Override
   public int getSkyDarken() {
      return this.level.getSkyDarken();
   }

   @Override
   public BiomeManager getBiomeManager() {
      return this.level.getBiomeManager();
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
      return this.level.getUncachedNoiseBiome(i + this.center.getX(), j + this.center.getY(), k + this.center.getZ());
   }

   @Override
   public boolean isClientSide() {
      return this.level.isClientSide();
   }

   @Deprecated
   @Override
   public int getSeaLevel() {
      return this.level.getSeaLevel();
   }

   @Override
   public DimensionType dimensionType() {
      return this.level.dimensionType();
   }

   @Override
   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }

   @Override
   public EnvironmentAttributeReader environmentAttributes() {
      return this.environmentAttributes();
   }

   @Override
   public boolean isStateAtPosition(BlockPos blockPos, Predicate<BlockState> predicate) {
      return this.level.isStateAtPosition(blockPos.offset(this.center), predicate);
   }

   @Override
   public boolean isFluidAtPosition(BlockPos blockPos, Predicate<FluidState> predicate) {
      return this.level.isFluidAtPosition(blockPos.offset(this.center), predicate);
   }

   @Override
   public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
      BlockPos globalPos = blockPos.offset(this.center);
      this.plot.ensureChunkFor(globalPos);
      return this.level.setBlock(globalPos, blockState, i, j);
   }

   @Override
   public boolean removeBlock(BlockPos blockPos, boolean bl) {
      return this.level.removeBlock(blockPos.offset(this.center), bl);
   }

   @Override
   public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i) {
      return this.level.destroyBlock(blockPos.offset(this.center), bl, entity, i);
   }

   @Override
   public ServerLevel getLevel() {
      return (ServerLevel)this.level;
   }
}
