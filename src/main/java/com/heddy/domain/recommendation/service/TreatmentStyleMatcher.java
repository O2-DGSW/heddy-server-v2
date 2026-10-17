package com.heddy.domain.recommendation.service;

import com.heddy.domain.recommendation.model.HairstyleCandidate;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Matches explicit treatment attributes to catalog names/tags; never infers them from a photo. */
final class TreatmentStyleMatcher {
    private static final Map<String, List<String>> SHAPES = Map.ofEntries(
            Map.entry("LAYERED", List.of("LAYERED", "레이어드")),
            Map.entry("BOB", List.of("BOB", "보브", "단발")),
            Map.entry("PIXIE", List.of("PIXIE", "픽시")),
            Map.entry("BUZZ", List.of("BUZZ", "버즈")),
            Map.entry("TWO_BLOCK", List.of("TWOBLOCK", "투블럭", "투블록")),
            Map.entry("ONE_LENGTH", List.of("ONELENGTH", "원랭스", "원렝스")),
            Map.entry("SHAG", List.of("SHAG", "샤기", "샤기컷")),
            Map.entry("WOLF", List.of("WOLF", "울프")));
    private static final Map<String, List<String>> PERMS = Map.ofEntries(
            Map.entry("C_CURL", List.of("CCURL", "C컬")),
            Map.entry("S_CURL", List.of("SCURL", "S컬")),
            Map.entry("WAVE", List.of("WAVE", "웨이브")),
            Map.entry("VOLUME", List.of("VOLUME", "볼륨")),
            Map.entry("DOWN", List.of("DOWNPERM", "다운펌")),
            Map.entry("SETTING", List.of("SETTING", "셋팅", "세팅")),
            Map.entry("DIGITAL", List.of("DIGITAL", "디지털")),
            Map.entry("MAGIC", List.of("MAGIC", "매직")),
            Map.entry("HIPPIE", List.of("HIPPIE", "히피")));

    Match match(HairstyleCandidate candidate, TreatmentRecord record) {
        List<String> details = new ArrayList<>();
        List<String> labels = new ArrayList<>(candidate.tags().values());
        labels.add(candidate.styleName());
        boolean cut = record.serviceTypes().contains(ServiceType.CUT)
                && candidate.serviceTypes().contains(ServiceType.CUT);
        boolean perm = record.serviceTypes().contains(ServiceType.PERM)
                && candidate.serviceTypes().contains(ServiceType.PERM);
        double detailWeight = 0, matchedWeight = 0;
        if (record.cutShape() != null && record.serviceTypes().contains(ServiceType.CUT)) {
            detailWeight += 3;
            if (cut && matches(record.cutShape(), labels, SHAPES)) {
                matchedWeight += 3;
                details.add("커트 형태 ‘" + label(record.cutShape(), SHAPES) + "’");
            }
        }
        if (record.permType() != null && record.serviceTypes().contains(ServiceType.PERM)) {
            detailWeight += 3;
            if (perm && matches(record.permType(), labels, PERMS)) {
                matchedWeight += 3;
                details.add("펌 종류 ‘" + label(record.permType(), PERMS) + "’");
            }
        }
        if (record.cutLength() != null && record.serviceTypes().contains(ServiceType.CUT)) {
            detailWeight += 2;
            if (cut && matchesLength(record.cutLength(), candidate.category())) {
                matchedWeight += 2;
                details.add("커트 길이 ‘" + lengthLabel(candidate.category()) + "’");
            }
        }
        Set<ServiceType> union = new java.util.HashSet<>(record.serviceTypes());
        union.addAll(candidate.serviceTypes());
        long common = record.serviceTypes().stream().filter(candidate.serviceTypes()::contains).count();
        double service = common / (double) union.size();
        // CUT alone is weak evidence, not a 40-point endorsement of every haircut.
        double score = 10 * service + (detailWeight == 0 ? 0 : 30 * matchedWeight / detailWeight);
        return new Match(score, List.copyOf(details));
    }

    private String label(String input, Map<String, List<String>> aliases) {
        String normalized = normalize(input);
        return aliases.entrySet().stream()
                .filter(e -> normalize(e.getKey()).equals(normalized)
                        || e.getValue().stream().anyMatch(v -> normalize(v).equals(normalized)))
                .map(e -> e.getValue().get(1)).findFirst().orElse(input);
    }

    private String lengthLabel(String category) {
        return switch (normalize(category)) {
            case "SHORT" -> "숏";
            case "BOB" -> "단발";
            case "MEDIUM" -> "미디엄";
            case "LONG" -> "롱";
            default -> category;
        };
    }

    private boolean matches(String input, List<String> labels, Map<String, List<String>> aliases) {
        String normalized = normalize(input);
        List<String> terms = aliases.entrySet().stream()
                .filter(e -> normalize(e.getKey()).equals(normalized)
                        || e.getValue().stream().anyMatch(v -> normalize(v).equals(normalized)))
                .map(Map.Entry::getValue).findFirst().orElse(List.of(normalized));
        // Unknown input is only an exact match; generic text such as CUT/PERM is not a style.
        if (!aliases.values().contains(terms)) {
            return normalized.length() >= 2 && labels.stream().anyMatch(v -> normalize(v).equals(normalized));
        }
        return labels.stream().map(TreatmentStyleMatcher::normalize)
                .anyMatch(label -> terms.stream().map(TreatmentStyleMatcher::normalize).anyMatch(label::contains));
    }

    private boolean matchesLength(String input, String category) {
        String length = switch (normalize(input)) {
            case "SHORT", "숏", "숏컷", "짧은머리" -> "SHORT";
            case "BOB", "BELOWCHIN", "단발", "턱아래" -> "BOB";
            case "MEDIUM", "BELOWSHOULDER", "미디엄", "중간머리", "어깨아래" -> "MEDIUM";
            case "LONG", "BELOWCHEST", "롱", "긴머리", "가슴아래" -> "LONG";
            default -> "";
        };
        return !length.isEmpty() && length.equals(normalize(category));
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]", "");
    }

    record Match(double score, List<String> details) { }
}
