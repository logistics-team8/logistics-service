package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DeliveryManagerJpaRepository extends JpaRepository<DeliveryManager, UUID> {

    @Query("""
            select manager.deliverySequence
            from DeliveryManager manager
            where manager.managerType = :managerType
              and ((:hubId is null and manager.hubId is null) or manager.hubId = :hubId)
              and manager.deletedAt is null
            order by manager.deliverySequence asc
            """)
    List<Integer> findActiveDeliverySequences(
            @Param("managerType") DeliveryManagerType managerType,
            @Param("hubId") UUID hubId
    );
}
