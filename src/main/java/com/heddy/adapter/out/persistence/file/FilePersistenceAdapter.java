package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.exception.FileError;
import com.heddy.domain.file.exception.FileException;
import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FilePersistenceAdapter implements FileRepositoryPort {

    private final FileJpaRepository repository;

    @Override
    public StoredFile insert(StoredFile file) {
        return repository.saveAndFlush(new FileEntity(file)).toDomain();
    }

    @Override
    public StoredFile transition(StoredFile file, FileStatus expectedStatus) {
        int updated = repository.applyTransition(
                file.fileId(), expectedStatus, file.status(), file.contentType(),
                file.fileSize(), file.sha256(), file.width(), file.height(), Instant.now());
        if (updated == 0) {
            throw new FileException(FileError.CONCURRENT_MODIFICATION);
        }
        return findById(file.fileId()).orElseThrow();
    }

    @Override
    public Optional<StoredFile> findById(UUID fileId) {
        return repository.findById(fileId).map(FileEntity::toDomain);
    }
}
