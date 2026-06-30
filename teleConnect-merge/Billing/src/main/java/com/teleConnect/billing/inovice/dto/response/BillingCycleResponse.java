package com.teleConnect.billing.inovice.dto.response;

import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingCycleResponse {

    private Long cycleID;
    private Long accountID;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private LocalDateTime generatedDate;
    private CycleStatus status;

    public static BillingCycleResponse from(BillingCycle cycle) {
        return BillingCycleResponse.builder()
                .cycleID(cycle.getCycleID())
                .accountID(cycle.getAccountID())
                .cycleStart(cycle.getCycleStart())
                .cycleEnd(cycle.getCycleEnd())
                .generatedDate(cycle.getGeneratedDate())
                .status(cycle.getStatus())
                .build();
    }
}
