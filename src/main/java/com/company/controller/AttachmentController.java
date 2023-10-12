package com.company.controller;

import com.company.entity.AttachmentEntity;
import com.company.repository.AttachmentRepository;
import com.company.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api")
public class AttachmentController {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${upload.server}")
    private String serverPath;


    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        AttachmentEntity attachmentEntity = fileStorageService.save(file);
        return ResponseEntity.ok(attachmentEntity);
    }

    @GetMapping("/view-file/{hashid}")
    public ResponseEntity<?> viewFile(@PathVariable String hashid) throws MalformedURLException {
           AttachmentEntity attachmentEntity=fileStorageService.viewFile(hashid);
           return ResponseEntity.ok()
                   .header(HttpHeaders.CONTENT_DISPOSITION, "inline; fileName=\""+ attachmentEntity.getOrginalName())
                   .contentType(MediaType.parseMediaType(attachmentEntity.getContentType()))
                   .contentLength(attachmentEntity.getFileSize())
                   .body(new FileUrlResource(String.format("%s/%s",this.serverPath,attachmentEntity.getUploadFolder())));
    }

   @GetMapping("/download/{hashid}")
    public ResponseEntity downloadFile(@PathVariable String hashid) throws MalformedURLException {
        AttachmentEntity attachmentEntity= fileStorageService.viewFile(hashid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=\""+ UriEncoder.encode(attachmentEntity.getOrginalName()))
                .contentType(MediaType.parseMediaType(attachmentEntity.getContentType()))
                .contentLength(attachmentEntity.getFileSize())
                .body(new FileUrlResource(String.format("%s/%s",this.serverPath,attachmentEntity.getUploadFolder())));
    }

    @DeleteMapping("/delete/{hashid}")
    public ResponseEntity deleteFile(@PathVariable String hashid){
        String file = fileStorageService.deleteFile(hashid);
        return ResponseEntity.ok(file);
    }


    // 2-usul file system

    @PostMapping("/addAttachment")
    public ResponseEntity<?> addAttach(MultipartHttpServletRequest request) throws IOException {
      AttachmentEntity attachmentEntity=fileStorageService.addAttachment(request);
        return ResponseEntity.ok(attachmentEntity);
    }

    @GetMapping("/downloadToServer/{hashid}")
    public  ResponseEntity<?> downloadToServer(@PathVariable String hashid, HttpServletResponse response) throws IOException {
        AttachmentEntity attachment= fileStorageService.downloadToServer(hashid, response);
        return ResponseEntity.ok(attachment);
    }

    @GetMapping("/viewFile/{hashId}")
    public  ResponseEntity<InputStreamResource> viewFile(@PathVariable String hashId, HttpServletResponse httpServletResponse) throws IOException {
        return fileStorageService.getFile(hashId, httpServletResponse);
    }

}
