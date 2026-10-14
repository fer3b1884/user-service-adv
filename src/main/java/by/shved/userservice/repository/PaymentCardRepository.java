package by.shved.userservice.repository;

import by.shved.userservice.entity.PaymentCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository
        extends JpaRepository<PaymentCardEntity, Long>, JpaSpecificationExecutor<PaymentCardEntity> {
    @Query(value = """
        SELECT COUNT(*)
        FROM payment_cards
        WHERE user_id = :userId
        """, nativeQuery = true)
    int countCardsByUserId(@Param("userId") Long userId);
    List<PaymentCardEntity> findAllByUser_Id(Long userId);
}
