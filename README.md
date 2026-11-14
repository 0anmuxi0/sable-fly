# Sable Fly

Patch sources for running Sable on Minecraft 26.2 (official builds target 1.21.1 / 26.3 only). This repository lists only the differences from the official Sable builds.

## Rendering

- Sub-level rendering is rewritten for 26.2's extraction pipeline: the immediate-mode `renderBlockEntities` dispatcher is intentionally empty; block entities are extracted by `LevelExtractorMixin` and transformed during `LevelRenderer.submitBlockEntities`.
- The batched terrain path present in 26.3 is disabled; per-block submission is kept pending a GPU-visibility root cause.
- 26.2 has no `RenderTypes.linesDepthBias()`; high-contrast block outlines fall back to `RenderTypes.lines()`.

## Particles

- `getLightCoords(F)I` moved from `TerrainParticle` to the `Particle` base class, so the injection target moves with it.
- `TerrainParticleMixin` no longer `extends Particle` — when a mixin's superclass equals its target, mixin hierarchy validation rejects it ("Super class 'Particle' was not found in the hierarchy of target class 'Particle'"). Position fields are read through `@Shadow` instead.

## Fluids

- `FluidState.getHeightForCamera(...)` was removed in 26.2; `SubLevelFluidHelper` uses `getHeight(...)` for both the collision top and the eye-height check.

## Physics

- `RapierPhysicsPipeline.getLinearVelocity/getAngularVelocity` return a zero snapshot for removed bodies instead of throwing. Serializing unloaded sub-levels during server shutdown used to abort world saving with `Body has been removed`.

## Dynamic directional shading

- `SectionCompiler.compile` uses the 26.2 four-parameter signature `(SectionPos, RenderSectionRegion, VertexSorting, SectionBufferBuilderPack)`.
- The AO cache `ThreadLocal` lives in `BlockModelLighter`, not `SectionCompiler`; the cache mixin targets the correct class.

## Entities

- `Entity.setRemoved` is final on 26.2, so deregistration hooks are injected into `remove(RemovalReason)` instead.
- The 1.21.1 passenger-pinning overwrite in `saveWithoutId` is dropped — 26.2 already pins passengers to the anchor.
- The interpolation mixin reads 26.2's `InterpolationHandler` accessors (`hasActiveInterpolation()/yRot()/xRot()`); 26.3's `PositionAndRotation` and `createInterpolationHandler()` invoker do not exist.

## Plot persistence

- Chunk serialization uses 26.2 `Strategy`-based paletted-container codecs and `getCompoundOrEmpty` NBT accessors.
- Sub-level compilation skips tasks while `SectionRenderDispatcher` still has a null `SectionCompiler` (26.2 injects it after model load), instead of crashing on first use.
