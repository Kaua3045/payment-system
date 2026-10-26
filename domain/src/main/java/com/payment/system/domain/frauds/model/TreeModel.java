package com.payment.system.domain.frauds.model;

import com.payment.system.domain.frauds.features.FeatureVector;

public final class TreeModel {

    static final int LEAF = -1;
    private final double baseScore;
    private final Tree[] trees;

    TreeModel(double baseScore, Tree[] trees) {
        this.baseScore = baseScore;
        this.trees = trees;
    }

    public int treeCount() {
        return trees.length;
    }

    public double rawScore(FeatureVector x) {
        double sum = baseScore;
        for (Tree t : trees) sum += t.leafValue(x);
        return sum;
    }

    public double score(FeatureVector x) {
        return Sigmoid.sigmoid(rawScore(x));
    }

    static final class Tree {
        private final int[] feature;
        private final double[] threshold;
        private final int[] left;
        private final int[] right;
        private final double[] value;

        Tree(int[] feature, double[] threshold, int[] left, int[] right, double[] value) {
            this.feature = feature;
            this.threshold = threshold;
            this.left = left;
            this.right = right;
            this.value = value;
        }

        double leafValue(FeatureVector x) {
            int i = 0;                                  // init in source
            // iterative (without recursion). It terminates because the parser guarantees that every child has an index GREATER than its parent.
            // Left if x <= threshold (same as scikit-learn; XGBoost uses "<").
            while (left[i] != LEAF) i = x.get(feature[i]) <= threshold[i] ? left[i] : right[i];
            return value[i];
        }
    }
}
