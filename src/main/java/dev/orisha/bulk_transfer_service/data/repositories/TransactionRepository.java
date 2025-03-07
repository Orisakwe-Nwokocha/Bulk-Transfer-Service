package dev.orisha.bulk_transfer_service.data.repositories;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import dev.orisha.bulk_transfer_service.data.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

//    Page<Transaction> findAllByBatchId(String batchId, Pageable pageable);

    boolean existsByBatchId(String batchId);

    Page<Transaction> findAllByBatchIdAndTransactionState(String batchId,
                                                          TransactionState transactionState,
                                                          Pageable pageable);

}
