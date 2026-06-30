package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.Payment;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.entity.enums.PaymentMethod;
import com.teleConnect.billing.inovice.entity.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PaymentRepository Tests")
class PaymentRepositoryTest {

    @Autowired PaymentRepository paymentRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired BillingCycleRepository cycleRepository;

    private Invoice savedInvoice;

    @BeforeEach
    void clean() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        cycleRepository.deleteAll();

        BillingCycle c = new BillingCycle();
        c.setAccountID(1001L); c.setStatus(CycleStatus.Generated);
        c.setCycleStart(LocalDate.of(2026, 5, 1)); c.setCycleEnd(LocalDate.of(2026, 5, 31));
        BillingCycle savedCycle = cycleRepository.save(c);

        Invoice inv = new Invoice();
        inv.setAccountID(1001L); inv.setStatus(InvoiceStatus.Generated);
        inv.setDueDate(LocalDate.of(2026, 6, 15)); inv.setTotalAmount(BigDecimal.valueOf(500));
        inv.setCycle(savedCycle);
        savedInvoice = invoiceRepository.save(inv);
    }

    private Payment makePayment(String txRef, BigDecimal amount, PaymentMethod method, PaymentStatus status) {
        Payment p = new Payment();
        p.setInvoice(savedInvoice); p.setTransactionRef(txRef);
        p.setAmountPaid(amount); p.setPaymentMethod(method);
        p.setStatus(status); p.setPaymentDate(LocalDateTime.of(2026, 6, 10, 12, 0));
        return p;
    }

    @Test @DisplayName("save — id auto-generated")
    void save_idGenerated() {
        Payment p = paymentRepository.save(makePayment("TXN-001", BigDecimal.valueOf(500), PaymentMethod.UPI, PaymentStatus.Success));
        assertThat(p.getPaymentID()).isNotNull();
    }

    @Test @DisplayName("findById — returns saved payment")
    void findById_found() {
        Payment saved = paymentRepository.save(makePayment("TXN-001", BigDecimal.valueOf(500), PaymentMethod.UPI, PaymentStatus.Success));
        assertThat(paymentRepository.findById(saved.getPaymentID())).isPresent();
    }

    @Test @DisplayName("findById — empty for unknown id")
    void findById_notFound() {
        assertThat(paymentRepository.findById(99999L)).isEmpty();
    }

    @Test @DisplayName("delete — payment no longer found")
    void delete_removed() {
        Payment saved = paymentRepository.save(makePayment("TXN-001", BigDecimal.valueOf(500), PaymentMethod.UPI, PaymentStatus.Success));
        paymentRepository.deleteById(saved.getPaymentID());
        assertThat(paymentRepository.findById(saved.getPaymentID())).isEmpty();
    }

    @Test @DisplayName("findByTransactionRef — returns matching payment")
    void findByTxRef_match() {
        paymentRepository.save(makePayment("TXN-100", BigDecimal.valueOf(500), PaymentMethod.UPI, PaymentStatus.Success));
        Optional<Payment> result = paymentRepository.findByTransactionRef("TXN-100");
        assertThat(result).isPresent();
    }

    @Test @DisplayName("findByTransactionRef — empty for unknown ref")
    void findByTxRef_notFound() {
        assertThat(paymentRepository.findByTransactionRef("NOTEXIST")).isEmpty();
    }

    @Test @DisplayName("findByTransactionRef — amount matches")
    void findByTxRef_amountMatches() {
        paymentRepository.save(makePayment("TXN-200", BigDecimal.valueOf(750), PaymentMethod.Card, PaymentStatus.Success));
        Optional<Payment> result = paymentRepository.findByTransactionRef("TXN-200");
        assertThat(result.get().getAmountPaid()).isEqualByComparingTo(BigDecimal.valueOf(750));
    }

    @Test @DisplayName("sumPaidAmountByInvoiceId — returns sum of Success payments")
    void sumPaid_success() {
        paymentRepository.save(makePayment("TXN-1", BigDecimal.valueOf(200), PaymentMethod.UPI, PaymentStatus.Success));
        paymentRepository.save(makePayment("TXN-2", BigDecimal.valueOf(150), PaymentMethod.Card, PaymentStatus.Success));
        BigDecimal sum = paymentRepository.sumPaidAmountByInvoiceId(savedInvoice.getInvoiceID(), PaymentStatus.Success);
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(350));
    }

    @Test @DisplayName("sumPaidAmountByInvoiceId — returns 0 for no payments")
    void sumPaid_zero() {
        BigDecimal sum = paymentRepository.sumPaidAmountByInvoiceId(savedInvoice.getInvoiceID(), PaymentStatus.Success);
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("sumPaidAmountByInvoiceId — Failed payments not counted")
    void sumPaid_excludesFailed() {
        paymentRepository.save(makePayment("TXN-1", BigDecimal.valueOf(300), PaymentMethod.UPI, PaymentStatus.Failed));
        BigDecimal sum = paymentRepository.sumPaidAmountByInvoiceId(savedInvoice.getInvoiceID(), PaymentStatus.Success);
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("sumPaidAmountByInvoiceId — only Success counted in mixed batch")
    void sumPaid_onlySuccessCounted() {
        paymentRepository.save(makePayment("TXN-1", BigDecimal.valueOf(200), PaymentMethod.UPI, PaymentStatus.Success));
        paymentRepository.save(makePayment("TXN-2", BigDecimal.valueOf(100), PaymentMethod.Card, PaymentStatus.Failed));
        BigDecimal sum = paymentRepository.sumPaidAmountByInvoiceId(savedInvoice.getInvoiceID(), PaymentStatus.Success);
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(200));
    }
}
