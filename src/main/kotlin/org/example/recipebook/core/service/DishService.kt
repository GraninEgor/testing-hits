package org.example.recipebook.core.service

import com.fasterxml.jackson.databind.JsonNode
import org.example.recipebook.api.dto.DishCreateDto
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.api.dto.DishPatchDto
import org.example.recipebook.api.dto.ProductCreateDto
import org.example.recipebook.core.filter.DishFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

interface DishService {
    fun getAll(filter: DishFilter, pageable: Pageable): Page<DishDto>
    fun getOne(id: Long): DishDto
    fun getMany(ids: List<Long>): List<DishDto>
    fun create(dto: DishCreateDto,  file: List<MultipartFile>?): DishDto

    @Throws(IOException::class)
    fun patch(id: Long, dto: DishPatchDto, files: List<MultipartFile>?): DishDto

    @Throws(IOException::class)
    fun patchMany(ids: List<Long>, patchNode: JsonNode): List<Long>
    fun delete(id: Long): DishDto?
    fun deleteMany(ids: List<Long>)
}