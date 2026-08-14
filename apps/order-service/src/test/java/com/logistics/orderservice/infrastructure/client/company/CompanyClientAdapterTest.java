package com.logistics.orderservice.infrastructure.client.company;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.ApiResponse;
import com.logistics.orderservice.error.OrderErrorCode;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyClientAdapterTest {

    @Mock
    private CompanyFeignClient companyFeignClient;

    private CompanyClientAdapter adapter;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        adapter = new CompanyClientAdapter(companyFeignClient);
        companyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("업체 조회 404를 수령 업체 없음 오류로 변환한다")
    void getCompanyInfo_notFound() {
        given(companyFeignClient.getCompany(companyId))
                .willThrow(feignException(404));

        assertReceiverCompanyNotFound();
    }

    @Test
    @DisplayName("업체 조회 응답 데이터가 없으면 수령 업체 없음 오류로 변환한다")
    void getCompanyInfo_nullResponseData() {
        given(companyFeignClient.getCompany(companyId))
                .willReturn(ApiResponse.success(null));

        assertReceiverCompanyNotFound();
    }

    private void assertReceiverCompanyNotFound() {
        assertThatThrownBy(() -> adapter.getCompanyInfo(companyId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        org.assertj.core.api.Assertions.assertThat(
                                ((BusinessException) exception).getErrorCode()
                        ).isEqualTo(OrderErrorCode.RECEIVER_COMPANY_NOT_FOUND)
                );
    }

    private FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://company-product-service/internal/v1/companies/" + companyId,
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8
        );
        Response response = Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("CompanyFeignClient", response);
    }
}
