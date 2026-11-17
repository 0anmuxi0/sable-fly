package dev.ryanhcode.sable.mixin.compatibility.create;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复子关卡上 Create 动态结构（Contraption）渲染<b>全黑 / 跨区块变黑</b>。
 *
 * <h2>完整光照链路（逐字节码确认）</h2>
 * <pre>
 * ContraptionVisual.setupStructure()
 *   └─ new BlockModelBuilder(new ContraptionVisual$1(renderLevel, blocks), positions).build()
 *        └─ ModelBuilderImpl.buildBlockModelBuilder
 *             └─ BakedModelBufferer            // 烘焙方块模型到顶点
 *                  └─ BlockModelLighter.getLightCoords(state, getter, pos)
 *                       └─ Cache.getLightCoords
 *                            └─ LevelRenderer.getLightCoords(BrightnessGetter, getter, state, pos)
 *                                 └─ BrightnessGetter.DEFAULT.packedBrightness(getter, pos)
 *                                      └─ BlockAndLightGetter.getBrightness(SKY,  pos)
 *                                         BlockAndLightGetter.getBrightness(BLOCK, pos)
 * </pre>
 * 光照在<b>烘焙顶点时</b>就被写死，之后再改 section 光照不会有效果。
 *
 * <h2>为什么黑</h2>
 * 子关卡上的 contraption，方块坐标是 <b>plot 虚拟坐标</b>（约 ±20.48M）。
 * {@code getBrightness} 经 {@code getLightEngine()}（即
 * {@code VirtualRenderWorld.lightEngine}）采样，而那里在 plot 坐标处没有任何光照数据
 * → 恒为 0 → 烘焙进顶点 → 全黑。结构移动进入新区块时，新区域的方块同样没有数据，
 * 表现为「只有当前区块有光，移出去就黑」。
 *
 * <h2>修复</h2>
 * 在 {@link BlockModelLighter#getLightCoords} 上做重定向：
 * 若 {@code pos} 落在某个子关卡内，用该子关卡的 {@code renderPose}
 * 把 pos 投影到<b>真实世界坐标</b>，再从真实的 {@code ClientLevel}
 * 采样光照后返回。这样烘焙出的顶点带的就是结构实际所在位置的真实光照。
 *
 * <p>主世界 contraption 完全不受影响（位置不在子关卡内时直接走原逻辑）。
 */
@Mixin(value = BlockModelLighter.class, remap = false)
public abstract class ContraptionLightSamplingMixin {

    private static final AtomicLong SABLE_LOG = new AtomicLong();

    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void sable$sampleWorldLight(BlockState state, BlockAndTintGetter getter,
                                        BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (!(Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel subLevel)) {
            return;
        }
        Pose3dc pose = subLevel.renderPose();
        Vector3d sample = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vector3d projected = new Vector3d();
        pose.transformPosition(sample, projected);
        BlockPos worldPos = BlockPos.containing(projected.x, projected.y, projected.z);

        // 从真实世界采样光照（与 BrightnessGetter.DEFAULT 的逻辑一致）
        int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, worldPos);
        int block = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, worldPos);
        int packed = net.minecraft.util.LightCoordsUtil.pack(block, sky);
        // 方块自身发光
        int emission = state.getLightEmission();
        if (LightCoordsUtilBlock(packed) < emission) {
            packed = net.minecraft.util.LightCoordsUtil.withBlock(packed, emission);
        }
        long n = SABLE_LOG.incrementAndGet();
        if (n <= 5 || n % 3000 == 0) {
            Sable.LOGGER.info("[light3-diag] plotPos={} -> worldPos={} sky={} block={} packed={}",
                    pos, worldPos, sky, block, packed);
        }
        cir.setReturnValue(packed);
    }

    private static int LightCoordsUtilBlock(int packed) {
        return net.minecraft.util.LightCoordsUtil.block(packed);
    }
}
