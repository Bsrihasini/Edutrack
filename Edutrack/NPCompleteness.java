package Edutrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CO5 - NP-COMPLETENESS AND APPROXIMATION
 *
 * 1. SAT (DPLL solver)               -> examination / scheduling constraints
 * 2. 3-SAT -> CLIQUE                 -> academic constraint reduction
 * 3. CLIQUE -> INDEPENDENT-SET -> VERTEX-COVER -> conflict analysis
 * 4. Vertex-Cover 2-approximation    -> covering examination conflicts
 */
public class NPCompleteness {

    // =========================================================
    // CONFLICT GRAPH (vertices <= 63, adjacency as bitmasks)
    // =========================================================

    public static class Graph {

        public final List<String> names = new ArrayList<>();
        public long[] adj;

        public Graph(List<String> names) {
            this.names.addAll(names);
            this.adj = new long[names.size()];
        }

        public int size() {
            return names.size();
        }

        public void addEdge(int u, int v) {
            if (u == v) {
                return;
            }
            adj[u] |= 1L << v;
            adj[v] |= 1L << u;
        }

        public boolean hasEdge(int u, int v) {
            return (adj[u] & (1L << v)) != 0;
        }

        public int edgeCount() {
            int c = 0;
            for (long a : adj) {
                c += Long.bitCount(a);
            }
            return c / 2;
        }

        public List<int[]> edges() {
            List<int[]> list = new ArrayList<>();
            for (int u = 0; u < size(); u++) {
                for (int v = u + 1; v < size(); v++) {
                    if (hasEdge(u, v)) {
                        list.add(new int[]{u, v});
                    }
                }
            }
            return list;
        }

        public Graph complement() {
            Graph g = new Graph(names);
            for (int u = 0; u < size(); u++) {
                for (int v = u + 1; v < size(); v++) {
                    if (!hasEdge(u, v)) {
                        g.addEdge(u, v);
                    }
                }
            }
            return g;
        }
    }

    private static String clean(String s) {
        return s.replace("\"", "").trim();
    }

    // Two course presentations conflict when at least `threshold`
    // students are enrolled in both (their exams cannot share a slot).
    public static Graph buildConflictGraph(File folder, int threshold) {

        Map<String, List<Integer>> studentCourses = new HashMap<>();
        List<String> courseNames = new ArrayList<>();
        Map<String, Integer> courseIndex = new HashMap<>();

        File file = new File(folder, "studentInfo.csv");

        if (file.exists()) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {

                br.readLine(); // header

                String line;

                while ((line = br.readLine()) != null) {

                    String[] p = line.split(",", 4);

                    if (p.length < 3) {
                        continue;
                    }

                    String course = clean(p[0]) + "-" + clean(p[1]);

                    Integer idx = courseIndex.get(course);

                    if (idx == null) {
                        idx = courseNames.size();
                        courseIndex.put(course, idx);
                        courseNames.add(course);
                    }

                    studentCourses
                            .computeIfAbsent(clean(p[2]), k -> new ArrayList<>())
                            .add(idx);
                }

            } catch (IOException e) {
                // fall through with whatever was read
            }
        }

        if (courseNames.isEmpty() || courseNames.size() > 63) {
            return fallbackGraph();
        }

        int n = courseNames.size();
        int[][] shared = new int[n][n];

        for (List<Integer> list : studentCourses.values()) {

            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {

                    int a = list.get(i);
                    int b = list.get(j);

                    if (a != b) {
                        shared[a][b]++;
                        shared[b][a]++;
                    }
                }
            }
        }

        Graph g = new Graph(courseNames);

        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (shared[u][v] >= threshold) {
                    g.addEdge(u, v);
                }
            }
        }

        return g;
    }

    private static Graph fallbackGraph() {

        List<String> names = new ArrayList<>();

        for (char c = 'A'; c <= 'H'; c++) {
            names.add("EXAM-" + c);
        }

        Graph g = new Graph(names);

        int[][] e = {{0, 1}, {0, 2}, {1, 2}, {2, 3}, {3, 4}, {4, 5},
                {5, 6}, {6, 7}, {4, 7}, {1, 5}};

        for (int[] x : e) {
            g.addEdge(x[0], x[1]);
        }

        return g;
    }

    // =========================================================
    // 1. SAT - DPLL (unit propagation + branching)
    // Literal k > 0 means variable k is true, k < 0 means false.
    // =========================================================

    private static long decisions;

    // returns assignment[1..n] with 1 = true, -1 = false, or null if UNSAT
    public static int[] solveSAT(int numVars, List<int[]> clauses) {

        decisions = 0;

        int[] assign = new int[numVars + 1];

        if (!dpll(clauses, assign)) {
            return null;
        }

        for (int i = 1; i <= numVars; i++) {
            if (assign[i] == 0) {
                assign[i] = 1;
            }
        }

        return assign;
    }

    private static boolean dpll(List<int[]> clauses, int[] assign) {

        boolean changed = true;

        // ---- unit propagation ----
        while (changed) {

            changed = false;

            for (int[] clause : clauses) {

                boolean satisfied = false;
                int unassigned = 0;
                int lastLiteral = 0;

                for (int lit : clause) {

                    int val = assign[Math.abs(lit)];

                    if (val == 0) {
                        unassigned++;
                        lastLiteral = lit;
                    } else if ((val == 1) == (lit > 0)) {
                        satisfied = true;
                        break;
                    }
                }

                if (satisfied) {
                    continue;
                }

                if (unassigned == 0) {
                    return false; // clause falsified
                }

                if (unassigned == 1) {
                    assign[Math.abs(lastLiteral)] = lastLiteral > 0 ? 1 : -1;
                    changed = true;
                }
            }
        }

        // ---- pick a branching variable ----
        int branch = 0;

        for (int[] clause : clauses) {

            boolean satisfied = false;
            int candidate = 0;

            for (int lit : clause) {

                int val = assign[Math.abs(lit)];

                if (val == 0) {
                    if (candidate == 0) {
                        candidate = Math.abs(lit);
                    }
                } else if ((val == 1) == (lit > 0)) {
                    satisfied = true;
                    break;
                }
            }

            if (!satisfied && candidate != 0) {
                branch = candidate;
                break;
            }
        }

        if (branch == 0) {
            return true; // every clause satisfied
        }

        decisions++;

        for (int value = 1; value >= -1; value -= 2) {

            int[] copy = assign.clone();
            copy[branch] = value;

            if (dpll(clauses, copy)) {
                System.arraycopy(copy, 0, assign, 0, assign.length);
                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------
    // Exam timetabling as SAT: variable x(e,s) = exam e in slot s
    // ---------------------------------------------------------
    private static String examSchedulingReport(Graph g) {

        StringBuilder sb = new StringBuilder();

        int n = g.size();

        sb.append("1. SAT - EXAMINATION SCHEDULING CONSTRAINTS\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Exams (course presentations) : ").append(n).append("\n");
        sb.append("Conflict pairs (shared students) : ").append(g.edgeCount()).append("\n");
        sb.append("Rule: conflicting exams cannot share a time slot.\n");

        int clique = Long.bitCount(maximumClique(g));

        sb.append("Largest group of mutually conflicting exams : ").append(clique)
                .append("  (so at least ").append(clique).append(" slots are needed)\n\n");

        sb.append(String.format("%-8s %8s %10s %10s %10s%n",
                "Slots", "Vars", "Clauses", "Decisions", "Result"));
        sb.append("------------------------------------------------------------\n");

        int[] firstSolution = null;
        int firstSlots = 0;

        for (int slots = 2; slots <= 6; slots++) {

            List<int[]> clauses = new ArrayList<>();

            // each exam gets at least one slot
            for (int e = 0; e < n; e++) {
                int[] c = new int[slots];
                for (int s = 0; s < slots; s++) {
                    c[s] = e * slots + s + 1;
                }
                clauses.add(c);
            }

            // each exam gets at most one slot
            for (int e = 0; e < n; e++) {
                for (int s = 0; s < slots; s++) {
                    for (int t = s + 1; t < slots; t++) {
                        clauses.add(new int[]{-(e * slots + s + 1), -(e * slots + t + 1)});
                    }
                }
            }

            // conflicting exams never share a slot
            for (int[] edge : g.edges()) {
                for (int s = 0; s < slots; s++) {
                    clauses.add(new int[]{
                            -(edge[0] * slots + s + 1),
                            -(edge[1] * slots + s + 1)});
                }
            }

            int[] sol = solveSAT(n * slots, clauses);

            sb.append(String.format("%-8d %8d %10d %10d %10s%n",
                    slots, n * slots, clauses.size(), decisions,
                    sol == null ? "UNSAT" : "SAT"));

            if (sol != null && firstSolution == null) {
                firstSolution = sol;
                firstSlots = slots;
            }
        }

        if (firstSolution == null) {

            sb.append("\nNo schedule found with up to 6 slots.\n\n");

        } else {

            sb.append("\nSmallest feasible timetable (").append(firstSlots).append(" slots):\n");

            boolean valid = true;
            int[] slotOf = new int[n];

            for (int e = 0; e < n; e++) {
                for (int s = 0; s < firstSlots; s++) {
                    if (firstSolution[e * firstSlots + s + 1] == 1) {
                        slotOf[e] = s;
                    }
                }
            }

            for (int s = 0; s < firstSlots; s++) {

                sb.append("  Slot ").append(s + 1).append(" : ");

                List<String> list = new ArrayList<>();

                for (int e = 0; e < n; e++) {
                    if (slotOf[e] == s) {
                        list.add(g.names.get(e));
                    }
                }

                sb.append(list).append("\n");
            }

            for (int[] edge : g.edges()) {
                if (slotOf[edge[0]] == slotOf[edge[1]]) {
                    valid = false;
                }
            }

            sb.append("Verification: no conflicting exams share a slot -> ")
                    .append(valid ? "OK" : "FAILED").append("\n\n");
        }

        return sb.toString();
    }

    // =========================================================
    // 2. 3-SAT -> CLIQUE
    // One vertex per (clause, literal). Join two vertices when they
    // are in different clauses and their literals do not contradict.
    // Formula satisfiable  <=>  graph has a clique of size = #clauses
    // =========================================================

    private static String literalName(int lit) {
        return (lit < 0 ? "~" : "") + "x" + Math.abs(lit);
    }

    public static Graph threeSatToClique(int[][] formula) {

        List<String> names = new ArrayList<>();

        for (int i = 0; i < formula.length; i++) {
            for (int j = 0; j < 3; j++) {
                names.add("C" + (i + 1) + ":" + literalName(formula[i][j]));
            }
        }

        Graph g = new Graph(names);

        for (int i = 0; i < formula.length; i++) {
            for (int j = i + 1; j < formula.length; j++) {
                for (int a = 0; a < 3; a++) {
                    for (int b = 0; b < 3; b++) {
                        if (formula[i][a] != -formula[j][b]) {
                            g.addEdge(i * 3 + a, j * 3 + b);
                        }
                    }
                }
            }
        }

        return g;
    }

    // Maximum clique (Bron-Kerbosch with pivoting) -> bitmask of vertices
    private static long bestClique;

    public static long maximumClique(Graph g) {

        bestClique = 0;

        long all = g.size() == 64 ? -1L : (1L << g.size()) - 1;

        bronKerbosch(g, 0L, all, 0L);

        return bestClique;
    }

    private static void bronKerbosch(Graph g, long r, long p, long x) {

        if (p == 0 && x == 0) {
            if (Long.bitCount(r) > Long.bitCount(bestClique)) {
                bestClique = r;
            }
            return;
        }

        if (Long.bitCount(r) + Long.bitCount(p) <= Long.bitCount(bestClique)) {
            return;
        }

        // choose pivot with most neighbours in P
        long px = p | x;
        int pivot = -1;
        int best = -1;

        for (int u = 0; u < g.size(); u++) {
            if ((px & (1L << u)) != 0) {
                int c = Long.bitCount(p & g.adj[u]);
                if (c > best) {
                    best = c;
                    pivot = u;
                }
            }
        }

        long candidates = p & ~g.adj[pivot];

        for (int v = 0; v < g.size(); v++) {

            if ((candidates & (1L << v)) == 0) {
                continue;
            }

            bronKerbosch(g, r | (1L << v), p & g.adj[v], x & g.adj[v]);

            p &= ~(1L << v);
            x |= (1L << v);
        }
    }

    private static String formulaToString(int[][] formula) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < formula.length; i++) {

            sb.append("(");

            for (int j = 0; j < 3; j++) {
                sb.append(literalName(formula[i][j]));
                if (j < 2) {
                    sb.append(" v ");
                }
            }

            sb.append(")");

            if (i < formula.length - 1) {
                sb.append(" ^ ");
            }
        }

        return sb.toString();
    }

    private static boolean satisfiable(int[][] formula, int vars) {

        List<int[]> clauses = new ArrayList<>();

        for (int[] c : formula) {
            clauses.add(c.clone());
        }

        return solveSAT(vars, clauses) != null;
    }

    private static String reductionReport() {

        StringBuilder sb = new StringBuilder();

        sb.append("2. 3-SAT -> CLIQUE (REDUCTION OF ACADEMIC CONSTRAINTS)\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Idea: x1 = 'Exam 1 in morning', x2 = 'Exam 2 in morning',\n");
        sb.append("x3 = 'Exam 3 in morning'. Each clause is a rule the timetable\n");
        sb.append("must satisfy.\n\n");

        int[][] satisfiableFormula = {
                {1, 2, 3},
                {-1, 2, -3},
                {1, -2, 3},
                {-1, -2, 3}
        };

        // all 8 sign patterns over x1,x2,x3 -> contradictory
        int[][] unsatisfiableFormula = new int[8][3];

        for (int mask = 0; mask < 8; mask++) {
            for (int v = 0; v < 3; v++) {
                unsatisfiableFormula[mask][v] = ((mask >> v) & 1) == 1 ? (v + 1) : -(v + 1);
            }
        }

        int[][][] formulas = {satisfiableFormula, unsatisfiableFormula};
        String[] labels = {"Formula A", "Formula B"};

        for (int f = 0; f < formulas.length; f++) {

            int[][] formula = formulas[f];
            int k = formula.length;

            Graph g = threeSatToClique(formula);

            long clique = maximumClique(g);
            int omega = Long.bitCount(clique);

            boolean viaClique = omega == k;
            boolean viaSat = satisfiable(formula, 3);

            sb.append(labels[f]).append(": ").append(formulaToString(formula)).append("\n");
            sb.append("  Graph: ").append(g.size()).append(" vertices, ")
                    .append(g.edgeCount()).append(" edges, target clique size k = ")
                    .append(k).append("\n");
            sb.append("  Largest clique found : ").append(omega).append("\n");
            sb.append("  Clique of size k exists -> ").append(viaClique ? "YES (satisfiable)" : "NO (unsatisfiable)").append("\n");
            sb.append("  DPLL solver says        -> ").append(viaSat ? "SAT" : "UNSAT").append("\n");
            sb.append("  Reduction agrees with solver: ").append(viaClique == viaSat ? "YES" : "NO").append("\n");

            if (viaClique) {

                int[] value = new int[4];

                sb.append("  Clique vertices      : ");

                for (int v = 0; v < g.size(); v++) {
                    if ((clique & (1L << v)) != 0) {
                        sb.append(g.names.get(v)).append(" ");
                        int lit = formula[v / 3][v % 3];
                        value[Math.abs(lit)] = lit > 0 ? 1 : -1;
                    }
                }

                sb.append("\n  Assignment read from clique : ");

                for (int v = 1; v <= 3; v++) {
                    sb.append("x").append(v).append("=")
                            .append(value[v] == -1 ? "F" : "T").append(" ");
                }

                sb.append("\n");
            }

            sb.append("\n");
        }

        // ---------- CLIQUE -> INDEPENDENT-SET -> VERTEX-COVER ----------

        sb.append("3. CLIQUE -> INDEPENDENT-SET -> VERTEX-COVER\n");
        sb.append("------------------------------------------------------------\n");

        Graph g = threeSatToClique(satisfiableFormula);
        Graph gc = g.complement();
        int n = g.size();

        int omega = Long.bitCount(maximumClique(g));

        // brute-force independent set and vertex cover of the complement
        int alpha = 0;
        int tau = n;

        for (int mask = 0; mask < (1 << n); mask++) {

            boolean independent = true;
            boolean cover = true;

            for (int u = 0; u < n && (independent || cover); u++) {

                boolean inU = (mask & (1 << u)) != 0;

                long nb = gc.adj[u];

                if (inU && (nb & mask) != 0) {
                    independent = false;
                }

                if (!inU && (nb & ~((long) mask)) != 0) {
                    cover = false;
                }
            }

            if (independent) {
                alpha = Math.max(alpha, Integer.bitCount(mask));
            }

            if (cover) {
                tau = Math.min(tau, Integer.bitCount(mask));
            }
        }

        sb.append("Graph G (from Formula A)      : ").append(n).append(" vertices, ")
                .append(g.edgeCount()).append(" edges\n");
        sb.append("Complement graph G'           : ").append(gc.edgeCount()).append(" edges\n\n");

        sb.append("Step 1  CLIQUE -> INDEPENDENT-SET\n");
        sb.append("  max clique in G            = ").append(omega).append("\n");
        sb.append("  max independent set in G'  = ").append(alpha).append("   (brute force)\n");
        sb.append("  equal -> ").append(omega == alpha ? "OK" : "MISMATCH").append("\n\n");

        sb.append("Step 2  INDEPENDENT-SET -> VERTEX-COVER\n");
        sb.append("  vertices - independent set = ").append(n).append(" - ").append(alpha)
                .append(" = ").append(n - alpha).append("\n");
        sb.append("  min vertex cover in G'     = ").append(tau).append("   (brute force)\n");
        sb.append("  equal -> ").append(n - alpha == tau ? "OK" : "MISMATCH").append("\n\n");

        sb.append("Meaning: a conflict-free group of ").append(alpha)
                .append(" items exists exactly when the other ").append(tau)
                .append(" items can cover every conflict.\n\n");

        return sb.toString();
    }

    // =========================================================
    // 4. VERTEX COVER
    // ---------------------------------------------------------
    // 2-approximation: repeatedly take an uncovered edge (u,v),
    // put BOTH endpoints in the cover, remove all edges touching
    // u or v. The chosen edges form a matching M, and every cover
    // needs one endpoint per matching edge, so OPT >= |M| and
    // |cover| = 2|M| <= 2 OPT.
    // =========================================================

    public static List<Integer> vertexCoverApprox(Graph g, List<int[]> matchingOut) {

        long[] adj = g.adj.clone();
        List<Integer> cover = new ArrayList<>();

        for (int u = 0; u < g.size(); u++) {

            while (adj[u] != 0) {

                int v = Long.numberOfTrailingZeros(adj[u]);

                cover.add(u);
                cover.add(v);

                if (matchingOut != null) {
                    matchingOut.add(new int[]{u, v});
                }

                // remove every edge touching u or v
                for (int w = 0; w < g.size(); w++) {
                    adj[w] &= ~(1L << u);
                    adj[w] &= ~(1L << v);
                }

                adj[u] = 0;
                adj[v] = 0;
            }
        }

        return cover;
    }

    // exact minimum vertex cover by brute force (n <= ~25)
    public static int vertexCoverExact(Graph g, int[] bestMaskOut) {

        int n = g.size();
        int best = n;
        int bestMask = (1 << n) - 1;

        for (int mask = 0; mask < (1 << n); mask++) {

            int size = Integer.bitCount(mask);

            if (size >= best) {
                continue;
            }

            boolean ok = true;

            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) == 0 && (g.adj[u] & ~((long) mask)) != 0) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                best = size;
                bestMask = mask;
            }
        }

        if (bestMaskOut != null) {
            bestMaskOut[0] = bestMask;
        }

        return best;
    }

    private static String vertexCoverReport(Graph g) {

        StringBuilder sb = new StringBuilder();

        sb.append("4. VERTEX-COVER 2-APPROXIMATION - EXAMINATION CONFLICTS\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Goal: pick a small set of exams so that every conflict pair\n");
        sb.append("has at least one exam in the set (e.g. rooms with extra\n");
        sb.append("invigilators).\n\n");
        sb.append("Conflict graph: ").append(g.size()).append(" exams, ")
                .append(g.edgeCount()).append(" conflicts\n\n");

        List<int[]> matching = new ArrayList<>();

        long t0 = System.nanoTime();
        List<Integer> cover = vertexCoverApprox(g, matching);
        long approxTime = (System.nanoTime() - t0) / 1000;

        int[] mask = new int[1];

        t0 = System.nanoTime();
        int opt = vertexCoverExact(g, mask);
        long exactTime = (System.nanoTime() - t0) / 1000;

        sb.append("Matching edges chosen (|M| = ").append(matching.size()).append("):\n");

        for (int[] e : matching) {
            sb.append("  ").append(g.names.get(e[0])).append(" -- ")
                    .append(g.names.get(e[1])).append("\n");
        }

        sb.append("\nApprox cover (2 x |M| = ").append(cover.size()).append("):\n  ");

        for (int v : cover) {
            sb.append(g.names.get(v)).append(" ");
        }

        sb.append("\n\nOptimal cover (brute force, size ").append(opt).append("):\n  ");

        for (int v = 0; v < g.size(); v++) {
            if ((mask[0] & (1 << v)) != 0) {
                sb.append(g.names.get(v)).append(" ");
            }
        }

        // verify the approximate cover really covers every edge
        boolean[] in = new boolean[g.size()];

        for (int v : cover) {
            in[v] = true;
        }

        boolean valid = true;

        for (int[] e : g.edges()) {
            if (!in[e[0]] && !in[e[1]]) {
                valid = false;
            }
        }

        sb.append("\n\n");
        sb.append(String.format("%-28s %10s %12s%n", "Method", "Size", "Time (us)"));
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format("%-28s %10d %12d%n", "2-approximation", cover.size(), approxTime));
        sb.append(String.format("%-28s %10d %12d%n", "Exact (brute force 2^n)", opt, exactTime));

        sb.append("\nCover valid (every conflict covered) : ").append(valid ? "YES" : "NO").append("\n");
        sb.append(String.format("Approximation ratio                  : %.2f  (guarantee: <= 2)%n",
                (double) cover.size() / opt));
        sb.append("Lower bound check: |M| = ").append(matching.size())
                .append(" <= OPT = ").append(opt).append(" -> ")
                .append(matching.size() <= opt ? "OK" : "FAILED").append("\n\n");

        return sb.toString();
    }

    // =========================================================
    // FULL REPORT USED BY THE GUI
    // =========================================================

    public static String runAll(File dataFolder) {

        StringBuilder sb = new StringBuilder();

        sb.append("============================================================\n");
        sb.append("           CO5 - NP-COMPLETENESS AND APPROXIMATION\n");
        sb.append("============================================================\n\n");

        Graph g = buildConflictGraph(dataFolder, 5);

        sb.append(examSchedulingReport(g));
        sb.append(reductionReport());
        sb.append(vertexCoverReport(g));

        sb.append("============================================================\n");
        sb.append("NP-Completeness algorithms executed successfully.\n");
        sb.append("============================================================\n");

        return sb.toString();
    }
}
