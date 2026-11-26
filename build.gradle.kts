import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    id("java-library")
    id("maven-publish")
}

val versionName = "1.2.1"

group = "com.kylecorry"
version = versionName

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "orion"
            from(components["java"])

            pom {
                name.set("Orion")
                description.set("Reusable Detekt rules for Kotlin projects.")
                url.set("https://github.com/kylecorry31/orion")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("kylecorry31")
                        name.set("Kyle Corry")
                        email.set("kylecorry31@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/kylecorry31/orion.git")
                    developerConnection.set("scm:git:ssh://github.com:kylecorry31/orion.git")
                    url.set("https://github.com/kylecorry31/orion")
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("dev.detekt:detekt-api:2.0.0-alpha.6")

    testImplementation(kotlin("test"))
    testImplementation("dev.detekt:detekt-test:2.0.0-alpha.6")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// WHY: JUnit 6 requires Java 17, while the published rules remain compatible with Java 11.
configurations.named("testCompileClasspath") {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}

configurations.named("testRuntimeClasspath") {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}

tasks.test {
    useJUnitPlatform()
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.named<JavaCompile>("compileTestJava") {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

kotlin {
    jvmToolchain(11)
}
