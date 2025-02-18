package dev.orisha.bulk_transfer_service.services.impls;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.services.BulkTransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    public void performBulkTransfer(MultipartFile multipartFile, String batchId) {
        if (batchId == null) {
            batchId = generateBatchId();
        }

        log.info("Bulk transfer process started with batch ID: {}", batchId);
        List<Transaction> transactions = extractTransactionsFromFile(multipartFile, batchId);

        if (transactions.isEmpty()) {
            log.warn("No transactions extracted from file for batch ID: {}", batchId);
            return;
        }

        log.info("Saving {} transactions to database for batch ID: {}", transactions.size(), batchId);
        transactionRepository.saveAll(transactions);

        log.info("Initiating async transaction processing for batch ID: {}", batchId);
        asyncTransferService.processTransactions(batchId);
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
        List<Transaction> transactions = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
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
        } catch (Exception e) {
            log.error("Error extracting transactions from file for batch ID: {}", batchId, e);
        }
        return transactions;
    }

    public static String generateBatchId() {
        return "BATCH-" + UUID.randomUUID();
    }

}
