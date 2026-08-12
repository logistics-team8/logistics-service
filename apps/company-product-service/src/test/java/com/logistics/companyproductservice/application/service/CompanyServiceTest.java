package com.logistics.companyproductservice.application.service;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.companyproductservice.application.dto.CompanyInfo;
import com.logistics.companyproductservice.application.error.CompanyErrorCode;
import com.logistics.companyproductservice.domain.model.Company;
import com.logistics.companyproductservice.domain.repository.CompanyRepository;
import com.logistics.companyproductservice.presentation.dto.request.CompanyCreateRequest;
import com.logistics.companyproductservice.presentation.dto.request.CompanyUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService")
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CustomUserDetails userDetails;

    @Mock
    private CompanyCreateRequest createRequest;

    @Mock
    private CompanyUpdateRequest updateRequest;

    @InjectMocks
    private CompanyService companyService;

    private UUID hubId;
    private UUID companyId;

    private Company existingCompany(UUID id, UUID hubId) {
        Company company = Company.create("기존업체", Company.Type.PRODUCER, hubId, "기존주소");
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @BeforeEach
        void setUp() {
            hubId = UUID.randomUUID();
        }

        @Test
        @DisplayName("이미 존재하는 이름으로 생성하면 DUPLICATE_RESOURCE 예외가 발생한다")
        void throwsWhenNameAlreadyExists() {
            when(createRequest.getName()).thenReturn("삼성전자");
            when(userDetails.getRole()).thenReturn("MASTER");
            when(companyRepository.existsByName("삼성전자")).thenReturn(true);

            assertThatThrownBy(() -> companyService.create(createRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE));

            verify(companyRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("HUB_MANAGER가 담당 허브가 아닌 hubId로 생성하면 NOT_OWNED_COMPANY 예외가 발생한다")
        void throwsWhenHubManagerCreatesForDifferentHub() {
            UUID otherHubId = UUID.randomUUID();
            when(createRequest.getHubId()).thenReturn(otherHubId);
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);

            assertThatThrownBy(() -> companyService.create(createRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CompanyErrorCode.NOT_OWNED_COMPANY));

            verify(companyRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("HUB_MANAGER가 담당 허브로 생성하면 정상적으로 생성된다")
        void succeedsWhenHubManagerCreatesForOwnHub() {
            when(createRequest.getName()).thenReturn("삼성전자");
            when(createRequest.getHubId()).thenReturn(hubId);
            when(createRequest.getType()).thenReturn(Company.Type.PRODUCER);
            when(createRequest.getAddress()).thenReturn("경기도");
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);
            when(companyRepository.existsByName("삼성전자")).thenReturn(false);
            when(companyRepository.saveAndFlush(any(Company.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Company result = companyService.create(createRequest, userDetails);

            assertThat(result.getName()).isEqualTo("삼성전자");
            assertThat(result.getHubId()).isEqualTo(hubId);
        }

        @Test
        @DisplayName("MASTER는 어떤 허브로도 생성할 수 있다")
        void masterCanCreateForAnyHub() {
            when(createRequest.getName()).thenReturn("삼성전자");
            when(createRequest.getHubId()).thenReturn(hubId);
            when(createRequest.getType()).thenReturn(Company.Type.PRODUCER);
            when(createRequest.getAddress()).thenReturn("경기도");
            when(userDetails.getRole()).thenReturn("MASTER");
            when(companyRepository.existsByName("삼성전자")).thenReturn(false);
            when(companyRepository.saveAndFlush(any(Company.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Company result = companyService.create(createRequest, userDetails);

            assertThat(result.getName()).isEqualTo("삼성전자");
            verify(userDetails, never()).getHubId();
        }
    }

    @Nested
    @DisplayName("update() / delete() - 소속 검증")
    class Ownership {

        @BeforeEach
        void setUp() {
            hubId = UUID.randomUUID();
            companyId = UUID.randomUUID();
        }

        @Test
        @DisplayName("HUB_MANAGER가 담당 허브 소속 업체를 수정하면 성공한다")
        void hubManagerCanUpdateOwnHubCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);
            when(updateRequest.getName()).thenReturn("새이름");
            when(updateRequest.getAddress()).thenReturn("새주소");

            Company result = companyService.update(companyId, updateRequest, userDetails);

            assertThat(result.getName()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("HUB_MANAGER가 다른 허브 소속 업체를 수정하려 하면 NOT_OWNED_COMPANY 예외가 발생한다")
        void hubManagerCannotUpdateOtherHubCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> companyService.update(companyId, updateRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CompanyErrorCode.NOT_OWNED_COMPANY));
        }

        @Test
        @DisplayName("COMPANY_MANAGER가 본인 소속 업체를 수정하면 성공한다")
        void companyManagerCanUpdateOwnCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("COMPANY_MANAGER");
            when(userDetails.getCompanyId()).thenReturn(companyId);
            when(updateRequest.getName()).thenReturn("새이름");
            when(updateRequest.getAddress()).thenReturn("새주소");

            Company result = companyService.update(companyId, updateRequest, userDetails);

            assertThat(result.getName()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("COMPANY_MANAGER가 다른 회사를 수정하려 하면 NOT_OWNED_COMPANY 예외가 발생한다")
        void companyManagerCannotUpdateOtherCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("COMPANY_MANAGER");
            when(userDetails.getCompanyId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> companyService.update(companyId, updateRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CompanyErrorCode.NOT_OWNED_COMPANY));
        }

        @Test
        @DisplayName("MASTER는 어떤 업체든 수정할 수 있다")
        void masterCanUpdateAnyCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("MASTER");
            when(updateRequest.getName()).thenReturn("새이름");
            when(updateRequest.getAddress()).thenReturn("새주소");

            Company result = companyService.update(companyId, updateRequest, userDetails);

            assertThat(result.getName()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("존재하지 않는 업체를 수정하려 하면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void throwsWhenCompanyNotFound() {
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.update(companyId, updateRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("수정하려는 이름이 이미 다른 업체에 존재하면 DUPLICATE_RESOURCE 예외가 발생한다")
        void throwsWhenUpdatingToExistingName() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("MASTER");
            when(updateRequest.getName()).thenReturn("다른업체");
            when(companyRepository.existsByName("다른업체")).thenReturn(true);

            assertThatThrownBy(() -> companyService.update(companyId, updateRequest, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE));
        }

        @Test
        @DisplayName("HUB_MANAGER가 담당 허브 소속 업체를 삭제하면 성공한다")
        void hubManagerCanDeleteOwnHubCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("HUB_MANAGER");
            when(userDetails.getHubId()).thenReturn(hubId);
            when(userDetails.getId()).thenReturn(UUID.randomUUID());

            companyService.delete(companyId, userDetails);

            assertThat(company.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("COMPANY_MANAGER가 다른 회사를 삭제하려 하면 NOT_OWNED_COMPANY 예외가 발생한다")
        void companyManagerCannotDeleteOtherCompany() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));
            when(userDetails.getRole()).thenReturn("COMPANY_MANAGER");
            when(userDetails.getCompanyId()).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> companyService.delete(companyId, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CompanyErrorCode.NOT_OWNED_COMPANY));

            assertThat(company.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("getCompanyInfo() / getCompanyInfos()")
    class Info {

        @BeforeEach
        void setUp() {
            hubId = UUID.randomUUID();
            companyId = UUID.randomUUID();
        }

        @Test
        @DisplayName("존재하는 companyId면 CompanyInfo를 반환한다")
        void returnsCompanyInfoWhenExists() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.of(company));

            CompanyInfo result = companyService.getCompanyInfo(companyId);

            assertThat(result.id()).isEqualTo(companyId);
            assertThat(result.hubId()).isEqualTo(hubId);
        }

        @Test
        @DisplayName("존재하지 않는 companyId면 RESOURCE_NOT_FOUND 예외가 발생한다")
        void throwsWhenCompanyInfoNotFound() {
            when(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.getCompanyInfo(companyId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("요청한 id 중 일부가 존재하지 않으면 RESOURCE_NOT_FOUND 예외가 발생한다 (전체 실패)")
        void throwsWhenSomeCompanyIdsNotFound() {
            UUID missingId = UUID.randomUUID();
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findAllByIds(List.of(companyId, missingId)))
                    .thenReturn(List.of(company));

            assertThatThrownBy(() -> companyService.getCompanyInfos(List.of(companyId, missingId)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("요청한 id가 전부 존재하면 CompanyInfo 목록을 반환한다")
        void returnsAllCompanyInfosWhenAllExist() {
            Company company = existingCompany(companyId, hubId);
            when(companyRepository.findAllByIds(List.of(companyId)))
                    .thenReturn(List.of(company));

            List<CompanyInfo> result = companyService.getCompanyInfos(List.of(companyId));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(companyId);
        }
    }
}