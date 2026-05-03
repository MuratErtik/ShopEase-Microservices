package org.n11bootcamp.productservice.repositories;


import org.n11bootcamp.productservice.entities.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {


    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameAndBrandAndColorIgnoreCase(String name, String brand, String color);



}
