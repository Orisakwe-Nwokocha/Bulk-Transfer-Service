package dev.orisha.bulk_transfer_service.controller;

import dev.orisha.bulk_transfer_service.dto.responses.ApiResponse;
import dev.orisha.bulk_transfer_service.services.BulkTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/bulk-transfer")
@Slf4j
public class BulkTransferController {

    private final BulkTransferService bulkTransferService;

    @Autowired
    public BulkTransferController(final BulkTransferService bulkTransferService) {
        this.bulkTransferService = bulkTransferService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> performBulkTransfer(@RequestParam("file") MultipartFile multipartFile,
                                                 @RequestParam(required = false) String batchId) {
        log.info("REST request to process bulk transfer with batch ID: {} and file: {}", batchId, multipartFile.getOriginalFilename());
        ApiResponse<?> apiResponse = bulkTransferService.performBulkTransfer(multipartFile, batchId);
        return ResponseEntity.ok().body(apiResponse);
    }

}
