package dev.orisha.bulk_transfer_service.services.impls;

import dev.orisha.bulk_transfer_service.constants.ResponseCodes;
import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.dto.responses.ApiResponse;
import dev.orisha.bulk_transfer_service.dto.responses.BulkTransferResponse;
import dev.orisha.bulk_transfer_service.services.AsyncTransferService;
import dev.orisha.bulk_transfer_service.services.BulkTransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BulkTransferServiceImpl implements BulkTransferService {

    private final TransactionRepository transactionRepository;
    private final AsyncTransferService asyncTransferService;


    public BulkTransferServiceImpl(final TransactionRepository transactionRepository,
                                   final AsyncTransferService asyncTransferService) {
        this.transactionRepository = transactionRepository;
        this.asyncTransferService = asyncTransferService;
    }

    @Override
    @Transactional
    public ApiResponse<BulkTransferResponse> performBulkTransfer(MultipartFile multipartFile, String batchId) {
        var apiResponse = new ApiResponse<BulkTransferResponse>();
        apiResponse.setStatus(ResponseCodes.FAILED.getCode());
        apiResponse.setMessage(ResponseCodes.FAILED.getDescription());

        if (batchId == null) {
            batchId = generateBatchId();
        }

        log.info("Bulk transfer process started with batch ID: {}", batchId);
        List<Transaction> transactions = extractTransactionsFromFile(multipartFile, batchId);

        if (transactions == null || transactions.isEmpty()) {
            log.warn("No transactions extracted from file for batch ID: {}", batchId);
            apiResponse.setMessage("Uploaded Excel file is empty or contains no data");
            return apiResponse;
        }

        log.info("Saving {} transactions to database for batch ID: {}", transactions.size(), batchId);
        transactionRepository.saveAll(transactions);

        asyncTransferService.processTransactions(batchId);

        apiResponse.setStatus(ResponseCodes.SUCCESS.getCode());
        apiResponse.setMessage(ResponseCodes.SUCCESS.getDescription());
        apiResponse.setData(new BulkTransferResponse(batchId));
        return apiResponse;
    }

    private static String getCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private List<Transaction> extractTransactionsFromFile(MultipartFile multipartFile, String batchId) {
        try (Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Transaction> transactions = new ArrayList<>();
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) { // <=1 means only the header or no rows at all
                log.warn("Uploaded Excel file is empty or contains no data rows!");
                return null;
            }
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                Transaction transaction = new Transaction();
                transaction.setAmount(new BigDecimal(getCellValue(row, 17)));
                transaction.setDestinationBankCode(getCellValue(row, 3));
                transaction.setDestinationInstitutionCode(getCellValue(row, 3));
                transaction.setChannel(Integer.parseInt(getCellValue(row, 4)));
                transaction.setOriginatorAccountName(getCellValue(row, 11));
                transaction.setOriginatorAccountNumber(getCellValue(row, 12));
                transaction.setBeneficiaryAccountName(getCellValue(row, 5));
                transaction.setBeneficiaryAccountNumber(getCellValue(row, 6));
                transaction.setNarration(getCellValue(row, 9));
                transaction.setPaymentReference(getCellValue(row, 16));
                transaction.setProcessorId("SYSTEM");
                transaction.setTransactionState(TransactionState.PENDING);
                transaction.setBatchId(batchId);

                transactions.add(transaction);
            }
            return transactions;

        } catch (IOException | EncryptedDocumentException | NumberFormatException e) {
            log.error("Error extracting transactions from file for batch ID: {}", batchId, e);
        }
        return null;
    }

    public static String generateBatchId() {
        return "BATCH-" + UUID.randomUUID();
    }

}
