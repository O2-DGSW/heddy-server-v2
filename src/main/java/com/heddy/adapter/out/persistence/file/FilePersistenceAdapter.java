package com.heddy.adapter.out.persistence.file;

import com.heddy.domain.file.model.StoredFile;
import com.heddy.domain.file.port.out.FileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FilePersistenceAdapter implements FileRepositoryPort {

    private final FileJpaRepository repository;

    @Override
    public StoredFile save(StoredFile file) {
        FileEntity entity = repository.findById(file.fileId())
                .orElseGet(() -> new FileEntity(file));
        entity.update(file);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<StoredFile> findById(UUID fileId) {
        return repository.findById(fileId).map(FileEntity::toDomain);
    }
}
