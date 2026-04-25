package com.surense.supporthub.user.customer;

import com.surense.supporthub.user.customer.dto.CreateCustomerRequest;
import com.surense.supporthub.user.customer.dto.CustomerResponse;
import com.surense.supporthub.user.customer.dto.UpdateCustomerRequest;
import com.surense.supporthub.user.domain.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(CreateCustomerRequest req);

    @Mapping(target = "agentId", source = "agent.id")
    CustomerResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(UpdateCustomerRequest req, @MappingTarget User user);
}
