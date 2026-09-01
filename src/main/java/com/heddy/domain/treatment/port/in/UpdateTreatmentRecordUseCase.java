package com.heddy.domain.treatment.port.in;

import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.model.TreatmentRecord;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** 내 시술기록의 전달된 필드만 수정한다. */
public interface UpdateTreatmentRecordUseCase {

    TreatmentRecord update(Command command);

    record Command(
            UUID requesterId,
            UUID recordId,
            Patch<Set<ServiceType>> serviceTypes,
            Patch<String> salonName,
            Patch<String> designerName,
            Patch<Instant> performedAt,
            Patch<Integer> satisfaction,
            Patch<Long> priceAmount,
            Patch<String> priceCurrency,
            Patch<UUID> appointmentId,
            Patch<String> memo,
            Patch<String> nextVisitCautions,
            Patch<Integer> durationMinutes,
            Patch<String> treatmentContent
    ) {
        /** 소요 시간·시술 내용 도입 전 호출부와의 호환을 위한 생성자. */
        public Command(
                UUID requesterId,
                UUID recordId,
                Patch<Set<ServiceType>> serviceTypes,
                Patch<String> salonName,
                Patch<String> designerName,
                Patch<Instant> performedAt,
                Patch<Integer> satisfaction,
                Patch<Long> priceAmount,
                Patch<String> priceCurrency,
                Patch<UUID> appointmentId,
                Patch<String> memo,
                Patch<String> nextVisitCautions
        ) {
            this(requesterId, recordId, serviceTypes, salonName, designerName, performedAt,
                    satisfaction, priceAmount, priceCurrency, appointmentId, memo,
                    nextVisitCautions, Patch.absent(), Patch.absent());
        }
    }

    /** {@code present=false}는 미전달, {@code present=true,value=null}은 명시적 삭제다. */
    record Patch<T>(boolean present, T value) {
        public static <T> Patch<T> absent() {
            return new Patch<>(false, null);
        }

        public static <T> Patch<T> present(T value) {
            return new Patch<>(true, value);
        }

        public T orElse(T current) {
            return present ? value : current;
        }
    }
}
