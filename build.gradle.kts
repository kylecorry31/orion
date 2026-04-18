plugins {
    kotlin("jvm") version "2.2.21"
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.35.0"
}

val versionName = "1.0.0"

group = "com.kylecorry"
version = versionName

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")

    testImplementation(kotlin("test"))
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test-utils:1.23.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

mavenPublishing {
    coordinates(group.toString(), "orion", versionName)

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

    publishToMavenCentral()
    signAllPublications()
}
