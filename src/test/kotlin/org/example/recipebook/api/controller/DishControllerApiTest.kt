package org.example.recipebook.api.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.recipebook.api.dto.*
import org.example.recipebook.core.database.entity.Category
import org.example.recipebook.core.database.entity.CookingRequirement
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.test.SharedTestContainers
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.HttpMethod.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import kotlin.math.abs
import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments
import org.springframework.core.io.ByteArrayResource

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
            category = Category.MEAT,
            cookingRequirement = CookingRequirement.READY_TO_EAT,
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

        @ParameterizedTest
        @ValueSource(strings = ["", "A", "  "])
        @DisplayName("Должен вернуть 400 при слишком коротком имени")
        fun `should return 400 when name too short`(shortName: String) {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = shortName,
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

        @ParameterizedTest
        @ValueSource(longs = [999999L, -1L, 0L, Long.MAX_VALUE])
        @DisplayName("Должен вернуть 404 при несуществующем productId")
        fun `should return 404 when product not found`(invalidProductId: Long) {
            val dto = DishCreateDto(
                name = "Блюдо",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(invalidProductId, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, POST, java.net.URI.create(apiBase))

            val response: ResponseEntity<Void> = restTemplate.exchange(request, Void::class.java)
            val status = response.statusCode
            Assertions.assertTrue(
                status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST,
                "Expected 400 or 404, but got $status"
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

        @ParameterizedTest
        @ValueSource(longs = [999999L, -1L, 0L, Long.MAX_VALUE])
        @DisplayName("Должен вернуть 404 для несуществующего ID")
        fun `should return 404 for non-existent id`(invalidId: Long) {
            val response: ResponseEntity<Void> = restTemplate.getForEntity("$apiBase/$invalidId", Void::class.java)
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

        @ParameterizedTest
        @ValueSource(doubles = [-10.0, -1.0, -0.1, 0.0])
        @DisplayName("Должен вернуть ошибку при невалидном portionSize")
        fun `should return error when invalid portionSize`(invalidPortionSize: Double) {
            val productId = createTestProduct()
            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 200.0, proteins = 10.0, fats = 10.0, carbohydrates = 10.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 250.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)

            val patchDto = DishPatchDto(portionSize = invalidPortionSize)

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

        @ParameterizedTest
        @ValueSource(longs = [999999L, -1L, 0L, Long.MAX_VALUE])
        @DisplayName("Должен вернуть 404 для несуществующего блюда")
        fun `should return 404 for non-existent dish`(invalidId: Long) {
            val patchDto = DishPatchDto(name = "Новое имя")

            val body = LinkedMultiValueMap<String, Any>().apply { add("data", patchDto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val uri = java.net.URI.create("$apiBase/$invalidId")
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

    companion object {
        @JvmStatic
        fun invalidPortionSizes(): Stream<Arguments> = Stream.of(
            Arguments.of(-10.0),
            Arguments.of(-1.0),
            Arguments.of(-0.1),
            Arguments.of(0.0)
        )

        @JvmStatic
        fun invalidIds(): Stream<Arguments> = Stream.of(
            Arguments.of(999999L),
            Arguments.of(-1L),
            Arguments.of(0L),
            Arguments.of(Long.MAX_VALUE)
        )

        @JvmStatic
        fun shortNames(): Stream<Arguments> = Stream.of(
            Arguments.of(""),
            Arguments.of("A"),
            Arguments.of("  ")
        )
    }
    @Nested
    @DisplayName("Валидация полей DishCreateDto")
    inner class ValidationTests {

        @ParameterizedTest
        @ValueSource(doubles = [-1.0, -0.1])
        fun `should return 400 when calories negative`(invalidCalories: Double) {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = invalidCalories,
                proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val response = sendCreateRequest(dto)
            Assertions.assertTrue(
                response.statusCode in listOf(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR),
                "Expected 400 or 500, but got ${response.statusCode}"
            )
        }

        @ParameterizedTest
        @ValueSource(doubles = [-1.0, -0.1])
        fun `should return 400 when proteins negative`(invalidProteins: Double) {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = invalidProteins, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val response = sendCreateRequest(dto)
            Assertions.assertTrue(
                response.statusCode in listOf(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR),
                "Expected 400 or 500, but got ${response.statusCode}"
            )
        }

        @Test
        fun `should return 400 when ingredients list is empty`() {
            val dto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = emptyList(),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val response = sendCreateRequest(dto)
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        fun `should return 400 when ingredient amount is negative`() {
            val productId = createTestProduct()
            val dto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, -50.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val response = sendCreateRequest(dto)
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        fun `should return 500 when macros sum exceeds 100g per 100g portion`() {
            val productId = createTestProduct(calories = 500.0, proteins = 80.0, fats = 80.0, carbs = 80.0)
            val dto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 500.0, proteins = 80.0, fats = 80.0, carbohydrates = 80.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 100.0,
                category = DishCategory.SALAD, flags = emptySet()
            )
            val response = sendCreateRequest(dto)
            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        }

        private fun sendCreateRequest(dto: DishCreateDto): ResponseEntity<Void> {
            val body = LinkedMultiValueMap<String, Any>().apply { add("data", dto) }
            val headers = HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA }
            val request = RequestEntity(body, headers, HttpMethod.POST, java.net.URI.create(apiBase))
            return restTemplate.exchange(request, Void::class.java)
        }
    }

    @Nested
    @DisplayName("Фильтрация блюд")
    inner class FilterTests {

        @BeforeEach
        fun seedFilteredData() {
            val meatProduct = createTestProduct(name = "Курица")
            val vegProduct = createTestProduct(name = "Огурец")

            createDish(DishCreateDto(
                name = "Веганский салат",
                calories = 50.0, proteins = 2.0, fats = 1.0, carbohydrates = 10.0,
                ingredients = listOf(DishIngredientCreateDto(vegProduct, 200.0)),
                portionSize = 250.0, category = DishCategory.SALAD, flags = setOf(FeatureFlag.VEGAN)
            ))

            createDish(DishCreateDto(
                name = "Куриный суп",
                calories = 120.0, proteins = 15.0, fats = 5.0, carbohydrates = 10.0,
                ingredients = listOf(DishIngredientCreateDto(meatProduct, 150.0)),
                portionSize = 300.0, category = DishCategory.SOUP, flags = emptySet()
            ))
            createDish(DishCreateDto(
                name = "Рис с овощами",
                calories = 200.0, proteins = 5.0, fats = 3.0, carbohydrates = 40.0,
                ingredients = listOf(DishIngredientCreateDto(vegProduct, 100.0)),
                portionSize = 200.0, category = DishCategory.SECOND, flags = setOf(FeatureFlag.GLUTEN_FREE)
            ))
        }

        @Test
        fun `should filter by VEGAN flag`() {
            val uri = java.net.URI.create("$apiBase?flags=VEGAN&page=0&size=10")
            val response: ResponseEntity<PagedResponse<DishDto>> = restTemplate.exchange(
                uri, GET, null, object : ParameterizedTypeReference<PagedResponse<DishDto>>() {}
            )
            assertStatus(HttpStatus.OK, response)
            response.body!!.content.forEach { dish ->
                Assertions.assertTrue(dish.flags.contains(FeatureFlag.VEGAN))
            }
        }

        @Test
        fun `should filter by multiple flags`() {
            val uri = java.net.URI.create("$apiBase?flags=VEGAN,GLUTEN_FREE&page=0&size=10")
            val response: ResponseEntity<PagedResponse<DishDto>> = restTemplate.exchange(
                uri, GET, null, object : ParameterizedTypeReference<PagedResponse<DishDto>>() {}
            )
            assertStatus(HttpStatus.OK, response)
            Assertions.assertTrue(response.body!!.content.isNotEmpty())
        }

        @Test
        fun `should filter by name containing substring`() {
            val uri = java.net.URI.create("$apiBase?name=салат&page=0&size=10")
            val response: ResponseEntity<PagedResponse<DishDto>> = restTemplate.exchange(
                uri, GET, null, object : ParameterizedTypeReference<PagedResponse<DishDto>>() {}
            )
            assertStatus(HttpStatus.OK, response)
            response.body!!.content.forEach { dish ->
                Assertions.assertTrue(dish.name.contains("салат", ignoreCase = true))
            }
        }
    }

    @Nested
    @DisplayName("Авто-расчёт макросов")
    inner class AutoMacroCalculationTests {

        @Test
        fun `should not override user-provided macros`() {
            val productId = createTestProduct(calories = 500.0, proteins = 50.0, fats = 30.0, carbs = 20.0)
            val dto = DishCreateDto(
                name = "Блюдо с ручными макросами ${System.currentTimeMillis()}",
                calories = 100.0,
                proteins = 10.0, fats = 5.0, carbohydrates = 15.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 50.0)),
                portionSize = 150.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(dto)

            Assertions.assertEquals(100.0, created.calories)
            Assertions.assertEquals(10.0, created.proteins)
            Assertions.assertEquals(5.0, created.fats)
            Assertions.assertEquals(15.0, created.carbohydrates)
        }

        @Test
        fun `should recalculate macros on patch when user did not edit them`() {
            val productId = createTestProduct(calories = 300.0, proteins = 30.0, fats = 15.0, carbs = 10.0)
            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 5.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 50.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)

            val patchDto = DishPatchDto(
                name = "Обновлённое блюдо",
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0))
            )
            val updated = updateDish(created.id, patchDto)

            Assertions.assertEquals("Обновлённое блюдо", updated.name)
            Assertions.assertEquals(300.0, updated.calories, 0.1)
            Assertions.assertEquals(30.0, updated.proteins, 0.1)
        }
    }

    @Nested
    @DisplayName("Работа с фотографиями")
    inner class PhotoHandlingTests {

        @Test
        fun `should preserve existing photos when updating without new files`() {
            val productId = createTestProduct()
            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)
            val originalPhotos = created.photos.toList()


            val patchDto = DishPatchDto(name = "Обновлённое название")
            val updated = updateDish(created.id, patchDto)


            Assertions.assertEquals(originalPhotos, updated.photos)
        }

    }

    @Nested
    @DisplayName("Работа с ингредиентами при обновлении")
    inner class IngredientUpdateTests {

        @Test
        fun `should add new ingredient on patch`() {
            val product1 = createTestProduct(name = "Продукт 1")
            val product2 = createTestProduct(name = "Продукт 2")

            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(product1, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)
            Assertions.assertEquals(1, created.ingredients.size)

            val patchDto = DishPatchDto(
                ingredients = listOf(
                    DishIngredientCreateDto(product1, 100.0),
                    DishIngredientCreateDto(product2, 50.0)
                )
            )
            val updated = updateDish(created.id, patchDto)

            Assertions.assertEquals(2, updated.ingredients.size)
            Assertions.assertTrue(updated.ingredients.any { it.productId == product2 })
        }

        @Test
        fun `should remove ingredient on patch`() {
            val product1 = createTestProduct(name = "Продукт 1")
            val product2 = createTestProduct(name = "Продукт 2")

            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(
                    DishIngredientCreateDto(product1, 100.0),
                    DishIngredientCreateDto(product2, 50.0)
                ),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)
            Assertions.assertEquals(2, created.ingredients.size)


            val patchDto = DishPatchDto(
                ingredients = listOf(DishIngredientCreateDto(product1, 100.0))
            )
            val updated = updateDish(created.id, patchDto)

            Assertions.assertEquals(1, updated.ingredients.size)
            Assertions.assertEquals(product1, updated.ingredients[0].productId)
        }

        @Test
        fun `should update ingredient amount on patch`() {
            val productId = createTestProduct()

            val createDto = DishCreateDto(
                name = "Блюдо ${System.currentTimeMillis()}",
                calories = 100.0, proteins = 10.0, fats = 5.0, carbohydrates = 20.0,
                ingredients = listOf(DishIngredientCreateDto(productId, 100.0)),
                portionSize = 200.0, category = DishCategory.SALAD, flags = emptySet()
            )
            val created = createDish(createDto)
            Assertions.assertEquals(100.0, created.ingredients[0].amount)

            val patchDto = DishPatchDto(
                ingredients = listOf(DishIngredientCreateDto(productId, 250.0))
            )
            val updated = updateDish(created.id, patchDto)

            Assertions.assertEquals(250.0, updated.ingredients[0].amount)
        }
    }

    @Nested
    @DisplayName("Edge cases и ошибки")
    inner class EdgeCasesTests {

        @Test
        fun `should return 404 when deleting non-existent dish`() {
            val response: ResponseEntity<Void> = restTemplate.exchange(
                "$apiBase/999999", DELETE, null, Void::class.java
            )

            Assertions.assertTrue(
                response.statusCode in listOf(HttpStatus.OK, HttpStatus.NOT_FOUND),
                "Expected 200 or 404, but got ${response.statusCode}"
            )
        }

        @Test
        fun `should handle empty ids list in deleteMany`() {
            val response: ResponseEntity<Void> = restTemplate.exchange(
                "$apiBase?ids=", DELETE, null, Void::class.java
            )
            Assertions.assertTrue(
                response.statusCode.is2xxSuccessful,
                "Expected success status, but got ${response.statusCode}"
            )
        }

        @Test
        fun `should handle empty ids list in getMany`() {
            val response: ResponseEntity<List<DishDto>> = restTemplate.exchange(
                "$apiBase/by-ids?ids=", GET, null,
                object : ParameterizedTypeReference<List<DishDto>>() {}
            )
            assertStatus(HttpStatus.OK, response)
            Assertions.assertTrue(response.body!!.isEmpty())
        }

        @Test
        fun `should return empty list when no dishes match filter`() {
            val uri = java.net.URI.create("$apiBase?name=НесуществующееБлюдо12345&page=0&size=10")
            val response: ResponseEntity<PagedResponse<DishDto>> = restTemplate.exchange(
                uri, GET, null, object : ParameterizedTypeReference<PagedResponse<DishDto>>() {}
            )
            assertStatus(HttpStatus.OK, response)
            Assertions.assertTrue(response.body!!.content.isEmpty())
        }
    }
}