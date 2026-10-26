package com.payment.system.domain.frauds.model;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.frauds.features.FeatureVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeModelTest extends UnitTest {

    @Test
    void returnsTreeCount() {
        TreeModel model = new TreeModel(
                1.5,
                new TreeModel.Tree[]{
                        leaf(2.0),
                        leaf(3.0)
                }
        );

        assertEquals(2, model.treeCount());
    }

    @Test
    void calculatesRawScoreWithBaseAndMultipleTrees() {
        TreeModel model = new TreeModel(
                -2.0,
                new TreeModel.Tree[]{
                        leaf(3.0),
                        leaf(-0.5)
                }
        );

        assertEquals(0.5, model.rawScore(emptyVector()), 1e-12);
    }

    @Test
    void calculatesRawScoreWithNoTrees() {
        TreeModel model = new TreeModel(2.5, new TreeModel.Tree[0]);

        assertEquals(2.5, model.rawScore(emptyVector()), 1e-12);
        assertEquals(0, model.treeCount());
    }

    @Test
    void evaluatesLeafTree() {
        TreeModel model = new TreeModel(
                -1.0,
                new TreeModel.Tree[]{leaf(0.7)}
        );

        assertEquals(-0.3, model.rawScore(emptyVector()), 1e-12);
    }

    @Test
    void followsLeftBranchWhenFeatureEqualsThreshold() {
        TreeModel model = new TreeModel(
                0.0,
                new TreeModel.Tree[]{
                        split(
                                FeatureVector.NEW_PAYEE,
                                1.0,
                                1,
                                2,
                                10.0,
                                20.0
                        )
                }
        );

        FeatureVector vector = FeatureVector.builder()
                .set(FeatureVector.NEW_PAYEE, 1)
                .build();

        assertEquals(10.0, model.rawScore(vector), 1e-12);
    }

    @Test
    void followsLeftBranchWhenFeatureIsBelowThreshold() {
        TreeModel model = new TreeModel(
                0.0,
                new TreeModel.Tree[]{
                        split(
                                FeatureVector.NEW_PAYEE,
                                2.0,
                                1,
                                2,
                                10.0,
                                20.0
                        )
                }
        );

        FeatureVector vector = FeatureVector.builder()
                .set(FeatureVector.NEW_PAYEE, 1)
                .build();

        assertEquals(10.0, model.rawScore(vector), 1e-12);
    }

    @Test
    void followsRightBranchWhenFeatureIsAboveThreshold() {
        TreeModel model = new TreeModel(
                0.0,
                new TreeModel.Tree[]{
                        split(
                                FeatureVector.NEW_PAYEE,
                                1.0,
                                1,
                                2,
                                10.0,
                                20.0
                        )
                }
        );

        FeatureVector vector = FeatureVector.builder()
                .set(FeatureVector.NEW_PAYEE, 2)
                .build();

        assertEquals(20.0, model.rawScore(vector), 1e-12);
    }

    @Test
    void calculatesScoreUsingSigmoid() {
        TreeModel model = new TreeModel(
                0.0,
                new TreeModel.Tree[]{leaf(0.0)}
        );

        assertEquals(0.5, model.score(emptyVector()), 1e-12);
    }

    private static FeatureVector emptyVector() {
        return FeatureVector.builder().build();
    }

    private static TreeModel.Tree leaf(double value) {
        return new TreeModel.Tree(
                new int[]{-1},
                new double[]{0.0},
                new int[]{TreeModel.LEAF},
                new int[]{TreeModel.LEAF},
                new double[]{value}
        );
    }

    private static TreeModel.Tree split(
            int feature,
            double threshold,
            int leftChild,
            int rightChild,
            double leftValue,
            double rightValue
    ) {
        return new TreeModel.Tree(
                new int[]{feature, -1, -1},
                new double[]{threshold, 0.0, 0.0},
                new int[]{leftChild, TreeModel.LEAF, TreeModel.LEAF},
                new int[]{rightChild, TreeModel.LEAF, TreeModel.LEAF},
                new double[]{0.0, leftValue, rightValue}
        );
    }
}
