# Starblazer Craft

Minecraft **1.20.1 / Forge 47.x / Java 17** 用の独立 MOD です。Create などへの依存はありません。
船・航空機・宇宙艦へ向けた基盤を段階的に実装します。

## 現在の内容（0.1.2）

- レンチアイテム（日本語・英語表示、16×16 テクスチャ、厚みのある3Dモデル）。
- 将来のコアを起点に対角2点を指定し、直方体の範囲を保存。
- Shift＋左クリックで範囲外のブロックを追加選択。
- コア・エンティティ化・物理は未実装。ワールドのブロックは変更しません。

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

JAR: `build/libs/starblazer-craft-1.20.1-0.1.2.jar`

GitHub Actions でも Forge コンパイル・Java の選択計算テスト・素材検査を行います。
ゲーム実起動・マルチプレイの操作テストとは別です。
ソースは main、配布 JAR は Releases に置き、JAR は Git 管理に含めません。

## 資料・素材確認

- [テクスチャ一覧（手持ち・設置時の PNG 見本）](テクスチャ一覧.md)

- [テクスチャ・JSON の日本語ガイド](docs/TEXTURES.md)
- [自動テストと素材ラボの説明](docs/AUTOMATION.md)
- [素材ラボ（最後に明示公開した版）](https://python121212.github.io/Starblazer-Craft/)

説明・仕様・進捗は主に Markdown を更新します。
**サイトは自動更新しません。**「ページを更新して」と指示されたときだけ明示的に公開します。
