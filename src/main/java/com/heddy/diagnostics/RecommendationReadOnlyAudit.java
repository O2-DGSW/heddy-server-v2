package com.heddy.diagnostics;

import com.heddy.adapter.out.ai.OnnxPhotoHairLengthAdapter;
import com.heddy.application.recommendation.service.RecommendationPhotoResolver;
import com.heddy.domain.file.model.*;
import com.heddy.domain.file.port.out.*;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import java.lang.reflect.Proxy;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.heddy.domain.account.model.HairProfile;
import com.heddy.domain.account.model.HairProfile.*;
import com.heddy.domain.treatment.model.*;
import com.heddy.domain.recommendation.model.*;
import com.heddy.domain.recommendation.model.HairstyleCandidate.*;
import com.heddy.domain.recommendation.service.*;

/** Operator-only entry point: no web endpoint, no Spring startup, SELECT-only transaction, no personal exports. */
public final class RecommendationReadOnlyAudit {
    static <E extends Enum<E>> E val(Class<E> cls, String value) {
        return value == null ? null : Enum.valueOf(cls, value);
    }
    static <E extends Enum<E>> Set<E> enums(Class<E> cls, String value) {
        Set<E> out = new HashSet<>();
        for (String s : value.replace("[", "").replace("]", "").replace("\"", "").split(","))
            if (!s.isBlank()) out.add(val(cls, s.trim()));
        return out;
    }
    static Set<UUID> ids(Connection c, String sql, UUID user) throws Exception {
        Set<UUID> out = new HashSet<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setObject(1,user);
            try (ResultSet r=p.executeQuery()) { while(r.next()) if(r.getObject(1)!=null) out.add(r.getObject(1,UUID.class)); }
        }
        return out;
    }
    static HairProfile profile(Connection c, UUID user) throws Exception {
        try(PreparedStatement p=c.prepareStatement("SELECT hair_type,hair_condition,hair_length,hair_thickness,available_care_time_minutes,created_at,updated_at FROM hair_profiles WHERE user_id=?")) {
            p.setObject(1,user);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) return null;
                return new HairProfile(null,user,val(HairType.class,r.getString(1)),val(HairCondition.class,r.getString(2)),val(HairLength.class,r.getString(3)),val(HairThickness.class,r.getString(4)),(Integer)r.getObject(5),r.getTimestamp(6).toInstant(),r.getTimestamp(7).toInstant());
            }
        }
    }
    static List<TreatmentRecord> history(Connection c, UUID user) throws Exception {
        List<TreatmentRecord> out = new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT record_id,service_types::text,performed_at,satisfaction,created_at,cut_length,cut_shape,perm_type FROM treatment_records WHERE user_id=? ORDER BY performed_at DESC,created_at DESC,record_id DESC LIMIT 10")) {
            p.setObject(1,user);
            try(ResultSet r=p.executeQuery()) { while(r.next()) {
                int score=r.getInt(4); Integer satisfaction=r.wasNull()?null:score;
                out.add(new TreatmentRecord(r.getObject(1,UUID.class), user,enums(ServiceType.class,r.getString(2)),null,null,r.getTimestamp(3).toInstant(),satisfaction,null,null,null,null,null,null,null,"Asia/Seoul",r.getString(6),r.getString(7),r.getString(8),null,List.of(),photos(c,r.getObject(1,UUID.class)),r.getTimestamp(5).toInstant()));
            }}
        }
        return out;
    }
    static List<HairstyleCandidate> catalog(Connection c) throws Exception {
        Map<UUID,Map<UUID,String>> tags = new HashMap<>();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT h.hairstyle_id,h.style_tag_id,t.tag_name FROM hairstyle_style_tags h JOIN style_tags t ON t.style_tag_id=h.style_tag_id")) {
            while(r.next()) tags.computeIfAbsent(r.getObject(1,UUID.class), ignored->new HashMap<>()).put(r.getObject(2,UUID.class), r.getString(3));
        }
        List<HairstyleCandidate> out=new ArrayList<>();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT a.*,p.* FROM hairstyle_assets a JOIN hairstyle_recommendation_profiles p USING(hairstyle_id) JOIN files f ON f.file_id=a.thumbnail_file_id WHERE a.active AND f.status='READY' ORDER BY a.hairstyle_id")) {
            while(r.next()) {
                UUID id=r.getObject("hairstyle_id",UUID.class);
                out.add(new HairstyleCandidate(id,r.getString("style_name"),r.getString("category"),r.getObject("thumbnail_file_id",UUID.class),r.getString("ar_mode"),r.getBoolean("active"),r.getString("asset_version"),
                enums(ServiceType.class,r.getString("service_types")),enums(HairLength.class,r.getString("compatible_hair_lengths")),enums(HairType.class,r.getString("compatible_hair_types")),enums(HairThickness.class,r.getString("compatible_hair_thicknesses")),enums(HairCondition.class,r.getString("compatible_hair_conditions")),enums(HairCondition.class,r.getString("contraindicated_hair_conditions")),val(HairLength.class,r.getString("minimum_hair_length")),r.getInt("estimated_daily_care_minutes"),val(ManagementDifficulty.class,r.getString("management_difficulty")),val(ChemicalStressLevel.class,r.getString("chemical_stress_level")),r.getInt("editorial_priority"),r.getString("metadata_version"),tags.getOrDefault(id,Map.of())));
            }
        }
        return out;
    }
    static List<TreatmentPhoto> photos(Connection c, UUID record) throws Exception {
        List<TreatmentPhoto> photos = new ArrayList<>();
        try (var p = c.prepareStatement("SELECT photo_id,file_id,image_type,sort_order,created_at FROM treatment_record_photos WHERE record_id=? ORDER BY sort_order,photo_id")) {
            p.setObject(1, record);
            try (var rs = p.executeQuery()) { while (rs.next()) photos.add(new TreatmentPhoto(
                    rs.getObject(1,UUID.class), record, rs.getObject(2,UUID.class),
                    ImageType.valueOf(rs.getString(3)), rs.getInt(4), rs.getTimestamp(5).toInstant())); }
        }
        return photos;
    }
    static Optional<StoredFile> file(Connection c, UUID fileId) throws Exception {
        try (var p = c.prepareStatement("SELECT * FROM files WHERE file_id=?")) {
            p.setObject(1, fileId);
            try (var rs = p.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new StoredFile(fileId, rs.getObject("upload_id",UUID.class),
                        rs.getObject("user_id",UUID.class), FileOwnerType.valueOf(rs.getString("owner_type")),
                        FilePurpose.valueOf(rs.getString("purpose")), FileStatus.valueOf(rs.getString("status")),
                        rs.getString("object_key"), rs.getString("content_type"), rs.getString("file_name"),
                        rs.getLong("file_size"), rs.getString("sha256"), null, null,
                        rs.getTimestamp("expires_at").toInstant(), rs.getTimestamp("created_at").toInstant(), null));
            }
        }
    }
    @SuppressWarnings("unchecked")
    static <T> T readOnlyPort(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    public static void main(String[] args) throws Exception {
        String key = System.getenv().getOrDefault("AWS_S3_ACCESS_KEY", "");
        AwsCredentialsProvider credentials = key.isBlank() ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(key, System.getenv("AWS_S3_SECRET_KEY")));
        try (S3Client s3 = S3Client.builder().region(Region.of(System.getenv("AWS_S3_REGION")))
                    .credentialsProvider(credentials).build();
             Connection c = DriverManager.getConnection(System.getenv("DB_URL"),System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD"))) {
            c.setReadOnly(true); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            try (var stmt = c.createStatement()) { stmt.execute("SET LOCAL statement_timeout='15s'"); }
            FileStoragePort storage = readOnlyPort(FileStoragePort.class, (proxy, method, values) -> {
                if (!method.getName().equals("readObject")) throw new UnsupportedOperationException("Audit only reads files");
                StoredFile file = (StoredFile) values[0]; long max = (Long) values[1];
                try (var stream = s3.getObject(GetObjectRequest.builder().bucket(System.getenv("AWS_S3_BUCKET")).key(file.objectKey()).build())) {
                    byte[] bytes = stream.readNBytes(Math.toIntExact(max + 1));
                    if (bytes.length > max) throw new IllegalStateException("Input too large");
                    return bytes;
                }
            });
            FileRepositoryPort files = readOnlyPort(FileRepositoryPort.class, (proxy, method, values) -> {
                if (!method.getName().equals("findAllById")) throw new UnsupportedOperationException("Audit only reads files");
                List<StoredFile> found = new ArrayList<>();
                for (Object id : (Collection<?>) values[0]) file(c, (UUID) id).ifPresent(found::add);
                return found;
            });
            var analyzer = new OnnxPhotoHairLengthAdapter(storage,
                    System.getenv().getOrDefault("HAIR_PHOTO_LENGTH_MODEL_PATH", "/opt/heddy/photo-models/selfie.onnx"));
            try {
                List<HairstyleCandidate> candidates = catalog(c);
                var scorer = new RuleBasedV1Scorer(); var diversifier = new RecommendationDiversifier();
                Instant now = Instant.now();
                System.out.println("READ_ONLY_RECOMMENDATION_AUDIT catalog=" + candidates.size());
                try (var stmt = c.createStatement(); var rs = stmt.executeQuery(
                        args.length > 0 && args[0].equals("--latest-account")
                        ? "SELECT u.user_id,left(md5(u.user_id::text),8) alias FROM users u JOIN recommendation_runs r USING(user_id) WHERE u.status='ACTIVE' ORDER BY r.generated_at DESC LIMIT 1"
                        : "SELECT user_id,left(md5(user_id::text),8) alias FROM users WHERE status='ACTIVE' ORDER BY alias LIMIT 100")) {
                    while (rs.next()) {
                        UUID id = rs.getObject(1,UUID.class); String alias = rs.getString(2);
                        HairProfile original = profile(c,id); List<TreatmentRecord> history = history(c,id);
                        TreatmentRecordRepositoryPort records = readOnlyPort(TreatmentRecordRepositoryPort.class, (proxy, method, values) -> {
                            if (!method.getName().equals("findByIdAndUserId")) throw new UnsupportedOperationException("Audit only reads records");
                            return history.stream().filter(record -> record.recordId().equals(values[0]) && id.equals(values[1])).findFirst();
                        });
                        var resolver = new RecommendationPhotoResolver(records,files,analyzer);
                        var resolved = resolver.resolve(id,original,history);
                        HairProfile effective = resolved.profile(); var evidence = resolved.evidence();
                        System.out.println("account=" + alias + " entered=" + (original == null ? null : original.hairLength())
                                + " effective=" + (effective == null ? null : effective.hairLength()) + " photo=" + evidence.status()
                                + " observed=" + evidence.observedLength() + " applied=" + evidence.applied());
                        if (effective == null || effective.hairLength() == null) {
                            System.out.println("  result=RECOMMENDATION_HAIR_LENGTH_REQUIRED"); continue;
                        }
                        var context = new RecommendationContext(effective,
                                ids(c,"SELECT style_tag_id FROM user_style_preferences WHERE user_id=? AND preference_type='PREFERRED'",id),
                                ids(c,"SELECT style_tag_id FROM user_style_preferences WHERE user_id=? AND preference_type='EXCLUDED'",id),
                                ids(c,"SELECT hairstyle_id FROM saved_styles WHERE user_id=?",id),history,now);
                        var eligible = scorer.scoreAll(candidates,context).stream().map(ScoredRecommendation::candidate).toList();
                        var similarities = resolver.similarities(evidence, eligible);
                        context = new RecommendationContext(effective,context.preferredTagIds(),context.excludedTagIds(),
                                context.savedHairstyleIds(),history,now,similarities);
                        var scores = scorer.scoreAll(candidates,context);
                        System.out.println("  photo_compared_styles=" + similarities.size());
                        System.out.println("  history=" + history.size() + " preferred=" + context.preferredTagIds().size()
                                + " saved=" + context.savedHairstyleIds().size() + " eligible=" + scores.size());
                        for (var item : diversifier.topThree(scores)) {
                            var b = item.scoreBreakdown();
                            System.out.printf(Locale.ROOT,"  %s [%s] score=%.2f history=%.2f hair=%.2f preferred=%.2f saved=%.2f photo=%.2f%n",
                                    item.candidate().styleName(),item.candidate().category(),item.finalScore(),b.historyScore(),
                                    b.hairCompatibilityScore(),b.preferredTagScore(),b.savedStyleScore(),b.photoSimilarityScore());
                        }
                        var withoutHistory = new RecommendationContext(effective,context.preferredTagIds(),context.excludedTagIds(),context.savedHairstyleIds(),List.of(),now,similarities);
                        System.out.println("  without_history=" + diversifier.topThree(scorer.scoreAll(candidates,withoutHistory)).stream().map(x -> x.candidate().styleName()).toList());
                    }
                }
            } finally { analyzer.close(); c.rollback(); }
        }
    }
}
