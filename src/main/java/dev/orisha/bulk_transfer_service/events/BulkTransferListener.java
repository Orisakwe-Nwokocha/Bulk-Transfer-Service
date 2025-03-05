package dev.orisha.bulk_transfer_service.events;

import dev.orisha.bulk_transfer_service.services.AsyncTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BulkTransferListener {

    private final AsyncTransferService asyncTransferService;

    @Autowired
    public BulkTransferListener(final AsyncTransferService asyncTransferService) {
        this.asyncTransferService = asyncTransferService;
    }

    @Async
    @EventListener
    public void handleBulkTransferCompleted(BulkTransferCompletedEvent event) {
        System.out.println(Thread.currentThread().getName());
        log.info("Thread name: {}", Thread.currentThread().getName());
        log.info("Bulk transfer file parsed and saved. Initiating async processing for batch ID: {}", event.batchId());
        asyncTransferService.processTransactions(event.batchId());
    }
}
