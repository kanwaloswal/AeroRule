public class Customer {
    private String id;
    private int riskScore;
    private double annualRevenue;

    public Customer() {
    }

    public Customer(String id, int riskScore, double annualRevenue) {
        this.id = id;
        this.riskScore = riskScore;
        this.annualRevenue = annualRevenue;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public double getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(double annualRevenue) { this.annualRevenue = annualRevenue; }
}
