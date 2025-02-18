package dev.orisha.bulk_transfer_service.services.impls;


import dev.orisha.bulk_transfer_service.config.EasypayProperties;
import dev.orisha.bulk_transfer_service.constants.ResponseCodes;
import dev.orisha.bulk_transfer_service.dto.easypay.requests.NibbsEasyPayTQSRequest;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayFundsTransferResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayNeResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayTsqResponse;
import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.requests.NameEnquiryRawRequest;
import dev.orisha.bulk_transfer_service.dto.responses.*;
import dev.orisha.bulk_transfer_service.services.NibssEasypayInterbankService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class NibssEasypayInterbankServiceImpl implements NibssEasypayInterbankService {

    private final EasypayProperties properties;
    private final NibssEasypayHttpServiceImpl httpService;
    private final ModelMapper modelMapper;

    public NibssEasypayInterbankServiceImpl(final EasypayProperties properties,
                                            final NibssEasypayHttpServiceImpl httpService,
                                            final ModelMapper modelMapper) {
        this.properties = properties;
        this.httpService = httpService;
        this.modelMapper = modelMapper;
    }

    @Override
    public NameEnquiryResponseDto nameEnquiry(NameEnquiryRawRequest rawRequest) {
        NameEnquiryResponseDto responseDto = new NameEnquiryResponseDto();
        responseDto.setStatus(ResponseCodes.FAILED.getCode());
        responseDto.setMessage(ResponseCodes.FAILED.getDescription());
        NibssEasypayNeResponse response = new NibssEasypayNeResponse();
        String sessionId = rawRequest.getTransactionId();
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            rawRequest.setTransactionId(sessionId);
        }
        if (properties.getTestMode()) {
            response.setResponseCode("00");
            response.setSessionID(sessionId);
            response.setAccountName("John Doe");
            response.setAccountNumber(rawRequest.getAccountNumber());
            response.setDestinationInstitutionCode(rawRequest.getDestinationInstitutionCode());
            response.setBankVerificationNumber("1234567890");
            response.setKycLevel(2);
            response.setChannelCode(rawRequest.getChannelCode());
        } else {
            response = httpService.nameEnquiry(rawRequest);
        }
        if (response != null) {
            if (response.getResponseCode().equals("00")) {
                responseDto.setStatus(ResponseCodes.SUCCESS.getCode());
                responseDto.setMessage(ResponseCodes.SUCCESS.getDescription());
                NameEnquiryResponse nameEnquiryResponse = modelMapper.map(response, NameEnquiryResponse.class);
                responseDto.setData(nameEnquiryResponse);
            } else {
                responseDto.setStatus(response.getResponseCode());
                responseDto.setMessage(response.getResponseCode());
            }
        }
        return responseDto;
    }

    @Override
    public FundsTransferResponseDto fundsTransfer(FundsTransferRawRequest rawRequest) {
        log.info("FundsTransferDCRawRequest {}", rawRequest);
        FundsTransferResponseDto responseDto = new FundsTransferResponseDto();
        responseDto.setStatus(ResponseCodes.FAILED.getCode());
        responseDto.setMessage(ResponseCodes.FAILED.getDescription());
        NibssEasypayFundsTransferResponse response = new NibssEasypayFundsTransferResponse();
        NameEnquiryRawRequest neRawRequest = modelMapper.map(rawRequest, NameEnquiryRawRequest.class);
        NameEnquiryResponseDto enquiryResponseDto = nameEnquiry(neRawRequest);
        if (enquiryResponseDto.getStatus().equals("00")) {
            NameEnquiryResponse nameEnquiryResponse = enquiryResponseDto.getData();
            modelMapper.map(nameEnquiryResponse, rawRequest);
            rawRequest.setSourceInstitutionCode(properties.getSourceInstitutionCode());
            rawRequest.setBillerId(properties.getBillerId());
            rawRequest.setMandateReferenceNumber(properties.getMandateReferenceNumber());
            rawRequest.setTransactionLocation(properties.getTransactionLocation());
            if (rawRequest.getTransactionId() == null) {
                rawRequest.setTransactionId(UUID.randomUUID().toString());
            }
            if (properties.getTestMode()) {
                FundsTransferResponse fundsTransferData = modelMapper.map(nameEnquiryResponse, FundsTransferResponse.class);
                fundsTransferData.setPaymentReference(rawRequest.getTransactionId());
                fundsTransferData.setProcessorReference(rawRequest.getTransactionId());
                responseDto.setStatus(ResponseCodes.SUCCESS.getCode());
                responseDto.setMessage(ResponseCodes.SUCCESS.getDescription());
                responseDto.setData(fundsTransferData);
                return responseDto;
            }

            rawRequest.setOriginatorAccountNumber(properties.getOriginatorAccountNumber());
            rawRequest.setOriginatorBankVerificationNumber(properties.getOriginatorBankVerificationNumber());
            rawRequest.setOriginatorKYCLevel(properties.getOriginatorKYCLevel());
            try {
                response = httpService.fundsTransfer(rawRequest);
            } catch (Exception e) {
                log.error("error occurred calling easypay fundsTransfer service", e);
            }
            if (response == null) {
                NibssEasypayTsqResponse tsqResponse = txnStatusQueryCall(rawRequest.getTransactionId());
                if (tsqResponse != null) {
                    FundsTransferResponse transferResponse = modelMapper.map(tsqResponse, FundsTransferResponse.class);
                    responseDto.setStatus(transferResponse.getResponseCode());
                    responseDto.setMessage(transferResponse.getResponseCode());
                    responseDto.setData(transferResponse);
                    return responseDto;
                }
                return responseDto;
            }
            if (response.getResponseCode().equals("00")) {
                responseDto.setStatus(ResponseCodes.SUCCESS.getCode());
                responseDto.setMessage(ResponseCodes.SUCCESS.getDescription());
                FundsTransferResponse fundsTransferData = modelMapper.map(response, FundsTransferResponse.class);
                fundsTransferData.setProcessorReference(response.getTransactionId());
                responseDto.setData(fundsTransferData);
            } else {
                responseDto.setStatus(response.getResponseCode());
                responseDto.setMessage(response.getResponseCode());
            }
        }

        return responseDto;
    }

    @Override
    public TSQResponseDto txnStatusQuery(String paymentReference) {
        TSQResponseDto responseDto = new TSQResponseDto();
        responseDto.setStatus(ResponseCodes.FAILED.getCode());
        responseDto.setMessage(ResponseCodes.FAILED.getDescription());
        NibssEasypayTsqResponse txnStatusQueryCall = txnStatusQueryCall(paymentReference);
        if (txnStatusQueryCall != null) {
            String responseCode = txnStatusQueryCall.getResponseCode();
            if ("00".equals(responseCode)) {
                responseDto.setStatus(ResponseCodes.SUCCESS.getCode());
                responseDto.setMessage(ResponseCodes.SUCCESS.getDescription());
                TSQResponse tsqData = new TSQResponse();
                tsqData.setProcessorReference(txnStatusQueryCall.getTransactionId());
                responseDto.setData(tsqData);
            } else {
                responseDto.setStatus(responseCode);
                responseDto.setMessage(responseCode);
            }
        }

        return responseDto;
    }

    private NibssEasypayTsqResponse txnStatusQueryCall(String paymentReference) {
        NibssEasypayTsqResponse response = new NibssEasypayTsqResponse();
        if (properties.getTestMode()) {
            response.setResponseCode(ResponseCodes.SUCCESS.getCode());
            response.setSessionID(paymentReference);
        } else {
            try {
                response = httpService.txnStatusQuery(new NibbsEasyPayTQSRequest(paymentReference));
            } catch (Exception e) {
                log.error("error occurred calling easypay txnStatusQuery", e);
            }
        }
        return response;
    }

}
