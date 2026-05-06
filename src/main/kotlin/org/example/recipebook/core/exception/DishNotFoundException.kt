package org.example.recipebook.core.exception

class DishNotFoundException(productId: Long) :
    RuntimeException("Dish not found: $productId") {
}