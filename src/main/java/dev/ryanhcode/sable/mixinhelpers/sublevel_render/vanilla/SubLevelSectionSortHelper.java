package dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;

public final class SubLevelSectionSortHelper {
   private SubLevelSectionSortHelper() {
   }

   public static Vec3 plotCamera(SectionPos sectionPos, Vec3 cameraPos) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return cameraPos;
      } else {
         SubLevel subLevel = Sable.HELPER.getContaining(level, sectionPos);
         return subLevel == null ? cameraPos : subLevel.logicalPose().transformPositionInverse(cameraPos);
      }
   }
}
