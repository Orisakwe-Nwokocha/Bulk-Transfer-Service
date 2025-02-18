package dev.orisha.bulk_transfer_service.dto.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NameEnquiryRawRequest {

    private String transactionId;

    private String destinationInstitutionCode;

    private Integer channelCode;

    private String accountNumber;

}
