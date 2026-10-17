package com.heddy.domain.recommendation.service;

import java.util.List;

/** Soft intersection-over-union of normalized hair masks, not a face or identity comparison. */
public final class HairShapeSimilarity {
    private HairShapeSimilarity() { }
    public static Double compare(List<Double> first, List<Double> second) {
        if (first.size() != 96 || second.size() != 96) return null;
        double intersection = 0, union = 0;
        for (int i = 0; i < first.size(); i++) {
            double a = first.get(i), b = second.get(i);
            if (!Double.isFinite(a) || !Double.isFinite(b) || a < 0 || b < 0 || a > 1 || b > 1) return null;
            intersection += Math.min(a, b);
            union += Math.max(a, b);
        }
        return union == 0 ? null : Math.round(intersection / union * 1000) / 1000.0;
    }
}
