package org.example.recipebook.core.service;

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.api.dto.DishCreateDto
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.api.dto.DishPatchDto
import org.example.recipebook.core.database.entity.Dish
import org.example.recipebook.core.database.entity.DishCategory
import org.example.recipebook.core.database.entity.DishIngredient
import org.example.recipebook.core.database.entity.FeatureFlag
import org.example.recipebook.core.database.repository.DishIngredientRepository
import org.example.recipebook.core.filter.DishFilter
import org.example.recipebook.core.database.repository.DishRepository
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.mapper.toDishDto
import org.example.recipebook.core.mapper.toEntity
import org.example.recipebook.core.mapper.updateWithNull
import org.hibernate.Hibernate
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
    private val dishIngredientRepository: DishIngredientRepository
) : DishService {

    override fun getAll(filter: DishFilter, pageable: Pageable): Page<DishDto> {
        val spec: Specification<Dish> = filter.toSpecification()
        return dishRepository.findAll(spec, pageable).map(Dish::toDishDto)
    }

    override fun getOne(id: Long): DishDto =
        dishRepository.findById(id)
            .orElseThrow()
            .toDishDto()

    override fun getMany(ids: List<Long>): List<DishDto> =
        dishRepository.findAllById(ids).map(Dish::toDishDto)

    override fun create(dto: DishCreateDto, file: List<MultipartFile>?): DishDto {

        val photoUrls = file?.map { f ->
            val uploadDir = "uploads/"
            val fileName = "${UUID.randomUUID()}_${f.originalFilename}"

            val path = Paths.get(uploadDir + fileName)
            Files.createDirectories(path.parent)
            f.transferTo(path)

            "/uploads/$fileName"
        } ?: emptyList()

        val dish = dto.toEntity().apply {
            if (photoUrls.isNotEmpty()) photos = photoUrls
        }


        dish.ingredients = dto.ingredients.map { ing ->
            val product = productRepository.findById(ing.productId).orElseThrow()

            DishIngredient(
                dish = dish,
                product = product,
                amount = ing.amount
            )
        }.toMutableList()

        return dishRepository.save(dish).toDishDto()
    }

    override fun patch(id: Long, dto: DishPatchDto, files: List<MultipartFile>?): DishDto {
        val dish = dishRepository.findById(id).orElseThrow()

        dto.name?.let { dish.name = it }
        dto.portionSize?.let { dish.portionSize = it }
        dto.category?.let { dish.category = it }
        dto.calories?.let { dish.calories = it }
        dto.proteins?.let { dish.proteins = it }
        dto.fats?.let { dish.fats = it }
        dto.carbohydrates?.let { dish.carbohydrates = it }

        val currentPhotos = dish.photos.toList()
        val photosToKeep = dto.photos?.filter { it in currentPhotos } ?: currentPhotos
        val newPhotoUrls = files?.mapNotNull { f ->
            try {
                val uploadDir = "uploads/"
                val fileName = "${UUID.randomUUID()}_${f.originalFilename}"
                val path = Paths.get(uploadDir + fileName)
                Files.createDirectories(path.parent)
                f.transferTo(path)
                "/uploads/$fileName"
            } catch (e: Exception) { null }
        } ?: emptyList()
        dish.photos = (photosToKeep + newPhotoUrls).distinct().toMutableList()

        // 🔹 Флаги
        dto.flags?.let { newFlags ->
            dish.flags = newFlags.toMutableSet()
        }

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
                    existing.amount = ing.amount  // обновляем количество
                } else {
                    val product = productRepository.findById(ing.productId)
                        .orElseThrow {
                            ResponseStatusException(HttpStatus.NOT_FOUND, "Product ${ing.productId} not found")
                        }
                    dish.ingredients.add(
                        DishIngredient(
                            dish = dish,
                            product = product,
                            amount = ing.amount
                        )
                    )
                }
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
        val dishes = dishRepository.findAllById(ids)

        dishes.forEach { dish ->
            val dishDto = dish.toDishDto()
            objectMapper.readerForUpdating(dishDto)
                .readValue<DishDto>(patchNode)

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

    override fun deleteMany(ids: List<Long>) =
        dishRepository.deleteAllById(ids)
}