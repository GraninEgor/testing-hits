package org.example.recipebook.api.controller

import org.example.recipebook.api.dto.*
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.test.SharedTestContainers
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.HttpMethod.*
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

        @Test
        fun `should return 404 for non-existent id`() {
            val response: ResponseEntity<Void> = restTemplate.getForEntity("$apiBase/999999", Void::class.java)
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
}

data class PagedResponse<T>(
    val content: List<T> = emptyList(),
    val number: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)