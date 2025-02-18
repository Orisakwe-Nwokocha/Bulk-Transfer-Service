package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.dto.responses.ApiResponse;
import dev.orisha.bulk_transfer_service.dto.responses.BulkTransferResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BulkTransferService {

    ApiResponse<BulkTransferResponse> performBulkTransfer(MultipartFile multipartFile, String batchId);

}
