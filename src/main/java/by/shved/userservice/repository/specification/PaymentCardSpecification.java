package by.shved.userservice.repository.specification;

import by.shved.userservice.entity.PaymentCardEntity;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {
    private static final String USER_FIELD = "user";
    private static final String USER_NAME_FIELD = "name";
    private static final String USER_SURNAME_FIELD = "surname";

    public static Specification<PaymentCardEntity> hasUserName(String userName){
        return (root, query, criteriaBuilder) -> {
            if (userName == null || userName.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.join(USER_FIELD).get(USER_NAME_FIELD)),
                    "%" + userName.strip().toLowerCase() + "%"
            );
        };
    }

    public static Specification<PaymentCardEntity> hasUserSurname(String userSurname){
        return (root, query, criteriaBuilder) -> {
            if (userSurname == null || userSurname.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.join(USER_FIELD).get(USER_SURNAME_FIELD)),
                    "%" + userSurname.strip().toLowerCase() + "%"
            );
        };
    }
}
