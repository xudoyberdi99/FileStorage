package com.company.service;

import com.company.entity.AttachmentEntity;
import com.company.entity.enums.FileEnum;
import com.company.repository.AttachmentRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;

@Service
public class FileStorageService {
    @Autowired
    private  AttachmentRepository attachmentRepository;
    private final Hashids hashids;

    @Value("${upload.server}")
    private String serverPath;

    public FileStorageService() {
        this.hashids = new Hashids(getClass().getName(),6);
    }

    public AttachmentEntity save(MultipartFile multipartFile) throws IOException {
        AttachmentEntity attachment=new AttachmentEntity();
        attachment.setOrginalName(multipartFile.getOriginalFilename());
        attachment.setFileSize(multipartFile.getSize());
        attachment.setContentType(multipartFile.getContentType());
        attachment.setFileEnum(FileEnum.DRAFT);
        attachment.setExtension(getExtinons(multipartFile.getOriginalFilename()));
        AttachmentEntity attachmentEntity = attachmentRepository.save(attachment);

        Date date=new Date();
        String path=String.format("%s/upload_files/%d/%d/%d/",this.serverPath,
                1900+date.getYear(),1+date.getMonth(),date.getDate());
        File uploadFolder=new File(path);
        if (!uploadFolder.exists() && uploadFolder.mkdirs()){
            System.out.println("created folder");
        }
        attachment.setHashId(hashids.encode(attachment.getId()));
        String pathlocal=String.format("/upload_files/%d/%d/%d/%s.%s",
                1900+date.getYear(),
                1+date.getMonth(),
                date.getDate(),
                attachment.getHashId(),
                attachment.getExtension());

        attachment.setUploadFolder(pathlocal);
        attachmentRepository.save(attachment);

        uploadFolder=uploadFolder.getAbsoluteFile();
        System.out.println(uploadFolder);
        File file=new File(uploadFolder,String.format("%s.%s",attachment.getHashId(),attachment.getExtension()));
        multipartFile.transferTo(file);

        return attachment;
    }


    public AttachmentEntity viewFile(String hashId) {
        Optional<AttachmentEntity> byHashId = attachmentRepository.findByHashId(hashId);
        if (!byHashId.isPresent()){
            return new AttachmentEntity();
        }
        return byHashId.get();
    }

    public String  deleteFile(String hashId){
        Optional<AttachmentEntity> byHashId = attachmentRepository.findByHashId(hashId);
        if (!byHashId.isPresent()){
            return "not found file";
        }
        File file=new File(String.format("%s/%s",this.serverPath,byHashId.get().getUploadFolder()));
        if (file.delete()){
            attachmentRepository.delete(byHashId.get());
        }
        return "delete file success";
    }

    private  String getExtinons(String filename){
        String ext=null;
        if (filename !=null && !filename.isEmpty()){
            int dot = filename.lastIndexOf('.');
            if (dot>0 && dot<= filename.length()-2){
                ext=filename.substring(dot+1);
            }
        }
        return ext;
    }


    // 2-usul File System
    public AttachmentEntity addAttachment(MultipartHttpServletRequest request) throws IOException {

        Iterator<String> fileNames = request.getFileNames();

        while (fileNames.hasNext()){
            MultipartFile file = request.getFile(fileNames.next());
            if (file!=null){
                AttachmentEntity attachment=new AttachmentEntity();
                attachment.setFileSize(file.getSize());
                attachment.setContentType(file.getContentType());
                attachment.setOrginalName(file.getOriginalFilename());
                attachment.setFileEnum(FileEnum.DRAFT);
                attachment.setExtension(getExtinons(file.getOriginalFilename()));
                AttachmentEntity attachmentEntity = attachmentRepository.save(attachment);

                Date date=new Date();
                String path=String.format("%s/upload_files/%d/%d/%d/",this.serverPath,
                        1900+date.getYear(),1+date.getMonth(),date.getDate());
                File uploadFolder=new File(path);
                if (!uploadFolder.exists() && uploadFolder.mkdirs()){
                    System.out.println("created folder");
                }
                attachment.setHashId(hashids.encode(attachment.getId()));
                String pathlocal=String.format("/upload_files/%d/%d/%d/%s.%s",
                        1900+date.getYear(),
                        1+date.getMonth(),
                        date.getDate(),
                        attachment.getHashId(),
                        attachment.getExtension());
                attachment.setUploadFolder(pathlocal);
                attachmentRepository.save(attachment);
                String local=attachment.getHashId()+"."+attachment.getExtension();
                Path path1= Paths.get(path,local);
                Files.copy(file.getInputStream(), path1);
                return attachment;
            }
        }
        return new AttachmentEntity();
    }


    public AttachmentEntity downloadToServer(String hashid, HttpServletResponse response) throws IOException {
        Optional<AttachmentEntity> optionalAttachment = attachmentRepository.findByHashId(hashid);
        if (optionalAttachment.isPresent()){
            AttachmentEntity attachment = optionalAttachment.get();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=\""+ UriEncoder.encode(attachment.getOrginalName()));
            response.setContentType(attachment.getContentType());
            response.setContentLengthLong(attachment.getFileSize());
            FileInputStream fileInputStream=new FileInputStream(this.serverPath+attachment.getUploadFolder());
            FileCopyUtils.copy(fileInputStream,response.getOutputStream());
            return attachment;
        }
        return new AttachmentEntity();
    }


    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public ResponseEntity<InputStreamResource> getFile(String hashid, HttpServletResponse httpServletResponse) throws FileNotFoundException {
        Optional<AttachmentEntity> optionalAttachment = attachmentRepository.findByHashId(hashid);
        if (!optionalAttachment.isPresent()){
            return null;
        }
        AttachmentEntity attachment = optionalAttachment.get();
        Path path = Paths.get(this.serverPath,attachment.getUploadFolder());
        File file = new File(path.toFile().toString());
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOrginalName());
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");
        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);

    }


}
