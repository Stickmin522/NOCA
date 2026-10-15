pluginManagement {
    repositories {
        System.getenv("NOCA_MAVEN_MIRROR")?.let { maven { url = uri(it) } }
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        System.getenv("NOCA_MAVEN_MIRROR")?.let { maven { url = uri(it) } }
        google(); mavenCentral()
    }
}
rootProject.name = "NOCA"
include(":app")
