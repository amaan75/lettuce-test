plugins {
    id("java")
    id("io.spring.dependency-management")  version("1.0.11.RELEASE")
    id("org.springframework.boot") version("2.5.4")
}

group = "com.demo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:2.5.4")
    implementation("org.springframework.boot:spring-boot-starter-web:2.5.4")
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.5.4"))
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    compileOnly("org.projectlombok:lombok:1.18.20")
    implementation("org.apache.commons:commons-pool2:2.12.0")
}

tasks.test {
    useJUnitPlatform()
}




dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.5.4")
    }
}



