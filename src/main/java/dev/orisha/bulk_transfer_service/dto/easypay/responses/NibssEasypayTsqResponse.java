package dev.orisha.bulk_transfer_service.dto.easypay.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NibssEasypayTsqResponse {

    private String sessionID;

    private String transactionId;

    private String sourceInstitutionCode;

    private String channelCode;

    private String responseCode;

}
