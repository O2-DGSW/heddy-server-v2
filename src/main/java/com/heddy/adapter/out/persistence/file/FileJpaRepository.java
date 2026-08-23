package com.heddy.adapter.out.persistence.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface FileJpaRepository extends JpaRepository<FileEntity, UUID> {
}
