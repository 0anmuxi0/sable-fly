package dev.ryanhcode.sable.compatibility.create.flywheel;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Wrapper around one sublevel's Flywheel embedding.
 *
 * <p>Holds the {@link com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding}
 * created via {@code VisualizationContext.createEmbedding(origin)} (which carries its own
 * renderOrigin) plus the list of BlockEntities whose visuals were created through it.</p>
 *
 * <p>Every frame {@link FlywheelCompat} calls
 * {@link #updateTransforms(Pose3dc, Vec3i)} to write the sublevel render pose into the
 * embedding: translate(position - parentRenderOrigin) * rotate(orientation)
 * * translate(-(rotationPoint - embeddingOrigin)), matching official Sable 1.21.1's
 * updateEmbeddingTransforms.</p>
 */
public final class SubLevelEmbedding {
    private final com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding embedding;
    private final List<BlockEntity> blockEntities = new ArrayList<>();
    private float skyLightScale = 1.0F;

    // Reused objects to avoid per-frame allocation.
    private final Matrix4f poseMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();
    private final Quaternionf orientationStorage = new Quaternionf();
    private final Vector3d offsetStorage = new Vector3d();

    public SubLevelEmbedding(com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding embedding) {
        this.embedding = embedding;
    }

    public com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding embedding() {
        return this.embedding;
    }

    public List<BlockEntity> blockEntities() {
        return this.blockEntities;
    }

    public float skyLightScale() {
        return this.skyLightScale;
    }

    public void setSkyLightScale(float skyLightScale) {
        this.skyLightScale = skyLightScale;
    }

    /**
     * Refreshes the embedding transform for the current frame.
     *
     * @param renderPose   the sublevel's interpolated render pose
     * @param parentOrigin the parent Flywheel engine's renderOrigin (floored camera pos)
     */
    public void updateTransforms(Pose3dc renderPose, Vec3i parentOrigin) {
        Vector3dc position = renderPose.position();
        Vector3dc rotationPoint = renderPose.rotationPoint();

        Matrix4f transformation = this.poseMatrix;
        transformation.identity();
        transformation.setTranslation(
                (float) (position.x() - (double) parentOrigin.getX()),
                (float) (position.y() - (double) parentOrigin.getY()),
                (float) (position.z() - (double) parentOrigin.getZ()));
        transformation.rotate(this.orientationStorage.set(renderPose.orientation()));

        // Shift the rotation point into the embedding's local coordinate system.
        Vec3i localOrigin = this.embedding.renderOrigin();
        Vector3d localOffset = rotationPoint.sub(
                (double) localOrigin.getX(),
                (double) localOrigin.getY(),
                (double) localOrigin.getZ(),
                this.offsetStorage);
        transformation.translate((float) -localOffset.x, (float) -localOffset.y, (float) -localOffset.z);

        Matrix3f normal = transformation.normal(this.normalMatrix);
        this.embedding.transforms(transformation, normal);
    }

    public void delete() {
        this.blockEntities.clear();
        this.embedding.delete();
    }

    /** The embedding origin for a sublevel: its plot's center block. */
    public static Vec3i embeddingOrigin(ClientSubLevel subLevel) {
        return subLevel.getPlot().getCenterBlock();
    }
}
