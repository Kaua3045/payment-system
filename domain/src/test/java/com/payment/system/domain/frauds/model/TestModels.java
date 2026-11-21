package com.payment.system.domain.frauds.model;

import com.payment.system.domain.frauds.features.FeatureVector;

import java.io.StringReader;

/** Minimal model for testing: new recipient (feature 6) => logit 1,0 (p=0,73); know => logit -2,5 (p=0,076). */
public final class TestModels {

    private TestModels() {}

    public static String model(String treesSection) {
        return "TREEMODEL 1\nFEATURES " + String.join(",", FeatureVector.NAMES) + "\nBASE -2.0\n" + treesSection + "END\n";
    }

    public static final String TINY = model("TREES 1\nTREE 3\n0 S 6 0.5 1 2\n1 L -0.5\n2 L 3.0\n");

    public static TreeModel tiny() throws Exception {
        return TreeModelParser.parse(new StringReader(TINY));
    }
}
