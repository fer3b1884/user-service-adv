package by.shved.userservice.mapper;

import by.shved.userservice.dto.paymentcard.PaymentCardRequest;
import by.shved.userservice.dto.paymentcard.PaymentCardResponse;
import by.shved.userservice.entity.PaymentCardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    PaymentCardEntity toEntity(PaymentCardRequest request);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponse toResponse(PaymentCardEntity entity);
}
