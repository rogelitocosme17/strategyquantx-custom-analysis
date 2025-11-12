package SQ.CustomAnalysis;

import com.strategyquant.lib.*;

import java.util.ArrayList;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class CustomFilter extends CustomAnalysisMethod {
	private static final double MinSDFilter_THRESHOLD = 50.0;
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
    /**
     * set the type of CA snippet here - it is either used as:
     * - strategy filter - it will call filterStrategy() method for one strategy
     * - databank processor - it will call processDatabank() for all strategies in databank
     * 
     * Uncomment the one you want to use.
     */
	public CustomFilter() {
		super("CustomFilter", TYPE_FILTER_STRATEGY);
		//super("CustomFilter", TYPE_PROCESS_DATABANK);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
		Result mainResult = rg.subResult(rg.getMainResultKey());
		SQStats stats = mainResult.stats(Directions.Both, PlTypes.Money, SampleTypes.InSample);

		String raw = getInputArgs();
		double value = Double.parseDouble(raw.trim());

		double score = computeScore(stats);
		return score >= value;
	}

	private static double computeScore(SQStats s) {
		double retDD = 0;
		double stability = 0;
		double profitFactor = 0;
		double sharpeRatio = 0;
		double expectancyScore = 0;
		double stagnationDays = 0;

		if (s != null) {
			retDD           = safeGet(s, "ReturnDDRatio");
			stability       = safeGet(s, "Stability");
			profitFactor    = safeGet(s, "ProfitFactor");
			sharpeRatio     = safeGet(s, "SharpeRatio");
			expectancyScore = safeGet(s, "RExpectancy");
			stagnationDays  = safeGet(s, "Stagnation");
		}

		// --- Weights identical to Variant 1 ---
		final double W_RETDD   = 1.4;
		final double W_STAB    = 3.8;
		final double W_PF      = 2.2;
		final double W_SHARPE  = 1.6;
		final double W_EXP     = 0.6;
		final double W_STAGNEG = 0.008;

		// 1) Compute raw score
		double raw = (W_RETDD * retDD)
				+ (W_STAB * stability)
				+ (W_PF * profitFactor)
				+ (W_SHARPE * sharpeRatio)
				+ (W_EXP * expectancyScore)
				- (W_STAGNEG * stagnationDays);

		// 2) Normalize raw value to 0–100 range
		final double MIN_EXPECTED = 0.0;   // expected minimum raw score
		final double MAX_EXPECTED = 20.0;  // expected maximum raw score
		double norm;

		if (MAX_EXPECTED > MIN_EXPECTED) {
			norm = 100.0 * (raw - MIN_EXPECTED) / (MAX_EXPECTED - MIN_EXPECTED);
		} else {
			norm = 0.0;
		}

		// 3) Clamp to 0–100
		if (norm < 0.0)   norm = 0.0;
		if (norm > 100.0) norm = 100.0;

		return norm;
	}

	// Safe getter (avoids NaN, null, or missing stats)
	private static double safeGet(SQStats stats, String key) {
		try {
			double v = stats.getDouble(key);
			return Double.isFinite(v) ? v : 0.0;
		} catch (Throwable t) {
			return 0.0;
		}
	}
	
	//------------------------------------------------------------------------
	
	@Override
	public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
		return databankRG;
	}

	private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r");
    }
}