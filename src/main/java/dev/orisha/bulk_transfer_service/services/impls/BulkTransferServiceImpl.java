package dev.orisha.bulk_transfer_service.services.impls;

import dev.orisha.bulk_transfer_service.constants.ResponseCodes;
import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.dto.responses.ApiResponse;
import dev.orisha.bulk_transfer_service.dto.responses.BulkTransferResponse;
import dev.orisha.bulk_transfer_service.events.BulkTransferCompletedEvent;
import dev.orisha.bulk_transfer_service.services.BulkTransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class BulkTransferServiceImpl implements BulkTransferService {

    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkTransferServiceImpl(final TransactionRepository transactionRepository,
                                   final ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
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

        boolean batchExists = transactionRepository.existsByBatchId(batchId);
        if (batchExists) {
            log.warn("Batch {} has already been processed, skipping re-processing.", batchId);
            apiResponse.setMessage("This batch has already been processed");
            return apiResponse;
        }

        log.info("Bulk transfer process started with batch ID: {}", batchId);

        try (Stream<Row> rowStream = getSheetRowStream(multipartFile)) {
            String finalBatchId = batchId;
            List<Transaction> batch = new ArrayList<>();
            rowStream.forEach(row -> {
                Transaction transaction = mapRowToTransaction(row, finalBatchId);
                batch.add(transaction);

                if (batch.size() == 2) {
                    saveTransactionsInBatches(batch);
                    batch.clear();
                }
            });

            if (!batch.isEmpty()) {
                saveTransactionsInBatches(batch);
            }
        } catch (Exception e) {
            log.error("Error reading file for batch ID: {}", batchId, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            apiResponse.setMessage("Failed to process file");
            return apiResponse;
        }

        eventPublisher.publishEvent(new BulkTransferCompletedEvent(batchId));

        apiResponse.setStatus(ResponseCodes.SUCCESS.getCode());
        apiResponse.setMessage(ResponseCodes.SUCCESS.getDescription());
        apiResponse.setData(new BulkTransferResponse(batchId));
        return apiResponse;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTransactionsInBatches(List<Transaction> transactions) {
        transactionRepository.saveAll(transactions);
    }

    private Transaction mapRowToTransaction(Row row, String batchId) {
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
        return transaction;
    }

    private Stream<Row> getSheetRowStream(MultipartFile multipartFile) throws IOException {
        Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        return StreamSupport.stream(sheet.spliterator(), false)
                .skip(1);
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
