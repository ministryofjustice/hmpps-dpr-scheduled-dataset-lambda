import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm") version "2.0.21"
  id("jacoco")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
  id("org.owasp.dependencycheck")  version "8.2.1"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.590")
  implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
  implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
  implementation("org.quartz-scheduler:quartz:2.5.0")

  implementation("com.google.code.gson:gson:2.11.0")
  implementation("com.google.guava:guava:33.3.1-jre")

  implementation("software.amazon.awssdk:redshiftdata:2.29.20")
  //implementation("software.amazon.awssdk:athena:2.29.20")
  implementation("aws.sdk.kotlin:dynamodb:1.3.90")

  //fixes for shadow jar

  implementation("org.apache.httpcomponents:httpcore:4.4.16")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
  implementation("jakarta.activation:jakarta.activation-api:2.1.0")
  implementation("software.amazon.awssdk:apache-client:2.29.20")
  implementation("org.slf4j:slf4j-api:2.0.16")
  implementation("software.amazon.awssdk:netty-nio-client:2.29.2")
  implementation("aws.sdk.kotlin:aws-http:1.3.90")
  implementation("aws.sdk.kotlin:aws-http-jvm:1.3.90")
  implementation("aws.smithy.kotlin:http-auth-aws:1.3.28")
  implementation("aws.smithy.kotlin:http-auth-aws-jvm:1.3.28")
  implementation("aws.smithy.kotlin:aws-json-protocols:1.3.28")
  implementation("aws.smithy.kotlin:aws-json-protocols-jvm:1.3.28")

  // Testing
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
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

tasks.assemble {
  dependsOn(tasks.shadowJar)
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

tasks {
  withType<Test> {
    useJUnitPlatform()
  }
  withType<ShadowJar> {
    // <WORKAROUND for="https://github.com/johnrengelman/shadow/issues/448">
    configurations = listOf(
      project.configurations.implementation.get(),
      project.configurations.runtimeOnly.get()
    ).onEach { it.isCanBeResolved = true }
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
