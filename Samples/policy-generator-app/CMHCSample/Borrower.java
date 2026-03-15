package com.aerorule.sample;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Borrower {
    private String borrowerId;
    private int creditScore; // must be >= 600
    private BigDecimal income;
    private BigDecimal equity;
    private boolean isNonPermanentResident;
    private boolean isSelfEmployed;
    private List<String> persons; // borrower, co-borrower, guarantor names/roles
    private String creditWorthiness;
}