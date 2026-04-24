package org.example.recipebook

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.api.dto.ProductCreateDto
import org.example.recipebook.core.database.entity.*
import org.example.recipebook.core.database.repository.DishRepository
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.service.ProductServiceImpl
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductServiceImpl tests fixed")
class ProductServiceImplTest {

    @Mock lateinit var productRepository: ProductRepository
    @Mock lateinit var dishRepository: DishRepository
    @Mock lateinit var objectMapper: ObjectMapper

    @InjectMocks lateinit var service: ProductServiceImpl

    private fun productEntity() = Product(
        name = "product",
        calories = 100.0,
        proteins = 10.0,
        fats = 10.0,
        carbohydrates = 10.0,
        category = Category.MEAT,
        cookingRequirement = CookingRequirement.READY_TO_EAT
    )

    private fun dto(p: Double, f: Double, c: Double) =
        ProductCreateDto(
            name = "test",
            calories = 100.0,
            proteins = p,
            fats = f,
            carbohydrates = c,
            composition = null,
            category = Category.MEAT,
            cookingRequirement = CookingRequirement.READY_TO_EAT
        )


    @ParameterizedTest
    @CsvSource(
        "10,10,10,true",
        "50,40,10,true",
        "50,50,10,false",
        "0,0,0,true"
    )
    fun `patch BJU equivalence`(p: Double, f: Double, c: Double, ok: Boolean) {

        val entity = productEntity()

        whenever(productRepository.findById(1L))
            .thenReturn(Optional.of(entity))

        if (ok) {
            whenever(productRepository.save(any<Product>()))
                .thenAnswer { it.arguments[0] }
        }

        if (ok) {
            assertDoesNotThrow {
                service.patch(1L, dto(p, f, c), null)
            }
        } else {
            assertThrows(Exception::class.java) {
                service.patch(1L, dto(p, f, c), null)
            }
        }
    }

    @Test
    fun `delete used product throws`() {

        val entity = productEntity()

        `when`(productRepository.findById(1L))
            .thenReturn(Optional.of(entity))

        `when`(dishRepository.existsByIngredientsProductId(1L))
            .thenReturn(true)

        `when`(dishRepository.findAll())
            .thenReturn(emptyList())

        assertThrows(IllegalStateException::class.java) {
            service.delete(1L)
        }
    }

    @Test
    fun `delete unused product success`() {

        val entity = productEntity()

        `when`(productRepository.findById(1L))
            .thenReturn(Optional.of(entity))

        `when`(dishRepository.existsByIngredientsProductId(1L))
            .thenReturn(false)

        val result = service.delete(1L)

        Assertions.assertNotNull(result)
        verify(productRepository).delete(entity)
    }
}