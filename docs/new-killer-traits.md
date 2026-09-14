# 新杀手词条：实现与验证说明

## 状态与工作区

实现在 SparkTraits 的 `feat/new-killer-traits` 分支，分支从本地 `main` 的 `778d628` 创建。

原 `feat/traits-gui-movement` 的 tracked/untracked 改动保存在具名 stash：

- 名称：`pre-new-killer-traits-gui-eligibility-2026-09-10`
- 对象：`c212dbdf399a6c072bb35cc5e469275776c2d177`

旧 GUI 改动未应用到本分支。SparkStrength 伴随改动留在原 `feat/killer-team-economy` 工作区；SparkWitch 伴随改动留在原 `main` 工作区，原有其他 WIP 未覆盖。没有修改 SparkAssist 或 SparkFactionAPI 源码。

## 功能

| ID | 名称 | 行为 |
|---|---|---|
| `team_first` | 团队至上 | 个人收入不变，自身计入共享钱包的贡献增加 20%。需要支持该加成的 Strength 团队经济。 |
| `exhilarated` | 亢奋 | 真实击杀后获得 5 秒速度 III；重复击杀刷新，不累计等级。 |
| `close_quarters` | 狭路相逢 | 仅当玩家杀手商店包含精确物品 `wathe:knife` 时可分配；背包持有匕首或商店中其他 `KnifeItem` 不算，原有杀手资格、词条互斥和瞬发职业排除不变。前摇最早第 3 tick 出刀，举刀最多 200 tick，举刀移速 ×2；成功抵挡近战后收刀，双方施加规定 CD/缓慢，攻击者失明。 |
| `last_escape` | 绝处逢生 | 开局隐藏，每局一次，在其他适用保护之后抵挡致命击杀。结束疯魔、减半随身道具剩余 CD（撬锁器/强制近战锁除外）；进入 200 tick 脱险，50% 灰白持续到死亡或本局结束。 |

脱险状态包含隐身、双向实体无碰撞、教授式列车门穿行、无敌，以及攻击/使用/挖掘/库存交易禁用。保留移动和视角，不使用旁观模式或全局 noclip；掉出列车 `wathe:fell_out_of_train` 仍会死亡。

速度 II 的脱险加成由专属临时移动属性实现，效果不低于原版速度 II，且不会削弱已有更强速度或移除其他来源效果；不会额外显示原版速度 II 药水图标。普通隐身语义保留，不扩展为隐藏所有装备/自定义透视。

强制近战锁独立于普通 CD，以玩家/物品类型计时，同种物品换槽不能绕过。自身至少 5 秒，攻击者武器至少 15 秒；更长的普通 CD 不被缩短。球棍/仪礼剑等禁用时显示红叉和红色秒数，普通蓄力/技能 CD 不误显示为强制锁。

## 核心实现位置

- 词条定义：[KillerTraits.java](../src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraits.java)
- 近战、举刀和精确冷却：[combat/](../src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/combat/)
- 脱险状态及生命周期：[escape/](../src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/escape/)
- 公共可选集成接口：[SparkTraitsApi.java](../src/main/java/dev/caecorthus/sparktraits/api/SparkTraitsApi.java)
- 客户端输入、HUD、视觉：[client/killer/](../src/client/java/dev/caecorthus/sparktraits/client/killer/)
- Strength：`SparkTraitsCompat`、`KillerTeamEconomyService/Rules`、静语者/老兵近战入口。
- Witch：`SparkTraitsKillerBridge`、仪礼剑/自定义点击武器、既有保命入口及灰白单次合成。

## 审查后修复

- 匕首普通 CD/持有物无效的攻击包必须被拒绝，不能只跳过反制而继续击杀。
- 静语者替换匕首流程独立验证普通 CD。
- 枪械及恶魔猎手自定义包在脱险时提前拒绝，不能绕过原版交互包拦截。
- 延迟离线死亡在确认完成且没有新的在线实体接管 UUID 后保存玩家数据。
- 船、矿车及直接推挤补充双向无碰撞守卫。
- Traits/Witch 用公共协议协商单次视觉合成，避免两次灰白叠加。
- Second Strike 使用安全调用范围并识别真正吸收，避免退出疯魔误触发补刀。

## 验证结果

### 自动化

- **SparkTraits：加入商店精确匕首资格限制后，230 个 JUnit 测试通过，0 失败/跳过；新增 7 个资格回归测试。** 以下客户端/GPU记录来自此前 223 测试版本，本次仅重跑编译、自动化和打包校验，未重跑实机分配。
- **SparkStrength：构建通过；7 个独立 Python/Java 契约测试通过。** 其 Gradle 测试任务没有发现 JUnit 用例，不能把它当作额外一套已运行测试。
- **SparkWitch 本次改动隔离副本：579 个 JUnit 测试通过，构建通过。**
- Traits 的 `remapJar`、`verifyModJarVersion`、`verifyAssassinScreenSelector` 通过。
- 三个源工作区 `git diff --check` 通过。

Traits 原基线有一个 API 测试直接构造 Minecraft 物品，导致未初始化注册表错误；已改为不依赖游戏注册表的无操作契约夹具，不修改生产注册逻辑。

### 实际客户端与 GPU

在独立的临时游戏目录运行了真实 Fabric 客户端，组合加载 Traits、Strength、Witch、FactionAPI 以及必需依赖。

- 实际执行主菜单到 Options 界面的操作并读取截图。
- 四个词条已进入运行时注册表。
- 强制加载并成功变换击杀、匕首/枪械包、船/矿车、道具冷却、交互管理和 HUD 相关目标类。
- 检查最终变换后的 `GameFunctions`：Tofana 保护在绝处逢生前，随后才是实际死亡；最外层为 Traits 的安全击杀范围。
- 两个实际后处理着色器均创建、编译并执行；纯红色输入经 50% 灰白后均为 `RGB(166,38,38)`，符合期望。
- 测试客户端正常退出。临时测试模组没有加入交付 jar。

这不等同于多人玩法验收。尚未实际完成两客户端对局中的举刀时序、被反制者操作、掉出列车、断线重连、门内到期、各职业完整交互及网络延迟矩阵。

## Witch 原有 WIP 与测试构建的区别

原 Witch 工作区有与本任务无关的未提交改动：

1. 毒苹果源码断言失败；备份证实相关生产文件与任务开始前逐字节相同。
2. 原有 `ScreenHandlerWraithConsumableDropMixin` 的 `slots` 映射导致组合启动失败。

没有覆盖这些 WIP。最终 Witch 测试 jar 使用 **`main=728de3f` + 本任务的 35 个修改/新增文件**，在 `/tmp/sparkwitch-new-killer-traits-isolated` 构建，不含上述 WIP。完整原工作区与此隔离副本不可混称为同一个通过验证的构建。

运行时还报告既有仪礼剑模型非法旋转角 `25.0` 等资源警告；没有阻止本次启动/GPU测试，未在本任务修复。

## 复现

需要 Java 21。普通环境可在 Traits 工程执行：

```bash
./gradlew test remapJar verifyModJarVersion verifyAssassinScreenSelector
```

Strength 独立契约测试：

```bash
python3 ../SparkStrength/src/test/python/test_new_killer_traits_contract.py -v
```

本机旧 `build/classes` 存在云同步占位文件，曾导致 Gradle 输出快照读取阻塞；验证时用临时 Gradle init 脚本将构建输出定向到 `/tmp/spark-new-killer-traits-builds`，未删除旧输出或修改工程构建配置。

## 交付限制

测试 jar 沿用当前工程版本号，不是另行发布的正式版本。联调应同时使用本次 Traits/Strength/Witch 三个构建，并保留匹配的必需依赖；不要把测试探针或 sources jar 放进正式模组目录。完整部署前仍需多人玩法验收。

隐藏词条查询命令的权限问题由用户启动的另一会话独立处理；本次没有擅自合入该会话的修改。
