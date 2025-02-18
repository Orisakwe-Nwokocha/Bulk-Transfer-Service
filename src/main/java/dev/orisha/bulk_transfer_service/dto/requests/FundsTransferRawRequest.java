package dev.orisha.bulk_transfer_service.dto.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;


@Getter
@Setter
@ToString
public class FundsTransferRawRequest {

    private String transactionId;

    private String nameEnquiryRef;

    private String sourceInstitutionCode;

    private String destinationInstitutionCode;

    private Integer channelCode;

    private String beneficiaryAccountName;

    private String beneficiaryAccountNumber;

    private String beneficiaryBankVerificationNumber;

    private String beneficiaryNarration;

    private Integer beneficiaryKYCLevel;

    private String originatorNarration;

    private String originatorAccountName;

    private String originatorAccountNumber;

    private String originatorBankVerificationNumber;

    private Integer originatorKYCLevel;

    private String transactionLocation;

    private String paymentReference;

    private BigDecimal amount;

    private String mandateReferenceNumber;

    private String billerId;


}
