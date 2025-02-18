package dev.orisha.bulk_transfer_service.dto.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class FundsTransferResponse {

    private String sessionID;

    private String processorReference;

    private String nameEnquiryRef;

    private String destinationInstitutionCode;

    private Integer channelCode;

    private String beneficiaryAccountName;

    private String beneficiaryAccountNumber;

    private String beneficiaryBankVerificationNumber;

    private String beneficiaryKYCLevel;

    private String originatorAccountName;

    private String originatorAccountNumber;

    private String originatorBankVerificationNumber;

    private String originatorKYCLevel;

    private String transactionLocation;

    private String narration;

    private String paymentReference;

    private BigDecimal amount;

    private String responseCode;

}
