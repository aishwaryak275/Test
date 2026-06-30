package com.teleconnect.plan.service;

import com.teleconnect.plan.dto.request.ServiceSubscriptionRequest;
import com.teleconnect.plan.dto.response.ServiceSubscriptionResponse;
import com.teleconnect.plan.entity.ServiceSubscription;
import com.teleconnect.plan.entity.TelecomPlan;
import com.teleconnect.plan.repository.ServiceSubscriptionRepository;
import com.teleconnect.plan.repository.TelecomPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceSubscriptionService {

    @Autowired
    private ServiceSubscriptionRepository repository;

    @Autowired
    private TelecomPlanRepository planRepository;

    private ServiceSubscriptionResponse toDTO(ServiceSubscription s) {
        ServiceSubscriptionResponse dto = new ServiceSubscriptionResponse();
        dto.setSubscriptionId(s.getSubscriptionId());
        dto.setLineId(s.getLineId());
        dto.setPlanId(s.getPlanId());
        dto.setAddOnId(s.getAddOnId());
        dto.setActivationDate(s.getActivationDate());
        dto.setExpiryDate(s.getExpiryDate());
        dto.setRenewalType(s.getRenewalType().name());
        dto.setStatus(s.getStatus().name());
        return dto;
    }

    public String validate(ServiceSubscriptionRequest req) {
        if (req.getLineId() == null)
            return "lineId is required";
        if (req.getPlanId() == null)
            return "planId is required";
        if (req.getActivationDate() == null)
            return "activationDate is required";
        if (req.getExpiryDate() == null)
            return "expiryDate is required";
        if (req.getExpiryDate() != null
                && req.getActivationDate() != null
                && !req.getExpiryDate().isAfter(req.getActivationDate()))
            return "expiryDate must be after activationDate";
        if (req.getRenewalType() == null)
            return "renewalType must be AutoRenew or Manual";
        TelecomPlan plan = planRepository
            .findById(req.getPlanId()).orElse(null);
        if (plan == null)
            return "Plan with planId " + req.getPlanId() + " not found";
        return null;
    }

    public void createSubscription(ServiceSubscriptionRequest req) {
        ServiceSubscription sub = new ServiceSubscription();
        sub.setLineId(req.getLineId());
        sub.setPlanId(req.getPlanId());
        sub.setAddOnId(req.getAddOnId());
        sub.setActivationDate(req.getActivationDate());
        sub.setExpiryDate(req.getExpiryDate());
        sub.setRenewalType(
            ServiceSubscription.RenewalType.valueOf(req.getRenewalType()));
        sub.setStatus(ServiceSubscription.Status.A);
        repository.save(sub);
    }

    public List<ServiceSubscriptionResponse> getAllSubscriptions() {
        return repository.findAll()
            .stream().map(this::toDTO)
            .collect(Collectors.toList());
    }

    public ServiceSubscriptionResponse getById(Integer subscriptionId) {
        ServiceSubscription sub = repository
            .findById(subscriptionId).orElse(null);
        if (sub == null) return null;
        return toDTO(sub);
    }

    public boolean updateSubscription(Integer subscriptionId,
            ServiceSubscriptionRequest req) {
        ServiceSubscription existing = repository
            .findById(subscriptionId).orElse(null);
        if (existing == null) return false;
        if (req.getAddOnId() != null)
            existing.setAddOnId(req.getAddOnId());
        if (req.getRenewalType() != null)
            existing.setRenewalType(ServiceSubscription.RenewalType
                .valueOf(req.getRenewalType()));
        if (req.getStatus() != null)
        existing.setStatus(ServiceSubscription.Status
            .valueOf(req.getStatus()));
        repository.save(existing);
        return true;
    }
}