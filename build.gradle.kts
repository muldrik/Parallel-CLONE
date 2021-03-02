plugins {
    kotlin("jvm")
    application
}

dependencies {
    testImplementation(kotlin("test-junit"))
}

kotlin.sourceSets.all {
    languageSettings.apply {
        useExperimentalAnnotation("kotlin.RequiresOptIn")
    }
}