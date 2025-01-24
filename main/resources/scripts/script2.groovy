package scripts

CumulativeConsumption = new double[LL]
NetInvestment = new double[LL]

CumulativeConsumption[0] = KI[0] + KS[0]
NetInvestment[0] = INW[0] - IMP[0]

for(i = 1; i < LL; i++)
CumulativeConsumption[i] = CumulativeConsumption[i - 1] + KI[i] + KS[i]

for(i = 1; i < LL; i++)
NetInvestment[i] = NetInvestment[i - 1] + INW[i] - IMP[i];