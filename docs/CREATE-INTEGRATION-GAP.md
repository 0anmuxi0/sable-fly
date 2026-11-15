# Sable-Fly 26.x Fabric —— 缺失的 Create 集成实现清单

> 调研日期：2026-11-16
> 适用分支：`mc26.1.2` / `mc26.2` / `mc26.3`
> 调研依据：`sable-fabric-1.21.1-2.0.6-reference.jar`（1.21.1 可用基线）、
> `Create-Fly-Aeronautics`（Aeronautics 1.3.0 / Simulated / Offroad，目标 MC 26.2）
> 全部结论均来自字节码 (`javap`) 与源码比对，非推测。

---

## 0. 一句话总结

**Sable 的 Fabric 版本从来就没有 Create 集成。** 1.21.1 Fabric 版 `sable-fabric-1.21.1-2.0.6`
的 `compatibility.create` mixin 数量是 **0**。所有真正干活的 Create 兼容都在
**Aeronautics / Simulated / Offroad** 三个配套模组里。

26.x 移植 Sable 时新写了 10 条 Create 兼容 mixin，但**全部是描边/渲染类**
（AABB 描边、BlockCluster 描边、Line/Item 描边、GhostBlock、ValueBox、SuperGlue、
ChainConveyor），**一条物理/逻辑集成都没有**。

结果：子关卡上的 Create 内容大量失效——Contraption 不渲染无碰撞、14 类方块实体
逻辑不通、飞轮走的是降级路径。

---

## 1. 现状核对表

### 1.1 各版本 `compatibility.create` mixin 数量

| 包 | 数量 | 说明 |
|---|---|---|
| `sable-fabric-1.21.1-2.0.6` | **0** | 1.21.1 Fabric 基线，完全没有 |
| `sable-fabric-26.2-3.0.0` | **10** | 26.x 移植时新写，全是渲染/描边 |
| `Sable-Fly` mc26.1.2（当前） | 15 | 上面 10 条 + 本次会话新增 5 条 |

### 1.2 当前 26.2 已有的 10 条（都是渲染/描边，与物理无关）

```
compatibility.create.AABBOutlineMixin
compatibility.create.BlockClusterOutlineMixin
compatibility.create.ChainConveyorRendererMixin
compatibility.create.GhostBlockRendererMixin
compatibility.create.ItemOutlineMixin
compatibility.create.LineOutlineMixin
compatibility.create.SuperGlueSelectionHandlerMixin
compatibility.create.TransparentGhostBlockRendererMixin
compatibility.create.ValueBoxMixin
compatibility.create.AllHandleMixin
```

### 1.3 本次会话新增的 5 条（mc26.1.2）

```
compatibility.create.VisualizationHelperMixin              # 三处 getVisualizer 拦截
compatibility.create.FlwApiLinkImplMixin                    # supportsVisualization 拦截
compatibility.create.BlockEntityRenderDispatcherCreateMixin # 线程局部 BE 上下文
compatibility.create.PlacementClientMixin                   # 放置指示器投影（子代理提交）
compatibility.create.BucketItemMixin                        # ⚠️ 无效代码，建议删除
```

> ⚠️ `BucketItemMixin` 已 review 判定为**无效**：它把 `level` 换成
> `subLevel.getLevel()`，但 `SubLevelContainer.getLevel()` 返回的就是主世界那个
> `Level` 对象（`SubLevel.level` 字段即构造时传入的主世界 level），换参数等于没换。

---

## 2. 缺失清单（共 ~24 条 mixin）

### 2.1 【P0】Contraption 物理桥接 —— 1 条

| 项 | 内容 |
|---|---|
| **文件** | `AbstractContraptionEntityMixin` |
| **目标类** | `com.zurrtum.create.content.contraptions.AbstractContraptionEntity` |
| **原归属** | **Simulated**（不是 Sable 自己） |
| **源码位置** | `Create-Fly-Aeronautics\simulated\src\main\java\dev\simulated_team\simulated\mixin\compat\contraptions\AbstractContraptionEntityMixin.java` |
| **规模** | 294 行 / 14.8 KB |
| **注册位置** | `simulated.mixins.json` |

**作用**：让 Create 的 Contraption 实体实现 Sable 的 `KinematicContraption` 接口，
在实体首次 tick 时把质量/包围盒/浮力簇/lift 数据构建好并注册到宿主 plot
（`addContraption`）与物理管线（`pipeline.add`），实体移除时注销。

**不做的后果**（原注释原文）：

> *"the contraption entity is not registered as a KinematicContraption, so the host
> sub-level's split system tears the bearing-mounted blocks off as unsupported —
> the bearing head simply drops and cannot rotate."*

即：轴承组装后，宿主子关卡的分裂系统会把轴承上的方块当作无支撑而撕裂 → 轴承头掉落、
不能旋转、**没有碰撞**、**不渲染**。

**实现要点（从源码提取）**：

```java
@Mixin(value = AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityMixin
        extends Entity
        implements KinematicContraption {

    @Shadow(remap = false) protected Contraption contraption;
    @Shadow(remap = false) public abstract Vec3 applyRotation(Vec3 localPos, float partialTicks);
    @Shadow(remap = false) public abstract Vec3 getPrevAnchorVec();
    @Shadow(remap = false) public abstract Vec3 getAnchorVec();

    @Unique private final Vector3d simulated$cachedGlobalPosition = new Vector3d();
    @Unique private final Object2ObjectMap<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext>
            simulated$liftProviderContexts = new Object2ObjectOpenHashMap<>();
    @Unique private final FloatingClusterContainer simulated$floatingClusterContainer = new FloatingClusterContainer();
    @Unique private BoundingBox3i simulated$localBounds;
    @Unique private MassTracker simulated$massTracker;
    @Unique private boolean simulated$initialized = false;
}
```

| 注入点 | 类型 | 用途 |
|---|---|---|
| `moveCollidedEntitiesOnDisassembly` | `@Redirect` | `applyRotation` → 子关卡位姿变换 |
| `moveCollidedEntitiesOnDisassembly` | `@WrapOperation` | `Entity.setPos` → 世界坐标修正 |
| `tick` (INVOKE) | `@Inject` | **首次 tick 时初始化**：建 quality/bounds/lift → addToPlot + addToPipeline |
| `remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V` (HEAD) | `@Inject` | 注销 plot + pipeline |

**26.2 适配要点（原文注释）**：

- `Entity.setRemoved` 是 `final`（1.21.1 可覆写）→ 改注入 `onRemoval`（`remove()` 内部调用，时序等价）
- `saveWithoutId` 的 passenger 逻辑 26.2 官方已内建（字节码确认）→ 1.21.1 的 `@Overwrite` 不需要
- 内部 `tick()` 用 `this.collidingEntities.entrySet()`，上游注入点同为 `Map.entrySet` 调用，可匹配

**→ 26.1.2 移植需额外核对**：`collidingEntities` 字段名、`tick()` 内 `entrySet()` 调用位置、
`Entity.setRemoved` 是否 final、`Entity$RemovalReason` 是否同名。

---

### 2.2 【P0】Flywheel 子关卡嵌入式渲染 —— 3 条 ⭐ 飞轮的正解

| 文件 | 目标类 | 作用 |
|---|---|---|
| `FlywheelCompatBlockEntityStorageMixin` | `com.zurrtum.create.client.flywheel.impl.visualization.storage.BlockEntityStorage` | **核心**：拦截 `createRaw` 里的 `visualizer.createVisual(context, be, pt)`，子关卡 BE 的 `VisualizationContext` 换成该子关卡的 `VisualEmbedding`（独立 `renderOrigin`） |
| `FlywheelCompatRenderDispatcherMixin` | `...VisualizationManagerImpl$RenderDispatcherImpl` | 每帧 `onStartLevelRender` HEAD 驱动所有子关卡 embedding 的变换更新 |
| `FlywheelCompatAbstractBlockEntityVisualMixin` | `...lib.visual.AbstractBlockEntityVisual` | 光照适配：子关卡 BE 采样光照时，把 plot 虚拟坐标（±20.48M）经渲染位姿变换到真实世界坐标，再走父世界光照引擎；并施加子关卡天空光缩放 |

**这是飞轮问题的正解，取代当前的降级方案。**

当前 Sable-Fly 的做法（`VisualizationHelperMixin` + `FlwApiLinkImplMixin`）是让
`getVisualizer` 返回 `null`，把飞轮**踢回 vanilla `BlockEntityRenderer`**。
有效（用户已实测确认可见），但代价：
- 失去 Flywheel 的 GPU 实例化批量渲染
- 失去 OIT 透明排序
- 可能漏掉部分动画/光照细节

Aeronautics 的做法的让 Flywheel **继续工作**，只是给它一个"子关卡专属坐标系"。

**源码注释要点**：

> *"Intercepts `BlockEntityVisualizer.createVisual` inside Flywheel's
> `BlockEntityStorage.createRaw`: when the BE belongs to a sublevel, the
> visualization context is swapped for that sublevel's embedding (independent renderOrigin)."*

> *"CPU-side lighting adapter: when a sublevel BE's Flywheel visual samples light, the
> plot-virtual position is transformed by the sublevel render pose into real world
> coordinates first, and the sublevel sky-light scale is applied via scaleLightColor
> (mirroring official Sable 1.21.1)."*

> *"26.2 create-fly-fly `AbstractBlockEntityVisual` (verified by decompilation): 3 of the
> 6 `relight` overloads call `LightCoordsUtil.getLightCoords(BlockAndLightGetter, BlockPos)`
> with the BE's plot-virtual position (near ±20.48M). The parent-world light engine has no data..."*

---

### 2.3 【P1】飞轮方块实体 —— 1 条

| 项 | 内容 |
|---|---|
| 文件 | `FlywheelBlockEntityMixin` |
| 目标类 | `com.zurrtum.create.content.kinetics.flywheel.FlywheelBlockEntity` |
| 目录 | `aeronautics\...\mixin\flywheel_block_entity\` |

---

### 2.4 【P1】Create 方块实体逻辑 —— 14 条

全部位于 `aeronautics\src\main\java\dev\eriksonn\aeronautics\mixin\create_sublevel\`。

| # | mixin | 目标类 |
|---|---|---|
| 1 | `AirCurrentMixin` | `AirCurrent` |
| 2 | `BeltBlockEntityMixin` | `BeltBlockEntity` |
| 3 | `BeltMovementHandlerMixin` | `BeltMovementHandler` |
| 4 | `ChuteBlockEntityMixin` | `ChuteBlockEntity` |
| 5 | `CrushingWheelControllerBlockEntityMixin` | `CrushingWheelControllerBlockEntity` |
| 6 | `DeployerHandlerMixin` | `DeployerHandler` |
| 7 | `EjectorBlockEntityMixin` | `EjectorBlockEntity` |
| 8 | `EjectorItemEntityMixin` | `EjectorItemEntity` |
| 9 | `LecternControllerBlockEntityMixin` | `LecternControllerBlockEntity` |
| 10 | `LocalPlayerBeltMixin` | `LocalPlayer`（玩家站传送带） |
| 11 | `NozzleBlockEntityMixin` | `NozzleBlockEntity` |
| 12 | `StockKeeperCategoryMenuMixin` | `StockKeeperCategoryMenu` |
| 13 | `ToolboxHandlerMixin` | `ToolboxHandler` |
| 14 | `TransportedEntityInfoAccessor` | `TransportedEntityInfo`（`@Accessor`） |

**不做的后果**：子关卡上的传送带、溜槽、粉碎轮、机械手、发射器、讲台控制器、
喷嘴、工具箱、库存管理器**全部不工作**。

---

### 2.5 【P2】螺旋桨与碰撞 —— 3 条

| 文件 | 目标类 | 目录 |
|---|---|---|
| `BearingContraptionMixin` | `BearingContraption` | `mixin\propeller_bearing\` |
| `PropellerBearingContraptionEntityMixin` | `PropellerBearingContraptionEntity` | `mixin\propeller_bearing\` |
| `ContraptionColliderMixin` | `ContraptionCollider` | `mixin\propeller_collision\` |

---

### 2.6 【P2】Sable 事件钩子 —— 1 条

| 文件 | 目标类 |
|---|---|
| `SableCommonEventsMixin` | `dev.ryanhcode.sable.SableCommonEvents` |

目录：`aeronautics\...\mixin\sable_hooks\`

---

### 2.7 【P3】Ponder（思索）—— 4 条

| 文件 | 位置 |
|---|---|
| `DisplayWorldSectionInstructionMixin` | `mixin\ponder\` |
| `TickingInstructionAccessor` | `mixin\ponder\` |
| `TickingInstructionMixin` | `mixin\ponder\` |
| `WorldSectionElementImplAccessor` | `mixin\ponder\` |

---

### 2.8 【P3】渲染兼容（Sodium / Iris）—— 3 条

| 文件 | 位置 | 条件 |
|---|---|---|
| `WorldRenderingPhaseMixin` | `mixin\render\iris\` | 需 Iris |
| `DefaultMaterialsMixin` | `mixin\render\sodium\` | 需 Sodium |
| `SodiumWorldRendererMixin` | `mixin\render\sodium\` | 需 Sodium |

---

## 3. Sable 侧已就绪的对接点（无需改动）

这些是 Sable 本体**已经写好、等着被调用**的部分——说明原作者规划过完整集成：

### 3.1 `KinematicContraption` 接口（12 个方法）

`src/main/java/dev/ryanhcode/sable/api/sublevel/KinematicContraption.java`

```java
void sable$getLocalBounds(BoundingBox3i dest);
BlockGetter sable$blockGetter();
MassTracker sable$getMassTracker();
Vector3dc sable$getPosition(double partialTick);
Quaterniond sable$getOrientation(double partialTick);
Map<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext> sable$liftProviders();
FloatingClusterContainer sable$getFloatingClusterContainer();
boolean sable$shouldCollide();
boolean sable$isValid();
default Vector3dc sable$getPosition();
default Quaterniond sable$getOrientation();
default Pose3d sable$getLocalPose(Pose3d dest, double partialTick);
```

### 3.2 已有消费者（全部就绪，只等实现者）

| 类 | 用途 |
|---|---|
| `api/physics/PhysicsPipeline` | `add(KinematicContraption)` / `remove(KinematicContraption)` |
| `api/physics/mass/MergedMassTracker` | 合并 contraption 质量与惯量到子关卡 |
| `physics/floating_block/FloatingBlockController` | 浮力簇跟随 |
| `physics/impl/none/StaticPhysicsPipeline` | 静态管线实现 |
| `physics/impl/rapier/RapierPhysicsPipeline` | kinematic body + 碰撞体上传 |
| `physics/impl/rapier/Rapier3D` | native: `createKinematicContraption` 等 4 个方法 |
| `sublevel/ServerSubLevel` | lift provider 力计算循环 |
| `sublevel/plot/ServerLevelPlot` | `addContraption` / `removeContraption` / `getContraptions` |

### 3.3 已有但无人读取的资源

`src/main/resources/data/sable/tags/entity_type/create_contraption.json`

```json
{
  "replace": false,
  "values": [
    { "id": "create:contraption", "required": false },
    { "id": "create:stationary_contraption", "required": false },
    { "id": "create:oriented_contraption", "required": false },
    { "id": "create:gantry_contraption", "required": false },
    { "id": "create:carriage_contraption", "required": false }
  ]
}
```

**全 Sable 源码 grep：没有任何 Java 代码读取这个标签。** 它把 Create 的 5 种
Contraption 实体类型都列好了，说明原作者规划过，但桥接层从未实现。

---

## 4. 已确认的"真 Bug"（本次会话已修，供对照）

| # | 问题 | 位置 | 状态 |
|---|---|---|---|
| B1 | 服务端启动崩溃：5 个引用客户端类的 mixin 被放进通用 `mixins` 列表 | `sable.mixins.json` | ✅ 已修 |
| B2 | sub-level 方块实体完全不渲染：`LevelExtractorMixin` 被掏空成 `@Pseudo` 空壳，提取链路整条死掉 | `sublevel_render/block_entity_render/LevelExtractorMixin.java` | ✅ 已修 |
| B3 | 移动方块背面剔除丢失（结构内部面透出） | `MovingBlockFeatureRendererMixin` | ✅ 已修 |
| B4 | 异步区块编译被改成同步（卡顿） | `VanillaChunkedSubLevelRenderData.java` | ✅ 已修 |
| B5 | Create 动力件（飞轮/锯片等）在 sub-level 不渲染 | `VisualizationHelperMixin` + `FlwApiLinkImplMixin` 等 | ✅ 已修（用户实测确认，但为降级方案，见 2.2） |
| B6 | **走路一顿一顿/抽搐**：`buildSync` 门槛 `distanceSquared < 768`（≈27格），玩家在 27 格内所有脏 section 都在渲染线程同步编译 | `VanillaChunkedSubLevelRenderData.java:177` | ✅ 已修（待实测） |

---

## 5. 建议的实施顺序

### 阶段一：最小可用（4 条 mixin，**优先做这个**）

1. `AbstractContraptionEntityMixin`（Simulated）→ 解决 Contraption 不渲染 + 无碰撞
2. `FlywheelCompatBlockEntityStorageMixin`
3. `FlywheelCompatRenderDispatcherMixin`
4. `FlywheelCompatAbstractBlockEntityVisualMixin`

→ 先在 **mc26.1.2** 上跑通并实测有效，再考虑扩散。
→ 有效后可以把现有的 `VisualizationHelperMixin` / `FlwApiLinkImplMixin` 降级方案撤掉。

### 阶段二：Create 方块实体（14 条）

按使用频率排优先级：传送带（3 条）→ 溜槽 → 机械手 → 发射器 → 粉碎轮 → 其余。

### 阶段三：螺旋桨 + 事件钩子（4 条）

### 阶段四：Ponder + Sodium/Iris（7 条，按需）

### 跨版本

每完成一条，先在 `mc26.1.2` 验证，再移植 `mc26.2` / `mc26.3`。
三个分支的 `Create` 版本不同（26.1.2 用 `create-fly-26.1.2-6.0.9-4`，
26.2/26.3 用 `6.0.9-2`），需逐个核对 API。

---

## 6. 移植时的技术注意事项

### 6.1 mixin 注册

- 引用**客户端类**的 mixin 必须放 `sable.mixins.json` 的 **`client`** 数组，放 `mixins` 会导致服务端启动崩溃（B1 的教训）
- `compatibility.create.*` 前缀由 `SableMixinPlugin` 自动解析为 `modId = "create"`，未装 Create 时不加载
- Create 的类用 `@Pseudo` + `targets = {...}`，`remap = false`（Aeronautics 的写法）

### 6.2 泛型方法无法注入

`KineticBlockEntityRenderer.extractRenderState`、`VisualizationHelper.getVisualizer(T)`
都是泛型方法，**带描述符注入会报 `Scanned 0 target(s)`**。用**裸方法名**，
或用 `@WrapOperation` 拦截方法体内的**具体调用点**（不是方法本身）。

### 6.3 static 方法回调必须 static

`VisualizationHelper.canVisualize` 等是 `static`，回调方法也必须 `static`，
否则报 `non-static callback method ... targets a static method which is not supported`。

### 6.4 返回类型必须精确

`@WrapOperation` 的回调返回类型必须与调用点的**确切类型**一致
（如 `BlockEntityVisualizer<?>` 而非 `Object`），否则
`Found unexpected return type java.lang.Object, expected ...`。

### 6.5 纯工具类不能放 mixin 包

`CreateSupportsVisualizationContext` 这类纯工具类若放在 `mixin/` 包下，
Mixin 框架会试图把它当 mixin 转换 → `Mixin transformation of ... failed`。
必须放到非 mixin 包（如 `dev.ryanhcode.sable.compatibility.create`）。

---

## 7. 参考资源

### 7.1 源码

```
C:\Users\_anmuxi_\Desktop\Create-Fly-Aeronautics\
├── simulated\src\main\java\dev\simulated_team\simulated\mixin\compat\contraptions\
│   └── AbstractContraptionEntityMixin.java            # 294 行，P0
├── aeronautics\src\main\java\dev\eriksonn\aeronautics\mixin\
│   ├── compat\flywheel\                               # 3 条，P0
│   ├── flywheel_block_entity\                          # 1 条，P1
│   ├── create_sublevel\                                # 14 条，P1
│   ├── propeller_bearing\                              # 2 条，P2
│   ├── propeller_collision\                            # 1 条，P2
│   ├── sable_hooks\                                    # 1 条，P2
│   ├── ponder\                                         # 4 条，P3
│   └── render\{iris,sodium}\                           # 3 条，P3
└── offroad\src\main\java\dev\ryanhcode\offroad\mixin\
    └── borehead_collision\BoreheadContraptionEntityMixin.java
```

### 7.2 1.21.1 基线

```
C:\Users\_anmuxi_\Desktop\Create-Fly-Aeronautics\.dsh\baseline-1.21.1\
├── ca-1.21.1-1.3.2.jar          # 1.21.1 可用组合包
├── mixins-1.21.1\               # 全部 mixin 配置
│   ├── aeronautics.mixins.json
│   ├── aeronautics.neoforge.mixins.json
│   ├── offroad.mixins.json
│   ├── simulated.mixins.json
│   └── simulated-neoforge.mixins.json
└── cfr-0.152.jar                # 反编译器
```

### 7.3 文档

```
C:\Users\_anmuxi_\Desktop\Sable-Fly\libs\README.md
```

现有 26.2 移植记录，含"仍缺席（9 条，未注册）"清单——但**那 9 条都不涉及 Create 集成**，
与本文档的 24 条是两批完全不同的缺口。

### 7.4 工具

- JDK 25 javap：`C:\Program Files\Zulu\zulu-25\bin\javap.exe`
- MC 26.1.2 映射 jar：
  `C:\Users\_anmuxi_\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1.2\minecraft-merged-deobf-26.1.2.jar`
- MC 26.2：同目录 `26.2\...`
- Create Fly jar：`C:\Users\_anmuxi_\Desktop\Sable-Fly\libs\create-fly-26.1.2-6.0.9-4.jar`

---

## 8. 结论

**Contraption 在子关卡上不渲染、无碰撞，不是 26.1.2 的移植 bug，而是 Fabric 版 Sable
从未实现的集成。**

- 1.21.1 **NeoForge** 版 Sable 内建了 `AbstractContraptionEntityMixin`（Simulated 注释可证）
- 1.21.1 **Fabric** 版 Sable 没有——必须配合 **Simulated / Aeronautics**
- 26.x 移植时，Aeronautics 那套（24 条）**一条都没搬过来**

最小可用方案是阶段一的 4 条 mixin。


---

## 9. 实施记录 —— 26.1.2 阶段一（2026-11-16 完成）

### 9.1 已落地（提交 bfb4a52，gradlew build 通过）

| 项 | 文件 | 注册位置 |
|---|---|---|
| Contraption 物理桥接 | `mixin/compatibility/create/AbstractContraptionEntityMixin.java` | `mixins`（服务端） |
| Flywheel 嵌入式渲染 ×3 | `mixin/compatibility/create/FlywheelCompat{BlockEntityStorage,RenderDispatcher,AbstractBlockEntityVisual}Mixin.java` | `client` |
| 支持类 ×3 | `compatibility/create/flywheel/{FlywheelCompat,SubLevelEmbedding,FlywheelWhitelist}.java` | 非 mixin 包（6.5 规则） |
| 白名单分流 | `VisualizationHelperMixin` 改写 | `client`（保留） |
| 无效代码 | `BucketItemMixin.java` 删除 | — |

### 9.2 与参考源码的适配差异（26.1.2 实测核对）

| 差异点 | 26.2（Aeronautics 参考） | 26.1.2（本次适配） |
|---|---|---|
| relight 光照采样 | `LightCoordsUtil.getLightCoords(BlockAndLightGetter, BlockPos)` | `LevelRenderer.getLightCoords(BlockAndLightGetter, BlockPos)`（javap 确认，26.1.2 尚未拆出 LightCoordsUtil） |
| MassTracker 有效性判定 | `massTracker.isInvalid()` | `getCenterOfMass() == null`（26.1.2 build 空结构时返回 centerOfMass=null 的 tracker） |
| 白名单初始化时机 | 客户端 init 显式调用（Aeronautics 硬依赖 Create，时序安全） | `FlywheelWhitelist.contains()` 内 lazy init（Sable 不依赖 Create，client init 早于 Create 注册） |
| 日志 | `Simulated.LOGGER` | `Sable.LOGGER`，保留 `[ctrp-diag]` 前缀便于实测排查 |

已逐项核对并确认一致（javap，create-fly-26.1.2-6.0.9-4）：`tick()` 内唯一 `entrySet()` 注入点、
`moveCollidedEntitiesOnDisassembly` 内 `toLocalVector`/`setPos`/`teleportTo` 三调用点、
`remove(Entity$RemovalReason)`、Flywheel `BlockEntityStorage.createRaw` / `VisualizationManagerImpl$RenderDispatcherImpl`
（`this$0` + `onStartLevelRender(RenderContext)`）/`VisualManagerImpl.queueAdd/queueRemove`/`VisualizationContext.createEmbedding`/
`AbstractBlockEntityVisual` 三个带 BlockPos 的 `relight` 重载、`AllBlockEntityTypes` 全部 34 个白名单字段。

### 9.3 关键设计决策：降级方案改"白名单分流"而非整体撤下

直接撤下 `VisualizationHelperMixin` 会使**非白名单动力件直接消失**（字节码链路已验证）：

1. Create 的 `ClientLevelMixin.flywheel$decideNotToRenderEntity`：`supportsVisualization(be.level) && skipVanillaRender(be)` 时 cancel vanilla 渲染——子关卡 BE 的 `getLevel()` 是父世界，判定恒真；
2. Create 动力件的 `skipVanillaRender` 谓词恒 true；
3. 白名单外方块（传送带、普通齿轮、机械手、锯片等）不被 embedding 接管 → vanilla 渲染被取消而 Flywheel 又不渲染 → 消失。

因此三处拦截点改为按 `FlywheelWhitelist` 分流：

| 拦截点 | 白名单内（34 类核心动力件） | 白名单外 | 适配器关闭（`-Dsable.flywheelCompat=false`） |
|---|---|---|---|
| `canVisualize` | 放行 → 进 Flywheel（embedding context） | 拦 null → vanilla BER | 拦 null（旧降级行为） |
| `skipVanillaRender` | 放行 → vanilla 取消（防双渲染） | 拦 null → vanilla 保留 | 拦 null |
| `tryAddBlockEntity` | 一律拦 null（注册走 `syncSubLevelBlockEntities`，不走会产生 ±20.48M 错误坐标的区块编译路径） | 拦 null | 拦 null |

`FlwApiLinkImplMixin` / `BlockEntityRenderDispatcherCreateMixin` / `CreateSupportsVisualizationContext` 保留：
它们修复的是"Create renderer 在 vanilla 回退路径里跳过填模型"（19 个 renderer 的 `extractRenderState` 提前 return），
与 embedding 方案正交，是非白名单方块能正确显示的前提。

### 9.4 待实测清单（mc26.1.2 + create-fly-26.1.2-6.0.9-4）

- [ ] 机械轴承组装后：轴承头**不掉落、能旋转**，组装体在子关卡的**分裂系统不撕裂**
- [ ] Contraption **渲染可见**且跟随子关卡位姿（拆解时实体落到正确的世界坐标）
- [ ] 白名单内动力件（齿轮箱/离合器/泵/钻头/飞轮/仪表等）在子关卡上：**飞轮渲染可见、随结构旋转、光照正确**（不再依赖 vanilla BER）
- [ ] 白名单外动力件（传送带/齿轮/机械手/锯片/鼓风机等）在子关卡上：**仍可见**（vanilla BER 路径，行为与之前一致）
- [ ] 拆装/移动后无残留 ghost 视觉；退出重进世界后 embedding 正常重建
- [ ] 控制台无 mixin 应用错误（`defaultRequire=1` 会在注入失败时直接崩溃报错）
- [ ] 回退开关验证：`-Dsable.flywheelCompat=false` 启动时恢复降级方案行为

### 9.5 阶段一数量核对

`compatibility.create` mixin：15 条（原）− 1（BucketItemMixin 删）+ 4（阶段一新增）= **18 条**。

（26.2 / 26.3 分支待 26.1.2 实测通过后按第 5 节跨版本流程推进。）
