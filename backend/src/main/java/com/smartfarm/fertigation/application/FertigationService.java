package com.smartfarm.fertigation.application;

import com.smartfarm.fertigation.domain.*;
import com.smartfarm.irrigation.application.CommandService;
import com.smartfarm.irrigation.domain.CommandLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FertigationService {

    private final FertigationRecipeRepository recipeRepo;
    private final FertigationLogRepository logRepo;
    private final CommandService commandService;

    public List<FertigationRecipe> listRecipes(Long tenantId) {
        return recipeRepo.findByTenantIdOrIsSystemTrue(tenantId);
    }

    public FertigationRecipe createRecipe(FertigationRecipe recipe) {
        return recipeRepo.save(recipe);
    }

    public FertigationRecipe updateRecipe(Long id, FertigationRecipe updates) {
        FertigationRecipe r = recipeRepo.findById(id).orElseThrow(() -> new RuntimeException("配方不存在"));
        if (updates.getName() != null) r.setName(updates.getName());
        if (updates.getNitrogenRatio() != null) r.setNitrogenRatio(updates.getNitrogenRatio());
        if (updates.getPhosphorusRatio() != null) r.setPhosphorusRatio(updates.getPhosphorusRatio());
        if (updates.getPotassiumRatio() != null) r.setPotassiumRatio(updates.getPotassiumRatio());
        if (updates.getEcTarget() != null) r.setEcTarget(updates.getEcTarget());
        if (updates.getPhTarget() != null) r.setPhTarget(updates.getPhTarget());
        if (updates.getConcentrationMlPerL() != null) r.setConcentrationMlPerL(updates.getConcentrationMlPerL());
        if (updates.getIrrigationDurationMin() != null) r.setIrrigationDurationMin(updates.getIrrigationDurationMin());
        if (updates.getNotes() != null) r.setNotes(updates.getNotes());
        return recipeRepo.save(r);
    }

    public void deleteRecipe(Long id) { recipeRepo.deleteById(id); }

    /**
     * 执行水肥一体化：先开施肥泵，再开灌溉阀
     */
    public FertigationLog execute(Long tenantId, String pumpDeviceId, String valveDeviceId,
                                   String greenhouseNo, Long recipeId, String triggeredBy) {
        FertigationRecipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("配方不存在: " + recipeId));

        FertigationLog flog = new FertigationLog();
        flog.setTenantId(tenantId);
        flog.setDeviceId(pumpDeviceId);
        flog.setValveDeviceId(valveDeviceId);
        flog.setGreenhouseNo(greenhouseNo);
        flog.setRecipeId(recipeId);
        flog.setConcentrationMlPerL(recipe.getConcentrationMlPerL());
        flog.setDurationMin(recipe.getIrrigationDurationMin() != null ? recipe.getIrrigationDurationMin() : 30);
        flog.setEstimatedFertilizerMl(recipe.getConcentrationMlPerL() != null ? recipe.getConcentrationMlPerL() * flog.getDurationMin() * 2 : 0);
        flog.setTriggeredBy(triggeredBy);
        flog.setStatus(FertigationLog.FertigationStatus.RUNNING);
        flog.setStartedAt(OffsetDateTime.now());
        logRepo.save(flog);

        try {
            // 开施肥泵
            commandService.sendCommand(tenantId, pumpDeviceId,
                    CommandLog.CommandAction.OPEN_VALVE,
                    Map.of("duration_min", flog.getDurationMin(),
                           "concentration_ml_per_l", recipe.getConcentrationMlPerL() != null ? recipe.getConcentrationMlPerL() : 0,
                           "fertigation", true),
                    triggeredBy);

            // 开灌溉阀
            commandService.sendCommand(tenantId, valveDeviceId,
                    CommandLog.CommandAction.OPEN_VALVE,
                    Map.of("duration_min", flog.getDurationMin(), "fertigation", true),
                    triggeredBy);

            log.info("水肥执行: pump={}, valve={}, recipe={}, duration={}min",
                    pumpDeviceId, valveDeviceId, recipe.getName(), flog.getDurationMin());
        } catch (Exception e) {
            flog.setStatus(FertigationLog.FertigationStatus.FAILED);
            flog.setFailureReason(e.getMessage());
            logRepo.save(flog);
            throw e;
        }

        return flog;
    }

    public List<FertigationLog> listLogs(Long tenantId) {
        return logRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
