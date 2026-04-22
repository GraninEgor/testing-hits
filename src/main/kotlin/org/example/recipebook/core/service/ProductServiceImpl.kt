package org.example.recipebook.core.service;

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.recipebook.core.database.entity.Product
import org.example.recipebook.api.dto.ProductDto
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
class ProductServiceImpl(private val productRepository: ProductRepository, private val objectMapper: ObjectMapper) :
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

    override fun create(dto: ProductDto, file: MultipartFile?): ProductDto {

        val photoUrl = file?.let {
            val uploadDir = "uploads/"
            val fileName = UUID.randomUUID().toString() + "_" + it.originalFilename

            val path = Paths.get(uploadDir + fileName)
            Files.createDirectories(path.parent)
            it.transferTo(path)

            "/uploads/$fileName"
        }

        val product: Product = dto.toEntity().apply {
            if (photoUrl != null) {
                this.photos = listOf(photoUrl)
            }
        }

        val resultProduct: Product = productRepository.save(product)
        return resultProduct.toProductDto()
    }

    @Throws(IOException::class)
    override fun patch(id: Long, patchNode: JsonNode): ProductDto {
        val product: Product = productRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `$id` not found")
        }
        val productDto = product.toProductDto()
        objectMapper.readerForUpdating(productDto).readValue<ProductDto>(patchNode)
        product.updateWithNull(productDto)
        val resultProduct: Product = productRepository.save(product)
        return resultProduct.toProductDto()
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
        val product: Product? = productRepository.findById(id).orElse(null)
        if (product != null) {
            productRepository.delete(product)
        }
        return product?.toProductDto()
    }

    override fun deleteMany(ids: List<Long>) = productRepository.deleteAllById(ids)
}