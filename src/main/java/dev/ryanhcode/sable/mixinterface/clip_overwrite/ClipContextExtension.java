package dev.ryanhcode.sable.mixinterface.clip_overwrite;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Predicate;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public interface ClipContextExtension {
   @Nullable
   SubLevel sable$getIgnoredSubLevel();

   @Nullable
   Predicate<SubLevel> sable$getSubLevelIgnoring();

   void sable$setIgnoredSubLevel(@Nullable SubLevel subLevel);

   void sable$setSubLevelIgnoring(@Nullable Predicate<SubLevel> predicate);

   void sable$setIgnoreMainLevel(boolean ignore);

   boolean sable$isIgnoreMainLevel();

   void sable$setDoNotProject(boolean doNotProject);

   boolean sable$doNotProject();

   Block sable$getBlock();

   Fluid sable$getFluid();

   CollisionContext sable$getCollisionContext();
}
