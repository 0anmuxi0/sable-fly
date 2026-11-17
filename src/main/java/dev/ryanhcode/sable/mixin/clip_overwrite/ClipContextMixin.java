package dev.ryanhcode.sable.mixin.clip_overwrite;

import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Predicate;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ClipContext.class})
public class ClipContextMixin implements ClipContextExtension {
   @Unique
   @org.jetbrains.annotations.Nullable
   private SubLevel sable$ignoredSubLevel = null;
   @Unique
   private boolean sable$ignoreMainLevel = false;
   @Unique
   private boolean sable$doNotProject = false;
   @Unique
   @org.jetbrains.annotations.Nullable
   private Predicate<SubLevel> sable$subLevelIgnoring = null;

   @org.jetbrains.annotations.Nullable
   @Override
   public SubLevel sable$getIgnoredSubLevel() {
      return this.sable$ignoredSubLevel;
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public Predicate<SubLevel> sable$getSubLevelIgnoring() {
      return this.sable$subLevelIgnoring;
   }

   @Override
   public void sable$setIgnoredSubLevel(@org.jetbrains.annotations.Nullable SubLevel ignoredSubLevel) {
      this.sable$ignoredSubLevel = ignoredSubLevel;
   }

   @Override
   public void sable$setSubLevelIgnoring(@org.jetbrains.annotations.Nullable Predicate<SubLevel> subLevelIgnoring) {
      this.sable$subLevelIgnoring = subLevelIgnoring;
   }

   @Override
   public void sable$setIgnoreMainLevel(boolean ignoreWorld) {
      this.sable$ignoreMainLevel = ignoreWorld;
   }

   @Override
   public boolean sable$isIgnoreMainLevel() {
      return this.sable$ignoreMainLevel;
   }

   @Override
   public void sable$setDoNotProject(boolean doNotProject) {
      this.sable$doNotProject = doNotProject;
   }

   @Override
   public boolean sable$doNotProject() {
      return this.sable$doNotProject;
   }
}
