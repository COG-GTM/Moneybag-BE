package com.babakjan.moneybag.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class YearEndTaxSummaryResponse {
    private Long participantId;
    private int planYear;
    private List<TaxReportResponse> entries;
    private Double totalRothCatchUp;
    private Double totalRegularRoth;
    private Double totalPreTax;
    private Double grandTotal;
}
