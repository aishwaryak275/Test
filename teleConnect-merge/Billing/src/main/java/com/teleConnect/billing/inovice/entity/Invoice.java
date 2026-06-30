package com.teleConnect.billing.inovice.entity;

import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceID;

    @Column(nullable = false)
    private Long accountID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycleID", nullable = false)
    private BillingCycle cycle;

    @Column(precision = 10, scale = 2)
    private BigDecimal planCharges;

    @Column(precision = 10, scale = 2)
    private BigDecimal excessCharges;

    @Column(precision = 10, scale = 2)
    private BigDecimal addOnCharges;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxes;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;
}
