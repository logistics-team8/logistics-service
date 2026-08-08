package com.logistics.userservice.application;

import com.logistics.userservice.application.dto.AdminRejectCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {
    public void approvalUser(String userId) {}

    public void rejectUser(AdminRejectCommand command) {}
}
