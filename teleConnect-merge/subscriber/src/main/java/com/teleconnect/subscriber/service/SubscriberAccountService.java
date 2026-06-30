package com.teleconnect.subscriber.service;

import com.teleconnect.subscriber.dto.request.*;
import com.teleconnect.subscriber.dto.response.*;
import com.teleconnect.subscriber.entity.SubscriberAccount;
import com.teleconnect.subscriber.entity.SimLine;
import com.teleconnect.subscriber.repository.SubscriberAccountRepository;
import com.teleconnect.subscriber.repository.SimLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriberAccountService {

    @Autowired private SubscriberAccountRepository accountRepo;
    @Autowired private SimLineRepository simLineRepo;

    private AccountResponseDTO toDTO(SubscriberAccount a) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setAccountId(a.getAccountId());
        dto.setSubscriberId(a.getSubscriberId());
        dto.setAccountType(a.getAccountType().name());
        dto.setRegistrationDate(a.getRegistrationDate());
        dto.setKycStatus(a.getKycStatus().name());
        dto.setStatus(a.getStatus().name());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    public MessageDTO createAccount(CreateAccountRequest req) {
        SubscriberAccount account = new SubscriberAccount();
        account.setSubscriberId(req.getSubscriberId());
        account.setAccountType(
            SubscriberAccount.AccountType.valueOf(req.getAccountType()));
        account.setKycStatus(
            SubscriberAccount.KycStatus.valueOf(req.getKycStatus()));
        accountRepo.save(account);
        return new MessageDTO("Account created successfully");
    }

    public AccountResponseDTO getAccountById(Integer accountId) {
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        return toDTO(account);
    }

    public AccountListResponseDTO getAllAccounts(String status, Long subscriberId) {
        List<SubscriberAccount> all = accountRepo.findAll();
        if (status != null) {
            SubscriberAccount.AccountStatus s =
                SubscriberAccount.AccountStatus.valueOf(status);
            all = all.stream()
                .filter(a -> a.getStatus() == s)
                .collect(Collectors.toList());
        }
        if (subscriberId != null) {
            all = all.stream()
                .filter(a -> a.getSubscriberId().equals(subscriberId))
                .collect(Collectors.toList());
        }
        List<AccountResponseDTO> dtos = all.stream()
            .map(this::toDTO).collect(Collectors.toList());
        return new AccountListResponseDTO(dtos, dtos.size());
    }

    public List<AccountResponseDTO> getExpiredKycAccounts() {
        return accountRepo
            .findByKycStatus(SubscriberAccount.KycStatus.Expired)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public MessageDTO updateKyc(Integer accountId, UpdateKycRequest req) {
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        account.setKycStatus(
            SubscriberAccount.KycStatus.valueOf(req.getKycStatus()));
        accountRepo.save(account);
        return new MessageDTO("KYC status updated to " + req.getKycStatus());
    }

    public MessageDTO updateStatus(Integer accountId,
                                   UpdateAccountStatusRequest req) {
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        account.setStatus(
            SubscriberAccount.AccountStatus.valueOf(req.getStatus()));
        accountRepo.save(account);
        return new MessageDTO("Account status updated to " + req.getStatus());
    }

    public MessageDTO deleteAccount(Integer accountId) {
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        List<SimLine> simLines = simLineRepo.findByAccountId(accountId);
        long count = simLines.stream()
            .filter(sl -> sl.getStatus() == SimLine.SimStatus.Active)
            .peek(sl -> sl.setStatus(SimLine.SimStatus.Deactivated))
            .peek(simLineRepo::save)
            .count();
        account.setStatus(SubscriberAccount.AccountStatus.Terminated);
        accountRepo.save(account);
        return new MessageDTO("Account " + accountId +
            " terminated. SIM lines deactivated: " + count);
    }
}