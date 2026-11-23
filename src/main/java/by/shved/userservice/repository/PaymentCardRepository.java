package by.shved.userservice.repository;

import by.shved.userservice.entity.PaymentCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentCardRepository
        extends JpaRepository<PaymentCardEntity, Long>, JpaSpecificationExecutor<PaymentCardEntity> {
    List<PaymentCardEntity> findAllByUser_Id(Long userId);

    boolean existsByIdAndUser_Id(Long cardId, Long userId);

    @Query(value = """
        SELECT COUNT(*)
        FROM payment_cards
        WHERE user_id = :userId
        """, nativeQuery = true)
    int countCardsByUserId(@Param("userId") Long userId);
}
