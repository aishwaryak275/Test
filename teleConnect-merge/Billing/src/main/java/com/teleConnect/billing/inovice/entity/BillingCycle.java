package com.teleConnect.billing.inovice.entity;

import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_cycle")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleID;

    @Column(nullable = false)
    private Long accountID;

    @Column(nullable = false)
    private LocalDate cycleStart;

    @Column(nullable = false)
    private LocalDate cycleEnd;

    private LocalDateTime generatedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleStatus status;
}
