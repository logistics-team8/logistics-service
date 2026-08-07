package com.logistics.companyproductservice.application.service;

import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
import com.logistics.companyproductservice.presentation.dto.request.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product create(ProductCreateRequest request) {
        Product product = Product.create(
                request.getName(),
                request.getCompanyId(),
                request.getHubId(),
                request.getUnitPrice()
        );
        return productRepository.save(product);
    }
}