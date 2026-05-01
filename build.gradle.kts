plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = true
        parallel = true
        autoCorrect = false
        baseline = rootProject.file("config/detekt/detekt-baseline.xml")
    }

    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
    }
}

tasks.register("detektAll") {
    group = "verification"
    description = "Run Detekt on all source sets (KMP + Android)"
    dependsOn(
        ":shared:detektMetadataCommonMain",
        ":androidApp:detektAndroidProdDebug",
    )
}
