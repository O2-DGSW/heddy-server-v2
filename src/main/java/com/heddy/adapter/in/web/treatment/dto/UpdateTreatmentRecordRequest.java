package com.heddy.adapter.in.web.treatment.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.heddy.domain.treatment.model.ServiceType;
import com.heddy.domain.treatment.port.in.UpdateTreatmentRecordUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** 미전달과 명시적 null을 구분하는 시술기록 부분 수정 요청. */
@Schema(description = "전달한 필드만 수정하며 nullable 필드는 null로 삭제할 수 있습니다")
public class UpdateTreatmentRecordRequest {

    @Size(min = 1)
    private Set<ServiceType> serviceTypes;
    @Size(max = 50)
    private String salonName;
    @Size(max = 30)
    private String designerName;
    private Instant performedAt;
    @Min(1) @Max(5)
    private Integer satisfaction;
    @PositiveOrZero
    private Long priceAmount;
    @Pattern(regexp = "^[A-Za-z]{3}$")
    private String priceCurrency;
    private UUID appointmentId;
    private String memo;
    private String nextVisitCautions;

    private boolean serviceTypesPresent;
    private boolean salonNamePresent;
    private boolean designerNamePresent;
    private boolean performedAtPresent;
    private boolean satisfactionPresent;
    private boolean priceAmountPresent;
    private boolean priceCurrencyPresent;
    private boolean appointmentIdPresent;
    private boolean memoPresent;
    private boolean nextVisitCautionsPresent;
    private Integer durationMinutes;
    private boolean durationMinutesPresent;
    private String treatmentContent;
    private boolean treatmentContentPresent;

    @JsonSetter("service_types")
    public void setServiceTypes(Set<ServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes;
        serviceTypesPresent = true;
    }

    @JsonSetter("salon_name")
    public void setSalonName(String salonName) {
        this.salonName = salonName;
        salonNamePresent = true;
    }

    @JsonSetter("designer_name")
    public void setDesignerName(String designerName) {
        this.designerName = designerName;
        designerNamePresent = true;
    }

    @JsonSetter("performed_at")
    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
        performedAtPresent = true;
    }

    @JsonSetter("satisfaction")
    public void setSatisfaction(Integer satisfaction) {
        this.satisfaction = satisfaction;
        satisfactionPresent = true;
    }

    @JsonSetter("price_amount")
    public void setPriceAmount(Long priceAmount) {
        this.priceAmount = priceAmount;
        priceAmountPresent = true;
    }

    @JsonSetter("price_currency")
    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
        priceCurrencyPresent = true;
    }

    @JsonSetter("appointment_id")
    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
        appointmentIdPresent = true;
    }

    @JsonSetter("memo")
    public void setMemo(String memo) {
        this.memo = memo;
        memoPresent = true;
    }

    @JsonSetter("next_visit_cautions")
    public void setNextVisitCautions(String nextVisitCautions) {
        this.nextVisitCautions = nextVisitCautions;
        nextVisitCautionsPresent = true;
    }

    @JsonSetter("duration_minutes")
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
        durationMinutesPresent = true;
    }

    @JsonSetter("treatment_content")
    public void setTreatmentContent(String treatmentContent) {
        this.treatmentContent = treatmentContent;
        treatmentContentPresent = true;
    }

    public UpdateTreatmentRecordUseCase.Command toCommand(UUID requesterId, UUID recordId) {
        return new UpdateTreatmentRecordUseCase.Command(
                requesterId, recordId,
                patch(serviceTypesPresent, serviceTypes),
                patch(salonNamePresent, salonName),
                patch(designerNamePresent, designerName),
                patch(performedAtPresent, performedAt),
                patch(satisfactionPresent, satisfaction),
                patch(priceAmountPresent, priceAmount),
                patch(priceCurrencyPresent, priceCurrency),
                patch(appointmentIdPresent, appointmentId),
                patch(memoPresent, memo),
                patch(nextVisitCautionsPresent, nextVisitCautions),
                patch(durationMinutesPresent, durationMinutes),
                patch(treatmentContentPresent, treatmentContent));
    }

    private <T> UpdateTreatmentRecordUseCase.Patch<T> patch(boolean present, T value) {
        return present
                ? UpdateTreatmentRecordUseCase.Patch.present(value)
                : UpdateTreatmentRecordUseCase.Patch.absent();
    }
}
