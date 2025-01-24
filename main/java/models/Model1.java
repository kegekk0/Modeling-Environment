package models;

//used in controller, called by String modelName
public class Model1 {
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
    @Bind private double[] PKB; // GDP

    public void run() {
        if (KI == null || KS == null || INW == null || EKS == null || IMP == null) {
            throw new IllegalStateException("Model fields are not initialized correctly.");
        }
        if (KI.length != LL || KS.length != LL || INW.length != LL || EKS.length != LL || IMP.length != LL) {
            throw new IllegalStateException("Model arrays do not match the number of years (LL).");
        }

        PKB = new double[LL];
        PKB[0] = KI[0] + KS[0] + INW[0] + EKS[0] - IMP[0];
        for (int t = 1; t < LL; t++) {
            KI[t] = twKI[t] * KI[t - 1];
            KS[t] = twKS[t] * KS[t - 1];
            INW[t] = twINW[t] * INW[t - 1];
            EKS[t] = twEKS[t] * EKS[t - 1];
            IMP[t] = twIMP[t] * IMP[t - 1];
            PKB[t] = KI[t] + KS[t] + INW[t] + EKS[t] - IMP[t];
        }
    }
    public void setLL(int LL){
        this.LL = LL;
    }

    public int getLL(){
        return LL;
    }
}
