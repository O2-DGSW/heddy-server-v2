package com.heddy.application.treatment.service;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.file.port.out.FileStoragePort;
import com.heddy.domain.treatment.model.TreatmentPhoto;
import com.heddy.domain.treatment.model.TreatmentRecord;
import com.heddy.domain.treatment.model.TreatmentRecordFilter;
import com.heddy.domain.treatment.model.TreatmentRecordPage;
import com.heddy.domain.treatment.port.in.CreateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.DeleteTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.GetTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.in.ListTreatmentRecordsUseCase;
import com.heddy.domain.treatment.port.in.UpdateTreatmentRecordUseCase;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 시술기록 등록·단건 조회. 등록 때는 첨부 file_id 가 READY 인 요청자 소유인지 확인하고
 * (#28 의 완료 검증을 통과한 파일만 존재한다), 조회 때만 사진의 Presigned GET URL 을
 * 발급한다 — URL 은 어디에도 저장하지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class TreatmentRecordService implements CreateTreatmentRecordUseCase,
        GetTreatmentRecordUseCase, ListTreatmentRecordsUseCase,
        UpdateTreatmentRecordUseCase, DeleteTreatmentRecordUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String SORT_ASCENDING = "performedAt,asc";
    private static final String SORT_DESCENDING = "performedAt,desc";

    private final TreatmentRecordRepositoryPort recordRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;
    private final FileStoragePort fileStoragePort;

    public TreatmentRecordService(
            TreatmentRecordRepositoryPort recordRepositoryPort,
            FileRepositoryPort fileRepositoryPort,
            FileStoragePort fileStoragePort
    ) {
        this.recordRepositoryPort = recordRepositoryPort;
        this.fileRepositoryPort = fileRepositoryPort;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional
    public TreatmentRecord create(CreateTreatmentRecordUseCase.Command command) {
        // 도메인 팩터리가 불변식을 먼저 통과시킨다. attachPhoto 가 사진 장수 상한을 재검증한다.
        TreatmentRecord record = TreatmentRecord.create(
                command.userId(), command.serviceTypes(), command.salonName(), command.designerName(),
                command.performedAt(), command.satisfaction(), command.priceAmount(),
                command.priceCurrency(), command.appointmentId(), command.memo(),
                command.nextVisitCautions());
        for (CreateTreatmentRecordUseCase.Command.Photo photo : command.photos()) {
            requireOwnedReadyFile(command.userId(), photo.fileId());
            record = record.attachPhoto(
                    TreatmentPhoto.create(record.recordId(), photo.fileId(), photo.imageType()));
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
        List<ListTreatmentRecordsUseCase.Item> items = page.items().stream()
                .map(record -> new ListTreatmentRecordsUseCase.Item(
                        record, thumbnailUrl(record), null))
                .toList();
        return new ListTreatmentRecordsUseCase.Result(
                items, query.page(), query.size(), page.totalElements());
    }

    @Override
    @Transactional
    public TreatmentRecord update(UpdateTreatmentRecordUseCase.Command command) {
        TreatmentRecord current = ownedRecord(command.requesterId(), command.recordId());
        TreatmentRecord updated = current.update(
                command.serviceTypes().orElse(current.serviceTypes()),
                command.salonName().orElse(current.salonName()),
                command.designerName().orElse(current.designerName()),
                command.performedAt().orElse(current.performedAt()),
                command.satisfaction().orElse(current.satisfaction()),
                command.priceAmount().orElse(current.priceAmount()),
                command.priceCurrency().orElse(current.priceCurrency()),
                command.appointmentId().orElse(current.appointmentId()),
                command.memo().orElse(current.memo()),
                command.nextVisitCautions().orElse(current.nextVisitCautions()));
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
        // 공개 API에 소프트 삭제 개념이 없으므로 기록은 하드 삭제하고 사진 행은 FK CASCADE에 맡긴다.
        if (!recordRepositoryPort.deleteById(record.recordId())) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private TreatmentRecord ownedRecord(UUID requesterId, UUID recordId) {
        return recordRepositoryPort.findByIdAndUserId(recordId, requesterId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
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

    /** 목록 대표 이미지는 생성 순서상 첫 번째로 조회 가능한 사진을 사용한다. */
    private URI thumbnailUrl(TreatmentRecord record) {
        for (TreatmentPhoto photo : record.photos()) {
            URI url = downloadUrl(photo);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * 첨부 가능한 파일인지 #28 이 정리해둔 세 가지로 좁혀 본다. 오류 코드는 파일 도메인과 공유한다 —
     * 없으면 RESOURCE_NOT_FOUND, 남의 파일이면 FORBIDDEN_RESOURCE, 아직 업로드가 끝나지 않았으면
     * FILE_INVALID_STATE 로 답한다.
     */
    private void requireOwnedReadyFile(UUID requesterId, UUID fileId) {
        StoredFile file = fileRepositoryPort.findById(fileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!file.userId().equals(requesterId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_RESOURCE);
        }
        if (!file.isReady()) {
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
