package com.heddy.application.treatment.service;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.port.out.AnalysisStalenessPort;
import com.heddy.domain.analysis.port.out.LatestAnalysisStatusPort;
import com.heddy.domain.recommendation.port.out.RecommendationStalenessPort;
import com.heddy.domain.sharing.port.out.SharedRecordLookupPort;
import com.heddy.domain.treatment.exception.TreatmentError;
import com.heddy.domain.treatment.exception.TreatmentException;
import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentRecordFilter;
import com.heddy.domain.treatment.model.TreatmentRecordPage;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.DeleteTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetPhotoComparisonUseCase;
import com.heddy.domain.treatment.port.in.ListTreatmentRecordsUseCase;
import com.heddy.domain.treatment.port.in.ManageTreatmentPhotosUseCase;
import com.heddy.domain.treatment.port.in.UpdateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시술기록 등록·단건 조회. 등록 때는 첨부 file_id 가 READY 인 요청자 소유인지 확인하고
 * (#28 의 완료 검증을 통과한 파일만 존재한다), 조회 때만 사진의 Presigned GET URL 을
 * 발급한다 — URL 은 어디에도 저장하지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class TreatmentRecordService implements CreateTreatmentRecordUseCase,
        GetTreatmentRecordUseCase, ListTreatmentRecordsUseCase,
        UpdateTreatmentRecordUseCase, DeleteTreatmentRecordUseCase,
        ManageTreatmentPhotosUseCase, GetPhotoComparisonUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String SORT_ASCENDING = "performedAt,asc";
    private static final String SORT_DESCENDING = "performedAt,desc";

    private final TreatmentRecordRepositoryPort recordRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;
    private final AnalysisStalenessPort analysisStalenessPort;
    private final RecommendationStalenessPort recommendationStalenessPort;
    private final SharedRecordLookupPort sharedRecordLookupPort;
    private final LatestAnalysisStatusPort latestAnalysisStatusPort;

    @Autowired
    public TreatmentRecordService(
            TreatmentRecordRepositoryPort recordRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            ObjectProvider<AnalysisStalenessPort> analysisStalenessPortProvider,
            SharedRecordLookupPort sharedRecordLookupPort,
            LatestAnalysisStatusPort latestAnalysisStatusPort,
            ObjectProvider<RecommendationStalenessPort> recommendationStalenessPortProvider
    ) {
        this.recordRepositoryPort = recordRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.analysisStalenessPort = analysisStalenessPortProvider.getIfAvailable(() -> recordId -> { });
        this.sharedRecordLookupPort = sharedRecordLookupPort;
        this.latestAnalysisStatusPort = latestAnalysisStatusPort;
        this.recommendationStalenessPort = recommendationStalenessPortProvider
                .getIfAvailable(() -> recordId -> { });
    }

    /** 분석 도메인이 없는 단위 테스트와의 호환을 위한 생성자. */
    TreatmentRecordService(
            TreatmentRecordRepositoryPort recordRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort
    ) {
        this.recordRepositoryPort = recordRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.analysisStalenessPort = recordId -> { };
        this.recommendationStalenessPort = recordId -> { };
        this.sharedRecordLookupPort = (ownerId, recordIds, now) -> Set.of();
        this.latestAnalysisStatusPort = recordIds -> Map.of();
    }

    TreatmentRecordService(
            TreatmentRecordRepositoryPort recordRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort,
            AnalysisStalenessPort analysisStalenessPort,
            SharedRecordLookupPort sharedRecordLookupPort,
            LatestAnalysisStatusPort latestAnalysisStatusPort
    ) {
        this.recordRepositoryPort = recordRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
        this.analysisStalenessPort = analysisStalenessPort;
        this.recommendationStalenessPort = recordId -> { };
        this.sharedRecordLookupPort = sharedRecordLookupPort;
        this.latestAnalysisStatusPort = latestAnalysisStatusPort;
    }

    @Override
    @Transactional
    public TreatmentRecord create(CreateTreatmentRecordUseCase.Command command) {
        // 도메인 팩터리가 불변식을 먼저 통과시킨다. attachPhoto 가 사진 장수 상한을 재검증한다.
        TreatmentRecord record = TreatmentRecord.create(
                command.userId(), command.serviceTypes(), command.salonName(), command.designerName(),
                command.performedAt(), command.satisfaction(), command.priceAmount(),
                command.priceCurrency(), command.appointmentId(), command.memo(),
                command.nextVisitCautions(), command.durationMinutes(),
                command.treatmentContent());
        for (CreateTreatmentRecordUseCase.Command.Photo photo : command.photos()) {
            requireOwnedReadyFile(command.userId(), photo.fileId());
            record = record.attachPhoto(
                    TreatmentPhoto.create(record.recordId(), photo.fileId(),
                            photo.imageType(), photo.sortOrder()));
        }
        return recordRepositoryPort.insert(record);
    }

    @Override
    public GetTreatmentRecordUseCase.Result get(GetTreatmentRecordUseCase.Query query) {
        // 소유자 조건을 질의에 함께 실어 사진을 읽기 전에 DB 에서 거른다. 남의 기록은 없는 기록과
        // 같은 404 이고, 질의 횟수도 없는 기록과 같아야 존재 여부가 새지 않는다(#31).
        TreatmentRecord record = recordRepositoryPort
                .findByIdAndUserId(query.recordId(), query.requesterId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        Map<UUID, URI> photoUrls = new HashMap<>();
        for (TreatmentPhoto photo : record.photos()) {
            photoUrls.put(photo.photoId(), downloadUrl(photo));
        }
        return new GetTreatmentRecordUseCase.Result(record, photoUrls);
    }

    @Override
    public ListTreatmentRecordsUseCase.Result list(ListTreatmentRecordsUseCase.Query query) {
        validateListQuery(query);
        TreatmentRecordFilter filter = new TreatmentRecordFilter(
                query.requesterId(), query.serviceType(), normalizeFilter(query.designerName()),
                normalizeFilter(query.salonName()), query.from(), query.to(), query.page(), query.size(),
                SORT_ASCENDING.equals(query.sort()));
        TreatmentRecordPage page = recordRepositoryPort.findPage(filter);
        Map<UUID, URI> thumbnails = thumbnailsFor(page.items());
        // 페이지의 기록을 한 번에 넣고 묻는다. 기록마다 물으면 페이지 크기만큼 왕복이 는다.
        List<UUID> recordIds = page.items().stream().map(TreatmentRecord::recordId).toList();
        Set<UUID> sharedRecordIds = sharedRecordLookupPort.findSharedRecordIds(
                query.requesterId(), recordIds, Instant.now());
        Map<UUID, AnalysisJobStatus> analysisStatuses =
                latestAnalysisStatusPort.findLatestStatuses(recordIds);
        List<ListTreatmentRecordsUseCase.Item> items = page.items().stream()
                .map(record -> new ListTreatmentRecordsUseCase.Item(
                        record, thumbnails.get(record.recordId()),
                        // 분석을 한 번도 요청하지 않은 기록은 상태 자체가 없어 null 로 둔다.
                        // 별도 값으로 바꾸면 클라이언트가 상태 6종에 없는 값을 알아야 한다.
                        analysisStatuses.get(record.recordId()),
                        sharedRecordIds.contains(record.recordId())))
                .toList();
        return new ListTreatmentRecordsUseCase.Result(
                items, query.page(), query.size(), page.totalElements());
    }

    @Override
    @Transactional
    public TreatmentRecord update(UpdateTreatmentRecordUseCase.Command command) {
        TreatmentRecord current = ownedRecord(command.requesterId(), command.recordId());
        Long priceAmount = command.priceAmount().orElse(current.priceAmount());
        String priceCurrency = command.priceCurrency().orElse(current.priceCurrency());
        // 금액을 지우면 저장돼 있던 통화도 함께 지운다. 금액 없는 통화는 무엇의 통화인지
        // 알 수 없어 도메인이 거절하는데, 가격을 비우려고 price_amount 에만 null 을 보낸
        // 요청이 그 오류를 맞는 건 클라이언트가 풀 수 없는 문제다. 통화를 이번 요청에서
        // 직접 지정했다면 그 값은 존중하고 도메인 판단에 맡긴다.
        if (priceAmount == null && !command.priceCurrency().present()) {
            priceCurrency = null;
        }
        TreatmentRecord updated = current.update(
                command.serviceTypes().orElse(current.serviceTypes()),
                command.salonName().orElse(current.salonName()),
                command.designerName().orElse(current.designerName()),
                command.performedAt().orElse(current.performedAt()),
                command.satisfaction().orElse(current.satisfaction()),
                priceAmount,
                priceCurrency,
                command.appointmentId().orElse(current.appointmentId()),
                command.memo().orElse(current.memo()),
                command.nextVisitCautions().orElse(current.nextVisitCautions()),
                command.durationMinutes().orElse(current.durationMinutes()),
                command.treatmentContent().orElse(current.treatmentContent()));
        return recordRepositoryPort.update(updated)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    @Transactional
    public void delete(DeleteTreatmentRecordUseCase.Command command) {
        TreatmentRecord record = ownedRecord(command.requesterId(), command.recordId());
        for (TreatmentPhoto photo : record.photos()) {
            fileRepositoryPort.findById(photo.fileId()).ifPresent(file -> {
                if (file.status() != FileStatus.DELETED) {
                    fileRepositoryPort.transition(file.markDeleted(), file.status());
                }
            });
        }
        // FK CASCADE로 근거 행이 사라지기 전에 해당 추천 실행을 이력 상태로 전환한다.
        recommendationStalenessPort.markByReferenceRecordStale(record.recordId());
        // 공개 API에 소프트 삭제 개념이 없으므로 기록은 하드 삭제하고 사진 행은 FK CASCADE에 맡긴다.
        if (!recordRepositoryPort.deleteById(record.recordId())) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public ManageTreatmentPhotosUseCase.Result add(ManageTreatmentPhotosUseCase.AddCommand command) {
        TreatmentRecord record = ownedLockedRecord(command.requesterId(), command.recordId());
        // 기록 행을 잠근 뒤 계산해야 동시 추가가 같은 순번을 집지 않는다.
        int sortOrder = command.sortOrder() == null
                ? record.nextSortOrder() : command.sortOrder();
        TreatmentPhoto photo = TreatmentPhoto.create(
                record.recordId(), command.fileId(), command.imageType(), sortOrder);
        record.attachPhoto(photo);
        requireOwnedReadyFile(command.requesterId(), command.fileId());
        TreatmentPhoto saved = recordRepositoryPort.insertPhoto(photo);
        if (saved.imageType() == ImageType.AFTER) {
            analysisStalenessPort.markLatestStale(record.recordId());
        }
        return new ManageTreatmentPhotosUseCase.Result(saved, downloadUrl(saved));
    }

    @Override
    @Transactional
    public ManageTreatmentPhotosUseCase.Result update(ManageTreatmentPhotosUseCase.UpdateCommand command) {
        TreatmentRecord record = ownedRecord(command.requesterId(), command.recordId());
        TreatmentPhoto current = ownedPhoto(record, command.photoId());
        if (command.fileId() == null && command.imageType() == null && command.sortOrder() == null) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
        UUID fileId = command.fileId() == null ? current.fileId() : command.fileId();
        if (!fileId.equals(current.fileId())) {
            requireOwnedReadyFile(command.requesterId(), fileId);
            requireUnattachedFile(fileId);
        }
        ImageType imageType = command.imageType() == null ? current.imageType() : command.imageType();
        int sortOrder = command.sortOrder() == null ? current.sortOrder() : command.sortOrder();
        TreatmentPhoto updated = current.update(fileId, imageType, sortOrder);
        TreatmentPhoto saved = recordRepositoryPort.updatePhoto(updated)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        boolean typeCrossedAfter = current.imageType() != saved.imageType()
                && (current.imageType() == ImageType.AFTER || saved.imageType() == ImageType.AFTER);
        // 사진이 바뀌면 그 사진을 근거로 낸 분석 결과는 더 이상 맞지 않는다.
        boolean afterFileReplaced = saved.imageType() == ImageType.AFTER
                && !saved.fileId().equals(current.fileId());
        if (typeCrossedAfter || afterFileReplaced) {
            analysisStalenessPort.markLatestStale(record.recordId());
        }
        return new ManageTreatmentPhotosUseCase.Result(saved, downloadUrl(saved));
    }

    @Override
    @Transactional
    public void delete(ManageTreatmentPhotosUseCase.DeleteCommand command) {
        TreatmentRecord record = ownedRecord(command.requesterId(), command.recordId());
        TreatmentPhoto photo = ownedPhoto(record, command.photoId());
        StoredFile file = fileRepositoryPort.findById(photo.fileId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!recordRepositoryPort.deletePhoto(photo.photoId())) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (file.status() != FileStatus.DELETED) {
            fileRepositoryPort.transition(file.markDeleted(), file.status());
        }
        if (photo.imageType() == ImageType.AFTER) {
            analysisStalenessPort.markLatestStale(record.recordId());
        }
    }

    @Override
    public GetPhotoComparisonUseCase.Result getPhotoComparison(
            GetPhotoComparisonUseCase.Query query
    ) {
        TreatmentRecord record = ownedRecord(query.requesterId(), query.recordId());
        List<GetPhotoComparisonUseCase.Photo> before = comparisonPhotos(record, ImageType.BEFORE);
        List<GetPhotoComparisonUseCase.Photo> after = comparisonPhotos(record, ImageType.AFTER);
        if (before.isEmpty() || after.isEmpty()) {
            throw new TreatmentException(TreatmentError.PHOTO_COMPARISON_NOT_AVAILABLE);
        }
        return new GetPhotoComparisonUseCase.Result(
                record.recordId(), before, after,
                new GetPhotoComparisonUseCase.TreatmentSummary(
                        record.serviceTypes(), record.satisfaction(), record.nextVisitCautions()));
    }

    private TreatmentRecord ownedRecord(UUID requesterId, UUID recordId) {
        return recordRepositoryPort.findByIdAndUserId(recordId, requesterId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private TreatmentRecord ownedLockedRecord(UUID requesterId, UUID recordId) {
        return recordRepositoryPort.findByIdForUpdate(recordId)
                .filter(record -> record.userId().equals(requesterId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private TreatmentPhoto ownedPhoto(TreatmentRecord record, UUID photoId) {
        return record.photos().stream()
                .filter(photo -> photo.photoId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private List<GetPhotoComparisonUseCase.Photo> comparisonPhotos(
            TreatmentRecord record, ImageType imageType
    ) {
        return record.photos().stream()
                .filter(photo -> photo.imageType() == imageType)
                .map(photo -> {
                    URI url = downloadUrl(photo);
                    return url == null ? null
                            : new GetPhotoComparisonUseCase.Photo(
                                    photo.photoId(), photo.sortOrder(), url);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void validateListQuery(ListTreatmentRecordsUseCase.Query query) {
        boolean invalidRange = query.from() != null && query.to() != null
                && query.from().isAfter(query.to());
        boolean invalidPage = query.page() < 0 || query.size() < 1 || query.size() > MAX_PAGE_SIZE;
        boolean invalidSort = !SORT_ASCENDING.equals(query.sort())
                && !SORT_DESCENDING.equals(query.sort());
        boolean invalidDesigner = exceedsLength(query.designerName(), 30);
        boolean invalidSalon = exceedsLength(query.salonName(), 50);
        if (invalidRange || invalidPage || invalidSort || invalidDesigner || invalidSalon) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean exceedsLength(String value, int maximum) {
        return value != null && value.strip().length() > maximum;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    /**
     * 페이지 전체의 대표 사진 파일을 질의 한 번으로 읽고 같은 파일은 한 번만 서명한다. 기록마다
     * 파일을 다시 읽고 presigned URL 을 다시 발급하던 N+1(#66)을 쿼리·서명 횟수 고정으로 끊는다.
     * 대표 사진은 생성 순서상 첫 번째로 조회 가능한 사진이라는 기존 규칙을 그대로 둔다.
     */
    private Map<UUID, URI> thumbnailsFor(List<TreatmentRecord> records) {
        List<UUID> fileIds = records.stream()
                .flatMap(record -> record.photos().stream())
                .map(TreatmentPhoto::fileId)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, StoredFile> files = fileRepositoryPort.findAllById(fileIds).stream()
                .collect(Collectors.toMap(StoredFile::fileId, Function.identity()));
        Map<UUID, URI> signedUrls = new HashMap<>();
        Map<UUID, URI> thumbnails = new HashMap<>();
        for (TreatmentRecord record : records) {
            for (TreatmentPhoto photo : record.photos()) {
                StoredFile file = files.get(photo.fileId());
                if (file == null || !file.isReady()) {
                    continue;
                }
                // 같은 파일이 여러 기록의 대표 사진이어도 서명은 한 번만 발급한다.
                thumbnails.put(record.recordId(), signedUrls.computeIfAbsent(
                        photo.fileId(), fileId -> fileStoragePort.createDownloadUrl(file)));
                break;
            }
        }
        return thumbnails;
    }

    /**
     * 첨부 가능한 파일인지 #28 이 정리해둔 세 가지로 좁혀 본다. 오류 코드는 파일 도메인과 공유한다 —
     * 없으면 RESOURCE_NOT_FOUND, 남의 파일이면 FORBIDDEN_RESOURCE, 아직 업로드가 끝나지 않았으면
     * FILE_INVALID_STATE 로 답한다.
     */
    private void requireOwnedReadyFile(UUID requesterId, UUID fileId) {
        StoredFile file = fileRepositoryPort.findById(fileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!requesterId.equals(file.userId())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        if (!file.isReady()) {
            throw new ApplicationException(ErrorCode.FILE_INVALID_STATE);
        }
    }

    /**
     * 이미 다른 사진이 가리키는 파일은 붙이지 않는다. 한 파일을 두 사진이 공유하면 한쪽을
     * 지울 때 파일이 정리 대상이 되어 남은 사진의 URL 이 조용히 비게 된다.
     */
    private void requireUnattachedFile(UUID fileId) {
        if (recordRepositoryPort.isFileAttached(fileId)) {
            throw new ApplicationException(ErrorCode.FILE_INVALID_STATE);
        }
    }

    /** 파일 행이 준비 상태가 아니면(지워졌거나 미완료) URL 자리를 비워 둔다. */
    private URI downloadUrl(TreatmentPhoto photo) {
        return fileRepositoryPort.findById(photo.fileId())
                .filter(StoredFile::isReady)
                .map(fileStoragePort::createDownloadUrl)
                .orElse(null);
    }
}
