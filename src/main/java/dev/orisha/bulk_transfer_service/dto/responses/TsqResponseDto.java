package dev.orisha.bulk_transfer_service.dto.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TSQResponseDto {

    private String status;

    private String message;

    private TSQResponse data;
}
