// このファイルは、すべてのサブプロジェクト/モジュールに共通の設定を追加できます。
// プロジェクト全体のプラグインはここで宣言しますが、「apply false」で即時適用はしません。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
}