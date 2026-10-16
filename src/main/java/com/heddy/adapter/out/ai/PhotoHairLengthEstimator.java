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
