package org.example.recipebook.core.service;

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.api.dto.DishCreateDto
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.filter.DishFilter
import org.example.recipebook.core.database.repository.DishRepository
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.mapper.toDishDto
import org.example.recipebook.core.mapper.toEntity
import org.example.recipebook.core.mapper.updateWithNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Optional
import java.util.UUID

@Service
class DishServiceImpl(
    private val dishRepository: DishRepository,
    private val objectMapper: ObjectMapper,
    private val productRepository: ProductRepository
) :
    DishService {
    override fun getAll(filter: DishFilter, pageable: Pageable): Page<DishDto> {
        val spec: Specification<Dish> = filter.toSpecification()
        val dishes: Page<Dish> = dishRepository.findAll(spec, pageable)
        return dishes.map(Dish::toDishDto)
    }

    override fun getOne(id: Long): DishDto {
        val dishOptional: Optional<Dish> = dishRepository.findById(id)
        return dishOptional.orElse(null).toDishDto()
    }

    override fun getMany(ids: List<Long>): List<DishDto> {
        val dishes: List<Dish> = dishRepository.findAllById(ids)
        return dishes.map(Dish::toDishDto)
    }

    override fun create(dto: DishCreateDto, file: List<MultipartFile>?): DishDto {

        val photoUrls: List<String> = file?.map { f ->
            val uploadDir = "uploads/"
            val fileName = UUID.randomUUID().toString() + "_" + f.originalFilename

            val path = Paths.get(uploadDir + fileName)
            Files.createDirectories(path.parent)
            f.transferTo(path)

            "/uploads/$fileName"
        } ?: emptyList()

        val dish = dto.toEntity().apply {
            if (photoUrls.isNotEmpty()) {
                this.photos = photoUrls
            }
        }

        val ingredients = dto.ingredients.map { ing ->
            val product = productRepository.findById(ing.productId)
                .orElseThrow()

            DishIngredient(
                dish = dish,
                product = product,
                amount = ing.amount
            )
        }

        dish.ingredients = ingredients

        val saved = dishRepository.save(dish)

        return saved.toDishDto()
    }

    override fun patch(id: Long, patchNode: JsonNode): DishDto {

        val dish = dishRepository.findById(id)
            .orElseThrow()

        val dto = dish.toDishDto()

        val updated = objectMapper.readerForUpdating(dto)
            .readValue<DishDto>(patchNode)

        // ===== базовые поля =====
        dish.name = updated.name
        dish.photos = updated.photos
        dish.category = updated.category
        dish.portionSize = updated.portionSize
        dish.flags = updated.flags

        // ===== ингредиенты =====
        if (patchNode.has("ingredients")) {

            dish.ingredients = updated.ingredients.map { ing ->
                val product = productRepository.findById(ing.productId)
                    .orElseThrow()

                DishIngredient(
                    dish = dish,
                    product = product,
                    amount = ing.amount
                )
            }
        }

        recalc(dish)
        validate(dish)

        return dishRepository.save(dish).toDishDto()
    }

    private fun recalc(dish: Dish) {

        var c = 0.0
        var p = 0.0
        var f = 0.0
        var u = 0.0

        dish.ingredients.forEach {
            val k = it.amount / 100.0
            val pr = it.product

            c += pr.calories * k
            p += pr.proteins * k
            f += pr.fats * k
            u += pr.carbohydrates * k
        }

        dish.calories = c
        dish.proteins = p
        dish.fats = f
        dish.carbohydrates = u
    }

    private fun validate(dish: Dish) {
        val sum = dish.proteins + dish.fats + dish.carbohydrates

        if (sum > 100.0) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "BJU > 100 per 100g"
            )
        }
    }

    @Throws(IOException::class)
    override fun patchMany(ids: List<Long>, patchNode: JsonNode): List<Long> {
        val dishes: Collection<Dish> = dishRepository.findAllById(ids)
        for (dish in dishes) {
            val dishDto = dish.toDishDto()
            objectMapper.readerForUpdating(dishDto).readValue<DishDto>(patchNode)
            dish.updateWithNull(dishDto)
        }
        val resultDishes: List<Dish> = dishRepository.saveAll(dishes)
        return resultDishes.map(Dish::id)
    }

    override fun delete(id: Long): DishDto? {
        val dish: Dish? = dishRepository.findById(id).orElse(null)
        if (dish != null) {
            dishRepository.delete(dish)
        }
        return dish?.toDishDto()
    }

    override fun deleteMany(ids: List<Long>) = dishRepository.deleteAllById(ids)
}