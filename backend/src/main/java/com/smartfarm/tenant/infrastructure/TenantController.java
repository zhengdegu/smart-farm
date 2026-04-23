package com.smartfarm.tenant.infrastructure;

import com.smartfarm.tenant.application.TenantService;
import com.smartfarm.tenant.domain.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "多租户SaaS管理")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "租户列表")
    public List<Tenant> list() { return tenantService.listAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "租户详情")
    public Tenant get(@PathVariable Long id) { return tenantService.getById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建租户")
    public Tenant create(@RequestBody Tenant tenant) { return tenantService.create(tenant); }

    @PutMapping("/{id}")
    @Operation(summary = "更新租户")
    public Tenant update(@PathVariable Long id, @RequestBody Tenant tenant) {
        return tenantService.update(id, tenant);
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "停用租户")
    public void suspend(@PathVariable Long id) { tenantService.suspend(id); }

    @PostMapping("/{id}/activate")
    @Operation(summary = "启用租户")
    public void activate(@PathVariable Long id) { tenantService.activate(id); }
}
