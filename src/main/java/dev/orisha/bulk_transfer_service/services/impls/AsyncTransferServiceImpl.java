package dev.orisha.bulk_transfer_service.services.impls;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import dev.orisha.bulk_transfer_service.dto.requests.FundsTransferRawRequest;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponse;
import dev.orisha.bulk_transfer_service.dto.responses.FundsTransferResponseDto;
import dev.orisha.bulk_transfer_service.services.AsyncTransferService;
import dev.orisha.bulk_transfer_service.services.NibssEasypayInterbankService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
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
    @Async
//    @Async("taskExecutor")
    public void processTransactions(String batchId) {
        System.out.println(Thread.currentThread().getName());
        log.info("Processing transactions in thread: {}", Thread.currentThread().getName());
        log.info("Initiating async transaction processing for batch ID: {}", batchId);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Transaction> transactionPage = transactionRepository.findAllByBatchIdAndTransactionState(batchId, TransactionState.PENDING, pageable);
        while (!transactionPage.isEmpty()) {
            List<Transaction> transactions = transactionPage.getContent();
            transactions.parallelStream().forEach(this::processSingleTransaction);

            if (!transactionPage.hasNext()) {
                log.info("No more transactions to process for batchId: {}", batchId);
                break;
            }
//            pageable = transactionPage.nextPageable();
            transactionPage = transactionRepository.findAllByBatchIdAndTransactionState(batchId, TransactionState.PENDING, pageable);
        }

        log.info("Finished processing transactions for batch ID: {}", batchId);
    }

    private void processSingleTransaction(Transaction transaction) {
        int maxRetries = 3;
        int attempt = 0;

        System.out.println(Thread.currentThread().getName());
        log.info("Processing transaction {} in thread: {}", transaction.getPaymentReference(), Thread.currentThread().getName());

        while (true) {
            String paymentReference = transaction.getPaymentReference();
            try {
                if (transaction.getTransactionState() == TransactionState.PAID || transaction.getTransactionState() == TransactionState.FAILED) {
                    return;
                }

                FundsTransferRawRequest transferRawRequest = modelMapper.map(transaction, FundsTransferRawRequest.class);
                transferRawRequest.setTransactionId(paymentReference);

                FundsTransferResponseDto responseDto = nibssEasypayService.fundsTransfer(transferRawRequest);

                if (responseDto == null) {
                    log.warn("Null response for transaction {}", paymentReference);
                    transaction.setTransactionState(TransactionState.FAILED);
                } else {
                    FundsTransferResponse data = responseDto.getData();
                    if (data != null) {
                        log.info("Transaction {} processed: {}", paymentReference, data);
                        transaction.setPaymentReference(data.getSessionID() != null ? data.getSessionID() : paymentReference);
                        transaction.setProcessorReference(data.getProcessorReference() != null ? data.getProcessorReference() : transaction.getProcessorReference());
                    }
                    transaction.setTransactionState("00".equals(responseDto.getStatus()) ? TransactionState.PAID : TransactionState.FAILED);
                }

                transactionRepository.save(transaction);
                return;

            } catch (OptimisticLockingFailureException e) {
                attempt++;
                log.warn("Optimistic lock failure for transaction {}. Retrying {}/{}", paymentReference, attempt, maxRetries);

                if (attempt < maxRetries) {
                    transaction = transactionRepository.findById(transaction.getId()).orElse(null);
                    if (transaction == null) {
                        log.error("Transaction {} not found during retry. Skipping.", paymentReference);
                        return;
                    }
                } else {
                    log.error("Max retries reached for transaction {}. Marking as FAILED.", paymentReference);
                    transaction.setTransactionState(TransactionState.FAILED);
                    transactionRepository.save(transaction);
                    return;
                }
            } catch (Exception e) {
                log.error("Error processing transaction {}: {}", paymentReference, e.getMessage(), e);
                transaction.setTransactionState(TransactionState.FAILED);
                transactionRepository.save(transaction);
                return;
            }
        }
    }

}
