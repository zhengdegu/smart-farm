package com.smartfarm.tenant.application;

import com.smartfarm.tenant.domain.Tenant;
import com.smartfarm.tenant.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepo;

    public List<Tenant> listAll() { return tenantRepo.findAll(); }

    public Tenant getById(Long id) {
        return tenantRepo.findById(id).orElseThrow(() -> new RuntimeException("租户不存在: " + id));
    }

    public Tenant getByCode(String code) {
        return tenantRepo.findByCode(code).orElseThrow(() -> new RuntimeException("租户不存在: " + code));
    }

    public Tenant create(Tenant tenant) {
        if (tenantRepo.existsByCode(tenant.getCode())) {
            throw new RuntimeException("租户编码已存在: " + tenant.getCode());
        }
        return tenantRepo.save(tenant);
    }

    public Tenant update(Long id, Tenant updates) {
        Tenant t = getById(id);
        if (updates.getName() != null) t.setName(updates.getName());
        if (updates.getContactName() != null) t.setContactName(updates.getContactName());
        if (updates.getContactPhone() != null) t.setContactPhone(updates.getContactPhone());
        if (updates.getAddress() != null) t.setAddress(updates.getAddress());
        if (updates.getLatitude() != null) t.setLatitude(updates.getLatitude());
        if (updates.getLongitude() != null) t.setLongitude(updates.getLongitude());
        if (updates.getPlan() != null) t.setPlan(updates.getPlan());
        if (updates.getMaxDevices() != null) t.setMaxDevices(updates.getMaxDevices());
        if (updates.getMaxUsers() != null) t.setMaxUsers(updates.getMaxUsers());
        return tenantRepo.save(t);
    }

    public void suspend(Long id) {
        Tenant t = getById(id);
        t.setStatus(Tenant.TenantStatus.SUSPENDED);
        tenantRepo.save(t);
    }

    public void activate(Long id) {
        Tenant t = getById(id);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        tenantRepo.save(t);
    }
}
