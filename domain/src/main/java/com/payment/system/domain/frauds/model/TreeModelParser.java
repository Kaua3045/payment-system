package com.payment.system.domain.frauds.model;

import com.payment.system.domain.exceptions.ModelFormatException;
import com.payment.system.domain.frauds.features.FeatureVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the model file with AGGRESSIVE VALIDATION: the input is considered untrustworthy (it may be truncated, corrupted,
 * or from a different training version). Any anomaly triggers a ModelFormatException, and the entire model is rejected.
 * Format: see the dev-model.tree file (section 8). Validation covers the version, feature names and order,
 * size and depth limits (checked before allocation), finite numerical values, sequential indices, and the tree structure
 * (ensuring no cycles, orphaned nodes, or shared nodes).
 */
public final class TreeModelParser {

    public static final int MAX_TREES = 10_000;
    public static final int MAX_NODES_PER_TREE = 100_000;
    public static final int MAX_TOTAL_NODES = 2_000_000;
    public static final int MAX_DEPTH = 64;


    private final BufferedReader in;
    private int lineNo = 0;
    private int totalNodes = 0;

    private TreeModelParser(BufferedReader in) {
        this.in = in;
    }

    public static TreeModel parse(Path file) throws IOException, ModelFormatException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(r);
        }
    }

    public static TreeModel parse(Reader reader) throws IOException, ModelFormatException {
        BufferedReader br = reader instanceof BufferedReader b ? b : new BufferedReader(reader);
        return new TreeModelParser(br).parseModel();
    }

    private TreeModel parseModel() throws IOException, ModelFormatException {
        String[] header = tokens("TREEMODEL", 2);
        if (!header[1].equals("1")) throw err("Version not supported" + header[1]);
        // Features contract: the model specifies which features it was trained on; these must be our features, in the same order.
        List<String> names = Arrays.asList(tokens("FEATURES", 2)[1].split(",", -1));
        if (!names.equals(FeatureVector.NAMES))
            throw err("features of the model " + names + " != expected " + FeatureVector.NAMES);
        double base = finiteDouble(tokens("BASE", 2)[1], "BASE");
        int treeCount = boundedInt(tokens("TREES", 2)[1], "TREES", 1, MAX_TREES);
        TreeModel.Tree[] trees = new TreeModel.Tree[treeCount];
        for (int t = 0; t < treeCount; t++) trees[t] = parseTree(t);
        tokens("END", 1);
        if (readLine() != null) throw err("unexpected content after END");
        return new TreeModel(base, trees);
    }

    private TreeModel.Tree parseTree(int treeIndex) throws IOException, ModelFormatException {
        int n = boundedInt(tokens("TREE", 2)[1], "Tree size " + treeIndex, 1, MAX_NODES_PER_TREE);
        totalNodes += n;
        if (totalNodes > MAX_TOTAL_NODES) throw err("total of nodes above the threshold " + MAX_TOTAL_NODES);
        // we only allocate AFTER validating the declared size (to guard against a "memory bomb")
        int[] feature = new int[n];
        double[] threshold = new double[n];
        int[] left = new int[n];
        int[] right = new int[n];
        double[] value = new double[n];
        for (int i = 0; i < n; i++) {
            String line = readLine();
            if (line == null) throw err("file ended in the middle of the tree " + treeIndex);
            String[] t = line.split("\\s+");
            if (!String.valueOf(i).equals(t[0])) throw err("expected node " + i + ", received '" + t[0] + "'");
            if (t.length < 2) throw err("node incomplete");
            switch (t[1]) {
                case "S" -> {
                    if (t.length != 6) throw err("split node requires 6 tokens");
                    feature[i] = boundedInt(t[2], "feature", 0, FeatureVector.SIZE - 1);
                    threshold[i] = finiteDouble(t[3], "threshold");
                    left[i] = boundedInt(t[4], "left son", 0, Integer.MAX_VALUE);
                    right[i] = boundedInt(t[5], "right son", 0, Integer.MAX_VALUE);
                }
                case "L" -> {
                    if (t.length != 3) throw err("A leaf node requires 3 tokens.");
                    feature[i] = -1;
                    left[i] = TreeModel.LEAF;
                    right[i] = TreeModel.LEAF;
                    value[i] = finiteDouble(t[2], "payroll amount");
                }
                default -> throw err("unknown node type: '" + t[1] + "'");
            }
        }
        validateStructure(treeIndex, n, left, right);
        return new TreeModel.Tree(feature, threshold, left, right, value);
    }

    private void validateStructure(int treeIndex, int n, int[] left, int[] right) throws ModelFormatException {
        int[] parents = new int[n];
        int[] depth = new int[n];
        for (int i = 0; i < n; i++) {
            if (left[i] == TreeModel.LEAF) continue;
            int l = left[i], r = right[i];
            // A child always comes after its parent: this is what eliminates cycles (no node points "backward").
            if (l <= i || r <= i)
                throw err("tree " + treeIndex + ", node " + i + ": a child cannot point backward/at themselves");
            if (l >= n || r >= n) throw err("tree " + treeIndex + ", node " + i + ": child outside the interval");
            if (l == r) throw err("tree " + treeIndex + ", node " + i + ": equal children");
            parents[l]++;
            parents[r]++;
            depth[l] = depth[r] = depth[i] + 1;
            if (depth[l] > MAX_DEPTH) throw err("tree " + treeIndex + ": depth greater than " + MAX_DEPTH);
        }
        // exactly 1 parent per node (except for the root): every node is reachable, and none are shared
        if (parents[0] != 0) throw err("tree " + treeIndex + ": The root cannot have a father.");
        for (int i = 1; i < n; i++) {
            if (parents[i] != 1)
                throw err("tree " + treeIndex + ", node " + i + ": It should have exactly one father, it has " + parents[i]);
        }
    }

    private String readLine() throws IOException {
        String s;
        while ((s = in.readLine()) != null) {
            lineNo++;
            s = s.trim();
            if (!s.isEmpty() && !s.startsWith("#")) return s;
        }
        return null;
    }

    private String[] tokens(String keyword, int expectedCount) throws IOException, ModelFormatException {
        String line = readLine();
        if (line == null) throw err("unexpected end of file; expected " + keyword);
        String[] t = line.split("\\s+");
        if (!t[0].equals(keyword)) throw err("expected " + keyword + ", found '" + t[0] + "'");
        if (t.length != expectedCount) throw err(keyword + " expect " + expectedCount + " tokens, have " + t.length);
        return t;
    }

    private int boundedInt(String s, String what, int min, int max) throws ModelFormatException {
        try {
            int v = Integer.parseInt(s);
            if (v < min || v > max) throw err(what + " out of range [" + min + ", " + max + "]: " + v);
            return v;
        } catch (NumberFormatException e) {
            throw err(what + " it is not an integer: '" + s + "'");
        }
    }

    /**
     * parseDouble accepts "NaN" and "Infinity": hence the explicit check for finiteness.
     */
    private double finiteDouble(String s, String what) throws ModelFormatException {
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v)) throw err(what + " not finity: '" + s + "'");
            return v;
        } catch (NumberFormatException e) {
            throw err(what + " not a number: '" + s + "'");
        }
    }

    private ModelFormatException err(String message) {
        return new ModelFormatException(lineNo, message);
    }
}
