package dev.ryanhcode.sable.mixin.entity.entity_swimming;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming.SubLevelFluidHelper;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming.SubLevelFluidHelper.Found;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityFluidInteraction.class})
public class EntityFluidInteractionMixin {
   @Unique
   private final Map<Holder<Fluid>, Found> sable$subLevelFluids = new HashMap<>();
   @Unique
   private final Map<TagKey<Fluid>, Found> sable$subLevelCurrents = new HashMap<>();

   // 26.2 changed EntityFluidInteraction.update(Entity, boolean) to return void (26.3 returned boolean).
   // The 26.3 "fluid found" boolean was only used to short-circuit the caller; gathering into our maps at
   // HEAD preserves every observable effect, and the downstream getFluidHeight/isEyeInFluid hooks below
   // still surface the sub-level fluids, so the boolean return value is no longer needed.
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
   private double sable$subLevelFluidHeight(double original, TagKey<Fluid> fluid) {
      double height = original;

      for (Entry<Holder<Fluid>, Found> entry : this.sable$subLevelFluids.entrySet()) {
         if (entry.getKey().is(fluid)) {
            height = Math.max(height, entry.getValue().height);
         }
      }

      return height;
   }

   @ModifyReturnValue(
      method = {"isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"},
      at = {@At("RETURN")}
   )
   private boolean sable$subLevelEyeInFluid(boolean original, TagKey<Fluid> fluid) {
      if (original) {
         return true;
      } else {
         for (Entry<Holder<Fluid>, Found> entry : this.sable$subLevelFluids.entrySet()) {
            if (entry.getKey().is(fluid) && entry.getValue().eyesInside) {
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
   private void sable$applySubLevelCurrent(TagKey<Fluid> fluid, Entity entity, double scale, CallbackInfo ci) {
      Found found = this.sable$subLevelCurrents.get(fluid);
      if (found != null) {
         SubLevelFluidHelper.applyCurrent(entity, found, scale);
      }
   }
}
