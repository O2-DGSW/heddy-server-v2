package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ImageType;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 시술기록 등록 유스케이스. 불변식(미래 시술일 금지, 시술 종류 1개 이상, 사진 최대 10장)은
 * 도메인 팩터리가 검증하고, 첨부 file_id 의 존재·소유·READY 확인은 애플리케이션 서비스가 맡는다.
 */
public interface CreateTreatmentRecordUseCase {

    /** 저장된 기록을 돌려준다. 사진 URL 은 여기서 발급하지 않고 조회 때 발급한다. */
    TreatmentRecord create(Command command);

    /** {@code photos} 는 업로드를 마친(READY) 요청자 소유 파일만 가리킨다. */
    record Command(
            UUID userId,
            Set<ServiceType> serviceTypes,
            String salonName,
            String designerName,
            Instant performedAt,
            Integer satisfaction,
            Long priceAmount,
            String priceCurrency,
            UUID appointmentId,
            String memo,
            String nextVisitCautions,
            List<Photo> photos
    ) {
        public Command(
                UUID userId,
                Set<ServiceType> serviceTypes,
                String salonName,
                String designerName,
                Instant performedAt,
                Integer satisfaction,
                Long priceAmount,
                String priceCurrency,
                UUID appointmentId,
                List<Photo> photos
        ) {
            this(userId, serviceTypes, salonName, designerName, performedAt, satisfaction,
                    priceAmount, priceCurrency, appointmentId, null, null, photos);
        }

        public record Photo(UUID fileId, ImageType imageType) {
        }
    }
}
