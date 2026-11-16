package dev.ryanhcode.sable.mixinterface.plot.serialization;

import java.util.Collection;

public interface ChunkAccessExtension {
   void sable$setContainingSubLevels(Collection<String> containingSubLevels);

   Collection<String> sable$getContainingSubLevels();
}
