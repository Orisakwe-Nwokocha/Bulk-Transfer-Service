package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.requests.NameEnquiryRawRequest;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponseDto;
import dev.orisha.bulk_transfer_service.dto.responses.NameEnquiryResponseDto;
import dev.orisha.bulk_transfer_service.dto.responses.TSQResponseDto;

public interface NibssEasypayInterbankService {
    NameEnquiryResponseDto nameEnquiry(NameEnquiryRawRequest rawRequest);

    FundsTransferResponseDto fundsTransfer(FundsTransferRawRequest rawRequest);

    TSQResponseDto txnStatusQuery(String paymentReference);
}
