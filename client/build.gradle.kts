plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("java")
}

group = "com.chat.client"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies{
    implementation(project(":shared"))
}

javafx {
    version = "22.0.1"
    modules = listOf("javafx.controls", "javafx.base", "javafx.graphics")
}

application {
    mainClass = "com.chat.client.UIClient"
}