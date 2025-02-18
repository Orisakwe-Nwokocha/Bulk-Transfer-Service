package dev.orisha.bulk_transfer_service.services.impls;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.services.BulkTransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BulkTransferServiceImpl implements BulkTransferService {

    private final TransactionRepository transactionRepository;

    public BulkTransferServiceImpl(final TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void performBulkTransfer(MultipartFile multipartFile, String batchId) {
        if (batchId == null) {
            batchId = generateBatchId();
        }

        log.info("Bulk transfer started with id {}", batchId);

        List<Transaction> transactions = new ArrayList<>();

        try(Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
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
            throw new RuntimeException(e);
        }

        transactionRepository.saveAll(transactions);
        transactionRepository.flush();
        Page<Transaction> t = transactionRepository.findAll(PageRequest.of(0, 10));

    }

    @Async("taskExecutor")
    protected void processTransactions() {

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


    public static String generateBatchId() {
        return "BATCH-" + UUID.randomUUID();
    }

}
