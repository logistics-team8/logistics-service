package com.logistics.companyproductservice.application.service;

import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
import com.logistics.companyproductservice.presentation.dto.request.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product create(ProductCreateRequest request) {
        // TODO: company-service에 request.getCompanyId()가 실제 존재하는 Company인지 검증 필요
        // TODO: hub-service에 request.getHubId()가 실제 존재하는 Hub인지 검증 필요
        Product product = Product.create(
                request.getName(),
                request.getCompanyId(),
                request.getHubId(),
                request.getUnitPrice()
        );
        return productRepository.save(product);
    }
}