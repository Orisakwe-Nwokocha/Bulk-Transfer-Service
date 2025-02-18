package dev.orisha.bulk_transfer_service.dto.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NameEnquiryResponse {

    private String sessionID;

    private String destinationInstitutionCode;

    private Integer channelCode;

    private String accountNumber;

    private String accountName;

    private String bankVerificationNumber;

    private Integer kycLevel;

}
