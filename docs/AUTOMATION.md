# GitHub 上のテストと素材ビューアー

## 普段の確認方法

1. ソースを push / PR 作成すると **Actions → Build and test** が自動実行されます。手動実行も可能です。
2. 緑色なら現在実装されている自動テストは成功です。赤色なら失敗したステップのログを確認します。
3. 実行結果の Artifacts から `mod-jar` と `asset-preview` を取得できます。
4. main を更新してもサイトは更新されません。Pages の公開は、下記の明示的な手動操作時だけ行います。

素材ラボ: <https://python121212.github.io/Starblazer-Craft/>

GitHub Pages は有効化済みですが、**サイトの自動更新は無効**です。
push / PR や、通常の手動テストでは公開済みのサイトは変わりません。
普段は Markdown の仕様・説明・進捗を更新し、サイトは「ページを更新して」と依頼されたときだけ公開します。

### サイトを明示的に更新する手順

1. **Actions → Build and test → Run workflow** を開きます。
2. ブランチに **main** を選びます。
3. **サイトを更新する**（`deploy_pages`）にチェックを入れます。初期値はオフです。
4. 実行し、テストと `pages` ジョブの成功を確認します。

チェックを入れない実行ではテスト・Artifact 保存のみ行います。
main 以外のブランチではチェックを入れても公開しません。
`ENABLE_PAGES` 変数の設定は不要です。

リポジトリの公開設定や契約によって Pages の利用条件が異なる場合があります。
Pages の公開に失敗しても、成功済みのテストと Artifact は確認できます。

## 自動で行う検査

- Python 単体テスト（`tests/`）。将来の計算処理のテストを追加する場所です。
- Java 17 / Gradle 8.8 による Forge MOD のコンパイルと JAR ビルド。
- JAR の ZIP 整合性・コンパイル済みクラスとレンチ素材の同梱・資料の除外。
- Java の範囲計算テスト（端点の順序、境界、体積制限、整数オーバーフロー）。
- JAR のファイル順とタイムスタンプは固定しますが、バイト単位の再現性検査は現在行っていません。
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

Java のビルドには Java 17・Gradle 8.8 と Forge などの依存取得が必要です。
素材検査には Python 3.11 以降、JS 構文検査には Node.js を使用します。Python 側の追加ライブラリは不要です。

```sh
gradle --no-daemon build
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
