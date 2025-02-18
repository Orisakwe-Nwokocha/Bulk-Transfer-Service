package dev.orisha.bulk_transfer_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("nibss.easypay")
@Setter
@Getter
public class EasypayProperties {

    private Boolean testMode;

    private String institutionCode;

    private String sourceInstitutionCode;

    private String billerId;

    private String mandateReferenceNumber;

    private String originatorAccountName;

    private String originatorAccountNumber;

    private String originatorBankVerificationNumber;

    private Integer originatorKYCLevel;

    private String transactionLocation;

    private String clientId;

    private String clientSecret;

    private String tokenUrl;

    private String balanceEnquiryUrl;

    private String nameEnquiryUrl;

    private String fundsTransferUrl;

    private String tsqUrl;

}
