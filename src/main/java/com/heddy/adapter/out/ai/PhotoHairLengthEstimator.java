package com.heddy.adapter.out.ai;

import com.heddy.domain.account.model.HairProfile.HairLength;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Conservative visible-length estimate from MediaPipe's hair/face segmentation, not a gender classifier. */
final class PhotoHairLengthEstimator {
    HairLength estimate(float[][][] logits) {
        int h = logits.length, w = logits[0].length;
        boolean[][] face = new boolean[h][w];
        List<Integer> hairY = new ArrayList<>(), hairX = new ArrayList<>();
        double hairConfidence = 0, faceConfidence = 0;
        int facePixels = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float[] scores = logits[y][x];
            int best = 0;
            for (int c = 1; c < scores.length; c++) if (scores[c] > scores[best]) best = c;
            double sum = 0;
            for (float score : scores) sum += Math.exp(score - scores[best]);
            double probability = 1 / sum;
            if (probability < .65) continue;
            if (best == 1) { hairY.add(y); hairX.add(x); hairConfidence += probability; }
            if (best == 3) { face[y][x] = true; facePixels++; faceConfidence += probability; }
        }
        if (hairY.size() < 300 || facePixels < 500
                || hairConfidence / hairY.size() < .80 || faceConfidence / facePixels < .80) return null;
        List<List<Integer>> faces = components(face);
        faces.sort((a, b) -> Integer.compare(b.size(), a.size()));
        if (faces.isEmpty() || (faces.size() > 1 && faces.get(1).size() > faces.get(0).size() * .20)) return null;
        int[] fy = faces.getFirst().stream().mapToInt(p -> p / w).sorted().toArray();
        int[] fx = faces.getFirst().stream().mapToInt(p -> p % w).sorted().toArray();
        int[] hy = hairY.stream().mapToInt(Integer::intValue).sorted().toArray();
        int[] hx = hairX.stream().mapToInt(Integer::intValue).sorted().toArray();
        double faceTop = percentile(fy, .05), chin = percentile(fy, .95);
        double faceHeight = chin - faceTop;
        // Rear views, tiny faces, profiles/rotated faces and cropped hair cannot establish current length.
        double faceWidth = percentile(fx, .95) - percentile(fx, .05);
        if (faceHeight < 24 || faceWidth < 16 || faceWidth / faceHeight > 1.7
                || faceWidth / faceHeight < .35) return null;
        if (percentile(hx, .01) <= 2 || percentile(hx, .99) >= w - 3
                || percentile(hy, .99) >= h - 3 || chin >= h - 8) return null;
        double hairBottom = percentile(hy, .97);
        // Hair must be attached spatially to the visible face, not a second person in the background.
        if (percentile(hy, .05) > faceTop + faceHeight * .4
                || percentile(hx, .95) < percentile(fx, .05)
                || percentile(hx, .05) > percentile(fx, .95)) return null;
        double belowChin = (hairBottom - chin) / faceHeight;
        if (belowChin < -.20) return HairLength.SHORT;
        if (belowChin < .65) return HairLength.BELOW_CHIN;
        // A face crop cannot reliably distinguish chest-length; use shoulder as the conservative bound.
        return HairLength.BELOW_SHOULDER;
    }

    /** Face-relative hair occupancy; rejects profiles and incomplete framing before comparing silhouettes. */
    List<Double> shape(float[][][] logits) {
        if (estimate(logits) == null) return List.of();
        int h = logits.length, w = logits[0].length;
        List<Integer> faceX = new ArrayList<>(), faceY = new ArrayList<>();
        boolean[][] hair = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float[] scores = logits[y][x];
            int best = 0;
            for (int c = 1; c < scores.length; c++) if (scores[c] > scores[best]) best = c;
            double sum = 0;
            for (float score : scores) sum += Math.exp(score - scores[best]);
            if (1 / sum < .80) continue;
            if (best == 3) { faceX.add(x); faceY.add(y); }
            hair[y][x] = best == 1;
        }
        if (faceX.size() < 500) return List.of();
        int[] fx = faceX.stream().mapToInt(Integer::intValue).sorted().toArray();
        int[] fy = faceY.stream().mapToInt(Integer::intValue).sorted().toArray();
        double left = percentile(fx, .05), right = percentile(fx, .95);
        double top = percentile(fy, .05), bottom = percentile(fy, .95);
        double fw = right - left, fh = bottom - top, cx = (left + right) / 2;
        if (fw / fh < .55 || fw / fh > 1.25) return List.of();
        // A fixed grid in face coordinates makes scale/background independent comparisons possible.
        int columns = 8, rows = 12;
        double x0 = cx - fw * 1.25, y0 = top - fh * .9;
        double cellW = fw * 2.5 / columns, cellH = fh * 3.4 / rows;
        List<Double> shape = new ArrayList<>(columns * rows);
        for (int gy = 0; gy < rows; gy++) for (int gx = 0; gx < columns; gx++) {
            int seen = 0, occupied = 0;
            for (int y = (int)Math.floor(y0 + gy * cellH); y < y0 + (gy + 1) * cellH; y++) {
                for (int x = (int)Math.floor(x0 + gx * cellW); x < x0 + (gx + 1) * cellW; x++) {
                    seen++;
                    if (y >= 0 && y < h && x >= 0 && x < w && hair[y][x]) occupied++;
                }
            }
            shape.add(seen == 0 ? 0.0 : Math.round(occupied * 1000.0 / seen) / 1000.0);
        }
        return List.copyOf(shape);
    }

    private static int percentile(int[] sorted, double quantile) {
        return sorted[Math.min(sorted.length - 1, (int) (sorted.length * quantile))];
    }

    private static List<List<Integer>> components(boolean[][] mask) {
        int h = mask.length, w = mask[0].length;
        List<List<Integer>> result = new ArrayList<>();
        int[] queue = new int[h * w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (!mask[y][x]) continue;
            int read = 0, size = 1; queue[0] = y * w + x; mask[y][x] = false;
            while (read < size) {
                int pixel = queue[read++], py = pixel / w, px = pixel % w;
                for (int[] d : DIRECTIONS) {
                    int ny = py + d[0], nx = px + d[1];
                    if (ny >= 0 && ny < h && nx >= 0 && nx < w && mask[ny][nx]) {
                        mask[ny][nx] = false; queue[size++] = ny * w + nx;
                    }
                }
            }
            if (size >= 100) result.add(Arrays.stream(queue, 0, size).boxed().toList());
        }
        return result;
    }
    private static final int[][] DIRECTIONS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
}
