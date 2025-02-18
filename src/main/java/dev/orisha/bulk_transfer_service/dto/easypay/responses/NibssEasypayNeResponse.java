package dev.orisha.bulk_transfer_service.dto.easypay.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NibssEasypayNeResponse {

    private String sessionID;

    private String transactionId;

    private String destinationInstitutionCode;

    private Integer channelCode;

    private String accountNumber;

    private String accountName;

    private String bankVerificationNumber;

    private Integer kycLevel;

    private String responseCode;
}
