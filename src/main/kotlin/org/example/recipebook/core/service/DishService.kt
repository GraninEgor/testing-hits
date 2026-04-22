package org.example.recipebook.core.service

import com.fasterxml.jackson.databind.JsonNode
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.core.filter.DishFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.io.IOException

interface DishService {
    fun getAll(filter: DishFilter, pageable: Pageable): Page<DishDto>
    fun getOne(id: Long): DishDto
    fun getMany(ids: List<Long>): List<DishDto>
    fun create(dto: DishDto): DishDto

    @Throws(IOException::class)
    fun patch(id: Long, patchNode: JsonNode): DishDto

    @Throws(IOException::class)
    fun patchMany(ids: List<Long>, patchNode: JsonNode): List<Long>
    fun delete(id: Long): DishDto?
    fun deleteMany(ids: List<Long>)
}