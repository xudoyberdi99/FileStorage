package com.company.entity;

import com.company.entity.base.BaseEntity;
import com.company.entity.enums.FileEnum;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "attachment")
public class AttachmentEntity extends BaseEntity {

    private String orginalName;
    private String contentType;
    private Long fileSize;
    private String extension;
    private String hashId;
    private FileEnum fileEnum;
    private String uploadFolder;
}
