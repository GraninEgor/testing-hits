package org.example.recipebook.core.service

import com.fasterxml.jackson.databind.JsonNode
import org.example.recipebook.api.dto.ProductCreateDto
import org.example.recipebook.api.dto.ProductDto
import org.example.recipebook.core.filter.ProductFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

interface ProductService {
    fun getAll(filter: ProductFilter, pageable: Pageable): Page<ProductDto>
    fun getOne(id: Long): ProductDto
    fun getMany(ids: List<Long>): List<ProductDto>
    fun create(dto: ProductCreateDto, files: List<MultipartFile>??): ProductDto

    @Throws(IOException::class)
    fun patch( id: Long,  dto: ProductCreateDto,  files: List<MultipartFile>?): ProductDto

    @Throws(IOException::class)
    fun patchMany(ids: List<Long>, patchNode: JsonNode): List<Long>
    fun delete(id: Long): ProductDto?
    fun deleteMany(ids: List<Long>)
}