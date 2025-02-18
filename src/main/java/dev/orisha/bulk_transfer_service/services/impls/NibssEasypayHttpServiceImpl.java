package dev.orisha.bulk_transfer_service.services.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.orisha.bulk_transfer_service.config.EasypayProperties;
import dev.orisha.bulk_transfer_service.dto.easypay.requests.NibbsEasyPayTQSRequest;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayFundsTransferResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayNeResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayTsqResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.TokenResponse;
import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.requests.NameEnquiryRawRequest;
import dev.orisha.bulk_transfer_service.services.NibssEasypayHttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class NibssEasypayHttpServiceImpl implements NibssEasypayHttpService {

    private final ObjectMapper objectMapper;
    private final EasypayProperties properties;
    private final RestTemplate restTemplate;

    public NibssEasypayHttpServiceImpl(final ObjectMapper objectMapper,
                                       final EasypayProperties properties,
                                       final RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restTemplate = restTemplate;
    }


    @Override
    public NibssEasypayNeResponse nameEnquiry(NameEnquiryRawRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            HttpHeaders defaultHeadersToken = getDefaultHeadersToken();
            log.info("nameEnquiry request on {} with payload {} ", properties.getNameEnquiryUrl(), payload);
            HttpEntity<String> entity = new HttpEntity<>(payload, defaultHeadersToken);
            ResponseEntity<String> postForEntity = restTemplate.postForEntity(properties.getNameEnquiryUrl(), entity, String.class);
            String responsePayload = postForEntity.getBody();
            log.info("nameEnquiry response payload {} ", responsePayload);
            return objectMapper.readValue(responsePayload, NibssEasypayNeResponse.class);
        } catch (Exception e) {
            log.error("nameEnquiry error", e);
        }
        throw new RuntimeException("Failed to process nameEnquiry request");
    }

    @Override
    public NibssEasypayFundsTransferResponse fundsTransfer(FundsTransferRawRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            HttpHeaders defaultHeadersToken = getDefaultHeadersToken();
            log.info("fundsTransfer request on {} with payload {} ", properties.getFundsTransferUrl(), payload);
            HttpEntity<String> entity = new HttpEntity<>(payload, defaultHeadersToken);
            ResponseEntity<String> postForEntity = restTemplate.postForEntity(properties.getFundsTransferUrl(), entity, String.class);
            String responsePayload = postForEntity.getBody();
            log.info("fundsTransfer response payload {} ", responsePayload);
            return objectMapper.readValue(responsePayload, NibssEasypayFundsTransferResponse.class);
        } catch (Exception e) {
            log.error("fundsTransfer error", e);
        }
        throw new RuntimeException("Failed to process fundsTransfer request");
    }

    @Override
    public NibssEasypayTsqResponse txnStatusQuery(NibbsEasyPayTQSRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            HttpHeaders defaultHeadersToken = getDefaultHeadersToken();
            log.info("txnStatusQuery request on {} with payload {} ", properties.getTsqUrl(), payload);
            HttpEntity<String> entity = new HttpEntity<>(payload, defaultHeadersToken);
            ResponseEntity<String> postForEntity = restTemplate.postForEntity(properties.getTsqUrl(), entity, String.class);
            String responsePayload = postForEntity.getBody();
            log.info("txnStatusQuery response payload {} ", responsePayload);
            return objectMapper.readValue(responsePayload, NibssEasypayTsqResponse.class);
        } catch (Exception e) {
            log.error("txnStatusQuery error", e);
        }
        throw new RuntimeException("Failed to process txnStatusQuery request");
    }

    private HttpHeaders getDefaultHeadersToken() {
        HttpHeaders headers = new HttpHeaders();
        TokenResponse tokenResponseDto = transactionToken();
        if (tokenResponseDto.getToken_type().equalsIgnoreCase("Bearer")) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            String accessToken = tokenResponseDto.getAccess_token();
            headers.set("Authorization", String.format("%s%s", "Bearer ", accessToken));
        }
        return headers;
    }

    private TokenResponse transactionToken() {
        TokenResponse responseDto;
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", properties.getClientId());
        requestBody.add("client_secret", properties.getClientSecret());
        requestBody.add("scope", properties.getClientId() + "/.default");
        requestBody.add("grant_type", "client_credentials");
        HttpEntity<?> entity = new HttpEntity<>(requestBody, getDefaultHeaders());
        log.info("transactionToken request on {} with payload {} ", properties.getTokenUrl(), entity);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(properties.getTokenUrl(), entity, String.class);
            String responsePayload = responseEntity.getBody();
            log.info("transactionToken response payload {} ", responsePayload);
            responseDto = objectMapper.readValue(responsePayload, TokenResponse.class);
            return responseDto;
        } catch (Exception e) {
            log.error("Error occurred during request", e);
        }
        throw new RuntimeException("Cannot get transaction token");
    }

    private HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

}
