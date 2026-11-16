package dev.ryanhcode.sable.mixinterface.water_occlusion;

public interface CameraWaterOcclusionExtension {
   void sable$setIgnoreOcclusion(boolean ignoreOcclusion);

   boolean sable$isIgnoreOcclusion();

   boolean sable$isOccluded();
}
