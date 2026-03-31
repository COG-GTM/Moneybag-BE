package com.babakjan.moneybag.controller;

import com.babakjan.moneybag.entity.*;
import com.babakjan.moneybag.error.exception.ContributionValidationException;
import com.babakjan.moneybag.service.ContributionService;
import com.babakjan.moneybag.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContributionController.class)
@Import(JwtService.class)
class ContributionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContributionService contributionService;

    private final String ROOT_URL = "/contributions";

    @Test
    @WithMockUser
    void testCreateContribution_accepted() throws Exception {
        User user = User.builder().id(1L).firstName("John").lastName("Doe").build();
        Participant participant = Participant.builder()
                .id(1L)
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .priorYearFicaWages(130000.0)
                .user(user)
                .build();
        RetirementPlan plan = RetirementPlan.builder()
                .id(1L)
                .planType(PlanType.PLAN_401K)
                .planName("Company 401k")
                .employerName("Acme Corp")
                .build();
        Contribution contribution = Contribution.builder()
                .id(1L)
                .participant(participant)
                .plan(plan)
                .amount(5000.0)
                .contributionType(ContributionType.CATCH_UP)
                .taxTreatment(TaxTreatment.PRE_TAX)
                .payrollDate(LocalDate.of(2026, 6, 15))
                .build();

        given(contributionService.save(any()))
                .willReturn(contribution);

        String requestContent = """
                {
                    "participantId": 1,
                    "planId": 1,
                    "amount": 5000.0,
                    "contributionType": "CATCH_UP",
                    "taxTreatment": "PRE_TAX",
                    "payrollDate": "2026-06-15"
                }""";

        mockMvc
                .perform(
                        post(ROOT_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestContent)
                                .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.contributionType").value("CATCH_UP"))
                .andExpect(jsonPath("$.taxTreatment").value("PRE_TAX"));
    }

    @Test
    @WithMockUser
    void testCreateContribution_rejected_rothRequired() throws Exception {
        given(contributionService.save(any()))
                .willThrow(new ContributionValidationException(
                        "SECURE 2.0 Section 603: Catch-up contributions for participants with prior-year FICA wages exceeding the threshold must be designated as Roth contributions."));

        String requestContent = """
                {
                    "participantId": 1,
                    "planId": 1,
                    "amount": 5000.0,
                    "contributionType": "CATCH_UP",
                    "taxTreatment": "PRE_TAX",
                    "payrollDate": "2026-06-15"
                }""";

        mockMvc
                .perform(
                        post(ROOT_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestContent)
                                .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.contribution").exists());
    }

    @Test
    @WithMockUser
    void testGetContributionById() throws Exception {
        User user = User.builder().id(1L).firstName("John").lastName("Doe").build();
        Participant participant = Participant.builder()
                .id(1L)
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .priorYearFicaWages(130000.0)
                .user(user)
                .build();
        RetirementPlan plan = RetirementPlan.builder()
                .id(1L)
                .planType(PlanType.PLAN_401K)
                .planName("Company 401k")
                .employerName("Acme Corp")
                .build();
        Contribution contribution = Contribution.builder()
                .id(1L)
                .participant(participant)
                .plan(plan)
                .amount(5000.0)
                .contributionType(ContributionType.REGULAR)
                .taxTreatment(TaxTreatment.PRE_TAX)
                .payrollDate(LocalDate.of(2026, 6, 15))
                .build();

        given(contributionService.getById(1L))
                .willReturn(contribution);

        mockMvc
                .perform(get(ROOT_URL + "/{id}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(5000.0));
    }

    @Test
    @WithMockUser
    void testGetContributionsByParticipantId() throws Exception {
        User user = User.builder().id(1L).firstName("John").lastName("Doe").build();
        Participant participant = Participant.builder()
                .id(1L)
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .priorYearFicaWages(130000.0)
                .user(user)
                .build();
        RetirementPlan plan = RetirementPlan.builder()
                .id(1L)
                .planType(PlanType.PLAN_401K)
                .planName("Company 401k")
                .employerName("Acme Corp")
                .build();
        Contribution contribution = Contribution.builder()
                .id(1L)
                .participant(participant)
                .plan(plan)
                .amount(5000.0)
                .contributionType(ContributionType.REGULAR)
                .taxTreatment(TaxTreatment.PRE_TAX)
                .payrollDate(LocalDate.of(2026, 6, 15))
                .build();

        given(contributionService.getByParticipantId(1L))
                .willReturn(List.of(contribution));

        mockMvc
                .perform(get(ROOT_URL + "/participants/{participantId}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
