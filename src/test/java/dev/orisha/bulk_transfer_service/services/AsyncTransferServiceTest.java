package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import dev.orisha.bulk_transfer_service.data.repositories.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Sql("/data/data.sql")
public class AsyncTransferServiceTest {

    @Autowired
    private AsyncTransferService asyncTransferService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void testTransactionProcessing_withRejectedExecutionException() throws InterruptedException {
        List<Transaction> transactions = transactionRepository.findAllByBatchIdAndTransactionState("BATCH_123", TransactionState.PENDING);
        Assertions.assertEquals(5, transactions.size(), "There should be 5 pending transactions before processing.");

        asyncTransferService.processTransactions("BATCH_123");

        Thread.sleep(5000);

        List<Transaction> failedTransactions = transactionRepository.findAllByBatchIdAndTransactionState("BATCH_123", TransactionState.FAILED);
        assertTrue(failedTransactions.isEmpty(), "At least some transactions should be marked as FAILED due to rejection.");

    }
}
