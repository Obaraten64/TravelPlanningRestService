package com.travel.planning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServicesDTO {
    private String name;
    private String city;
}
