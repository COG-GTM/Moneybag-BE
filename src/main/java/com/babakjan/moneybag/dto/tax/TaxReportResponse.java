package com.babakjan.moneybag.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaxReportResponse {
    private Long participantId;
    private int planYear;
    private String planType;
    private Double regularRothTotal;
    private Double regularPreTaxTotal;
    private Double catchUpRothTotal;
    private Double catchUpPreTaxTotal;
    private Double superCatchUpRothTotal;
    private String w2Box12Code;
    private Double w2Box12Amount;
    private String distributionCode1099R;
}
