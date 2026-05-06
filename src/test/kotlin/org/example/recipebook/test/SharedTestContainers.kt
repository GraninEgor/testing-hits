package org.example.recipebook.test

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection  // ← Импорт
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Path

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
    ]
)
@ActiveProfiles("test")
@Import(SharedTestContainers.TestConfig::class)
abstract class SharedTestContainers {

    @TestConfiguration
    class TestConfig {
        @Bean
        @ServiceConnection
        fun postgresContainer(): PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("recipebook_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
    }

    companion object {
        val testUploadDir: Path by lazy {
            Files.createTempDirectory("recipebook-test-uploads").apply {
                toFile().deleteOnExit()
            }
        }

        init {
            System.setProperty("app.upload-dir", testUploadDir.toString())
        }
    }

    @LocalServerPort
    protected var port: Int = 0

    protected val apiUrl: String get() = "http://localhost:$port"
}