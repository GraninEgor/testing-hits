package org.example.recipebook.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.api.dto.*
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.test.SharedTestContainers
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.http.HttpMethod.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductControllerApiTest : SharedTestContainers() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val apiBase = "/rest/admin-ui/products"
    private val createdProductIds = mutableListOf<Long>()

    @AfterEach
    fun tearDown() {
        createdProductIds.forEach { id ->
            restTemplate.exchange("$apiBase/$id", DELETE, null, Void::class.java)
        }
        createdProductIds.clear()
    }

    private fun assertStatus(expected: HttpStatus, actual: ResponseEntity<out Any>?) {
        Assertions.assertNotNull(actual, "Response should not be null")
        Assertions.assertEquals(
            expected,
            actual!!.statusCode,
            "Expected status $expected, but got ${actual.statusCode}"
        )
    }

    private fun createTestProduct(
        name: String = "Тестовый продукт ${System.currentTimeMillis()}",
        calories: Double = 100.0,
        proteins: Double = 10.0,
        fats: Double = 5.0,
        carbs: Double = 0.0,
        category: Category = Category.MEAT,
        cookingRequirement: CookingRequirement = CookingRequirement.READY_TO_EAT
    ): Long {
        val dto = ProductCreateDto(
            name = name,
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbohydrates = carbs,
            category = category,
            cookingRequirement = cookingRequirement,
            flags = emptySet(),
            composition = null,
            photos = emptyList()
        )

        val body = LinkedMultiValueMap<String, Any>()
        body.add("data", dto)

        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }

        val response: ResponseEntity<ProductDto> = restTemplate.exchange(
            apiBase,
            POST,
            HttpEntity(body, headers),
            ProductDto::class.java
        )

        assertStatus(HttpStatus.OK, response)

        val product = response.body
            ?: throw AssertionError("Response body is null for product creation")

        createdProductIds.add(product.id)
        return product.id
    }

    private fun updateProduct(id: Long, dto: ProductCreateDto): ProductDto {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("data", dto)

        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }

        val uri = java.net.URI.create("$apiBase/$id")
        val response: ResponseEntity<ProductDto> = restTemplate.exchange(
            uri,
            PATCH,
            HttpEntity<MultiValueMap<String, Any>>(body, headers),
            ProductDto::class.java
        )

        assertStatus(HttpStatus.OK, response)
        return response.body
            ?: throw AssertionError("Response body is null for product update")
    }

    @Nested
    @DisplayName("POST /rest/admin-ui/products")
    inner class CreateProductTests {

        @Test
        fun `should create product with valid data`() {
            val dto = ProductCreateDto(
                name = "Тестовый продукт ${System.currentTimeMillis()}",
                calories = 120.0,
                proteins = 20.0,
                fats = 5.0,
                carbohydrates = 10.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = setOf(FeatureFlag.VEGAN),
                composition = "Свежие овощи",
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }

            val response: ResponseEntity<ProductDto> = restTemplate.exchange(
                apiBase,
                POST,
                HttpEntity(body, headers),
                ProductDto::class.java
            )

            assertStatus(HttpStatus.OK, response)
            val created = response.body!!
            Assertions.assertTrue(created.id > 0)
            Assertions.assertEquals(dto.name, created.name)
            Assertions.assertEquals(dto.category, created.category)
            createdProductIds.add(created.id)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "A", "Ab"])
        @DisplayName("Должен вернуть 400 при невалидном имени продукта")
        fun `should return 400 when name is invalid`(invalidName: String) {
            val dto = ProductCreateDto(
                name = invalidName,
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 0.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.BAD_REQUEST, response)
        }

        @ParameterizedTest
        @CsvSource(
            "-10.0, 10.0, 5.0, 0.0",
            "-0.01, 10.0, 5.0, 0.0",
            "100.0, -5.0, 5.0, 0.0",
            "100.0, 10.0, -1.0, 0.0",
            "100.0, 10.0, 5.0, -0.5",
            "100.0, 101.0, 5.0, 0.0",
            "100.0, 10.0, 101.0, 0.0",
            "100.0, 10.0, 5.0, 101.0"
        )
        @DisplayName("Должен вернуть 400 при невалидных числовых полях")
        fun `should return 400 when numeric fields are invalid`(
            calories: Double, proteins: Double, fats: Double, carbs: Double
        ) {
            val dto = ProductCreateDto(
                name = "Тестовый продукт",
                calories = calories, proteins = proteins, fats = fats, carbohydrates = carbs,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.BAD_REQUEST, response)
        }

        @Test
        fun `should return 400 when name too short`() {
            val dto = ProductCreateDto(
                name = "A",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 0.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.BAD_REQUEST, response)
        }

        @Test
        fun `should return 400 when calories negative`() {
            val dto = ProductCreateDto(
                name = "Невалидный продукт",
                calories = -10.0, proteins = 10.0, fats = 5.0, carbohydrates = 0.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.BAD_REQUEST, response)
        }

        @Test
        fun `should return 400 when protein exceeds 100`() {
            val dto = ProductCreateDto(
                name = "Невалидный продукт",
                calories = 100.0, proteins = 150.0, fats = 5.0, carbohydrates = 0.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.BAD_REQUEST, response)
        }
    }

    @Nested
    @DisplayName("GET /rest/admin-ui/products")
    inner class GetProductsTests {

        @BeforeEach
        fun seedData() {
            repeat(3) { i ->
                createTestProduct(
                    name = "Тестовый продукт $i",
                    calories = 100.0 + i * 10,
                    category = if (i % 2 == 0) Category.VEGETABLES else Category.MEAT
                )
            }
        }

        @Test
        fun `should return paged list`() {
            val uri = java.net.URI.create("$apiBase?page=0&size=10")
            val response: ResponseEntity<PagedResponse<ProductDto>> = restTemplate.exchange(
                uri,
                GET,
                null,
                object : ParameterizedTypeReference<PagedResponse<ProductDto>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            Assertions.assertTrue(response.body!!.content.size >= 3)
        }

        @Test
        fun `should filter by category`() {
            val uri = java.net.URI.create("$apiBase?category=VEGETABLES&page=0&size=10")
            val response: ResponseEntity<PagedResponse<ProductDto>> = restTemplate.exchange(
                uri,
                GET,
                null,
                object : ParameterizedTypeReference<PagedResponse<ProductDto>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            response.body!!.content.forEach { product ->
                Assertions.assertEquals(Category.VEGETABLES, product.category)
            }
        }

        @Test
        fun `should return product by id`() {
            val id = createdProductIds.firstOrNull() ?: throw IllegalStateException("No products created")

            val response: ResponseEntity<ProductDto> = restTemplate.getForEntity("$apiBase/$id", ProductDto::class.java)

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            Assertions.assertEquals(id, response.body!!.id)
        }

        @ParameterizedTest
        @ValueSource(longs = [999999, -1, 0, Long.MAX_VALUE])
        @DisplayName("Должен вернуть 404 для несуществующих или невалидных ID (эквивалентное разбиение)")
        fun `should return 404 for non-existent or invalid id`(invalidId: Long) {
            val response: ResponseEntity<Void> = restTemplate.getForEntity("$apiBase/$invalidId", Void::class.java)
            assertStatus(HttpStatus.NOT_FOUND, response)
        }

        @Test
        fun `should return many by ids`() {
            val ids = createdProductIds.take(2).joinToString(",")
            val uri = java.net.URI.create("$apiBase/by-ids?ids=$ids")

            val response: ResponseEntity<List<ProductDto>> = restTemplate.exchange(
                uri,
                GET,
                null,
                object : ParameterizedTypeReference<List<ProductDto>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            Assertions.assertEquals(2, response.body!!.size)
        }
    }

    @Nested
    @DisplayName("PATCH /rest/admin-ui/products/{id}")
    inner class UpdateProductTests {

        @Test
        fun `should update product`() {
            val id = createTestProduct()

            val patchDto = ProductCreateDto(
                name = "Обновлённый продукт",
                calories = 150.0,
                proteins = 15.0,
                fats = 8.0,
                carbohydrates = 12.0,
                category = Category.FROZEN,
                cookingRequirement = CookingRequirement.SEMI_FINISHED,
                flags = setOf(FeatureFlag.GLUTEN_FREE),
                composition = "Замороженный полуфабрикат",
                photos = emptyList()
            )

            val updated = updateProduct(id, patchDto)

            Assertions.assertEquals("Обновлённый продукт", updated.name)
            Assertions.assertEquals(150.0, updated.calories)
            Assertions.assertEquals(Category.FROZEN, updated.category)
            Assertions.assertEquals(CookingRequirement.SEMI_FINISHED, updated.cookingRequirement)
        }

        @Test
        fun `should return 400 or 500 when invalid calories`() {
            val id = createTestProduct()

            val patchDto = ProductCreateDto(
                name = "Обновлённый продукт",
                calories = -50.0,
                proteins = 10.0, fats = 5.0, carbohydrates = 0.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", patchDto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val uri = java.net.URI.create("$apiBase/$id")
            val request = RequestEntity(body, headers, PATCH, uri)

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            val status = response.statusCode
            Assertions.assertTrue(
                status == HttpStatus.BAD_REQUEST || status == HttpStatus.INTERNAL_SERVER_ERROR,
                "Expected 400 or 500, but got $status"
            )
        }

        @Test
        fun `should return 404 for non-existent product`() {
            val patchDto = ProductCreateDto(
                name = "Обновлённый продукт",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 0.0,
                category = Category.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = emptySet(),
                composition = null,
                photos = emptyList()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", patchDto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val uri = java.net.URI.create("$apiBase/999999")
            val request = RequestEntity(body, headers, PATCH, uri)

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.NOT_FOUND, response)
        }
    }

    @Nested
    @DisplayName("DELETE /rest/admin-ui/products/{id}")
    inner class DeleteProductTests {

        @Test
        fun `should delete product`() {
            val id = createTestProduct()

            val deleteResponse: ResponseEntity<Void> = restTemplate.exchange(
                "$apiBase/$id",
                DELETE,
                null,
                Void::class.java
            )
            Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.statusCode)

            val getResponse: ResponseEntity<Void> = restTemplate.getForEntity("$apiBase/$id", Void::class.java)
            Assertions.assertEquals(HttpStatus.NOT_FOUND, getResponse.statusCode)
        }

        @Test
        fun `should delete many products`() {
            val ids = mutableListOf<Long>()
            repeat(2) {
                ids.add(createTestProduct(name = "Продукт $it ${System.currentTimeMillis()}"))
            }

            val response: ResponseEntity<Void> = restTemplate.exchange(
                "$apiBase?ids=${ids.joinToString(",")}",
                DELETE,
                null,
                Void::class.java
            )
            Assertions.assertEquals(HttpStatus.OK, response.statusCode)

            ids.forEach { id ->
                val getResp: ResponseEntity<Void> = restTemplate.getForEntity("$apiBase/$id", Void::class.java)
                Assertions.assertEquals(HttpStatus.NOT_FOUND, getResp.statusCode)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /rest/admin-ui/products (bulk)")
    inner class BulkUpdateTests {

        @Test
        fun `should patch many products by ids`() {
            val ids = mutableListOf<Long>()
            repeat(2) {
                ids.add(createTestProduct(name = "Продукт $it ${System.currentTimeMillis()}"))
            }

            val patchJson = """{"calories": 150.0, "proteins": 15.0}"""
            val uri = java.net.URI.create("$apiBase?ids=${ids.joinToString(",")}")

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val request = RequestEntity(patchJson, headers, PATCH, uri)

            val response: ResponseEntity<List<Long>> = restTemplate.exchange(
                request,
                object : ParameterizedTypeReference<List<Long>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            Assertions.assertTrue(response.body!!.containsAll(ids))
        }
    }


    @Nested
    @DisplayName("Дополнительные тесты ProductController")
    inner class ProductControllerAdditionalTests : SharedTestContainers() {

        @Autowired
        private lateinit var restTemplate: TestRestTemplate

        private val apiBase = "/rest/admin-ui/products"
        private val objectMapper = ObjectMapper()

        @Nested
        @DisplayName("Тесты обработки файлов (multipart)")
        inner class MultipartFileTests {

            @Test
            fun `should create product with photo files`() {
                val dto = ProductCreateDto(
                    name = "Продукт с фото ${System.currentTimeMillis()}",
                    calories = 100.0,
                    proteins = 10.0,
                    fats = 5.0,
                    carbohydrates = 0.0,
                    category = Category.VEGETABLES,
                    cookingRequirement = CookingRequirement.READY_TO_EAT,
                    flags = emptySet(),
                    composition = "С фото",
                    photos = emptyList()
                )

                val fileResource = object : ByteArrayResource("fake-image-content".toByteArray()) {
                    override fun getFilename(): String = "test.jpg"
                }

                val body = LinkedMultiValueMap<String, Any>().apply {
                    // DTO отправляем как JSON-строку с явным Content-Type
                    add("data", dto)
                    add("files", fileResource)
                }

                val headers = HttpHeaders().apply {
                    contentType = MediaType.MULTIPART_FORM_DATA
                }

                val response: ResponseEntity<ProductDto> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(body, headers),
                    ProductDto::class.java
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
                Assertions.assertNotNull(response.body)
                Assertions.assertTrue(response.body!!.id > 0)
            }

            @Test
            fun `should create product without files when files param is optional`() {
                val dto = ProductCreateDto(
                    name = "Продукт без фото ${System.currentTimeMillis()}",
                    calories = 100.0,
                    proteins = 10.0,
                    fats = 5.0,
                    carbohydrates = 0.0,
                    category = Category.VEGETABLES,
                    cookingRequirement = CookingRequirement.READY_TO_EAT,
                    flags = emptySet(),
                    composition = null,
                    photos = emptyList()
                )

                val body = LinkedMultiValueMap<String, Any>().apply {
                    add("data", dto)
                }

                val headers = HttpHeaders().apply {
                    contentType = MediaType.MULTIPART_FORM_DATA
                }

                val response: ResponseEntity<ProductDto> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(body, headers),
                    ProductDto::class.java
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
                Assertions.assertNotNull(response.body)
            }
        }

        @Nested
        @DisplayName("Тесты граничных значений и edge cases")
        inner class EdgeCasesTests {

            @ParameterizedTest
            @CsvSource(
                "0.0, 0.0, 0.0, 0.0",
                "999999.99, 100.0, 100.0, 100.0",
                "0.01, 0.01, 0.01, 0.01"
            )
            @DisplayName("Должен создать продукт с граничными числовыми значениями")
            fun `should create product with boundary numeric values`(
                calories: Double, proteins: Double, fats: Double, carbs: Double
            ) {
                val dto = ProductCreateDto(
                    name = "Граничный продукт ${System.currentTimeMillis()}",
                    calories = calories,
                    proteins = proteins,
                    fats = fats,
                    carbohydrates = carbs,
                    category = Category.VEGETABLES,
                    cookingRequirement = CookingRequirement.READY_TO_EAT,
                    flags = emptySet(),
                    composition = null,
                    photos = emptyList()
                )

                val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
                val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }

                val response: ResponseEntity<ProductDto> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(body, headers),
                    ProductDto::class.java
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
            }

            @Test
            fun `should handle empty flags list`() {
                val dto = ProductCreateDto(
                    name = "Продукт без флагов ${System.currentTimeMillis()}",
                    calories = 100.0,
                    proteins = 10.0,
                    fats = 5.0,
                    carbohydrates = 0.0,
                    category = Category.VEGETABLES,
                    cookingRequirement = CookingRequirement.READY_TO_EAT,
                    flags = emptySet(),
                    composition = null,
                    photos = emptyList()
                )

                val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
                val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }

                val response: ResponseEntity<ProductDto> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(body, headers),
                    ProductDto::class.java
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
                Assertions.assertTrue(response.body?.flags?.isEmpty() == true)
            }

            @Test
            fun `should handle all feature flags`() {
                val dto = ProductCreateDto(
                    name = "Продукт со всеми флагами ${System.currentTimeMillis()}",
                    calories = 100.0,
                    proteins = 10.0,
                    fats = 5.0,
                    carbohydrates = 0.0,
                    category = Category.VEGETABLES,
                    cookingRequirement = CookingRequirement.READY_TO_EAT,
                    flags = FeatureFlag.values().toSet(),
                    composition = null,
                    photos = emptyList()
                )

                val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
                val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }

                val response: ResponseEntity<ProductDto> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(body, headers),
                    ProductDto::class.java
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
                Assertions.assertEquals(FeatureFlag.values().size, response.body?.flags?.size)
            }
        }

        @Nested
        @DisplayName("Тесты фильтрации и пагинации")
        inner class FilteringTests {

            @BeforeEach
            fun seedData() {
                listOf(
                    Category.VEGETABLES to 50.0,
                    Category.MEAT to 200.0,
                    Category.VEGETABLES to 30.0,
                    Category.FROZEN to 150.0
                ).forEachIndexed { index, (category, calories) ->
                    createTestProduct(
                        name = "Фильтр тест $index",
                        calories = calories,
                        category = category
                    )
                }
            }

            @Test
            fun `should filter by cooking requirement`() {
                val uri = java.net.URI.create("$apiBase?cookingRequirement=READY_TO_EAT&page=0&size=10")
                val response: ResponseEntity<PagedResponse<ProductDto>> = restTemplate.exchange(
                    uri,
                    GET,
                    null,
                    object : ParameterizedTypeReference<PagedResponse<ProductDto>>() {}
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
                response.body?.content?.forEach { product ->
                    Assertions.assertEquals(CookingRequirement.READY_TO_EAT, product.cookingRequirement)
                }
            }

            @Test
            fun `should handle large page size`() {
                val uri = java.net.URI.create("$apiBase?page=0&size=1000")
                val response: ResponseEntity<PagedResponse<ProductDto>> = restTemplate.exchange(
                    uri,
                    GET,
                    null,
                    object : ParameterizedTypeReference<PagedResponse<ProductDto>>() {}
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
            }
        }

        @Nested
        @DisplayName("Тесты bulk operations")
        inner class BulkOperationsTests {

            @Test
            fun `should patch many with partial update`() {
                val ids = mutableListOf<Long>()
                repeat(3) { ids.add(createTestProduct(name = "Bulk $it")) }

                val patchJson = """{"calories": 200.0}"""
                val uri = java.net.URI.create("$apiBase?ids=${ids.joinToString(",")}")

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }

                val request = RequestEntity(patchJson, headers, PATCH, uri)

                val response: ResponseEntity<List<Long>> = restTemplate.exchange(
                    request,
                    object : ParameterizedTypeReference<List<Long>>() {}
                )

                Assertions.assertEquals(HttpStatus.OK, response.statusCode)
                Assertions.assertEquals(ids.size, response.body?.size)

                ids.forEach { id ->
                    val getProduct = restTemplate.getForEntity("$apiBase/$id", ProductDto::class.java)
                    Assertions.assertEquals(200.0, getProduct.body?.calories)
                }
            }

            @Test
            fun `should patch many with empty ids list`() {
                val patchJson = """{"calories": 200.0}"""
                val uri = java.net.URI.create("$apiBase?ids=")

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }

                val request = RequestEntity(patchJson, headers, PATCH, uri)

                val response: ResponseEntity<List<Long>> = restTemplate.exchange(
                    request,
                    object : ParameterizedTypeReference<List<Long>>() {}
                )

                Assertions.assertTrue(
                    response.statusCode in listOf(HttpStatus.OK, HttpStatus.BAD_REQUEST),
                    "Expected OK or BAD_REQUEST, got ${response.statusCode}"
                )
            }

            @Test
            fun `should delete many with non-existent ids`() {
                val response: ResponseEntity<Void> = restTemplate.exchange(
                    "$apiBase?ids=999999,888888",
                    DELETE,
                    null,
                    Void::class.java
                )

                Assertions.assertTrue(
                    response.statusCode in listOf(HttpStatus.OK, HttpStatus.NOT_FOUND),
                    "Expected OK or NOT_FOUND, got ${response.statusCode}"
                )
            }
        }

        @Nested
        @DisplayName("Тесты обработки ошибок")
        inner class ErrorHandlingTests {

            @Test
            fun `should return 500 when required field data is missing in multipart`() {
                val body = LinkedMultiValueMap<String, Any>()

                val headers = HttpHeaders().apply {
                    contentType = MediaType.MULTIPART_FORM_DATA
                }

                val response: ResponseEntity<Void> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(body, headers),
                    Void::class.java
                )

                Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
            }

            @Test
            fun `should return 415 when content type is wrong for create`() {
                val dto = ProductCreateDto(
                    name = "Тест",
                    calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 0.0,
                    category = Category.VEGETABLES,
                    cookingRequirement = CookingRequirement.READY_TO_EAT,
                    flags = emptySet(),
                    composition = null,
                    photos = emptyList()
                )

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }

                val response: ResponseEntity<Void> = restTemplate.exchange(
                    apiBase,
                    POST,
                    HttpEntity(dto, headers),
                    Void::class.java
                )

                Assertions.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.statusCode)
            }

            @Test
            fun `should handle concurrent delete requests gracefully`() {
                val id = createTestProduct()

                val response1 = restTemplate.exchange(
                    "$apiBase/$id",
                    DELETE,
                    null,
                    Void::class.java
                )

                val response2 = restTemplate.exchange(
                    "$apiBase/$id",
                    DELETE,
                    null,
                    Void::class.java
                )

                val statuses = listOf(response1.statusCode, response2.statusCode)
                Assertions.assertTrue(
                    HttpStatus.NO_CONTENT in statuses || HttpStatus.NOT_FOUND in statuses,
                    "Expected NO_CONTENT or NOT_FOUND, got $statuses"
                )
            }
        }
    }
}

data class PagedResponse<T>(
    val content: List<T> = emptyList(),
    val number: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)