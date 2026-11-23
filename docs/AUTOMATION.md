# GitHub 上のテストと素材ビューアー

## 普段の確認方法

1. ソースを push / PR 作成すると **Actions → Build and test** が自動実行されます。手動実行も可能です。
2. 緑色なら現在実装されている自動テストは成功です。赤色なら失敗したステップのログを確認します。
3. 実行結果の Artifacts から `mod-jar` と `asset-preview` を取得できます。
4. 初回設定後は main の検査成功ごとに GitHub Pages に素材ラボが自動公開されます。

素材ラボ: <https://python121212.github.io/Starblazer-Craft/>

GitHub Pages は有効化済みです。main のテスト成功後に自動デプロイします。
`ENABLE_PAGES` 変数の設定は不要です。
手動で更新する場合は Build and test を main で実行してください。

リポジトリの公開設定や契約によって Pages の利用条件が異なる場合があります。
Pages の公開に失敗しても、成功済みのテストと Artifact は確認できます。

## 自動で行う検査

- Python 単体テスト（`tests/`）。将来の計算処理のテストを追加する場所です。
- JAR のビルド、ZIP 整合性、同じ入力から同じ JAR が得られること。
- JSON の構文、ローカル親モデル・テクスチャ・blockstates のモデル参照。
- 親モデル／テクスチャ変数の循環参照、重複した素材 ID。
- PNG の署名・サイズ・CRC・圧縮データ。
- 素材ビューアーの生成と JavaScript 構文。

**まだ物理エンジンは実装されていないため、重力・浮力の計算テストや負荷測定はありません。**
これは Minecraft / Forge 起動テスト、完全なリソース仕様検証、ブラウザー描画テストの代替ではありません。
未収録のバニラ素材は検査を通しますが、ビューアーでは警告します。

## ブロックが増えたら

本番素材は以下に追加します。次回の CI が自動収集するため、一覧ファイルの手編集は不要です。

```text
src/main/resources/assets/starblazercraft/
  blockstates/<ブロック名>.json
  models/block/<ブロック名>.json
  models/item/<ブロック名>.json
  textures/block/<画像名>.png
```

説明用素材は `docs/examples/assets/` に置きます。本番素材と同じ ID を置くと曖昧になるため、検査でエラーにします。
見本を本番へ移すときは、見本側から削除するか別 ID にしてください。
JSON や PNG の追加だけではブロック自体は登録されません。

### 3D 表示の対応範囲

- マウス／タッチで回転、ホイールで拡大縮小、視点リセット、モデル名検索。
- PNG の元画像へのリンク、ピクセルサイズ、モデル JSON 表示。
- `cube_all` / `cube_bottom_top` / `cube_column` / `cube` / `orientable` の標準親モデル。
- ローカル親モデルの継承、`#texture` 参照、`elements` の直方体、面別 UV・面回転、要素回転。
- `item/generated` / `item/handheld` は `layer0` の平面画像として表示。

簡易の平行投影・面の奥行き順描画です。複雑な交差形状や半透明は正確でない場合があります。
バニラ画像そのものは同梱しません（未解決の面は紫色）。未対応のバニラ親モデルは警告します。
独自 Forge ローダー、アニメーション、`display`、発光・陰影・tint、アイテム多層合成、blockstates の状態選択・回転は再現しません。
正確な見た目の最終確認はゲーム内で行ってください。

## ローカルで同じ検査を行う

Python 3.11 以降と、JS 構文検査用の Node.js が必要です。追加ライブラリは不要です。

```sh
python3 -m unittest discover -s tests -v
python3 scripts/asset_catalog.py
node --check tools/preview/viewer.js
python3 -m http.server 8000 --bind 0.0.0.0 --directory build/preview
```

ローカルのブラウザーで `http://localhost:8000` を開きます。
Artifact も展開したフォルダーを同様に HTTP 配信してください（`file://` では JSON を取得できません）。

## Release

`Publish release` ワークフローを main で手動実行します。
`mods.toml` のバージョンに応じた新規 Release を作成し、JAR を添付します。
既存バージョンは上書きしません。新しい配布を作る前にバージョンを上げてください。
通常の push / PR では Release は作成しません。
