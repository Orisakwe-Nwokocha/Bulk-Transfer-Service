package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.dto.easypay.requests.NibbsEasyPayTQSRequest;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayFundsTransferResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayNeResponse;
import dev.orisha.bulk_transfer_service.dto.easypay.responses.NibssEasypayTsqResponse;
import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.requests.NameEnquiryRawRequest;

public interface NibssEasypayHttpService {

    NibssEasypayNeResponse nameEnquiry(NameEnquiryRawRequest request);

    NibssEasypayFundsTransferResponse fundsTransfer(FundsTransferRawRequest request);

    NibssEasypayTsqResponse txnStatusQuery(NibbsEasyPayTQSRequest request);
}
