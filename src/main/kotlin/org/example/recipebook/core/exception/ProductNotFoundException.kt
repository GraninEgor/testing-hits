package org.example.recipebook.core.exception

class ProductNotFoundException(productId: Long) :
    RuntimeException("Product not found: $productId")