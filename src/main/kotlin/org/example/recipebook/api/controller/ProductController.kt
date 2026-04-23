package org.example.recipebook.api.controller
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import org.example.recipebook.api.dto.ProductCreateDto
import org.example.recipebook.api.dto.ProductDto
import org.example.recipebook.core.filter.ProductFilter
import org.example.recipebook.core.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/rest/admin-ui/products")
class ProductController(private val productService: ProductService) {
    @GetMapping
    fun getAll(@ModelAttribute filter: ProductFilter, pageable: Pageable): PagedModel<ProductDto> {
        val productDto: Page<ProductDto> = productService.getAll(filter, pageable)
        return PagedModel(productDto)
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): ProductDto = productService.getOne(id)

    @GetMapping("/by-ids")
    fun getMany(@RequestParam ids: List<Long>): List<ProductDto> = productService.getMany(ids)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @RequestPart("data") dto: ProductCreateDto,
        @RequestPart("file", required = false) file: MultipartFile?
    ): ProductDto {
        return productService.create(dto, file)
    }

    @PatchMapping("/{id}/photo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Throws(IOException::class)
    fun patch(@PathVariable id: Long, @RequestBody patchNode: JsonNode): ProductDto =
        productService.patch(id, patchNode)


    @PatchMapping
    @Throws(IOException::class)
    fun patchMany(@RequestParam @Valid ids: List<Long>, @RequestBody patchNode: JsonNode): List<Long> =
        productService.patchMany(ids, patchNode)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            productService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping
    fun deleteMany(@RequestParam ids: List<Long>) = productService.deleteMany(ids)
}
