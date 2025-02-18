package dev.orisha.bulk_transfer_service.services;

import org.springframework.web.multipart.MultipartFile;

public interface BulkTransferService {

    void performBulkTransfer(MultipartFile multipartFile, String batchId);

}
