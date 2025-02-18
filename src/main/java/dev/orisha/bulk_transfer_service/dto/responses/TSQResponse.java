package dev.orisha.bulk_transfer_service.dto.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TSQResponse {

    private String processorReference;

    private String transactionId;

    private String sourceInstitutionCode;

    private String channelCode;
    
}
