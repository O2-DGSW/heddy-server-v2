package com.heddy.application.analysis.service;

import com.heddy.domain.analysis.model.AnalysisJob;
import com.heddy.domain.analysis.model.AnalysisJobStatus;
import com.heddy.domain.analysis.model.AnalysisResult;
import com.heddy.domain.analysis.port.in.GetLatestAnalysisUseCase;
import com.heddy.domain.analysis.port.out.AnalysisJobRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisOverlayRepositoryPort;
import com.heddy.domain.analysis.port.out.AnalysisResultRepositoryPort;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import com.heddy.global.error.ApplicationException;
import com.heddy.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetLatestAnalysisService implements GetLatestAnalysisUseCase {

    private final TreatmentRecordRepositoryPort recordRepositoryPort;
    private final AnalysisResultRepositoryPort resultRepositoryPort;
    private final AnalysisOverlayRepositoryPort overlayRepositoryPort;
    private final AnalysisJobRepositoryPort jobRepositoryPort;

    @Override
    public Result get(Query query) {
        // 기록 소유권을 먼저 본다. 남의 기록은 없는 기록과 같은 404 이고, 분석이 있는지 여부도
        // 드러나면 안 된다(#31 컨벤션).
        recordRepositoryPort.findByIdAndUserId(query.recordId(), query.requesterId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        AnalysisResult analysis = resultRepositoryPort.findLatestByRecordId(query.recordId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));
        // 결과를 낸 작업의 상태를 함께 읽는다. 결과 자체에는 상태가 없고, 사진이 바뀌면
        // 작업만 STALE 로 전이되기 때문이다.
        //
        // 결과가 있는데 작업이 없을 수는 없다(job_id 는 NOT NULL FK 다). 그런 행이 나왔다면
        // 데이터가 깨진 것이므로, 상태를 SUCCEEDED 로 지어내 정상인 척하지 않고 드러낸다.
        AnalysisJobStatus status = jobRepositoryPort.findByIdAndUserId(
                        analysis.jobId(), query.requesterId())
                .map(AnalysisJob::status)
                .orElseThrow(() -> new IllegalStateException(
                        "분석 결과에 연결된 작업이 없습니다: " + analysis.analysisId()));
        return new Result(analysis, status,
                overlayRepositoryPort.findByAnalysisId(analysis.analysisId()));
    }
}
