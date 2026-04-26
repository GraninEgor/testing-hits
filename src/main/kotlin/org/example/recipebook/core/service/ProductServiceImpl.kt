package org.example.recipebook.core.service;

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.api.dto.ProductCreateDto
import org.example.recipebook.core.database.entity.Product
import org.example.recipebook.api.dto.ProductDto
import org.example.recipebook.core.database.repository.DishRepository
import org.example.recipebook.core.database.repository.ProductRepository
import org.example.recipebook.core.filter.ProductFilter
import org.example.recipebook.core.mapper.toEntity
import org.example.recipebook.core.mapper.toProductDto
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
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val objectMapper: ObjectMapper,
    private val dishRepository: DishRepository) :
    ProductService {
    override fun getAll(filter: ProductFilter, pageable: Pageable): Page<ProductDto> {
        val spec: Specification<Product> = filter.toSpecification()
        val products: Page<Product> = productRepository.findAll(spec, pageable)
        return products.map(Product::toProductDto)
    }

    override fun getOne(id: Long): ProductDto {
        val productOptional: Optional<Product> = productRepository.findById(id)
        return productOptional.orElse(null).toProductDto()
    }

    override fun getMany(ids: List<Long>): List<ProductDto> {
        val products: List<Product> = productRepository.findAllById(ids)
        return products.map(Product::toProductDto)
    }

    override fun create(dto: ProductCreateDto, files: List<MultipartFile>?): ProductDto {

        val photoUrls: List<String> = files?.map { f ->
            val uploadDir = "uploads/"
            val fileName = UUID.randomUUID().toString() + "_" + f.originalFilename

            val path = Paths.get(uploadDir + fileName)
            Files.createDirectories(path.parent)
            f.transferTo(path)

            "/uploads/$fileName"
        } ?: emptyList()

        val product: Product = dto.toEntity().apply {
            if (photoUrls.isNotEmpty()) {
                this.photos = photoUrls
            }
        }

        val resultProduct: Product = productRepository.save(product)
        return resultProduct.toProductDto()
    }

    @Throws(IOException::class)
    override fun patch(
        id: Long,
        dto: ProductCreateDto,
        files: List<MultipartFile>?
    ): ProductDto {
        val product = productRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")
        }

        // 1. Обновляем поля
        product.name = dto.name
        product.calories = dto.calories
        product.proteins = dto.proteins
        product.fats = dto.fats
        product.carbohydrates = dto.carbohydrates
        product.category = dto.category
        product.cookingRequirement = dto.cookingRequirement
        product.flags = dto.flags

        // 2. Валидация БЖУ
        val totalBju = (dto.proteins ?: 0.0) + (dto.fats ?: 0.0) + (dto.carbohydrates ?: 0.0)
        if (totalBju > 100) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Сумма БЖУ не может превышать 100 (сейчас $totalBju)"
            )
        }

        // 3. 🔥 РАБОТА С ФОТОГРАФИЯМИ 🔥

        // Текущие фото
        val currentPhotos = product.photos ?: emptyList()

        // Фото, которые пользователь НЕ удалил (если dto.photos == null → не менял)
        val photosToKeep = dto.photos
            ?.filter { it in currentPhotos }
            ?: currentPhotos

        // Новые файлы
        val newPhotoUrls = files?.mapNotNull { f ->
            try {
                val uploadDir = "uploads/"
                val fileName = "${UUID.randomUUID()}_${f.originalFilename}"
                val path = Paths.get(uploadDir + fileName)
                Files.createDirectories(path.parent)
                f.transferTo(path)
                "/uploads/$fileName"
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()

        // Объединяем
        product.photos = (photosToKeep + newPhotoUrls).distinct()

        return productRepository.save(product).toProductDto()
    }

    @Throws(IOException::class)
    override fun patchMany(ids: List<Long>, patchNode: JsonNode): List<Long> {
        val products: Collection<Product> = productRepository.findAllById(ids)
        for (product in products) {
            val productDto = product.toProductDto()
            objectMapper.readerForUpdating(productDto).readValue<ProductDto>(patchNode)
            product.updateWithNull(productDto)
        }
        val resultProducts: List<Product> = productRepository.saveAll(products)
        return resultProducts.map(Product::id)
    }

    override fun delete(id: Long): ProductDto? {

        val product = productRepository.findById(id).orElse(null)
            ?: return null

        val usedInDish = dishRepository.existsByIngredientsProductId(id)

        if (usedInDish) {
            val dishes = dishRepository.findAll()
                .filter { dish ->
                    dish.ingredients.any { it.product.id == id }
                }
                .map { it.name }

            throw IllegalStateException(
                "Нельзя удалить продукт. Используется в блюдах: ${dishes.joinToString(", ")}"
            )
        }

        productRepository.delete(product)
        return product.toProductDto()
    }

    override fun deleteMany(ids: List<Long>) = productRepository.deleteAllById(ids)
}