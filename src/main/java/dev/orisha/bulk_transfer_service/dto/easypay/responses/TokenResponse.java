package dev.orisha.bulk_transfer_service.dto.easypay.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TokenResponse {

    private String token_type;

    private Integer expires_in;

    private Integer ext_expires_in;

    private String access_token;

}
