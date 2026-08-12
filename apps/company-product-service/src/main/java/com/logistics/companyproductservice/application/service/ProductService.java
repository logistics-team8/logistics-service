package com.logistics.companyproductservice.application.service;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.PageableUtil;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.companyproductservice.application.dto.ProductInfo;
import com.logistics.companyproductservice.application.error.ProductErrorCode;
import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.model.StockTransactionType;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
import com.logistics.companyproductservice.domain.repository.StockTransactionRepository;
import com.logistics.companyproductservice.presentation.dto.request.ProductCreateRequest;
import com.logistics.companyproductservice.presentation.dto.request.ProductUpdateRequest;
import com.logistics.companyproductservice.presentation.dto.request.StockBatchAdjustRequest;
import com.logistics.companyproductservice.presentation.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final StockTransactionService stockTransactionService;

    @Transactional
    public Product create(ProductCreateRequest request, CustomUserDetails userDetails) {
        validateScope(request.getCompanyId(), request.getHubId(), userDetails);

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

    @Transactional
    public void decreaseStock(UUID productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (!product.hasEnoughStock(quantity)) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
        }

        product.decreaseStock(quantity);
    }

    @Transactional
    public void restoreStock(UUID productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        product.increaseStock(quantity);
    }

    public Product getProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public Product update(UUID id, ProductUpdateRequest request, CustomUserDetails userDetails) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateScope(product.getCompanyId(), product.getHubId(), userDetails);

        product.update(request.getName(), request.getUnitPrice());
        return product;
    }

    @Transactional
    public void delete(UUID id, CustomUserDetails userDetails) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateScope(product.getCompanyId(), product.getHubId(), userDetails);
        product.delete(userDetails.getId());
    }

    public Page<ProductResponse> search(String name, CustomUserDetails userDetails, Pageable pageable) {
        Pageable normalized = PageableUtil.normalize(pageable, ALLOWED_SORT);
        UUID hubFilter = "HUB_MANAGER".equals(userDetails.getRole()) ? userDetails.getHubId() : null;
        return productRepository.search(name, hubFilter, normalized).map(ProductResponse::from);
    }

    public ProductInfo getProductInfo(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        return ProductInfo.from(product);
    }

    @Transactional
    public void decreaseStockBatch(StockBatchAdjustRequest request) {
        boolean isNewRequest = stockTransactionService.tryClaim(request.getOrderId(), StockTransactionType.DECREASE);
        if (!isNewRequest) {
            return; // 이미 처리된 요청 - 멱등 처리
        }

        for (StockBatchAdjustRequest.Item item : request.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

            if (!product.hasEnoughStock(item.getQuantity())) {
                throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
            }

            product.decreaseStock(item.getQuantity());
        }
    }

    @Transactional
    public void restoreStockBatch(StockBatchAdjustRequest request) {
        boolean isNewRequest = stockTransactionService.tryClaim(request.getOrderId(), StockTransactionType.RESTORE);
        if (!isNewRequest) {
            return; // 이미 처리된 요청 - 멱등 처리
        }

        for (StockBatchAdjustRequest.Item item : request.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

            product.increaseStock(item.getQuantity());
        }
    }

    public List<ProductInfo> getProductInfos(List<UUID> productIds) {
        List<Product> products = productRepository.findAllByIds(productIds);
        if (products.size() != productIds.size()) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        return products.stream().map(ProductInfo::from).toList();
    }

    public boolean isStockTransactionProcessed(UUID orderId, StockTransactionType type) {
        return stockTransactionRepository.findByOrderIdAndType(orderId, type).isPresent();
    }

    private void validateScope(UUID companyId, UUID hubId, CustomUserDetails userDetails) {
        String role = userDetails.getRole();
        if ("MASTER".equals(role)) {
            return;
        }
        if ("HUB_MANAGER".equals(role) && hubId.equals(userDetails.getHubId())) {
            return;
        }
        if ("COMPANY_MANAGER".equals(role) && companyId.equals(userDetails.getCompanyId())) {
            return;
        }
        throw new BusinessException(ProductErrorCode.NOT_OWNED_PRODUCT);
    }
}