package org.example.recipebook.api.exception

import org.example.recipebook.core.exception.ProductNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import jakarta.validation.ConstraintViolationException
import org.example.recipebook.core.exception.DishNotFoundException
import org.springframework.transaction.TransactionSystemException
import org.slf4j.LoggerFactory
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Обработка ошибок валидации @Valid на уровне DTO
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate { field ->
            field.field to field.defaultMessage
        }
        log.warn("Validation failed: {}", errors)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("status" to 400, "errors" to errors))
    }

    /**
     * Обработка ошибок валидации Bean Validation на уровне Entity
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<Map<String, Any>> {
        val errors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }
        log.warn("Constraint violation: {}", errors)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("status" to 400, "errors" to errors))
    }

    /**
     * Обработка ошибок валидации внутри транзакции (TransactionSystemException)
     */
    @ExceptionHandler(TransactionSystemException::class)
    fun handleTransactionSystemException(ex: TransactionSystemException): ResponseEntity<Map<String, Any>> {
        val rootCause = ex.rootCause
        return when (rootCause) {
            is ConstraintViolationException -> handleConstraintViolationException(rootCause)
            else -> {
                log.error("Transaction system exception", ex)
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("status" to 500, "message" to "Internal server error"))
            }
        }
    }

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(ex: ProductNotFoundException): ResponseEntity<Map<String, Any>> {
        log.warn("Product not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf<String, Any>("status" to 404, "message" to (ex.message ?: "Product not found")))
    }

    @ExceptionHandler(DishNotFoundException::class)
    fun handleProductNotFound(ex: DishNotFoundException): ResponseEntity<Map<String, Any>> {
        log.warn("Dish not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf<String, Any>("status" to 404, "message" to (ex.message ?: "Dish not found")))
    }
    /**
     * Ошибка парсинга JSON / Kotlin null safety
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, Any>> {
        log.warn("JSON parse error: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("status" to 400, "message" to "Invalid request body: ${ex.mostSpecificCause.message}"))
    }

    /**
     * Несовместимый Content-Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException): ResponseEntity<Map<String, Any>> {
        log.warn("Unsupported media type: {}", ex.contentType)
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(mapOf("status" to 415, "message" to "Content-Type '${ex.contentType}' is not supported"))
    }

    /**
     * Ошибка преобразования типов параметров (например, id=999_999)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<Map<String, Any>> {
        log.warn("Type mismatch for parameter '{}': {}", ex.name, ex.value)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("status" to 400, "message" to "Invalid value for parameter '${ex.name}': ${ex.value}"))
    }

    /**
     * Ресурс не найден (404 для несуществующих путей)
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<Map<String, Any>> {
        log.debug("Resource not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("status" to 404, "message" to "Resource not found"))
    }

    /**
     * NullPointerException (например, orElse(...) must not be null)
     */
    @ExceptionHandler(NullPointerException::class)
    fun handleNullPointerException(ex: NullPointerException): ResponseEntity<Map<String, Any>> {
        log.error("Null pointer exception", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("status" to 500, "message" to "Internal server error"))
    }

    /**
     * Catch-all для необработанных исключений
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, Any>> {
        log.error("Unhandled exception", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("status" to 500, "message" to "Internal server error"))
    }
}