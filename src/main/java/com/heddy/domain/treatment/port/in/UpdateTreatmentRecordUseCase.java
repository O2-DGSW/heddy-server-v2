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
            Patch<String> nextVisitCautions
    ) {
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
