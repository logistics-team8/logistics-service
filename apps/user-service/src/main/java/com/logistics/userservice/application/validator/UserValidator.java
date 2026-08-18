package com.logistics.userservice.application.validator;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.company.CompanyInfo;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.port.CompanyClientPort;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.error.ClientErrorCode;
import com.logistics.userservice.error.UserErrorCode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;
    private final HubClientPort hubClientPort;
    private final CompanyClientPort companyClientPort;

    /**
     * 회원가입 중복 체크 로직 List 형태로 중복인 아이디 또는 Slack ID를 찾고 동일할 시 예외처리
     *
     * @param command
     */
    public void validateDuplicate(UserCreateCommand command) {
        List<User> existUsers =
                userRepository.findByUsernameOrSlackId(command.username(), command.slackId());

        for (User user : existUsers) {
            if (Objects.equals(command.username(), user.getUsername())) {
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_USERNAME);
            }
            if (Objects.equals(command.slackId(), user.getSlackId())) {
                throw new BusinessException(UserErrorCode.USER_DUPLICATE_SLACK_ID);
            }
        }
    }

    /**
     * 회원가입 요청 시 존재 여부 검증
     *
     * @param command 회원 가입 정보
     * @return 업체 관리자인 경우 CompanyInfo(hubId, companyId) 반환
     */
    public CompanyInfo validateSignUpAffiliation(UserCreateCommand command) {
        if (command.requestedRole() != RequestedRole.COMPANY_MANAGER) {
            if (!hubClientPort.existsById(command.hubId())) {
                throw new BusinessException(ClientErrorCode.HUB_NOT_FOUND);
            }
            return null;
        }
        return companyClientPort.getCompanyInfo(command.companyId());
    }

    /**
     * 회원가입 승인 시 허브 관리자의 본인 허브 유무 체크
     *
     * @param adminHubId 승인자의 소속 허브
     * @param adminRole 승인자의 권한
     * @param user 승인 할 User
     */
    public void validateManagerPermission(UUID adminHubId, Role adminRole, User user) {
        if (Role.HUB_MANAGER.equals(adminRole)) {
            if (!user.isManagedByHub(adminHubId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN);
            }
        }
    }

    /**
     * 허브 or 업체 실존 여부 검증
     *
     * @param user 검증 할 User
     */
    public void validateAffiliation(User user) {
        if (user.getCompanyId() == null) {
            if (!hubClientPort.existsById(user.getHubId())) {
                throw new BusinessException(ClientErrorCode.HUB_NOT_FOUND);
            }
        } else {
            companyClientPort.getCompanyInfo(user.getCompanyId());
        }
    }
}
