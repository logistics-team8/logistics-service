package com.logistics.userservice.infrastructure.client.company;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.error.ClientErrorCode;
import feign.FeignException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CompanyFeignAdapterTest - 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CompanyFeignAdapterUnitTest {
    @Mock private CompanyFeignClient companyFeignClient;
    @InjectMocks private CompanyFeignAdapter companyFeignAdapter;

    @Test
    @DisplayName("업체 조회에 성공하면 업체 정보를 반환한다.")
    void getCompanyInfo_success() {
        // given
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        CompanyResponse companyInfo = new CompanyResponse(companyId, hubId, "업체명", "주소");

        given(companyFeignClient.getCompanyInfo(companyId))
                .willReturn(ApiResponse.success(companyInfo));

        // when
        CompanyInfo result = companyFeignAdapter.getCompanyInfo(companyId);

        // then
        assertThat(result.companyId()).isEqualTo(companyInfo.id());
        assertThat(result.hubId()).isEqualTo(companyInfo.hubId());
    }

    @Test
    @DisplayName("업체의 ID가 일치하지 않으면 COMPANY_ID_INVALID 예외가 발생한다.")
    void getCompanyInfo_fail_when_invalid_companyId() {
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        CompanyResponse companyResponse = new CompanyResponse(UUID.randomUUID(), hubId, "업체", "주소");

        given(companyFeignClient.getCompanyInfo(companyId))
                .willReturn(ApiResponse.success(companyResponse));

        // when
        assertThatThrownBy(() -> companyFeignAdapter.getCompanyInfo(companyId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ClientErrorCode.COMPANY_ID_INVALID);
    }

    @Test
    @DisplayName("업체가 존재하지 않으면 COMPANY_NOT_FOUND 예외가 발생한다.")
    void getCompanyInfo_fail_when_not_found_company_id() {
        // given
        UUID companyId = UUID.randomUUID();

        given(companyFeignClient.getCompanyInfo(companyId))
                .willThrow(FeignException.NotFound.class);

        // when & then
        assertThatThrownBy(() -> companyFeignAdapter.getCompanyInfo(companyId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ClientErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("Company-service 호출에 실패하면 SERVICE_UNAVAILABLE 예외가 발생한다.")
    void getCompanyInfo_fail_when_service_unavailable() {
        // given
        UUID companyId = UUID.randomUUID();

        given(companyFeignClient.getCompanyInfo(companyId))
                .willThrow(FeignException.ServiceUnavailable.class);

        // when & then
        assertThatThrownBy(() -> companyFeignAdapter.getCompanyInfo(companyId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ClientErrorCode.SERVICE_UNAVAILABLE);
    }
}
