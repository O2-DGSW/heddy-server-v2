package com.heddy.application.account.service;

import com.heddy.domain.account.model.Account;
import com.heddy.domain.account.model.AccountDeletionRequest;
import com.heddy.domain.account.port.out.AccountDeletionRepositoryPort;
import com.heddy.domain.account.port.out.AccountRepositoryPort;
import com.heddy.domain.account.port.out.HairProfileRepositoryPort;
import com.heddy.domain.account.port.out.UserProfileRepositoryPort;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import com.heddy.domain.treatment.port.out.TreatmentRecordRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountDeletionProcessor {

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountDeletionRepositoryPort deletionRepositoryPort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final HairProfileRepositoryPort hairProfileRepositoryPort;
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
        treatmentRecordRepositoryPort.deleteAllByUserId(request.userId());
        hairProfileRepositoryPort.deleteByUserId(request.userId());
        userProfileRepositoryPort.deleteByUserId(request.userId());
        accountRepositoryPort.save(account.anonymizeAsDeleted());
        deletionRepositoryPort.save(request.complete(Instant.now()));
    }
}
