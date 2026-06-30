package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.LateFeeRequest;
import com.teleConnect.billing.inovice.dto.request.PayInvoiceRequest;
import com.teleConnect.billing.inovice.dto.request.WaiveLateFeeRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {

    // GET /invoices/{invoiceId}
    ApiResponse<InvoiceResponse> getInvoiceById(Long invoiceId);

    // GET /invoices/account/{accountId}
    ApiResponse<Page<InvoiceResponse>> getInvoicesByAccount(Long accountId, String status, Pageable pageable);

    // POST /invoices/{invoiceId}/pay
    ApiResponse<Void> recordPayment(Long invoiceId, PayInvoiceRequest request);

    // POST /invoices/{invoiceId}/latefee
    ApiResponse<Void> applyLateFee(Long invoiceId, LateFeeRequest request);

    // POST /invoices/{invoiceId}/latefee/waive
    ApiResponse<Void> waiveLateFee(Long invoiceId, WaiveLateFeeRequest request);
}
