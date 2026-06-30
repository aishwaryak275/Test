package com.teleConnect.billing.inovice.entity;

import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_dispute")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disputeID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoiceID", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private Long subscriberID;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal disputedAmount;

    @Column(length = 500)
    private String description;

    private String disputeReason;

    @Column(nullable = false)
    private LocalDateTime raisedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;
}
