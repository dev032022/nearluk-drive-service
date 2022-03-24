package com.nearluk.poc.controller;

import com.google.api.services.drive.model.File;
import com.nearluk.poc.drive.DriveManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api(tags = "Google Drive Upload Utility")
@Slf4j
@AllArgsConstructor
@RestController
public class FileController {

    private DriveManager driveManager;

    @ApiOperation(value = "Provide document/FileID to list the documents")
    @GetMapping({"/list"})
    public ResponseEntity<List<File>> listAll(@RequestParam(required = false) String parentId) throws Exception {
        List<File> files = driveManager.listAllByFolderID(parentId);
        return ResponseEntity.ok(files);
    }

    @ApiOperation(value = "Provide document/FileID to download from drive")
    @GetMapping("/download/{fileID}")
    public void download(@PathVariable String fileID, HttpServletResponse response) throws Exception {
        driveManager.downloadByFileID(fileID, response.getOutputStream());
    }

    @ApiOperation(value = "Provide document/FileID to delete from drive")
    @GetMapping("/delete/{fileID}")
    public void deleteByFileID(@PathVariable String fileID) throws Exception {
        driveManager.deleteByFileID(fileID);
    }

    @ApiOperation(value = "Upload Bulk files to Google Drive")
    @PostMapping(value = "/upload",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<File>> upload(@RequestParam("files") MultipartFile[] multipartFiles,
                                             @RequestParam(required = false) String path) throws Exception {
        List<File> fileList = driveManager.upload(multipartFiles, path);
        if (CollectionUtils.isEmpty(fileList)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(fileList);
    }
}
