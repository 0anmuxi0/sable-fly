package dev.ryanhcode.sable.mixin.entity.entity_swimming;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming.SubLevelFluidHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityFluidInteraction.class})
public class EntityFluidInteractionMixin {
   @Unique
   private final Map<Holder<Fluid>, SubLevelFluidHelper.Found> sable$subLevelFluids = new HashMap<>();
   @Unique
   private final Map<TagKey<Fluid>, SubLevelFluidHelper.Found> sable$subLevelCurrents = new HashMap<>();

   @Inject(
      method = {"update(Lnet/minecraft/world/entity/Entity;Z)V"},
      at = {@At("HEAD")}
   )
   private void sable$gatherSubLevelFluids(Entity entity, boolean ignoreCurrent, CallbackInfo ci) {
      SubLevelFluidHelper.gather(entity, ignoreCurrent, this.sable$subLevelFluids, this.sable$subLevelCurrents);
   }

   @ModifyReturnValue(
      method = {"getFluidHeight(Lnet/minecraft/tags/TagKey;)D"},
      at = {@At("RETURN")}
   )
   private double sable$subLevelFluidHeight(double original, TagKey<Fluid> tag) {
      double maxHeight = original;

      for (Entry entry : this.sable$subLevelFluids.entrySet()) {
         if (((Holder)entry.getKey()).is(tag)) {
            maxHeight = Math.max(maxHeight, ((SubLevelFluidHelper.Found)entry.getValue()).height);
         }
      }

      return maxHeight;
   }

   @ModifyReturnValue(
      method = {"isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"},
      at = {@At("RETURN")}
   )
   private boolean sable$subLevelEyeInFluid(boolean original, TagKey<Fluid> tag) {
      if (original) {
         return true;
      } else {
         for (Entry entry : this.sable$subLevelFluids.entrySet()) {
            if (((Holder)entry.getKey()).is(tag) && ((SubLevelFluidHelper.Found)entry.getValue()).eyesInside) {
               return true;
            }
         }

         return false;
      }
   }

   @Inject(
      method = {"applyCurrentTo(Lnet/minecraft/tags/TagKey;Lnet/minecraft/world/entity/Entity;D)V"},
      at = {@At("TAIL")}
   )
   private void sable$applySubLevelCurrent(TagKey<Fluid> tag, Entity entity, double strength, CallbackInfo ci) {
      SubLevelFluidHelper.Found found = this.sable$subLevelCurrents.get(tag);
      if (found != null) {
         SubLevelFluidHelper.applyCurrent(entity, found, strength);
      }
   }
}
