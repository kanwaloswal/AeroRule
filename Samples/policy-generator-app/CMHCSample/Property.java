package com.aerorule.sample;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    private String propertyId;
    private String description;
    private boolean goodAndMarketableTitle;
    private List<String> priorEncumbrances;
    private boolean ownerOccupied;
    private int numberOfUnits; // 1–4 (affects LTV & equity rules)
    private BigDecimal improvementAmount; // for CMHC Improvement / Eco Improvement
}