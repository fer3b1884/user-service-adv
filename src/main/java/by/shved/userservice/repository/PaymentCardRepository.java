package by.shved.userservice.repository;

import by.shved.userservice.entity.PaymentCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCardEntity, Long> {
    List<PaymentCardEntity> findAllByUser_Id(Long userId);
}
