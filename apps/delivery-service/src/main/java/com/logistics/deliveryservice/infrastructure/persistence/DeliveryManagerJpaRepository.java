package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Spring Data JPA가 실제 DB 작업을 수행하는 인터페이스
interface DeliveryManagerJpaRepository extends JpaRepository<DeliveryManager, UUID> {

    // 동적 메서드
    @Query("""
            select manager.deliverySequence
            from DeliveryManager manager
            where manager.managerType = :managerType
              and manager.hubId = :hubId
              and manager.deletedAt is null
            order by manager.deliverySequence asc
            """)
    List<Integer> findActiveDeliverySequences(
            @Param("managerType") DeliveryManagerType managerType,
            @Param("hubId") UUID hubId
    );


    // 동적 메서드
    @Query("""
            select manager
            from DeliveryManager manager
            where manager.deletedAt is null
              and (:hubId is null or manager.hubId = :hubId)
              and (:managerType is null or manager.managerType = :managerType)
            """)
    Page<DeliveryManager> search(
            @Param("hubId") UUID hubId,
            @Param("managerType") DeliveryManagerType managerType,
            Pageable pageable
    );
}
