buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
        classpath(libs.google.services) // הוספנו את זה
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}