package dev.ryanhcode.sable.physics.config.block_properties;

import com.google.common.collect.UnmodifiableIterator;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs.TagOrElementLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class PhysicsBlockPropertiesDefinitionLoader extends SimpleJsonResourceReloadListener<PhysicsBlockPropertiesDefinition> {
   public static final String NAME = "physics_block_properties";
   public static final Identifier ID = Sable.sablePath("physics_block_properties");
   public static final PhysicsBlockPropertiesDefinitionLoader INSTANCE = new PhysicsBlockPropertiesDefinitionLoader();
   private final ObjectList<PhysicsBlockPropertiesDefinition> definitions = new ObjectArrayList();

   private PhysicsBlockPropertiesDefinitionLoader() {
      super(PhysicsBlockPropertiesDefinition.CODEC, FileToIdConverter.json("physics_block_properties"));
   }

   @Override
   public String getName() {
      return super.getName();
   }

   protected void apply(Map<Identifier, PhysicsBlockPropertiesDefinition> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      this.definitions.clear();
      this.definitions.addAll(map.values());
      this.definitions.sort(Comparator.comparingInt(PhysicsBlockPropertiesDefinition::priority));
   }

   private static boolean hasBlocksInNamespace(String namespace) {
      for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
         if (id.getNamespace().equals(namespace)) {
            return true;
         }
      }

      return false;
   }

   public static void applyToBlocks(PhysicsBlockPropertiesDefinition definition) {
      TagOrElementLocation selector = definition.selector();
      ObjectArrayList<Block> blocks = new ObjectArrayList(16);
      boolean namespacePresent = hasBlocksInNamespace(selector.id().getNamespace());
      if (selector.tag()) {
         TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, selector.id());
         Optional<Named<Block>> tagBlocks = BuiltInRegistries.BLOCK.get(tagKey);
         if (tagBlocks.isPresent()) {
            for (Holder<Block> blockHolder : tagBlocks.get()) {
               Block block = blockHolder.value();
               blocks.add(block);
            }
         } else if (namespacePresent) {
            Sable.LOGGER.error("Failed to apply tag physics properties. Unknown tag: {}", selector.id());
         } else {
            Sable.LOGGER.debug("Skipping physics properties for {}: namespace {} is not installed", selector.id(), selector.id().getNamespace());
         }
      } else if (BuiltInRegistries.BLOCK.containsKey(selector.id())) {
         Block block = BuiltInRegistries.BLOCK.getValue(selector.id());
         blocks.add(block);
      } else if (namespacePresent) {
         Sable.LOGGER.error("Failed to apply tag physics properties. Unknown block: {}", selector.id());
      } else {
         Sable.LOGGER.debug("Skipping physics properties for {}: namespace {} is not installed", selector.id(), selector.id().getNamespace());
      }

      ObjectListIterator iterator = blocks.iterator();

      while (iterator.hasNext()) {
         Block block = (Block)iterator.next();
         StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
         UnmodifiableIterator stateIterator = stateDefinition.getPossibleStates().iterator();

         while (stateIterator.hasNext()) {
            BlockState state = (BlockState)stateIterator.next();
            ((BlockStateExtension)state).sable$loadProperties(stateDefinition, definition);
         }
      }
   }

   public void applyAll() {
      ObjectListIterator iterator = this.definitions.iterator();

      while (iterator.hasNext()) {
         PhysicsBlockPropertiesDefinition definition = (PhysicsBlockPropertiesDefinition)iterator.next();
         applyToBlocks(definition);
      }
   }

   public Collection<PhysicsBlockPropertiesDefinition> getDefinitions() {
      return this.definitions;
   }
}
