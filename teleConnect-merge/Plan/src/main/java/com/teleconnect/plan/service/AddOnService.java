package com.teleconnect.plan.service;

import com.teleconnect.plan.dto.request.AddOnRequest;
import com.teleconnect.plan.dto.response.AddOnResponse;
import com.teleconnect.plan.entity.AddOn;
import com.teleconnect.plan.repository.AddOnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddOnService {

    @Autowired
    private AddOnRepository repository;

    private AddOnResponse toDTO(AddOn a) {
        AddOnResponse dto = new AddOnResponse();
        dto.setAddOnId(a.getAddOnId());
        dto.setName(a.getName());
        dto.setType(a.getType().name());
        dto.setQuota(a.getQuota());
        dto.setValidityDays(a.getValidityDays());
        dto.setPrice(a.getPrice());
        dto.setStatus(a.getStatus().name());
        return dto;
    }

    public void createAddOn(AddOnRequest req) {
        AddOn addOn = new AddOn();
        addOn.setName(req.getName());
        addOn.setType(AddOn.AddOnType.valueOf(req.getType()));
        addOn.setQuota(req.getQuota());
        addOn.setValidityDays(req.getValidityDays());
        addOn.setPrice(req.getPrice());
        addOn.setStatus(AddOn.AddOnStatus.A);
        repository.save(addOn);
    }

    public List<AddOnResponse> getAllAddOns() {
        return repository.findAll()
            .stream().map(this::toDTO)
            .collect(Collectors.toList());
    }

    public AddOnResponse getAddOnById(Integer addOnId) {
        AddOn addOn = repository.findById(addOnId).orElse(null);
        if (addOn == null) return null;
        return toDTO(addOn);
    }
}
