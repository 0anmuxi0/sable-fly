package dev.ryanhcode.sable.mixin.particle;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block-break particles (TerrainParticle) scale their sampled light color while inside a
 * sub-level, matching the light environment of the physical body they came from.
 *
 * <p>26.2 reworked particle lighting into {@code Particle.getLightCoords(F)I} (a single
 * {@code LightCoordsUtil} call living in the Particle base class); TerrainParticle no longer
 * declares the method, so the injection target moved to Particle. The handler filters on
 * {@code this instanceof TerrainParticle} so other particle types keep vanilla sampling.</p>
 *
 * <p>The mixin deliberately does NOT extend Particle: when a mixin's declared superclass is the
 * target class itself, Mixin's superclass-hierarchy validation rejects it ("Super class 'Particle'
 * was not found in the hierarchy of target class 'Particle'"). Using {@link Shadow} for the
 * position fields instead keeps the injection valid.</p>
 */
@Mixin(Particle.class)
public abstract class TerrainParticleMixin {

    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;

    @Inject(method = "getLightCoords(F)I", at = @At("RETURN"), cancellable = true)
    private void sable$scaleLightCoords(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof TerrainParticle)) {
            return;
        }
        // The particle's own position tracks the sampled block closely enough for sub-level
        // membership (TerrainParticle keeps its private pos in sync with this position).
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(pos);
        if (subLevel != null) {
            cir.setReturnValue(subLevel.scaleLightColor(cir.getReturnValue()));
        }
    }
}
