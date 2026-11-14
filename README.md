# Sable 26.2 移植代码

本目录是 Sable（物理子关卡模组）适配 Minecraft 26.2 的**源码层**。
Sable 官方只发布 1.21.1 与 26.3 版本，26.2 无官方发行；这里的源码是在官方
26.2 二进制 jar 基础上重建/修补的部分，编译后注入 jar 生效。

## 目录结构

```
sable/
├── src/dev/ryanhcode/sable/     58 个 .java，按官方包结构组织
│   ├── mixin/                   Mixin 注入类（主体）
│   ├── mixinhelpers/            Mixin 辅助逻辑（避免注入类过重）
│   ├── mixinterface/            接口注入（@Mixin implements 目标）
│   ├── sublevel/render/         子关卡渲染数据与调度实现
│   └── command/                 命令数据访问
├── sable.mixins.json            主 mixin 配置（required: true）
└── sable-fabric.mixins.json     Fabric 侧 mixin 配置
```

## mixin 覆盖范围（按目录分组）

| 分组 | 作用 |
|---|---|
| `entity/entities_stick_sublevels` | 实体吸附子关卡、位置同步包改写 |
| `entity/entity_rotations_and_riding` | 实体随子关卡旋转、骑乘 |
| `entity/entity_sublevel_collision` | 子关卡碰撞 |
| `entity/entity_swimming` / `entity_pathfinding` | 流体交互、寻路 |
| `plot` / `plot.lighting` / `plot.serialization` | 子关卡区块、光照、存档序列化 |
| `sublevel_render/impl/vanilla` | 26.2 提取式渲染管线适配 |
| `dynamic_directional_shading` | 方向性 shading（AO 缓存 ThreadLocal） |
| `particle` | 粒子光照随子关卡缩放 |
| `compatibility/create` | Create 兼容（ValueBox、Outline、GhostBlock 等） |
| `conduit` / `explosion` / `portal` / `punching` 等 | 其余单点行为适配 |

## 26.2 与 1.21.1 的关键差异（已在源码注释中记录）

- 渲染改为**提取式**（extract → submit），旧的即时渲染接口不再适用；
  `VanillaSubLevelRenderDispatcher.renderBlockEntities` 为空实现属设计使然，
  方块实体由 `LevelExtractorMixin` 提取、`LevelRendererMixin` 在 `submitBlockEntities` 中变换提交。
- 深度缓冲为 reversed-Z（`CompareOp.GREATER_THAN_OR_EQUAL`）。
- `Particle.getLightCoords(F)I` 从 `TerrainParticle` 上移到 `Particle` 基类。
- `SectionCompiler.compile` 为 4 参数签名。
- `BlockModelLighter.CACHE`（AO 缓存 ThreadLocal）位于 `BlockModelLighter`，
  **不在** `SectionCompiler` 中。
- 26.2 无 `RenderTypes.linesDepthBias()`，高对比轮廓退化为 `RenderTypes.lines()`。

## 本轮修复：TerrainParticleMixin

**症状**：每次启动都报

```
sable.mixins.json:particle.TerrainParticleMixin from mod sable:
Super class 'net.minecraft.client.particle.Particle' of particle.TerrainParticleMixin
was not found in the hierarchy of target class 'net/minecraft/client/particle/Particle'
```

因 `sable.mixins.json` 为 `required: true`，该 mixin 完全未生效，
方块破坏粒子的子关卡光照缩放功能是空的。

**根因**：mixin 同时满足 `@Mixin(Particle.class)` 与 `extends Particle`。
当 mixin 声明的父类恰好是目标类本身时，Mixin 的父类层级校验必然失败
（类不在自身的父类链中）。

**修法**：不再 `extends Particle`，改用 `@Shadow` 声明所需的 `x/y/z` 字段
（26.2 `Particle` 中三者均为 `protected double`），注入点与方法签名保持不变。

**验证**：重启游戏后该错误消失，`FabricLoader/Mixin` 相关 ERROR 计数为 0，
玩家正常登录进入存档。

## 构建方式

编译与注入脚本位于 `../tools/sable-mixin-tooling/`：

- `compile-migrate.ps1` — 组装 classpath 并编译指定 mixin 子集
- `Repack.java` — 以基线 jar 为底，覆盖编译产物并写回两份 mixin 配置，附带自检
- `FullMixinAudit.java` / `MixinRegistryCheck.java` / `HandlerSigCheck.java` /
  `InjectSigCheck.java` — mixin 注册与注入签名一致性审计

单文件修补可直接编译后用 `jar uf` 注入，比重建整包更精准。
classpath 依赖：26.2 反混淆 Minecraft jar、Sable jar、Create-Fly jar、Fabric API、Mixin、JOML。
