package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.sharing.port.out.ShareRepositoryPort;
import com.heddy.domain.style.port.out.SavedStyleRepositoryPort;
import com.heddy.domain.style.port.out.UserStylePreferenceRepositoryPort;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountDeletionRepositoryPort deletionRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final HairProfileRepositoryPort hairProfileRepositoryPort;
    private final UserStylePreferenceRepositoryPort stylePreferenceRepositoryPort;
    private final ShareRepositoryPort shareRepositoryPort;
    private final SavedStyleRepositoryPort savedStyleRepositoryPort;
    private final TreatmentRecordRepositoryPort treatmentRecordRepositoryPort;
    private final FileRepositoryPort fileRepositoryPort;

    @Transactional
    public void process(AccountDeletionRequest request) {
        Account account = accountRepositoryPort.findByIdForUpdate(request.userId()).orElseThrow();
        fileRepositoryPort.findAllByUserId(request.userId()).forEach(file -> {
            if (file.status() != FileStatus.DELETED) {
                fileRepositoryPort.transition(file.markDeleted(), file.status());
            }
        });
        // 공유 연결이 기록·후보를 참조하므로 가장 먼저 없앤다. 공개 링크도 이 시점부터
        // 조회되지 않아 탈퇴 접수 후 데이터가 다시 노출되지 않는다.
        shareRepositoryPort.deleteAllByUserId(request.userId());
        savedStyleRepositoryPort.deleteAllByUserId(request.userId());
        stylePreferenceRepositoryPort.replace(request.userId(), List.of());
        treatmentRecordRepositoryPort.deleteAllByUserId(request.userId());
        hairProfileRepositoryPort.deleteByUserId(request.userId());
        userProfileRepositoryPort.deleteByUserId(request.userId());
        accountRepositoryPort.save(account.anonymizeAsDeleted());
        deletionRepositoryPort.save(request.complete(Instant.now()));
    }
}
