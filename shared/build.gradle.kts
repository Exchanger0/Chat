plugins {
    id("java")
}

group = "com.chat.shared"
version = "1.0.0"

dependencies {
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}