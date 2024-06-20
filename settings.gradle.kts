pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("/Users/sqsong/Public/Project/Github/github10/media-release/libraries/LocalMaven")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("/Users/sqsong/Public/Project/Github/github10/media-release/libraries/LocalMaven")
        }
    }
}

rootProject.name = "CustomViews"
include(":app")
include(":ImageFilter")
include(":PhotoView")
include(":OpenGLProcessor")
include(":OpenGLLib")
