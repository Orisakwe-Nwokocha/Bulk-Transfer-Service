package dev.orisha.bulk_transfer_service.services;

public interface AsyncTransferService {

    void processTransactions(String batchId);

}
