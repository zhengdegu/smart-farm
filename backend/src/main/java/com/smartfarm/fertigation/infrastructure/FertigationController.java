package com.smartfarm.fertigation.infrastructure;

import com.smartfarm.fertigation.application.FertigationService;
import com.smartfarm.fertigation.domain.FertigationLog;
import com.smartfarm.fertigation.domain.FertigationRecipe;
import com.smartfarm.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fertigation")
@RequiredArgsConstructor
@Tag(name = "水肥一体化", description = "水肥配方管理、施肥执行")
public class FertigationController {

    private final FertigationService fertigationService;

    @GetMapping("/recipes")
    @Operation(summary = "配方列表")
    public List<FertigationRecipe> listRecipes() {
        return fertigationService.listRecipes(SecurityUtils.currentTenantId());
    }

    @PostMapping("/recipes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建配方")
    public FertigationRecipe createRecipe(@RequestBody FertigationRecipe recipe) {
        recipe.setTenantId(SecurityUtils.currentTenantId());
        return fertigationService.createRecipe(recipe);
    }

    @PutMapping("/recipes/{id}")
    @Operation(summary = "更新配方")
    public FertigationRecipe updateRecipe(@PathVariable Long id, @RequestBody FertigationRecipe recipe) {
        return fertigationService.updateRecipe(id, recipe);
    }

    @DeleteMapping("/recipes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除配方")
    public void deleteRecipe(@PathVariable Long id) {
        fertigationService.deleteRecipe(id);
    }

    @PostMapping("/execute")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "执行水肥灌溉", description = "同时开启施肥泵和灌溉阀")
    public FertigationLog execute(@RequestBody ExecuteRequest req) {
        return fertigationService.execute(
                SecurityUtils.currentTenantId(),
                req.getPumpDeviceId(), req.getValveDeviceId(),
                req.getGreenhouseNo(), req.getRecipeId(),
                "USER:" + SecurityUtils.currentUserId());
    }

    @GetMapping("/logs")
    @Operation(summary = "施肥记录")
    public List<FertigationLog> listLogs() {
        return fertigationService.listLogs(SecurityUtils.currentTenantId());
    }

    @Data
    public static class ExecuteRequest {
        private String pumpDeviceId;
        private String valveDeviceId;
        private String greenhouseNo;
        private Long recipeId;
    }
}
