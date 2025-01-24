package models;

public class Model2 {
    @Bind private int LL; // Number of years
    @Bind private double[] twKI; // Growth rate of private consumption
    @Bind private double[] twKS; // Growth rate of public consumption
    @Bind private double[] twINW; // Investment growth
    @Bind private double[] twEKS; // Export growth
    @Bind private double[] twIMP; // Import growth
    @Bind private double[] KI; // Private consumption
    @Bind private double[] KS; // Public consumption
    @Bind private double[] INW; // Investments
    @Bind private double[] EKS; // Export
    @Bind private double[] IMP; // Import
    @Bind private double[] CumulativeConsumption; // Cumulative consumption over years
    @Bind private double[] NetInvestment; // Net investment over years

    public void run() {
        if (KI == null || KS == null || INW == null || EKS == null || IMP == null) {
            throw new IllegalStateException("Model fields are not initialized correctly.");
        }

        System.out.println("model2 " + LL);
        if (KI.length != LL || KS.length != LL || INW.length != LL || EKS.length != LL || IMP.length != LL) {
            throw new IllegalStateException("Model arrays do not match the number of years (LL).");
        }

        CumulativeConsumption = new double[LL];
        NetInvestment = new double[LL];

        // Initial values for year 0
        CumulativeConsumption[0] = KI[0] + KS[0];
        NetInvestment[0] = INW[0] - IMP[0];

        // Calculate cumulative consumption and net investment for each year
        for (int t = 1; t < LL; t++) {
            KI[t] = twKI[t] * KI[t - 1];
            KS[t] = twKS[t] * KS[t - 1];
            INW[t] = twINW[t] * INW[t - 1];
            EKS[t] = twEKS[t] * EKS[t - 1];
            IMP[t] = twIMP[t] * IMP[t - 1];

            // Cumulative consumption is the sum of private and public consumption
            CumulativeConsumption[t] = CumulativeConsumption[t - 1] + KI[t] + KS[t];

            // Net investment is the difference between investments and imports
            NetInvestment[t] = NetInvestment[t - 1] + INW[t] - IMP[t];
        }

    }
}
