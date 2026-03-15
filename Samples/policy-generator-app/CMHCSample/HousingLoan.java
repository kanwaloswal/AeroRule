package com.aerorule.sample;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousingLoan {
    private String loanId;
    private String loanType; // "HomeownerPurchase", "SmallRental", "HomeownerRefinance"
    private BigDecimal unpaidPrincipalBalance;
    private BigDecimal ltvRatio; // e.g. 0.95 for 95% LTV
    private BigDecimal lendingValue;
    private BigDecimal purchasePrice;
    private BigDecimal downPaymentAmount;
    private String downPaymentType; // "Traditional", "NonTraditional"
    private String amortizationPeriod; // "25 years", "30 years", "HomeStart"
    private String status; // "advanced", "default", "repaid"
    private Date coverageEffectiveDate;
    private Date defaultDate;
    private BigDecimal gdsRatio; // Gross Debt Service
    private BigDecimal tdsRatio; // Total Debt Service
    private BigDecimal contractInterestRate; // used for stress-test (max(contract + 2%, 5.25%))
    private int numberOfUnits; // 1–4
    private boolean ecoPlusEligible;
    private BigDecimal ecoImprovementAmount; // for Eco Improvement refund
}