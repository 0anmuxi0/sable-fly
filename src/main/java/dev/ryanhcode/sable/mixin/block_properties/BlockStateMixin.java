package dev.ryanhcode.sable.mixin.block_properties;

import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.BlockStateConditionSet;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinition;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({BlockState.class})
public class BlockStateMixin implements BlockStateExtension {
   @Unique
   @org.jetbrains.annotations.Nullable
   private Object[] sable$properties = null;

   @Override
   public void sable$loadProperties(StateDefinition<Block, BlockState> stateDefinition, PhysicsBlockPropertiesDefinition definition) {
      if (this.sable$properties == null) {
         this.sable$properties = new Object[PhysicsBlockPropertyTypes.count()];
      }

      this.sable$applyPropertySet(definition.properties());
      if (definition.overrides().isPresent()) {
         for (Entry<BlockStateConditionSet, Map<Identifier, Object>> override : definition.overrides().get().entrySet()) {
            if (override.getKey().matches(stateDefinition, (BlockState)(Object)this)) {
               this.sable$applyPropertySet(override.getValue());
            }
         }
      }
   }

   @Unique
   private void sable$applyPropertySet(Map<Identifier, Object> properties) {
      for (Entry<Identifier, Object> entry : properties.entrySet()) {
         int index = PhysicsBlockPropertyTypes.getPropertyType(entry.getKey()).id();
         this.sable$properties[index] = entry.getValue();
      }
   }

   @Override
   public <T> T sable$getProperty(PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<T> type) {
      return (T)(this.sable$properties != null && this.sable$properties[type.id()] != null ? this.sable$properties[type.id()] : type.defaultValue());
   }
}
