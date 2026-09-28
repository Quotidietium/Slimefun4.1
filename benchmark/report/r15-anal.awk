# Composite-lean paired analysis for r15 (multiblock-interact round).
# Input: 18 result files in pair order (p1base,p1opt,...,p9base,p9opt), min_ metrics only.
# Lean pool per pair = raw ratios of every non-target-scenario variant; target = multiblock-interact/*.
# Output: per-target corrected median/range, direction count, absolute base/opt medians.
function median_r(    i, j, t) {
    for (i = 1; i < nr; i++) for (j = i + 1; j <= nr; j++) if (rr[j] < rr[i]) { t = rr[i]; rr[i] = rr[j]; rr[j] = t }
    return (nr % 2 == 1) ? rr[int((nr + 1) / 2)] : (rr[nr / 2] + rr[nr / 2 + 1]) / 2
}
FNR == 1 { f++; pair = int((f + 1) / 2); side = (f % 2 == 1 ? "base" : "opt") }
$1 == "RESULT" && $5 ~ /^min_/ {
    key = $3 "|" $4
    val[side, pair, key] = $7 + 0
    seen[key] = 1
}
END {
    npool = 0
    for (k in seen) {
        split(k, ka, "|")
        if (ka[1] != "multiblock-interact") pool[++npool] = k
    }
    for (p = 1; p <= 9; p++) {
        nr = 0
        for (i = 1; i <= npool; i++) {
            k = pool[i]
            b = val["base", p, k]; o = val["opt", p, k]
            if (b > 0 && o > 0) rr[++nr] = o / b
        }
        lean[p] = median_r()
    }
    printf "%-38s %10s %10s %9s %9s %9s %6s\n", "variant", "base_med", "opt_med", "raw_med", "lean_med", "corr_med", "down"
    for (k in seen) {
        split(k, ka, "|")
        if (ka[1] != "multiblock-interact") continue
        nb = 0; down = 0; nc = 0
        for (p = 1; p <= 9; p++) {
            b = val["base", p, k]; o = val["opt", p, k]
            if (b <= 0 || o <= 0) continue
            raw = o / b
            bb[++nb] = b; ob[nb] = o
            if (raw < 1) down++
            cc[++nc] = raw / lean[p]
        }
        nr = nb
        for (i = 1; i <= nb; i++) rr[i] = bb[i]
        bmed = median_r()
        for (i = 1; i <= nb; i++) rr[i] = ob[i]
        omed = median_r()
        for (i = 1; i <= nb; i++) rr[i] = cc[i] * lean[1] # placeholder replaced below
        nr = 0
        for (i = 1; i <= nb; i++) { nr++; rr[nr] = bb[i]; }
        # raw median: ratio per pair
        nr = 0
        for (p = 1; p <= 9; p++) {
            b = val["base", p, k]; o = val["opt", p, k]
            if (b <= 0 || o <= 0) continue
            rr[++nr] = o / b
        }
        rawmed = median_r()
        nr = nc
        for (i = 1; i <= nc; i++) rr[i] = cc[i]
        cmed = median_r()
        cmin = cc[1]; cmax = cc[1]
        for (i = 2; i <= nc; i++) { if (cc[i] < cmin) cmin = cc[i]; if (cc[i] > cmax) cmax = cc[i] }
        # lean median across pairs
        nr = 0
        for (p = 1; p <= 9; p++) rr[++nr] = lean[p]
        lmed = median_r()
        printf "%-38s %10.1f %10.1f %9.3f %9.3f %9.3f %d/9  corr[%.3f-%.3f]\n", ka[2], bmed, omed, rawmed, lmed, cmed, down, cmin, cmax
    }
    printf "lean per pair:"
    for (p = 1; p <= 9; p++) printf " %.4f", lean[p]
    printf "\n"
}
