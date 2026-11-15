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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityFluidInteraction.class})
public class EntityFluidInteractionMixin {
   @Unique
   private final Map<Holder<Fluid>, SubLevelFluidHelper.Found> sable$subLevelFluids = new HashMap<>();
   @Unique
   private final Map<TagKey<Fluid>, SubLevelFluidHelper.Found> sable$subLevelCurrents = new HashMap<>();

   @Inject(
      method = {"update(Lnet/minecraft/world/entity/Entity;Z)Z"},
      at = {@At("HEAD")}
   )
   private void sable$gatherSubLevelFluids(Entity entity, boolean ignoreCurrent, CallbackInfoReturnable<Boolean> cir) {
      SubLevelFluidHelper.gather(entity, ignoreCurrent, this.sable$subLevelFluids, this.sable$subLevelCurrents);
   }

   @ModifyReturnValue(
      method = {"update(Lnet/minecraft/world/entity/Entity;Z)Z"},
      at = {@At("RETURN")}
   )
   private boolean sable$subLevelFluidFound(boolean original) {
      return original || !this.sable$subLevelFluids.isEmpty();
   }

   @ModifyReturnValue(
      method = {"getFluidHeight(Lnet/minecraft/tags/TagKey;)D"},
      at = {@At("RETURN")}
   )
   private double sable$subLevelFluidHeight(double original, TagKey<Fluid> fluid) {
      double height = original;

      for (Entry<Holder<Fluid>, SubLevelFluidHelper.Found> entry : this.sable$subLevelFluids.entrySet()) {
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
         for (Entry<Holder<Fluid>, SubLevelFluidHelper.Found> entry : this.sable$subLevelFluids.entrySet()) {
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
      SubLevelFluidHelper.Found found = this.sable$subLevelCurrents.get(fluid);
      if (found != null) {
         SubLevelFluidHelper.applyCurrent(entity, found, scale);
      }
   }
}
