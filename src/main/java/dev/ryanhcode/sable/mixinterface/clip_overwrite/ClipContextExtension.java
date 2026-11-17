package dev.ryanhcode.sable.mixinterface.clip_overwrite;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public interface ClipContextExtension {
   @org.jetbrains.annotations.Nullable
   SubLevel sable$getIgnoredSubLevel();

   @org.jetbrains.annotations.Nullable
   Predicate<SubLevel> sable$getSubLevelIgnoring();

   void sable$setIgnoredSubLevel(@org.jetbrains.annotations.Nullable SubLevel var1);

   void sable$setSubLevelIgnoring(@org.jetbrains.annotations.Nullable Predicate<SubLevel> var1);

   void sable$setIgnoreMainLevel(boolean var1);

   boolean sable$isIgnoreMainLevel();

   void sable$setDoNotProject(boolean var1);

   boolean sable$doNotProject();
}
