package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponse;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponseDto;
import dev.orisha.bulk_transfer_service.services.impls.AsyncTransferService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AsyncTransferServiceImpl implements AsyncTransferService {

    private final TransactionRepository transactionRepository;
    private final ModelMapper modelMapper;
    private final NibssEasypayInterbankService nibssEasypayService;

    @Autowired
    public AsyncTransferServiceImpl(final TransactionRepository transactionRepository,
                                    final ModelMapper modelMapper, NibssEasypayInterbankService nibssEasypayService) {
        this.transactionRepository = transactionRepository;
        this.modelMapper = modelMapper;
        this.nibssEasypayService = nibssEasypayService;
    }

    @Override
    @Async("taskExecutor")
    public void processTransactions(String batchId) {
        System.out.println(Thread.currentThread().getName());
        log.info("Processing transactions in thread: {}", Thread.currentThread().getName());

        log.info("Transaction processing started for batchId: {}", batchId);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Transaction> transactionPage = transactionRepository.findAllByBatchId(batchId, pageable);
        while (true) {
            log.info("Transaction processing started for: {}", transactionPage);
            List<Transaction> transactions = transactionPage.getContent();
            if (transactions.isEmpty()) {
                log.info("No transactions extracted from the database for batch ID: {}", batchId);
                break;
            }
            transactions.forEach(this::processSingleTransaction);
            if (!transactionPage.hasNext()) {
                log.info("No more transactions to process for batchId: {}", batchId);
                break;
            }

            pageable = transactionPage.nextPageable();
            transactionPage = transactionRepository.findAllByBatchId(batchId, pageable);
        }
    }

    private void processSingleTransaction(Transaction transaction) {
        try {
            FundsTransferRawRequest transferRawRequest = modelMapper.map(transaction, FundsTransferRawRequest.class);
            transferRawRequest.setTransactionId(transaction.getPaymentReference());

            FundsTransferResponseDto responseDto = nibssEasypayService.fundsTransfer(transferRawRequest);

            if (responseDto == null) {
                log.warn("Received null response for transaction {}", transaction.getPaymentReference());
                transaction.setTransactionState(TransactionState.FAILED);
            } else {
                FundsTransferResponse data = responseDto.getData();
                if (data != null) {
                    log.info("Transaction {} processed: {}", transaction.getPaymentReference(), data);
                    String paymentReference = data.getSessionID() != null ? data.getSessionID() : transaction.getPaymentReference();
                    String processorReference = data.getProcessorReference() != null ? data.getProcessorReference() : transaction.getProcessorReference();
                    transaction.setPaymentReference(paymentReference);
                    transaction.setProcessorReference(processorReference);
                }
                transaction.setTransactionState("00".equals(responseDto.getStatus()) ? TransactionState.PAID : TransactionState.FAILED);
            }

            transactionRepository.save(transaction);
            log.info("Transaction {} updated with state {}", transaction.getPaymentReference(), transaction.getTransactionState());

        } catch (Exception e) {
            log.error("Error processing transaction {}: {}", transaction.getPaymentReference(), e.getMessage(), e);
            transaction.setTransactionState(TransactionState.FAILED);
            transactionRepository.save(transaction);
        }
    }
}
