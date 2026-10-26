package com.payment.system.domain.frauds.model;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.ModelFormatException;
import com.payment.system.domain.frauds.features.FeatureVector;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.payment.system.domain.frauds.model.TestModels.model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeModelParserTest extends UnitTest {

    static TreeModel parse(String text) throws Exception {
        return TreeModelParser.parse(new StringReader(text));
    }

    @Test
    void parsesAndEvaluates() throws Exception {
        TreeModel m = TestModels.tiny();
        FeatureVector known = FeatureVector.builder().build();
        FeatureVector fresh = FeatureVector.builder().set(FeatureVector.NEW_PAYEE, 1).build();
        assertEquals(-2.5, m.rawScore(known), 0.0);    // -2.0 (base) + -0.5 (folha)
        assertEquals(1.0, m.rawScore(fresh), 0.0);     // -2.0 + 3.0
        assertEquals(1, m.treeCount());
    }

    @Test
    void ignoresCommentsAndBlankLines() throws Exception {
        assertEquals(1, parse("# gerado pelo treino\n\n" + TestModels.TINY.replace("BASE", "# base\nBASE")).treeCount());
    }

    @Test
    void acceptsSingleLeafTree() throws Exception {
        assertEquals(-1.3, parse(model("TREES 1\nTREE 1\n0 L 0.7\n")).rawScore(FeatureVector.builder().build()), 1e-12);
    }

    @Test
    void rejectsCorruptedOrHostileModels() {
        String tiny = TestModels.TINY;
        Map<String, String> bad = new LinkedHashMap<>();

        bad.put("unsupported model version",
                tiny.replace("TREEMODEL 1", "TREEMODEL 2"));

        bad.put("model features differ from expected names",
                tiny.replace("log_amount", "amount"));

        bad.put("model features are in the wrong order",
                tiny.replace("log_amount,amount_ratio_log",
                        "amount_ratio_log,log_amount"));

        bad.put("base score is NaN",
                tiny.replace("BASE -2.0", "BASE NaN"));

        bad.put("base score is infinite",
                tiny.replace("BASE -2.0", "BASE Infinity"));

        bad.put("split threshold is NaN",
                tiny.replace("0.5 1 2", "NaN 1 2"));

        bad.put("leaf value is infinite",
                tiny.replace("L 3.0", "L Infinity"));

        bad.put("split feature index is out of range",
                tiny.replace("S 6", "S 13"));

        bad.put("child index is out of range",
                tiny.replace("1 2\n1 L", "1 9\n1 L"));

        bad.put("child points to an earlier node",
                model("TREES 1\nTREE 3\n"
                        + "0 S 6 0.5 1 2\n"
                        + "1 S 6 0.5 0 2\n"
                        + "2 L 1.0\n"));

        bad.put("child points to its own parent node",
                model("TREES 1\nTREE 3\n"
                        + "0 S 6 0.5 0 2\n"
                        + "1 L 1.0\n"
                        + "2 L 1.0\n"));

        bad.put("split node has identical children",
                model("TREES 1\nTREE 3\n"
                        + "0 S 6 0.5 1 1\n"
                        + "1 L 1.0\n"
                        + "2 L 1.0\n"));

        bad.put("tree contains an orphaned node",
                model("TREES 1\nTREE 4\n"
                        + "0 S 6 0.5 1 2\n"
                        + "1 L 1.0\n"
                        + "2 L 1.0\n"
                        + "3 L 9.0\n"));

        bad.put("node is shared by multiple parents",
                model("TREES 1\nTREE 5\n"
                        + "0 S 6 0.5 1 2\n"
                        + "1 S 7 0.5 3 4\n"
                        + "2 S 7 0.5 3 4\n"
                        + "3 L 1.0\n"
                        + "4 L 2.0\n"));

        bad.put("node indices are not sequential",
                tiny.replace("1 L -0.5", "5 L -0.5"));

        bad.put("node has an unsupported type",
                tiny.replace("1 L -0.5", "1 X -0.5"));

        bad.put("tree definition is truncated",
                model("TREES 1\nTREE 3\n"
                        + "0 S 6 0.5 1 2\n"
                        + "1 L -0.5\n"));

        bad.put("model is missing the END marker",
                tiny.replace("END\n", ""));

        bad.put("model contains content after END",
                tiny + "TREE 1\n");

        bad.put("declared tree count exceeds the actual count",
                model("TREES 2\nTREE 1\n0 L 0.1\n"));

        bad.put("declared node count exceeds the configured limit",
                model("TREES 1\nTREE 999999999\n"));

        bad.put("declared tree count exceeds the configured limit",
                model("TREES 999999999\n"));

        bad.put("model file is empty", "");

        for (Map.Entry<String, String> entry : bad.entrySet()) {
            assertThrows(
                    ModelFormatException.class,
                    () -> parse(entry.getValue()),
                    entry.getKey()
            );
        }
    }

    @Test
    void enforcesMaxDepth() throws Exception {
        assertEquals(1, parse(chain(60)).treeCount());
        assertThrows(ModelFormatException.class, () -> parse(chain(70)));
    }

    @Test
    void acceptsBufferedReader() throws Exception {
        assertEquals(
                1,
                TreeModelParser.parse(
                        new BufferedReader(new StringReader(
                                model("TREES 1\nTREE 1\n0 L 0.7\n")
                        ))
                ).treeCount()
        );
    }

    @Test
    void parsesFromPath() throws Exception {
        Path file = Files.createTempFile("tree-model", ".tree");

        try {
            Files.writeString(
                    file,
                    model("TREES 1\nTREE 1\n0 L 0.7\n")
            );

            assertEquals(1, TreeModelParser.parse(file).treeCount());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsNodeWithOnlyIndex() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(model("TREES 1\nTREE 1\n0\n"))
        );
    }

    @Test
    void rejectsMalformedSplitNode() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(model(
                        "TREES 1\nTREE 1\n0 S 6 0.5 1\n"
                ))
        );
    }

    @Test
    void rejectsMalformedLeafNode() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(model(
                        "TREES 1\nTREE 1\n0 L 0.7 extra\n"
                ))
        );
    }

    @Test
    void rejectsNonNumericBaseScore() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(TestModels.TINY.replace("BASE -2.0", "BASE abc"))
        );
    }

    @Test
    void rejectsNonIntegerTreeCount() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(TestModels.TINY.replace("TREES 1", "TREES abc"))
        );
    }

    @Test
    void rejectsTreeCountBelowMinimum() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(TestModels.TINY.replace("TREES 1", "TREES 0"))
        );
    }

    @Test
    void rejectsTreeCountAboveMaximum() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(TestModels.TINY.replace("TREES 1", "TREES 10001"))
        );
    }

    @Test
    void rejectsHeaderWithExtraTokens() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(TestModels.TINY.replace("TREES 1", "TREES 1 extra"))
        );
    }

    @Test
    void rejectsRightChildPointingBackward() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(model(
                        "TREES 1\nTREE 3\n"
                                + "0 S 6 0.5 1 0\n"
                                + "1 L 1.0\n"
                                + "2 L 2.0\n"
                ))
        );
    }

    @Test
    void rejectsLeftChildOutsideInterval() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(model(
                        "TREES 1\nTREE 3\n"
                                + "0 S 6 0.5 3 2\n"
                                + "1 L 1.0\n"
                                + "2 L 2.0\n"
                ))
        );
    }

    @Test
    void rejectsRightChildOutsideInterval() {
        assertThrows(
                ModelFormatException.class,
                () -> parse(model(
                        "TREES 1\nTREE 3\n"
                                + "0 S 6 0.5 1 3\n"
                                + "1 L 1.0\n"
                                + "2 L 2.0\n"
                ))
        );
    }

    /**
     * Árvore "em escada" com 'levels' níveis.
     */
    static String chain(int levels) {
        StringBuilder sb = new StringBuilder("TREES 1\nTREE " + (2 * levels + 1) + "\n");
        for (int i = 0; i < levels; i++) {
            sb.append(2 * i).append(" S 6 0.5 ").append(2 * i + 1).append(' ').append(2 * i + 2).append('\n');
            sb.append(2 * i + 1).append(" L 0.1\n");
        }
        sb.append(2 * levels).append(" L 0.2\n");
        return model(sb.toString());
    }
}
