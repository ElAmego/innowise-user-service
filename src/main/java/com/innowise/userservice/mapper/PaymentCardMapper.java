package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.model.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {
    PaymentCardDto toDto(PaymentCard card);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardDto dto);
}