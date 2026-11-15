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
   private void sable$gatherSubLevelFluids(Entity var1, boolean var2, CallbackInfo var3) {
      SubLevelFluidHelper.gather(var1, var2, this.sable$subLevelFluids, this.sable$subLevelCurrents);
   }

   @ModifyReturnValue(
      method = {"getFluidHeight(Lnet/minecraft/tags/TagKey;)D"},
      at = {@At("RETURN")}
   )
   private double sable$subLevelFluidHeight(double var1, TagKey<Fluid> var3) {
      double var4 = var1;

      for (Entry var7 : this.sable$subLevelFluids.entrySet()) {
         if (((Holder)var7.getKey()).is(var3)) {
            var4 = Math.max(var4, ((SubLevelFluidHelper.Found)var7.getValue()).height);
         }
      }

      return var4;
   }

   @ModifyReturnValue(
      method = {"isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"},
      at = {@At("RETURN")}
   )
   private boolean sable$subLevelEyeInFluid(boolean var1, TagKey<Fluid> var2) {
      if (var1) {
         return true;
      } else {
         for (Entry var4 : this.sable$subLevelFluids.entrySet()) {
            if (((Holder)var4.getKey()).is(var2) && ((SubLevelFluidHelper.Found)var4.getValue()).eyesInside) {
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
   private void sable$applySubLevelCurrent(TagKey<Fluid> var1, Entity var2, double var3, CallbackInfo var5) {
      SubLevelFluidHelper.Found var6 = this.sable$subLevelCurrents.get(var1);
      if (var6 != null) {
         SubLevelFluidHelper.applyCurrent(var2, var6, var3);
      }
   }
}
