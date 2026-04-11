package org.example.recipebook.core.database.repository;

import org.example.recipebook.core.database.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.Optional

interface ProductRepository: JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}