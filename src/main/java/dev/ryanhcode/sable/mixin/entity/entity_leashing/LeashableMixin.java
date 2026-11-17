package dev.ryanhcode.sable.mixin.entity.entity_leashing;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Leashable.LeashData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({Leashable.class})
public interface LeashableMixin {
   @Overwrite
   default double leashDistanceTo(Entity handlerEntity) {
      ActiveSableCompanion helper = Sable.HELPER;
      Level level = handlerEntity.level();
      Entity leashedEntity = (Entity)(Object)this;
      Vec3 handlerPos = helper.projectOutOfSubLevel(level, handlerEntity.getBoundingBox().getCenter());
      Vec3 leashedPos = helper.projectOutOfSubLevel(level, leashedEntity.getBoundingBox().getCenter());
      return handlerPos.distanceTo(leashedPos);
   }

   @Overwrite
   default boolean checkElasticInteractions(Entity handlerEntity, LeashData leashData) {
      ActiveSableCompanion helper = Sable.HELPER;
      Level level = handlerEntity.level();
      Entity leashedEntity = (Entity)(Object)this;
      Vec3 handlerPos = helper.projectOutOfSubLevel(level, handlerEntity.position());
      Vec3 leashedPos = helper.projectOutOfSubLevel(level, leashedEntity.position());
      double elasticDistance = ((Leashable)this).leashElasticDistance();
      double d = (handlerPos.x - leashedPos.x) / elasticDistance;
      double e = (handlerPos.y - leashedPos.y) / elasticDistance;
      double g = (handlerPos.z - leashedPos.z) / elasticDistance;
      Vec3 impulse = leashedEntity.getDeltaMovement().add(Math.copySign(d * d * 0.4, d), Math.copySign(e * e * 0.4, e), Math.copySign(g * g * 0.4, g));
      SubLevel leashedSubLevel = helper.getContaining(leashedEntity);
      if (leashedSubLevel != null) {
         impulse = leashedSubLevel.logicalPose().transformNormalInverse(impulse);
      }

      leashedEntity.setDeltaMovement(impulse);
      return true;
   }
}
