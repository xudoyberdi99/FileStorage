package com.company.repository;

import com.company.entity.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<AttachmentEntity,Long> {
    Optional<AttachmentEntity>  findByHashId(String hashId);

}
