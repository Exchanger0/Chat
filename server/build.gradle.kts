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
}

application {
    mainClass = "com.chat.server.Server"
}