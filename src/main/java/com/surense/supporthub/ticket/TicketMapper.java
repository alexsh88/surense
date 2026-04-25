package com.surense.supporthub.ticket;

import com.surense.supporthub.ticket.domain.Ticket;
import com.surense.supporthub.ticket.dto.CreateTicketRequest;
import com.surense.supporthub.ticket.dto.TicketResponse;
import com.surense.supporthub.ticket.dto.UpdateTicketRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "customerId", source = "customer.id")
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "version", ignore = true)
    Ticket toEntity(CreateTicketRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdateTicketRequest req, @MappingTarget Ticket ticket);
}
