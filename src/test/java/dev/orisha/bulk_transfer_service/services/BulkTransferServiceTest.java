package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.constants.ResponseCodes;
import dev.orisha.bulk_transfer_service.dto.responses.ApiResponse;
import dev.orisha.bulk_transfer_service.dto.responses.BulkTransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BulkTransferServiceTest {

    @Autowired
    private BulkTransferService bulkTransferService;

    @Test
    void performBulkTransfer() {
        String transactions_excel_sheet = "/data/transactions.xlsx";
        try (InputStream inputStream = BulkTransferServiceTest.class.getResourceAsStream(transactions_excel_sheet)) {
            MultipartFile multipartFile = new MockMultipartFile("transactions.xlsx", inputStream);
            ApiResponse<BulkTransferResponse> apiResponse = bulkTransferService.performBulkTransfer(multipartFile, null);
            assertThat(apiResponse).isNotNull();
            assertThat(ResponseCodes.SUCCESS.getCode()).isEqualTo(apiResponse.getStatus());
            assertThat(apiResponse.getData()).isNotNull();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}