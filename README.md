# Starblazer Craft

Minecraft **1.20.1 / Forge 47.x / Java 17** 用の独立 MOD です。Create などへの依存はありません。
船・航空機・宇宙艦へ向けた基盤を段階的に実装します。

## 現在の内容（0.3.1）

- 同じワールド内でY=5000を宇宙への入口とする高高度環境の試作（建築上限は変更しません）。
- 地球と月の同一ワールド表示、上半球へ着陸・歩行できる小規模な解析的月面（採掘・建築は未対応）。
- 高度による空の暗化・星、エリトラ失速・酸欠・重力倍率。
- 設置可能なコアブロック。レンチで範囲選択を開始できます。
- クリエイティブインベントリに「Starblazer Craft」専用タブ（レンチのアイコン）。
- レンチアイテム（日本語・英語表示、16×16 テクスチャ、厚みのある3Dモデル）。
- 将来のコアを起点に対角2点を指定し、直方体の範囲を保存。
- Shift＋左クリックで範囲外のブロックを追加選択。
- 選択成功時、クリックしたブロックの設置音をその位置で再生。
- エンティティ化・船体物理は未実装。ワールドのブロックは変更しません。

[レンチの操作・制限・テスト方法](docs/WRENCH.md)

## 導入

1. Minecraft 1.20.1 と Forge 47.x、Java 17 を用意します。
2. [Releases](https://github.com/Python121212/Starblazer-Craft/releases) の該当バージョンの JAR を `mods` に配置します。

旧 v0.0.1 は空の MOD です。レンチは v0.1.0 以降に含まれます。

## ビルドと検査

Java の登録処理が必要なため、従来のメタデータ専用 JAR から **JavaFML / ForgeGradle** に移行しました。
Java 17、Gradle 8.8、Python 3.11 以降を使用します。PNG見本の生成には `requirements-dev.txt` の Pillow を使用します。

```sh
gradle --no-daemon build
python3 -m pip install -r requirements-dev.txt
python3 -m unittest discover -s tests -v
python3 scripts/asset_catalog.py
```

JAR: `build/libs/starblazer-craft-1.20.1-0.3.1.jar`

GitHub Actions でも Forge コンパイル・Java の選択計算テスト・素材検査を行います。
ゲーム実起動・マルチプレイの操作テストとは別です。
ソースは main、配布 JAR は Releases に置き、JAR は Git 管理に含めません。

## 資料・素材確認

- [コアブロックの説明](docs/CORE.md)

- [宇宙・地球・月面着陸の仕様と試験方法](docs/SPACE.md)

- [高高度・宇宙環境の仕様と未実装項目](docs/ATMOSPHERE.md)

- [専用クリエイティブタブの説明](docs/CREATIVE_TAB.md)

- [テクスチャ一覧（手持ち・設置時の PNG 見本）](テクスチャ一覧.md)

- [テクスチャ・JSON の日本語ガイド](docs/TEXTURES.md)
- [自動テストと素材ラボの説明](docs/AUTOMATION.md)
- [素材ラボ（最後に明示公開した版）](https://python121212.github.io/Starblazer-Craft/)

説明・仕様・進捗は主に Markdown を更新します。
**サイトは自動更新しません。**「ページを更新して」と指示されたときだけ明示的に公開します。
