package dev.orisha.bulk_transfer_service.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nibss")
@Setter
@Getter
public class NibbsProperties {

    private String fundsTransferUrl;
    private String nameEnquiryUrl;
    private String balanceEnquiryUrl;
    private String tsqUrl;
}
