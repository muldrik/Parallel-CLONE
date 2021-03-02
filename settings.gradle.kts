pluginManagement {
    fun RepositoryHandler.setup() {
        jcenter()
        if (this == pluginManagement.repositories) {
            gradlePluginPortal()
        }
    }
    repositories.setup()
    gradle.allprojects { repositories.setup() }

    plugins {
        kotlin("jvm").version("1.4.10")
    }
}