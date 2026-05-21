package com.expensemanager.app.data.model;

import java.util.ArrayList;
import java.util.List;

public class FinancialInsights {
    public int healthScore;
    public String healthMessage;
    public List<String> alerts = new ArrayList<>();
    public String dailyAllowance;
    public String monthPrediction;
    public String monthComparison;
    public String feedMessage;
}
