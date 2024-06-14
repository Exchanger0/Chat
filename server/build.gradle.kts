plugins {
    application
    id("java")
}

group = "com.chat.server"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.hibernate.orm:hibernate-core:6.5.2.Final")
}

application {
    mainClass = "com.chat.server.Server"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}