package dev.orisha.bulk_transfer_service.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@SpringBootTest
class BulkTransferServiceTest {

    @Autowired
    private BulkTransferService bulkTransferService;

    @Test
    void performBulkTransfer() {
        String transactions_excel_sheet = "/data/transactions.xlsx";
        try(InputStream inputStream = BulkTransferServiceTest.class.getResourceAsStream(transactions_excel_sheet)) {
//            ClassPathResource resource = new ClassPathResource("transactions.xlsx");
//            InputStream inputStream = resource.getInputStream();
            MultipartFile multipartFile = new MockMultipartFile("transactions.xlsx", inputStream);
            bulkTransferService.performBulkTransfer(multipartFile, null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}