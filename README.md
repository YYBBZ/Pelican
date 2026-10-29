# Pelican 2.4

## 安装
`/workspace/Pelican.apk`，约5.1MiB。包名 `app.pelican.monitor`，versionCode 8，支持Android8以上。

本版沿用Pelican2.3/2.2签名，可直接覆盖升级，历史/线路/基线保留；不要卸载。建议自行先导出备份（不含密钥）。更早原PelicanCheck使用另一包名/未知原私钥，不在本覆盖升级关系内。

## 主要变化
1. 输入Token筛查改本地估算：数量默认800或倍率默认5，二选一；估算不可用时使用正文字节补充规则。卡片蓝色「注入」、最终ERROR、详情明确差额与规则及疑似免责声明。输入异常单独不会通知或增加连续质量异常。
2. 历史趋势/管理两个入口独立等宽、有间距。保留既有缩放/横屏趋势、全选管理和完整SVG。
3. 七档思考强度中英文显示；auto不下发；需要预算时xhigh32000/max64000。输出上限不足禁止保存，不自动抬升。
4. 每轮样本1–99、补试0–20可自填并实际执行；越界禁止保存，补试>5提醒重复计费。
5. 自动质量下限×倍率（0.5–1.5默认0.9），上限100封顶；固定手填值优先；编辑/监控/执行同一算式，历史保留本轮实际值。
6. 按轮次题目顺序表：可增删自定义、拖动与上/下移，下一轮首位标记；轮内锁题，任一请求完整返回就前移，全部失败则停留说明。顺序/位次独立于检测间隔、按线路保存。
7. 升级本地重判旧历史，用所属线路规则，已删线路默认800；保留原始记录/原状态，不能重判的明示；不回调接口、不改基线、不发通知。

完整行为需求见 docs/REQUIREMENTS-2.4.md，交付及验证边界见 docs/RELEASE-2.4.md。

## 验证
253项本地逻辑/回环检查和16组Android JVM/SQLite验收通过；13次真实短生成及模型列表成功；包完整性/签名/对齐/密钥排除通过。Android16 JVM尝试受Linux ARM原生库缺失阻塞，未计入通过项。没有声称完成真机安装、实际绘制或长时后台验证。

## 构建
执行 `bash scripts/build.sh`。工具：JDK17、aapt2、D8/R8、zipalign、apksigner；Java API35、资源include API34，目标API34。
- Java源码在 app/src/main/java，依赖jar在 app/libs，许可在 app/src/main/assets。
- build.sh按Gradle骨架中的包名/版本生成manifest，执行本地回归，再DEX/对齐/签名。
- 本工作区 tools/robolectric 存在时会额外执行 scripts/test-runtime.sh；否则源码备份用户可按 verification/android-runtime/pom.xml 下载测试依赖。所有测试库都在tools，不进入APK。
- Android16兼容尝试单独记录于 verification/v2.4/android16-compatibility.log，不是构建通过条件。

私钥保留于 `/workspace/Pelican-signing/pelican.p12`，口令文件同目录，后续升级必须沿用；不应公开或写入APK/源码备份。
测试用URL/key仅用于获得授权的联调，密钥临时文件已移除、没有硬编码在工程或APK。

原2.3源码及安装包保留在 `/workspace/Pelican-backups/Pelican-2.3-before-2.4.zip`，不含签名私钥。
