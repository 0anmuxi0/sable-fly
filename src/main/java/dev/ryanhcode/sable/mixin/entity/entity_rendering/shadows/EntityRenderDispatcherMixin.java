package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelEntityShadowRenderer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Render shadows on sub-levels.
 *
 * <p>26.x equivalent of the 1.21.1 {@code @Inject(method = "renderShadow", at = @At("TAIL"))}
 * injection: vanilla no longer draws shadows directly from {@link EntityRenderDispatcher};
 * instead {@code submit} hands the extracted shadow pieces to the submit pipeline via
 * {@code SubmitNodeCollector#submitShadow}. We hook exactly that call site and attach the
 * sub-level shadow geometry to the same frame through {@code submitCustomGeometry} with
 * vanilla's shadow render type, so it is drawn in the same translucent pass.</p>
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

   @Unique
   private final Map<EntityRenderState, EntityRenderDispatcherMixin.SableShadowRenderData> sable$shadowRenderData = new IdentityHashMap<>();

   @Inject(
      method = {"extractEntity"},
      at = {@At("RETURN")}
   )
   private <E extends Entity> void sable$captureShadowEntity(E entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
      this.sable$shadowRenderData.put(cir.getReturnValue(), new EntityRenderDispatcherMixin.SableShadowRenderData(entity, partialTicks));
   }

   @Inject(
      method = {"submit"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitShadow(Lcom/mojang/blaze3d/vertex/PoseStack;FLjava/util/List;)V"
      )}
   )
   private <S extends EntityRenderState> void sable$renderShadowsOnSubLevels(
      S renderState, CameraRenderState camera, double x, double y, double z,
      PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci
   ) {
      EntityRenderDispatcherMixin.SableShadowRenderData data = this.sable$shadowRenderData.get(renderState);
      if (data == null) {
         return;
      }

      Entity entity = data.entity();
      float partialTick = data.partialTick();
      float shadowRadius = renderState.shadowRadius;
      if (shadowRadius <= 0.0F) {
         return;
      }

      // 26.x vanilla folds the distance fade into the strength before the old
      // renderShadow(Entity, float, float, ...) got it; replicate the default
      // EntityRenderer shadow strength factor here.
      float strength = (float)((1.0 - renderState.distanceToCameraSq / 256.0) * EntityRenderDispatcherMixin.SableShadowRenderData.DEFAULT_SHADOW_STRENGTH);
      if (strength <= 0.0F) {
         return;
      }

      submitNodeCollector.submitCustomGeometry(
         poseStack,
         RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png")),
         (pose, vertexConsumer) -> SubLevelEntityShadowRenderer.renderEntityShadowOnSubLevels(entity, strength, partialTick, shadowRadius, vertexConsumer, pose)
      );
   }

   @Inject(
      method = {"submit"},
      at = {@At("TAIL")}
   )
   private <S extends EntityRenderState> void sable$clearShadowData(
      S renderState, CameraRenderState camera, double x, double y, double z,
      PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci
   ) {
      this.sable$shadowRenderData.remove(renderState);
   }

   @Unique
   private record SableShadowRenderData(Entity entity, float partialTick) {
      private static final float DEFAULT_SHADOW_STRENGTH = 0.5F;
   }
}
