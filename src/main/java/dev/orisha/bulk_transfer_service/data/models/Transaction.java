package dev.orisha.bulk_transfer_service.data.models;

import dev.orisha.bulk_transfer_service.data.enums.TransactionState;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String destinationBankCode;

    private String destinationInstitutionCode;

    private Integer channel;

    private String originatorAccountName;

    private String originatorAccountNumber;

    private String beneficiaryAccountName;

    private String beneficiaryAccountNumber;

    private String narration;

    @NotNull
    @Column(unique = true)
    private String paymentReference;

    @NotNull
    private BigDecimal amount;

    private String processorId;

    @Enumerated(EnumType.STRING)
    private TransactionState transactionState;

    private String processorReference;

    private String batchId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant dateCreated;

    @UpdateTimestamp
    private Instant dateModified;

}
