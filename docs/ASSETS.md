# 卡面与第三方素材

卡面用于现有开发快照中的卡片识别和样式选择；商标、卡片图案和第三方素材的权利属于各自权利人，不因本仓库中的代码许可声明而转移或重新授权。正式分发前仍需核对使用条件。

| 卡面 | 记录的官方来源 |
| --- | --- |
| Suica | https://www.jreast.co.jp/suica/ic/whats/service.html/ |
| PASMO | https://www.pasmo.co.jp/ic-card/buy/type/ |
| ICOCA | https://www.jr-odekake.net/icoca/about/ |
| Kitaca | https://www.jrhokkaido.co.jp/kitaca/01about.html |
| TOICA | https://toica.jr-central.co.jp/outline/type/index.html |
| SUGOCA | https://www.jrkyushu.co.jp/sugoca/about/kinds/ |
| nimoca | https://www.nimoca.jp/about/type |
| PiTaPa | https://www.pitapa.com/ |

对应关系也保存于 `app/src/main/assets/cards/catalog.json`。部分来源页面或素材尺寸仍需复核；收录卡面不代表已验证相应卡种 NFC 功能。

多语言资源中包含通过 Google 翻译服务生成的机器翻译初稿；它们是构建时收集的静态资源，应用运行时不调用该服务。尚未完成所有语言的人工校对。

设计预览附带的 `dom-worker.mjs` 为既有预览运行配套文件，按原样归档；它不是 Android 应用代码。正式产品不依赖此预览文件。
