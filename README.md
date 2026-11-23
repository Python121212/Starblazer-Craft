# Starblazer Craft

Minecraft **1.20.1 / Forge 47.x** 用の、機能を一切追加しない空の MOD です。

Forge 標準の `lowcodefml` ローダーを使用し、MOD の認識に必要なメタデータのみを収録します。
Java クラス、アイテム、ブロック、レシピなどはありません。

## 導入

1. Minecraft 1.20.1 と Forge 47.x をインストールします（実行には Java 17 が必要です）。
2. [Releases](https://github.com/Python121212/Starblazer-Craft/releases) から JAR を取得します。
3. 対象プロファイルの `mods` フォルダーに JAR を配置します。

クライアント・サーバー両対応のメタデータです。Fabric 用ではありません。

## ビルド

Python 3.11 以降で実行できます。外部ライブラリ、JDK、ForgeGradle は不要です。

```sh
python3 scripts/build.py
```

出力: `build/libs/starblazer-craft-1.20.1-0.0.1.jar`

ソースは main、配布用 JAR は GitHub Releases に配置します。
JAR はバージョン管理に含めません。

## 動作確認について

JAR の構造とメタデータは検査していますが、Minecraft / Forge の実起動テストは未実施です。
