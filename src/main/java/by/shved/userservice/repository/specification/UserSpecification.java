package by.shved.userservice.repository.specification;

import by.shved.userservice.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    private static final String NAME_FIELD = "name";
    private static final String SURNAME_FIELD = "surname";

    public static Specification<UserEntity> hasName(String name){
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(NAME_FIELD)),
                    "%" + name.strip().toLowerCase() + "%"
            );
        };

    }

    public static Specification<UserEntity> hasSurname(String surname){
        return (root, query, criteriaBuilder) -> {
            if (surname == null || surname.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(SURNAME_FIELD)),
                    "%" + surname.strip().toLowerCase() + "%"
            );
        };
    }
}
