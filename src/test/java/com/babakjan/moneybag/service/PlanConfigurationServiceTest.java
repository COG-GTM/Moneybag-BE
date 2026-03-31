package com.babakjan.moneybag.service;

import com.babakjan.moneybag.dto.plan.PlanConfigurationDto;
import com.babakjan.moneybag.dto.plan.UpdatePlanConfigRequest;
import com.babakjan.moneybag.entity.PlanConfiguration;
import com.babakjan.moneybag.entity.PlanConfigurationAuditLog;
import com.babakjan.moneybag.error.exception.PlanNotFoundException;
import com.babakjan.moneybag.repository.PlanConfigurationAuditLogRepository;
import com.babakjan.moneybag.repository.PlanConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PlanConfigurationServiceTest {

    @Autowired
    private PlanConfigurationService planConfigurationService;

    @Autowired
    private PlanConfigurationRepository planConfigurationRepository;

    @Autowired
    private PlanConfigurationAuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        planConfigurationRepository.deleteAll();
    }

    @Test
    void testCreatePlanConfiguration() {
        PlanConfigurationDto dto = PlanConfigurationDto.builder()
                .planId("NEW-PLAN-001")
                .planName("New Test Plan")
                .planType("401K")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(null)
                .governmentalSpecialCatchUp(false)
                .build();

        PlanConfiguration created = planConfigurationService.createPlanConfiguration(dto);

        assertNotNull(created.getId());
        assertEquals("NEW-PLAN-001", created.getPlanId());
        assertEquals("New Test Plan", created.getPlanName());
        assertEquals("401K", created.getPlanType());
        assertTrue(created.getRothCatchUpAvailable());
    }

    @Test
    void testGetByPlanId() throws PlanNotFoundException {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("GET-PLAN-001")
                .planName("Existing Plan")
                .planType("403B")
                .rothCatchUpAvailable(false)
                .governmentalSpecialCatchUp(false)
                .build());

        PlanConfiguration found = planConfigurationService.getByPlanId("GET-PLAN-001");

        assertEquals("GET-PLAN-001", found.getPlanId());
        assertEquals("403B", found.getPlanType());
    }

    @Test
    void testGetByPlanId_NotFound() {
        assertThrows(PlanNotFoundException.class,
                () -> planConfigurationService.getByPlanId("NON-EXISTENT"));
    }

    // Audit log test: updating rothCatchUpAvailable creates an audit log entry
    @Test
    void testUpdatePlanConfiguration_AuditLogCreated() throws PlanNotFoundException {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("AUDIT-PLAN-001")
                .planName("Audit Test Plan")
                .planType("401K")
                .rothCatchUpAvailable(false)
                .governmentalSpecialCatchUp(false)
                .build());

        UpdatePlanConfigRequest updateRequest = UpdatePlanConfigRequest.builder()
                .rothCatchUpAvailable(true)
                .changedBy("admin@company.com")
                .reason("SECURE 2.0 compliance amendment")
                .build();

        PlanConfiguration updated = planConfigurationService.updatePlanConfiguration("AUDIT-PLAN-001", updateRequest);

        assertTrue(updated.getRothCatchUpAvailable());

        List<PlanConfigurationAuditLog> logs = planConfigurationService.getAuditLog("AUDIT-PLAN-001");
        assertFalse(logs.isEmpty());

        PlanConfigurationAuditLog log = logs.get(0);
        assertEquals("AUDIT-PLAN-001", log.getPlanId());
        assertEquals("rothCatchUpAvailable", log.getFieldChanged());
        assertEquals("false", log.getOldValue());
        assertEquals("true", log.getNewValue());
        assertEquals("admin@company.com", log.getChangedBy());
        assertEquals("SECURE 2.0 compliance amendment", log.getReason());
        assertNotNull(log.getTimestamp());
    }

    // Audit log test: updating rothEffectiveDate creates an audit log entry
    @Test
    void testUpdatePlanConfiguration_EffectiveDateAuditLog() throws PlanNotFoundException {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("AUDIT-PLAN-002")
                .planName("Audit Test Plan 2")
                .planType("401K")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(null)
                .governmentalSpecialCatchUp(false)
                .build());

        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.APRIL, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date newEffectiveDate = cal.getTime();

        UpdatePlanConfigRequest updateRequest = UpdatePlanConfigRequest.builder()
                .rothEffectiveDate(newEffectiveDate)
                .changedBy("hr@company.com")
                .reason("Mid-year plan amendment")
                .build();

        PlanConfiguration updated = planConfigurationService.updatePlanConfiguration("AUDIT-PLAN-002", updateRequest);

        assertNotNull(updated.getRothEffectiveDate());

        List<PlanConfigurationAuditLog> logs = planConfigurationService.getAuditLog("AUDIT-PLAN-002");
        assertFalse(logs.isEmpty());

        PlanConfigurationAuditLog log = logs.get(0);
        assertEquals("rothEffectiveDate", log.getFieldChanged());
        assertEquals("null", log.getOldValue());
        assertEquals("hr@company.com", log.getChangedBy());
        assertEquals("Mid-year plan amendment", log.getReason());
    }

    // Test: isRothAvailableForDate with effective date in the past
    @Test
    void testIsRothAvailableForDate_AfterEffective() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("DATE-PLAN-001")
                .planName("Date Test Plan")
                .planType("401K")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(cal.getTime())
                .governmentalSpecialCatchUp(false)
                .build());

        Calendar contributionCal = Calendar.getInstance();
        contributionCal.set(2025, Calendar.APRIL, 15, 0, 0, 0);
        contributionCal.set(Calendar.MILLISECOND, 0);

        assertTrue(planConfigurationService.isRothAvailableForDate("DATE-PLAN-001", contributionCal.getTime()));
    }

    // Test: isRothAvailableForDate with effective date in the future
    @Test
    void testIsRothAvailableForDate_BeforeEffective() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.APRIL, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("DATE-PLAN-002")
                .planName("Date Test Plan")
                .planType("401K")
                .rothCatchUpAvailable(true)
                .rothEffectiveDate(cal.getTime())
                .governmentalSpecialCatchUp(false)
                .build());

        Calendar contributionCal = Calendar.getInstance();
        contributionCal.set(2025, Calendar.MARCH, 15, 0, 0, 0);
        contributionCal.set(Calendar.MILLISECOND, 0);

        assertFalse(planConfigurationService.isRothAvailableForDate("DATE-PLAN-002", contributionCal.getTime()));
    }

    // Test: isRothAvailableForDate when Roth not available
    @Test
    void testIsRothAvailableForDate_RothNotAvailable() {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("DATE-PLAN-003")
                .planName("No Roth Plan")
                .planType("401K")
                .rothCatchUpAvailable(false)
                .governmentalSpecialCatchUp(false)
                .build());

        assertFalse(planConfigurationService.isRothAvailableForDate("DATE-PLAN-003", new Date()));
    }

    // Test: isRothAvailableForDate for non-existent plan
    @Test
    void testIsRothAvailableForDate_PlanNotFound() {
        assertFalse(planConfigurationService.isRothAvailableForDate("NON-EXISTENT", new Date()));
    }

    // Test: multiple audit log entries for multiple updates
    @Test
    void testMultipleAuditLogEntries() throws PlanNotFoundException {
        planConfigurationRepository.save(PlanConfiguration.builder()
                .planId("MULTI-AUDIT-001")
                .planName("Multi Audit Plan")
                .planType("401K")
                .rothCatchUpAvailable(false)
                .governmentalSpecialCatchUp(false)
                .build());

        // First update
        planConfigurationService.updatePlanConfiguration("MULTI-AUDIT-001",
                UpdatePlanConfigRequest.builder()
                        .rothCatchUpAvailable(true)
                        .changedBy("admin@company.com")
                        .reason("Enable Roth")
                        .build());

        // Second update
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JULY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        planConfigurationService.updatePlanConfiguration("MULTI-AUDIT-001",
                UpdatePlanConfigRequest.builder()
                        .rothEffectiveDate(cal.getTime())
                        .changedBy("hr@company.com")
                        .reason("Set effective date")
                        .build());

        List<PlanConfigurationAuditLog> logs = planConfigurationService.getAuditLog("MULTI-AUDIT-001");
        assertEquals(2, logs.size());
    }
}
