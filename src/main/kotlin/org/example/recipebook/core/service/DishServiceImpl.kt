package org.example.recipebook.core.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.api.dto.DishCreateDto
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.api.dto.DishIngredientCreateDto
import org.example.recipebook.api.dto.DishPatchDto
import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.core.database.repository.DishIngredientRepository
import org.example.recipebook.core.database.repository.DishRepository
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.exception.DishNotFoundException
import org.example.recipebook.core.exception.ProductNotFoundException
import org.example.recipebook.core.filter.DishFilter
import org.example.recipebook.core.mapper.toDishDto
import org.example.recipebook.core.mapper.toEntity
import org.example.recipebook.core.mapper.updateWithNull
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val productRepository: ProductRepository,
    private val dishIngredientRepository: DishIngredientRepository,
    @Value("\${app.upload-dir:uploads/}") private val uploadDir: String
) : DishService {
    override fun getAll(filter: DishFilter, pageable: Pageable): Page<DishDto> {
        val spec: Specification<Dish> = filter.toSpecification()
        return dishRepository.findAll(spec, pageable).map(Dish::toDishDto)
    }

    override fun getOne(id: Long): DishDto =
        dishRepository.findById(id)
            .orElseThrow { DishNotFoundException(id) }
            .toDishDto()

    override fun getMany(ids: List<Long>): List<DishDto> =
        dishRepository.findAllById(ids).map(Dish::toDishDto)

    override fun create(dto: DishCreateDto, files: List<MultipartFile>?): DishDto {
        val photoUrls = files?.map { saveFile(it) } ?: emptyList()

        val dish = dto.toEntity().apply {
            if (photoUrls.isNotEmpty()) photos = photoUrls.toMutableList()
        }

        dish.ingredients = dto.ingredients.map { ing ->
            val product = productRepository.findById(ing.productId)
                .orElseThrow { ProductNotFoundException(ing.productId) }
            DishIngredient(dish = dish, product = product, amount = ing.amount)
        }.toMutableList()

        if (dto.calories == null || dto.proteins == null ||
            dto.fats == null || dto.carbohydrates == null) {
            val macros = calculateCalories(dto.ingredients)
            dish.calories = dto.calories ?: macros.calories
            dish.proteins = dto.proteins ?: macros.protein
            dish.fats = dto.fats ?: macros.fat
            dish.carbohydrates = dto.carbohydrates ?: macros.carbs
        }

        validate(dish)
        return dishRepository.save(dish).toDishDto()
    }

    override fun patch(id: Long, dto: DishPatchDto, files: List<MultipartFile>?): DishDto {
        val dish = dishRepository.findById(id)
            .orElseThrow { DishNotFoundException(id) }

        dto.name?.let { dish.name = it }
        dto.portionSize?.let { dish.portionSize = it }
        dto.category?.let { dish.category = it }
        dto.calories?.let { dish.calories = it }
        dto.proteins?.let { dish.proteins = it }
        dto.fats?.let { dish.fats = it }
        dto.carbohydrates?.let { dish.carbohydrates = it }

        val currentPhotos = dish.photos.toList()
        val photosToKeep = dto.photos?.filter { it in currentPhotos } ?: currentPhotos
        val newPhotoUrls = files?.mapNotNull { saveFileOrNull(it) } ?: emptyList()
        dish.photos = (photosToKeep + newPhotoUrls).distinct().toMutableList()

        dto.flags?.let { dish.flags = it.toMutableSet() }

        dto.ingredients?.let { newIngredients ->
            val newProductIds = newIngredients.map { it.productId }.toSet()
            val orphans = dish.ingredients.filter { it.product.id !in newProductIds }
            if (orphans.isNotEmpty()) {
                dishIngredientRepository.deleteAll(orphans)
                dish.ingredients.removeAll(orphans)
            }
            newIngredients.forEach { ing ->
                val existing = dish.ingredients.find { it.product.id == ing.productId }
                if (existing != null) {
                    existing.amount = ing.amount
                } else {
                    val product = productRepository.findById(ing.productId)
                        .orElseThrow { ProductNotFoundException(ing.productId) }
                    dish.ingredients.add(DishIngredient(dish = dish, product = product, amount = ing.amount))
                }
            }
        }

        val userEditedMacros = dto.calories != null || dto.proteins != null ||
                dto.fats != null || dto.carbohydrates != null
        if (!userEditedMacros && dto.ingredients != null) {
            val macros = calculateCalories(dto.ingredients)
            dish.calories = macros.calories
            dish.proteins = macros.protein
            dish.fats = macros.fat
            dish.carbohydrates = macros.carbs
        }

        validate(dish)
        return dishRepository.save(dish).toDishDto()
    }

    @Throws(IOException::class)
    override fun patchMany(ids: List<Long>, patchNode: JsonNode): List<Long> {
        val dishes = dishRepository.findAllById(ids)
        dishes.forEach { dish ->
            val dishDto = dish.toDishDto()
            objectMapper.readerForUpdating(dishDto).readValue<DishDto>(patchNode)
            dish.updateWithNull(dishDto)
        }
        return dishRepository.saveAll(dishes).map { it.id }
    }

    @Transactional
    override fun delete(id: Long): DishDto? {
        val dish = dishRepository.findById(id).orElse(null) ?: return null
        Hibernate.initialize(dish.ingredients)
        Hibernate.initialize(dish.photos)
        Hibernate.initialize(dish.flags)
        val dto = dish.toDishDto()
        dishRepository.delete(dish)
        return dto
    }

    override fun deleteMany(ids: List<Long>) = dishRepository.deleteAllById(ids)


    fun calculateCalories(ingredients: List<DishIngredientCreateDto>): CalculatedMacros {
        if (ingredients.isEmpty()) {
            return CalculatedMacros(0.0, 0.0, 0.0, 0.0)
        }

        var calories = 0.0
        var protein = 0.0
        var fat = 0.0
        var carbs = 0.0

        for (ing in ingredients) {
            val product = productRepository.findById(ing.productId)
                .orElseThrow { ProductNotFoundException(ing.productId) }

            val coefficient = ing.amount / 100.0
            calories += product.calories * coefficient
            protein += product.proteins * coefficient
            fat += product.fats * coefficient
            carbs += product.carbohydrates * coefficient
        }

        return CalculatedMacros(calories, protein, fat, carbs)
    }

    private fun saveFile(file: MultipartFile): String {
        val fileName = "${UUID.randomUUID()}_${file.originalFilename}"
        val path = Paths.get(uploadDir, fileName)
        Files.createDirectories(path.parent)
        file.transferTo(path.toFile())
        return "/$uploadDir$fileName"
    }

    private fun saveFileOrNull(file: MultipartFile): String? = try {
        saveFile(file)
    } catch (e: Exception) {
        null
    }

    private fun validate(dish: Dish) {
        val totalMacros = dish.proteins + dish.fats + dish.carbohydrates
        val totalPer100g = if (dish.portionSize > 0) {
            totalMacros / dish.portionSize * 100
        } else {
            totalMacros
        }
        if (totalPer100g > 100.0 + 1e-9) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Сумма БЖУ на 100 г блюда не может превышать 100 г (сейчас: %.1f г)".format(totalPer100g)
            )
        }
    }
}

data class CalculatedMacros(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)