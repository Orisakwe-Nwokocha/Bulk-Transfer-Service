package dev.orisha.bulk_transfer_service.dto.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FundsTransferResponseDto {

    private String status;

    private String message;

    private FundsTransferResponse data;
    
}
