package com.aerorule.sample;

public class Customer {
    public String industry;
    public double totalRevenue;
    public int riskScore;

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
}
