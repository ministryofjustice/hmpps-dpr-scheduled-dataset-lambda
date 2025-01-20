plugins {
  kotlin("jvm") version "2.0.21"
  id("jacoco")
  id("com.github.johnrengelman.shadow") version "7.1.1" // originally 7.1.2 but downgraded since circle CI build was failing
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
  id("org.owasp.dependencycheck")  version "8.2.1"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}
/*
ext {
  junitVersion = '5.10.1'
  hamcrestVersion = '2.2'
  mockitoVersion = '5.2.0'
  amazonSdkVersion = '1.12.590'
  amazonRedShiftSdkVersion = '2.26.22'
  lambdaCoreVersion = '1.2.3'
  lambdaJavaEventsVersion = '3.11.3'
  systemLambdaVersion = '1.2.1'
}
*/
dependencies {
  /* */
  implementation("com.amazonaws:aws-java-sdk-s3:1.12.590")
  implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.590")
  implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
  implementation("com.amazonaws:aws-lambda-java-events:3.11.3")

  //implementation("software.amazon.awssdk:lambda:2.29.52")
  //implementation("software.amazon.awssdk:aws-core:2.29.52")
  //software.amazon.awssdk:lambda:2.29.52
  //software.amazon.awssdk:aws-core:2.29.52
  implementation("com.google.code.gson:gson:2.11.0")
  implementation("com.google.guava:guava:33.3.1-jre")

  implementation("software.amazon.awssdk:redshiftdata:2.29.20")
  implementation("software.amazon.awssdk:athena:2.29.20")
  implementation("aws.sdk.kotlin:dynamodb:1.4.2")

  // Fix dependency mismatch
  implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
  implementation("com.squareup.okhttp3:okhttp-coroutines:5.0.0-alpha.14")

  // Testing

  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
  testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
  testImplementation("org.testcontainers:postgresql:1.20.4")
  testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}

java.sourceCompatibility = JavaVersion.VERSION_21

tasks.jar {
  enabled = true
}

repositories {
  mavenLocal()
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

/*
shadowJar {
  zip64 true
}
*/
tasks {
  withType<Test> {
    useJUnitPlatform()
  }
}

/*
project.getTasksByName("check", false).forEach {
  val prefix = if (it.path.contains(":")) {
    it.path.substringBeforeLast(":")
  } else {
    ""
  }
  it.dependsOn("$prefix:ktlintCheck")
}
 */
/*
assemble {
  dependsOn shadowJar
}
 */