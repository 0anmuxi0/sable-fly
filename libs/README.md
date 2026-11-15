# libs/

本目录存放无法从公共 Maven 仓库获取的本地依赖 jar。

## sable-fabric-26.2-3.0.0.jar（26.2 移植版）

**状态：26.3 构件的 26.2 移植版。已在 MC 26.2 + Fabric Loader 0.19.5 实测双端可启动：**
- **客户端**：主菜单 → 进入存档（`Minecraft* 26.2 - Singleplayer`）→ 世界中稳定运行 ≥3 分钟
- **专用服务器**：`Done (0.625s)! For help, type "help"`，三个维度均完成 `(Sable) Saving sub-levels`
- **mixin 失败 = 0 / prepare 失败 = 0 / 协议错误 = 0 / 无 crash-report**

- 原始文件：`sable-26.3-original.jar.bak`（26.3-3.0.0 原件备份，用于对照）
- 源码：`.dsh-ops/sable-mixin-rebuild/src/`（32 个 Java 文件，全部针对 26.2 编译通过）
- 编译环境：`.dsh-ops/sable-mixin-rebuild/classpath.txt`（离线 classpath）

### 补丁内容
1. `fabric.mod.json` 与全部 fabric 模块版本声明 26.3 → 26.2；
2. 移除嵌套的 fcap 26.3.1（运行期由外部 fcap Fabric 26.2.1 提供）；
3. **39 个被移除的 mixin 中已重写并恢复 30 个**（详见下方清单）；
4. 清理 4 个依赖 `com.mojang.renderpearl`（26.2 不存在）的死代码类。

### 注册规模
| | 26.3 原件 | 26.2 当前 |
|---|---|---|
| `sable.mixins.json` | client 72 + mixins 133 = 205 | client 68 + mixins 128 = **196** |
| `sable-fabric.mixins.json` | 6 | 6 |
| 合计 | 211 | **202** |

全部 202 条注册项**每条都对应 jar 内真实 class**（`MixinRegistryCheck` 实测 `TOTAL_MISSING=0`）。

---

## 降级清单（逐条标注：已恢复 / 仍降级 / 仍缺席）

> 本清单取代之前版本的旧说明。旧版曾声称 InterpolationHandler 属于 26.3 新增特性，该判断**已被实测推翻**
> （26.2 确实有 `InterpolationHandler`，含 position/yRot/xRot/interpolateTo/hasActiveInterpolation/
> setInterpolationLength/interpolate/cancel 全部成员，且 `Entity.getInterpolation()` 是 public），故该误判已删除。
> 真正在 26.2 缺失的只有：`PositionAndRotation` 类、`InterpolationHandler.target()`、
> `Entity.createInterpolationHandler()` —— 这 3 处已用等价调用改写。

### ✅ 已恢复（30 条，功能与原版对齐）

原 39 条被移除项中，以下 30 条已在 26.2 上重写、注册并实证可运行：

| 分组 | mixin |
|---|---|
| 实体贴子关卡 · 插值/骑乘/网络 | `entities_stick_sublevels.LivingEntityMixin`、`entities_stick_sublevels.ClientPacketListenerMixin`、`packet_mixin.ClientboundEntityPositionSyncPacketMixin`、`packet_mixin.ClientboundTeleportEntityPacketMixin`、`player.ServerGamePacketListenerImplMixin` |
| 实体碰撞 | `entity_sublevel_collision.EntityMixin`、`entity_sublevel_collision.ServerGamePacketListenerImplMixin`、`entity_collision.LevelReaderMixin` |
| 实体渲染/朝向 | `entity_rendering.EntityRendererMixin`、`entity_rotations_and_riding.ClientPacketListenerMixin`、`entity_rotations_and_riding.EntityRenderDispatcherMixin`、`entity_swimming.CameraMixin`、`entities_stick_sublevels.EntityHitboxDebugRendererMixin` |
| 射线检测 | `clip_overwrite.BlockGetterMixin`、`entity.arrows_hit_blocks.AbstractArrowMixin` |
| 子关卡渲染 | `sublevel_render.impl.vanilla.LevelRendererMixin`、`sublevel_render.block_entity_render.LevelExtractorMixin`、`sublevel_render.block_entity_render.LevelRendererMixin`、`block_decal_render.LevelRendererMixin` |
| plot 序列化 | `plot.LevelChunkMixin`、`plot.ClientChunkCacheMixin`、`plot.ServerChunkCacheMixin`、`plot.serialization.LevelChunkTicksMixin`、`plot.lighting.ClientPacketListenerMixin` |
| 相机 | `camera.new_camera_types.GameRendererMixin` |
| 招牌交互 | `sign_interaction.SignBlockEntityMixin` |
| 命令 | `command.ExecuteCommandMixin` |
| 粒子 | `particle.ClientLevelMixin`、`particle.ParticleMixin`、`particle.SuspendedParticleMixin` |

### ⚠️ 仍降级（已恢复但有行为差异，5 条）

| # | mixin / 功能 | 26.3 行为 | 26.2 现状 | 依据 |
|---|---|---|---|---|
| D1 | `sublevel_render.impl.vanilla.LevelRendererMixin` · 子关卡地形 | GPU 批处理地形 + OIT 透明排序 | **退回逐方块 `submitMovingBlock` 提交，无 GPU 批处理与 OIT** | 26.2 缺 `com.mojang.renderpearl.*`、`DynamicGpuData`、`client.renderer.oit.*`；`SubLevelBatchedTerrainRenderer` 及其唯一引用方 `ChunkSectionsToRenderMixin` 已从 jar 删除，避免静态初始化即 `NoClassDefFoundError`（实测 renderpearl 残留类 = 0） |
| D2 | `block_decal_render.LevelRendererMixin` · 方块破坏 overlay | `overlay = materialFlag \|\| subLevel != null` 显式传参 | **26.2 的 `submitBreakingBlockModel(PoseStack,List,I)V` 没有 overlay boolean 参数**，overlay 由 `BlockBreakingRenderState`（record：blockPos/blockState/progress）驱动；子关卡内表现的细微差异未实测对照 | 反编译 `LevelRenderer.submitBlockDestroyAnimation` 实测：26.2 破坏动画只有「取 blockState → collectParts → submitBreakingBlockModel(progress)」一条路径，无 boolean |
| D3 | `sign_interaction.SignBlockEntityMixin` · 招牌正/背面判定 | 3D 向量点积 `signNormal.dot(deltaDir) > 0` 判 FRONT/BACK（返回 `SignTextSlot`） | **改用 26.2 原生角度公式** `degreesDifferenceAbs(signYRot, playerYRot) <= 90`（已反编译确认与 26.2 原实现逐行一致）+ 子关卡局部坐标变换。**斜视招牌的边界行为与 26.3 略有差异** | 26.2 无 `SignTextSlot` 类（javap 确认），目标方法为 `isFacingFrontText(Player)Z` |
| D4 | `entity_rendering.EntityRendererMixin` · 剔除插值 | `getDeltaTracker().getGameTimeDeltaPartialTick(true)` | **固定 1.0F**，剔除框在亚 tick精度下有 <1 tick 偏差 | 26.2 `shouldRender(Entity,Frustum,DDD)Z` 无 partialTicks 参数（javap 确认） |
| D5 | `plot.LevelChunkMixin` · plot 区块同步 | `forEachBlockEntityTag` + `clearAllBlockEntities` 双钩子 | **已重新实现**：用 `@WrapOperation` 挂在 26.2 的 `Consumer.accept`（方法体内唯一调用点，实测 ordinal=0）上，以 `BlockEntityTagOutput` 代理记录 wanted map，再在 RETURN 做 prune + resync。功能等价，但**尚未在含子关卡的实机上冒烟** | 26.2 `replaceWithPacketData(FriendlyByteBuf,Map,Consumer<BlockEntityTagOutput>)` 签名不同；早期 `Consumer<Object[]>` 桥接写法错误（26.2 传的是 `BlockEntityTagOutput` 而非 `Object[]`）已删除 |

### ❌ 仍缺席（9 条，未注册）

这些 mixin 未在 26.2 重写，对应功能不可用：

| # | mixin | 缺席原因 |
|---|---|---|
| M1 | `options.OptionsScreenMixin` | 26.2 `createOnlineButton()` 为 private 且返回 `LayoutElement`；属纯 UI 入口，优先级最低 |
| M2 | `toast.IntegratedServerMixin` | 纯接口实现（`SableToastableServer`），仅影响子关卡加载/保存失败的 toast 提示 |
| M3 | `block_placement.BlockPlaceContextMixin` | `method="*"` 的 `@Redirect` 需实测确认 26.2 `BlockPlaceContext` 内存在 `Direction.getFacingAxis/orderedByNearest` 调用点，未完成实测 |
| M4 | `command.DataCommandsMixin` | **`/data` 子关卡目标不可用**。Sable 的 `SubLevelDataAccessor.PROVIDER` 声明类型为 `net.minecraft.server.commands.ArgProvider$Factory`，该符号在 26.2 完全不存在（javap 确认 class not found），Sable 该类在 26.2 无法链接。改写为 26.2 的 `Function<String, DataCommands.DataProvider>` 形态需要接入 Sable 子关卡 NBT 面且无运行期验证，在「优先不引入新崩溃」的修复轮中不做。**`/data get|merge|modify` 对实体/方块实体/存储正常，仅子关卡目标不可用** |
| M5 | `entity.entities_in_blocks.EntityMixin` | 未重写 |
| M6 | `entity.entity_pathfinding.FlyNodeEvaluatorMixin` | `@Redirect` 的 owner 在 26.2 需从 `Mob` 改为 `Entity`（方法声明在父类），未完成实测 |
| M7 | `explosion.ExplosionMixin` | 未重写；`ServerExplosion` 的 shadow 字段名未逐条核对 |
| M8 | `portal.EntityMixin` | 未重写 |
| M9 | `respawn_point.sleeping.LivingEntityMixin` | 26.2 `setPosToBed` 为 `private void`（26.3 为 boolean），需改 `@Overwrite` 形态；仅影响睡床位置在子关卡上的投影 |

### 🗑️ 已删除（2 条 + 2 个附属类，因 26.2 缺 `renderpearl`）

| 类 | 原因 |
|---|---|
| `sublevel_render.impl.vanilla.ChunkSectionsToRenderMixin` | 注入目标 `renderGroup(...com.mojang.renderpearl.api.commands.RenderPass, ...GpuSampler, ...GpuTextureView)Z` 在 26.2 不存在 |
| `sublevel_render.impl.vanilla.ResortTransparencyTaskMixin` | 同上，OIT 相关 |
| `dev.ryanhcode.sable.sublevel.render.SubLevelBatchedTerrainRenderer` | 引用 6 个 26.2 不存在的 `renderpearl`/`DynamicGpuData`/`oit` 类，静态初始化即 `NoClassDefFoundError`；仅被上面那条已删 mixin 引用（`RefFinder` 实测） |
| （其内部类一并删除） | — |

### 其它非 mixin 的已知数据层告警（不影响运行）

运行期日志有约 105 条 ERROR，**全部为数据/账号问题，无一条 mixin 或崩溃**：
- `sable:retain_in_sub_level` 引用 `minecraft:cushion`（26.2 已移除该方块）→ 该 tag 加载失败
- `simulated` 若干 advancement / nameplate recipe JSON 在 26.2 解析失败
- Realms / user-properties 401（开发账号 JWT）

---

## ForgeConfigAPIPort-v26.2.1-mc26.2.x-Fabric.jar

Modrinth maven 坐标 `forge-config-api-port:26.2.1` 返回的是 NeoForge 变体（多加载器版本的 maven 缺陷），
fabric 运行期需要本 Fabric 变体（含 `fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry`）。
项目构建已改为 files() 引用本文件。

## create-fly

Create Fly 依赖由 JitPack 解析（`com.github.LyXith:create-fly-fly:pack-26.2-latest`，
实测构件版本 6.0.9-1），不再需要本地 jar。

## 4in1 交付包

`build/libs/create-aeronautics-4in1-fabric-26.2-1.3.0.jar`（31.1 MB）= Sable + Simulated + Offroad + Aeronautics
四合一（nested jars）；运行期还需要：Fabric API 0.161.0+26.2 与 ForgeConfigAPIPort Fabric 26.2.1（本目录有）。
