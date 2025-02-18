package dev.orisha.bulk_transfer_service.services.impls;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponse;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponseDto;
import dev.orisha.bulk_transfer_service.services.BulkTransferService;
import dev.orisha.bulk_transfer_service.services.NibssEasypayInterbankService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ModelMapper modelMapper;
    private final NibssEasypayInterbankService nibssEasypayService;

    public BulkTransferServiceImpl(final TransactionRepository transactionRepository,
                                   final ModelMapper modelMapper,
                                   final NibssEasypayInterbankService nibssEasypayService) {
        this.transactionRepository = transactionRepository;
        this.modelMapper = modelMapper;
        this.nibssEasypayService = nibssEasypayService;
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
        processTransactions(batchId);
    }

    @Async("taskExecutor")
    protected void processTransactions(String batchId) {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Transaction> transactionPage = transactionRepository.findAllByBatchId(batchId, pageable);
        while (true) {
            List<Transaction> transactions = transactionPage.getContent();
            if (transactions.isEmpty()) {
                break;
            }

            for (Transaction transaction : transactions) {
                FundsTransferRawRequest transferRawRequest = modelMapper.map(transaction, FundsTransferRawRequest.class);
                transferRawRequest.setTransactionId(transaction.getPaymentReference());
                FundsTransferResponseDto fundsTransferResponseDto = nibssEasypayService.fundsTransfer(transferRawRequest);
                if (fundsTransferResponseDto != null) {
                    FundsTransferResponse data = fundsTransferResponseDto.getData();
                    if (data != null) {
                        System.out.println(data);
                        if (data.getSessionID() != null) {
                            transaction.setPaymentReference(data.getSessionID());
                        }
                        if (data.getProcessorReference() != null) {
                            transaction.setProcessorReference(data.getProcessorReference());
                        }
                    }
                    if ("00".equals(fundsTransferResponseDto.getStatus())) {
                        transaction.setTransactionState(TransactionState.PAID);
                    } else {
                        transaction.setTransactionState(TransactionState.FAILED);
                    }
                    transactionRepository.save(transaction);
                }
            }

            if (!transactionPage.hasNext()) {
                break;
            }

            pageable = transactionPage.nextPageable();
            transactionPage = transactionRepository.findAllByBatchId(batchId, pageable);
        }
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
