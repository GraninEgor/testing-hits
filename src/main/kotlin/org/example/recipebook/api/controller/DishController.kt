package org.example.recipebook.api.controller

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import org.example.recipebook.api.dto.DishCreateDto
import org.example.recipebook.api.dto.DishDto
import org.example.recipebook.core.filter.DishFilter
import org.example.recipebook.core.service.DishService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/rest/admin-ui/dishes")
class DishController(private val dishService: DishService) {
    @GetMapping
    fun getAll(
        @ParameterObject @ModelAttribute filter: DishFilter,
        @ParameterObject pageable: Pageable
    ): PagedModel<DishDto> {
        val dishDto: Page<DishDto> = dishService.getAll(filter, pageable)
        return PagedModel(dishDto)
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): DishDto = dishService.getOne(id)

    @GetMapping("/by-ids")
    fun getMany(@RequestParam ids: List<Long>): List<DishDto> = dishService.getMany(ids)

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @RequestPart("data") @Valid dto: DishCreateDto,
        @RequestPart("files", required = false) files: List<MultipartFile>?
    ): DishDto {
        return dishService.create(dto, files)
    }



    @PatchMapping("/{id}")
    @Throws(IOException::class)
    fun patch(@PathVariable id: Long, @RequestBody patchNode: JsonNode): DishDto = dishService.patch(id, patchNode)

    @PatchMapping
    @Throws(IOException::class)
    fun patchMany(@RequestParam @Valid ids: List<Long>, @RequestBody patchNode: JsonNode): List<Long> =
        dishService.patchMany(ids, patchNode)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): DishDto? = dishService.delete(id)

    @DeleteMapping
    fun deleteMany(@RequestParam ids: List<Long>) = dishService.deleteMany(ids)
}