# 26.3 移植待处理文件（porting-pending）

本目录存放从原版 Sable NeoForge（sable-neoforge-1.21.1-2.0.6.jar 反编译）移植到 Sable-Fly Fabric 26.3 时，
**注入目标或依赖 API 在 Minecraft 26.3 + Create Fly 26.3（com.zurrtum.create 6.0.9）中已不存在**的文件。

## 处理原则

- 文件内容**一字未改**（保持反编译原貌），仅移出 `src/main/java` 编译路径。
- 每个文件的功能在 26.3 中**无处附着**：注入目标类/方法已被 Create Fly 重构移除，或依赖 NeoForge 专属 API（能力系统、Registrate 注册体系）无 Fabric 对应。
- **未删除任何功能**：当对应目标在 Create Fly 26.3 重新出现或找到 Fabric 等价 API 时，把文件移回原目录并完成 API 适配即可恢复。

## 文件清单与原因

| 文件 | 移出原因 |
| --- | --- |
| mixin/.../contraptions/ContraptionControlsRendererMixin.java | 目标方法 `renderInContraption` 重构为 `extractRenderState/submit` 渲染状态体系；`ContraptionMatrices` 类已移除 |
| mixin/.../hose_pulley/HosePulleyFluidHandlerMixin.java | 目标方法 `drainInternal` 重构为 Fabric Transfer 风格（`onExtract/getStack/setStack`）；NeoForge `IFluidHandler/FluidAction` 无对应 |
| mixin/.../inventory_manipulation/CapManipulationBehaviourBaseMixin.java | NeoForge `BlockCapability` 与 `Level.getCapability` 在 26.3 不存在（改为泛型抽象 `getCapability`）；第二个注入点失效 |
| mixin/.../redstone_contacts/AllBlockEntityTypesMixin.java | 26.3 不用 Registrate（`CreateRegistrate/BlockEntityEntry` 不存在）；且 26.3 无 `RedstoneContactBlockEntity` 类型 |
| mixin/.../redstone_contacts/RedstoneContactBlockMixin.java | 依赖 `RedstoneContactBlockEntity` + Registrate 注册体系（26.3 无对应） |
| mixin/.../render_fixes/SafeBlockEntityRendererMixin.java | 目标类 `SafeBlockEntityRenderer` 在 Create Fly 26.3 不存在 |
| mixin/.../schematics/SchematicRendererMixin.java | 目标方法 `drawLayer` 重构为 `update/render/redraw`；`ShadedBlockSbbBuilder/BlockRenderDispatcher/ModelData` 已移除 |
| mixin/.../stock_ticker/EntityHatsMixin.java | 目标类 `EntityHats` 在 Create Fly 26.3 不存在 |
| mixinhelper/.../redstone_contact/RedstoneContactBlockEntity.java | 26.3 无对应类型（`RedstoneContactBlock` 保留但无 BE）；`SmartBlockEntity` API 已变 |
| mixinhelper/.../redstone_contact/RedstoneContactBlockEntityTypeGetter.java | 依赖 Registrate `BlockEntityEntry`（26.3 不存在） |

## 恢复条件

1. Create Fly 后续版本恢复对应类/方法，或
2. 找到 Fabric 26.3 的等价 API（Transfer API、Fabric 注册体系），完成类型映射后移回 `src/main/java/dev/ryanhcode/sable/` 原目录并在 `sable.mixins.json` 重新注册。

## 原则说明

移植原则：照原版修（原版 Sable NeoForge 反编译为规格），零删除。目标消失时移除注册、保留完整实现于本目录，等待适配。
