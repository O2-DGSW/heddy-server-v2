package com.heddy.domain.file.port.out;

import com.heddy.domain.file.model.StoredFile;

import java.util.Optional;
import java.util.UUID;

public interface FileRepositoryPort {

    StoredFile save(StoredFile file);

    Optional<StoredFile> findById(UUID fileId);
}
