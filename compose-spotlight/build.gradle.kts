plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.daiatech.composespotlight"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // Core dependencies
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose-android:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.10.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
}

// Library version
val libraryVersion = "1.0.0"
val libraryGroup = "io.github.DeepanshuPratik"
val libraryArtifactId = "compose-spotlight"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = libraryGroup
                artifactId = libraryArtifactId
                version = libraryVersion

                pom {
                    name.set("Compose Spotlight")
                    description.set("A feature spotlight library for Jetpack Compose that allows you to highlight UI elements with tooltips and audio narration")
                    url.set("https://github.com/DeepanshuPratik/compose-spotlight")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("DeepanshuPratik")
                            name.set("Deepanshu Pratik")
                            email.set("deepanshupratik@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/DeepanshuPratik/compose-spotlight.git")
                        developerConnection.set("scm:git:ssh://github.com/DeepanshuPratik/compose-spotlight.git")
                        url.set("https://github.com/DeepanshuPratik/compose-spotlight")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "sonatype"
                url = if (libraryVersion.endsWith("SNAPSHOT")) {
                    uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                    password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    signing {
        // Sign only if credentials are available
        if (hasProperty("signing.keyId")) {
            sign(publishing.publications["release"])
        }
    }
}
