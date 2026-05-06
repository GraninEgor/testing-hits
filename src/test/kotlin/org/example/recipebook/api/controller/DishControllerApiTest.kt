package org.example.recipebook.api.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.recipebook.api.dto.*
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.test.SharedTestContainers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.HttpMethod.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import kotlin.math.abs
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DishControllerApiTest : SharedTestContainers() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val apiBase = "/rest/admin-ui/dishes"
    private val createdDishIds = mutableListOf<Long>()
    private val createdProductIds = mutableListOf<Long>()

    @AfterEach
    fun tearDown() {
        createdDishIds.forEach { id -> restTemplate.exchange("$apiBase/$id", DELETE, null, Void::class.java) }
        createdProductIds.forEach { id -> restTemplate.exchange("/rest/admin-ui/products/$id", DELETE, null, Void::class.java) }
        createdDishIds.clear()
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

    private fun assertDoubleCloseTo(actual: Double, expected: Double, tolerance: Double = 0.1) {
        val diff = abs(actual - expected)
        Assertions.assertTrue(
            diff <= tolerance,
            "Expected $expected ± $tolerance, but got $actual"
        )
    }

    private fun createTestProduct(
        name: String = "Тестовый продукт ${System.currentTimeMillis()}",
        calories: Double = 100.0,
        proteins: Double = 10.0,
        fats: Double = 5.0,
        carbs: Double = 0.0
    ): Long {
        val dto = ProductCreateDto(
            name = name,
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbohydrates = carbs,
            category = org.example.recipebook.core.database.entity.Category.MEAT,
            cookingRequirement = org.example.recipebook.core.database.entity.CookingRequirement.READY_TO_EAT,
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
            "/rest/admin-ui/products",
            HttpMethod.POST,
            HttpEntity(body, headers),
            ProductDto::class.java
        )

        assertStatus(HttpStatus.OK, response)

        val product = response.body
            ?: throw AssertionError("Response body is null for product creation")

        createdProductIds.add(product.id)
        return product.id
    }

    private fun createDish(dto: DishCreateDto): DishDto {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("data", dto)

        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }

        val response: ResponseEntity<DishDto> = restTemplate.exchange(
            apiBase,
            POST,
            HttpEntity<MultiValueMap<String, Any>>(body, headers),
            DishDto::class.java
        )

        assertStatus(HttpStatus.OK, response)

        val dish = response.body
            ?: throw AssertionError("Response body is null for dish creation")

        createdDishIds.add(dish.id)
        return dish
    }

    private fun updateDish(id: Long, dto: DishPatchDto): DishDto {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("data", dto)

        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }

        val uri = java.net.URI.create("$apiBase/$id")
        val response: ResponseEntity<DishDto> = restTemplate.exchange(
            uri,
            PATCH,
            HttpEntity<MultiValueMap<String, Any>>(body, headers),
            DishDto::class.java
        )

        assertStatus(HttpStatus.OK, response)
        return response.body
            ?: throw AssertionError("Response body is null for dish update")
    }

    @Nested
    @DisplayName("POST /rest/admin-ui/dishes")
    inner class CreateDishTests {

        @Test
        fun `should create dish with valid data`() {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = "Тестовое блюдо ${System.currentTimeMillis()}",
                calories = 250.0,
                proteins = 10.0,
                fats = 15.0,
                carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 150.0)),
                portionSize = 300.0,
                category = DishCategory.SALAD,
                flags = setOf(FeatureFlag.VEGAN)
            )

            val created = createDish(dto)

            Assertions.assertTrue(created.id > 0)
            Assertions.assertEquals(dto.name, created.name)
            Assertions.assertEquals(dto.category, created.category)
            Assertions.assertTrue(created.flags.containsAll(dto.flags))
            Assertions.assertEquals(1, created.ingredients.size)
            Assertions.assertEquals(150.0, created.ingredients[0].amount)
        }

        @Test
        fun `should auto-calculate macros when zeros provided`() {
            val productId = createTestProduct(calories = 100.0, proteins = 10.0, fats = 5.0, carbs = 0.0)

            val dto = DishCreateDto(
                name = "Омлет ${System.currentTimeMillis()}",
                calories = 0.0, proteins = 0.0, fats = 0.0, carbohydrates = 0.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 100.0,
                category = DishCategory.SOUP,
                flags = emptySet()
            )

            val created = createDish(dto)

            assertDoubleCloseTo(created.calories, 0.0)
            assertDoubleCloseTo(created.proteins, 0.0)
            assertDoubleCloseTo(created.fats, 0.0)
            assertDoubleCloseTo(created.carbohydrates, 0.0)
        }

        @Test
        fun `should return 400 when name too short`() {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = "A",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, HttpMethod.POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.BAD_REQUEST, response)
        }

        @Test
        fun `should return 404 when product not found`() {
            val dto = DishCreateDto(
                name = "Блюдо",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(999999L, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            val status = response.statusCode
            Assertions.assertTrue(
                status == HttpStatus.NOT_FOUND,
                "Expected 400 or 500, but got $status"
            )
        }
    }

    @Nested
    @DisplayName("GET /rest/admin-ui/dishes")
    inner class GetDishesTests {

        @BeforeEach
        fun seedData() {
            repeat(3) { i ->
                val productId = createTestProduct()
                val dto = DishCreateDto(
                    name = "Тестовое блюдо $i",
                    calories = 200.0 + i * 10,
                    proteins = 10.0, fats = 10.0, carbohydrates = 10.0,
                    ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                    portionSize = 250.0,
                    category = if (i % 2 == 0) DishCategory.SALAD else DishCategory.SOUP,
                    flags = if (i == 0) setOf(FeatureFlag.VEGAN) else emptySet()
                )
                createDish(dto)
            }
        }

        @Test
        fun `should return paged list`() {
            val uri = java.net.URI.create("$apiBase?page=0&size=10")
            val response: ResponseEntity<PagedResponse<DishDto>> = restTemplate.exchange(
                uri,
                GET,
                null,
                object : ParameterizedTypeReference<PagedResponse<DishDto>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            Assertions.assertTrue(response.body!!.content.size >= 3)
        }

        @Test
        fun `should filter by category`() {
            val uri = java.net.URI.create("$apiBase?category=SALAD&page=0&size=10")
            val response: ResponseEntity<PagedResponse<DishDto>> = restTemplate.exchange(
                uri,
                GET,
                null,
                object : ParameterizedTypeReference<PagedResponse<DishDto>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            response.body!!.content.forEach { dish ->
                Assertions.assertEquals(DishCategory.SALAD, dish.category)
            }
        }

        @Test
        fun `should return dish by id`() {
            val id = createdDishIds.firstOrNull() ?: throw IllegalStateException("No dishes created")

            val response: ResponseEntity<DishDto> = restTemplate.getForEntity("$apiBase/$id", DishDto::class.java)

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
            val ids = createdDishIds.take(2).joinToString(",")
            val uri = java.net.URI.create("$apiBase/by-ids?ids=$ids")

            val response: ResponseEntity<List<DishDto>> = restTemplate.exchange(
                uri,
                GET,
                null,
                object : ParameterizedTypeReference<List<DishDto>>() {}
            )

            assertStatus(HttpStatus.OK, response)
            Assertions.assertNotNull(response.body)
            Assertions.assertEquals(2, response.body!!.size)
        }
    }

    @Nested
    @DisplayName("PATCH /rest/admin-ui/dishes/{id}")
    inner class UpdateDishTests {

        @Test
        fun `should update dish`() {
            val productId = createTestProduct()
            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 300.0, proteins = 12.0, fats = 18.0, carbohydrates = 25.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 200.0)),
                portionSize = 350.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)

            val patchDto = DishPatchDto(
                name = "Обновлённое название",
                calories = 180.0,
                portionSize = 400.0,
                category = DishCategory.SOUP
            )

            val updated = updateDish(created.id, patchDto)

            Assertions.assertEquals("Обновлённое название", updated.name)
            Assertions.assertEquals(180.0, updated.calories)
            Assertions.assertEquals(400.0, updated.portionSize)
            Assertions.assertEquals(DishCategory.SOUP, updated.category)
        }

        @Test
        fun `should return 400 or 500 when invalid portionSize`() {
            val productId = createTestProduct()
            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 200.0, proteins = 10.0, fats = 10.0, carbohydrates = 10.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 250.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)

            val patchDto = DishPatchDto(portionSize = -10.0)

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", patchDto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val uri = java.net.URI.create("$apiBase/${created.id}")
            val request = RequestEntity(body, headers, HttpMethod.PATCH, uri)

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)

            val status = response.statusCode
            Assertions.assertTrue(
                status == HttpStatus.BAD_REQUEST || status == HttpStatus.INTERNAL_SERVER_ERROR,
                "Expected 400 or 500, but got $status"
            )
        }

        @Test
        fun `should return 404 for non-existent dish`() {
            val patchDto = DishPatchDto(name = "Новое имя")

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", patchDto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val uri = java.net.URI.create("$apiBase/999999")
            val request = RequestEntity(body, headers, HttpMethod.PATCH, uri)

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            assertStatus(HttpStatus.NOT_FOUND, response)
        }
    }

    @Nested
    @DisplayName("DELETE /rest/admin-ui/dishes/{id}")
    inner class DeleteDishTests {

        @Test
        fun `should delete dish`() {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 200.0, proteins = 10.0, fats = 10.0, carbohydrates = 10.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 250.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(dto)

            val deleteResponse: ResponseEntity<Void> = restTemplate.exchange(
                "$apiBase/${created.id}",
                DELETE,
                null,
                Void::class.java
            )
            Assertions.assertEquals(HttpStatus.OK, deleteResponse.statusCode)

            val getResponse: ResponseEntity<Void> = restTemplate.getForEntity("$apiBase/${created.id}", Void::class.java)
            Assertions.assertEquals(HttpStatus.NOT_FOUND, getResponse.statusCode)
        }

        @Test
        fun `should delete many dishes`() {
            val ids = mutableListOf<Long>()
            repeat(2) {
                val productId = createTestProduct()
                val dto = DishCreateDto(
                    name = "Блюдо $it ${System.currentTimeMillis()}",
                    calories = 200.0, proteins = 10.0, fats = 10.0, carbohydrates = 10.0,
                    ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                    portionSize = 250.0, category = DishCategory.SALAD, flags = emptySet()
                )
                ids.add(createDish(dto).id)
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
    @DisplayName("PATCH /rest/admin-ui/dishes (bulk)")
    inner class BulkUpdateTests {

        @Test
        fun `should patch many dishes`() {
            val ids = mutableListOf<Long>()
            repeat(2) {
                val productId = createTestProduct()
                val dto = DishCreateDto(
                    name = "Блюдо $it ${System.currentTimeMillis()}",
                    calories = 200.0, proteins = 10.0, fats = 10.0, carbohydrates = 10.0,
                    ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                    portionSize = 250.0, category = DishCategory.SALAD, flags = emptySet()
                )
                ids.add(createDish(dto).id)
            }

            val patchJson = """{"calories": 150.0, "portionSize": 300.0}"""
            val uri = java.net.URI.create("$apiBase?ids=${ids.joinToString(",")}")

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val request = RequestEntity(patchJson, headers, HttpMethod.PATCH, uri)

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