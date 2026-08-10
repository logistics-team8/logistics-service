package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Product;

public interface ProductRepository {

    Product save(Product product);
}