package com.logistics.companyproductservice.application.service;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.companyproductservice.application.dto.ProductInfo;
import com.logistics.companyproductservice.application.error.ProductErrorCode;
import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.model.StockTransactionType;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
import com.logistics.companyproductservice.presentation.dto.request.ProductCreateRequest;
import com.logistics.companyproductservice.presentation.dto.request.ProductUpdateRequest;
import com.logistics.companyproductservice.presentation.dto.request.StockBatchAdjustRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockTransactionService stockTransactionService;

    @Mock
    private CustomUserDetails userDetails;

    @Mock
    private ProductCreateRequest createRequest;

    @Mock
    private ProductUpdateRequest updateRequest;

    @InjectMocks
    private ProductService productService;

    private UUID hubId;
    private UUID companyId;
    private UUID productId;

    private Product existingProduct(UUID id, UUID companyId, UUID hubId, int stockQuantity) {
        Product product = Product.create("기존상품", companyId, hubId, BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "stockQuantity", stockQuantity);
        return product;
    }

    @BeforeEach
    void setUp() {
        hubId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("create() - 소속 검증")
    class Create {

        @Test
        @DisplayName("HUB_MANAGER가 담당 허브가 아닌 상품을 생성하면 NOT_OWNED_PRODUCT 예외가 발생한다")
        void throwsWhenHubManagerCreatesForDifferentHub() {
            when(createRequest.getCompanyId()).thenReturn(companyId);
            when(createRequest.getHubId()).thenReturn(UUID.randomUUID());
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);

            assertThatThrownBy(() -> productService.create(createRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ProductErrorCode.NOT_OWNED_PRODUCT));
        }

        @Test
        @DisplayName("COMPANY_MANAGER가 본인 소속 업체 상품을 생성하면 성공한다")
        void succeedsWhenCompanyManagerCreatesForOwnCompany() {
            when(createRequest.getCompanyId()).thenReturn(companyId);
            when(createRequest.getHubId()).thenReturn(hubId);
            when(createRequest.getName()).thenReturn("새상품");
            when(createRequest.getUnitPrice()).thenReturn(BigDecimal.valueOf(5000));
            when(userDetails.getRole()).thenReturn("COMPANY_MANAGER");
            when(userDetails.getCompanyId()).thenReturn(companyId);
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Product result = productService.create(createRequest, userDetails);

            assertThat(result.getName()).isEqualTo("새상품");
            assertThat(result.getStockQuantity()).isZero();
        }

        @Test
        @DisplayName("COMPANY_MANAGER가 다른 업체 상품을 생성하려 하면 NOT_OWNED_PRODUCT 예외가 발생한다")
        void throwsWhenCompanyManagerCreatesForDifferentCompany() {
            when(createRequest.getCompanyId()).thenReturn(companyId);
            when(createRequest.getHubId()).thenReturn(hubId);
            when(userDetails.getRole()).thenReturn("COMPANY_MANAGER");
            when(userDetails.getCompanyId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> productService.create(createRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ProductErrorCode.NOT_OWNED_PRODUCT));
        }
    }

    @Nested
    @DisplayName("update() / delete() - 소속 검증")
    class Ownership {

        @Test
        @DisplayName("존재하지 않는 상품을 수정하려 하면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void throwsWhenUpdatingNonExistentProduct() {
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(productId, updateRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("HUB_MANAGER가 담당 허브 소속 상품을 수정하면 성공한다")
        void hubManagerCanUpdateOwnHubProduct() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);
            when(updateRequest.getName()).thenReturn("새상품명");
            when(updateRequest.getUnitPrice()).thenReturn(BigDecimal.valueOf(2000));

            Product result = productService.update(productId, updateRequest, userDetails);

            assertThat(result.getName()).isEqualTo("새상품명");
        }

        @Test
        @DisplayName("COMPANY_MANAGER가 다른 업체 상품을 수정하려 하면 NOT_OWNED_PRODUCT 예외가 발생한다")
        void companyManagerCannotUpdateOtherCompanyProduct() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
            when(userDetails.getRole()).thenReturn("COMPANY_MANAGER");
            when(userDetails.getCompanyId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> productService.update(productId, updateRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ProductErrorCode.NOT_OWNED_PRODUCT));
        }

        @Test
        @DisplayName("MASTER가 상품을 삭제하면 성공한다")
        void masterCanDeleteAnyProduct() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
            when(userDetails.getRole()).thenReturn("MASTER");
            when(userDetails.getId()).thenReturn(UUID.randomUUID());

            productService.delete(productId, userDetails);

            assertThat(product.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("HUB_MANAGER가 다른 허브 소속 상품을 삭제하려 하면 NOT_OWNED_PRODUCT 예외가 발생한다")
        void hubManagerCannotDeleteOtherHubProduct() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> productService.delete(productId, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ProductErrorCode.NOT_OWNED_PRODUCT));

            assertThat(product.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("decreaseStock() - 재고 차감")
    class DecreaseStock {

        @Test
        @DisplayName("재고가 충분하면 정상적으로 차감된다")
        void succeedsWhenEnoughStock() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

            productService.decreaseStock(productId, 5);

            assertThat(product.getStockQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("재고가 부족하면 INSUFFICIENT_STOCK 예외가 발생한다")
        void throwsWhenInsufficientStock() {
            Product product = existingProduct(productId, companyId, hubId, 3);
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.decreaseStock(productId, 5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ProductErrorCode.INSUFFICIENT_STOCK));

            assertThat(product.getStockQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void throwsWhenProductNotFound() {
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.decreaseStock(productId, 5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("restoreStock() - 재고 복원")
    class RestoreStock {

        @Test
        @DisplayName("재고를 정상적으로 복원한다")
        void restoresStock() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

            productService.restoreStock(productId, 5);

            assertThat(product.getStockQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void throwsWhenProductNotFound() {
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.restoreStock(productId, 5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("decreaseStockBatch() / restoreStockBatch() - 멱등 처리")
    class BatchIdempotency {

        @Test
        @DisplayName("처음 들어온 orderId면 재고를 실제로 차감한다")
        void decreasesStockWhenNewOrder() {
            UUID orderId = UUID.randomUUID();
            Product product = existingProduct(productId, companyId, hubId, 10);
            StockBatchAdjustRequest.Item item = mockItem(productId, 5);
            StockBatchAdjustRequest request = mockBatchRequest(orderId, List.of(item));

            when(stockTransactionService.tryClaim(orderId, StockTransactionType.DECREASE)).thenReturn(true);
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

            productService.decreaseStockBatch(request);

            assertThat(product.getStockQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("이미 처리된 orderId로 차감 요청이 오면 재고를 건드리지 않는다")
        void doesNotTouchStockWhenDecreaseAlreadyProcessed() {
            UUID orderId = UUID.randomUUID();
            StockBatchAdjustRequest request = mock(StockBatchAdjustRequest.class);
            when(request.getOrderId()).thenReturn(orderId);

            when(stockTransactionService.tryClaim(orderId, StockTransactionType.DECREASE)).thenReturn(false);

            productService.decreaseStockBatch(request);

            verify(productRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("처음 들어온 orderId면 재고를 실제로 복원한다")
        void restoresStockWhenNewOrder() {
            UUID orderId = UUID.randomUUID();
            Product product = existingProduct(productId, companyId, hubId, 10);
            StockBatchAdjustRequest.Item item = mockItem(productId, 5);
            StockBatchAdjustRequest request = mockBatchRequest(orderId, List.of(item));

            when(stockTransactionService.tryClaim(orderId, StockTransactionType.RESTORE)).thenReturn(true);
            when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

            productService.restoreStockBatch(request);

            assertThat(product.getStockQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("이미 처리된 orderId로 복원 요청이 오면 재고를 건드리지 않는다")
        void doesNotTouchStockWhenRestoreAlreadyProcessed() {
            UUID orderId = UUID.randomUUID();
            StockBatchAdjustRequest request = mock(StockBatchAdjustRequest.class);
            when(request.getOrderId()).thenReturn(orderId);

            when(stockTransactionService.tryClaim(orderId, StockTransactionType.RESTORE)).thenReturn(false);

            productService.restoreStockBatch(request);

            verify(productRepository, never()).findByIdForUpdate(any());
        }

        private StockBatchAdjustRequest.Item mockItem(UUID productId, int quantity) {
            StockBatchAdjustRequest.Item item = mock(StockBatchAdjustRequest.Item.class);
            when(item.getProductId()).thenReturn(productId);
            when(item.getQuantity()).thenReturn(quantity);
            return item;
        }

        private StockBatchAdjustRequest mockBatchRequest(UUID orderId, List<StockBatchAdjustRequest.Item> items) {
            StockBatchAdjustRequest request = mock(StockBatchAdjustRequest.class);
            when(request.getOrderId()).thenReturn(orderId);
            when(request.getItems()).thenReturn(items);
            return request;
        }
    }

    @Nested
    @DisplayName("getProductInfo() / getProductInfos()")
    class Info {

        @Test
        @DisplayName("존재하는 productId면 ProductInfo를 반환한다")
        void returnsProductInfoWhenExists() {
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));

            ProductInfo result = productService.getProductInfo(productId);

            assertThat(result.id()).isEqualTo(productId);
            assertThat(result.companyId()).isEqualTo(companyId);
        }

        @Test
        @DisplayName("존재하지 않는 productId면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void throwsWhenProductInfoNotFound() {
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductInfo(productId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("요청한 id 중 일부가 존재하지 않으면 RESOURCE_NOT_FOUND 예외가 발생한다 (전체 실패)")
        void throwsWhenSomeProductIdsNotFound() {
            UUID missingId = UUID.randomUUID();
            Product product = existingProduct(productId, companyId, hubId, 10);
            when(productRepository.findAllByIds(List.of(productId, missingId)))
                    .thenReturn(List.of(product));

            assertThatThrownBy(() -> productService.getProductInfos(List.of(productId, missingId)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("search() - 허브 스코프 필터링")
    class Search {

        @Test
        @DisplayName("HUB_MANAGER로 조회하면 본인 담당 허브로 필터링해서 조회한다")
        void filtersToOwnHubWhenHubManager() {
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
            when(productRepository.search(any(), any(), any())).thenReturn(Page.empty());

            productService.search("상품", userDetails, pageable);

            verify(productRepository).search(eq("상품"), eq(hubId), any());
        }

        @Test
        @DisplayName("MASTER로 조회하면 허브 필터 없이 전체 조회한다")
        void noFilterWhenMaster() {
            when(userDetails.getRole()).thenReturn("MASTER");
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
            when(productRepository.search(any(), any(), any())).thenReturn(Page.empty());

            productService.search("상품", userDetails, pageable);

            verify(productRepository).search(eq("상품"), isNull(), any());
        }
    }
}