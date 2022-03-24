package com.nearluk.poc.drive;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class DriveManager {

    private Drive drive;

    public List<File> listAll() throws Exception {

        FileList result = drive.files().list().setPageSize(10)
                .setFields("nextPageToken, files(id, name)").execute();

        return result.getFiles();
    }

    public List<File> listAllByFolderID(String folderID) throws Exception {

        if (folderID == null) {
            folderID = "root";
        }

        String query = "'" + folderID + "' in parents";

        FileList result = drive.files().list().setQ(query).setPageSize(10)
                .setFields("nextPageToken, files(id, name)").execute();


        log.info("Number of files found in ({}) is ", folderID, result.size());

        return result.getFiles();
    }

    public void downloadByFileID(String fileID, OutputStream outputStream) throws Exception {
        if (fileID != null) {
            drive.files().get(fileID).executeMediaAndDownloadTo(outputStream);
        }
    }

    public void deleteByFileID(String fileID) throws Exception {
        drive.files().delete(fileID).execute();
        log.info("{} document deleted successfully.", fileID);
    }

    public List<File> upload(MultipartFile[] multipartFiles, String filePath) throws Exception {
        String folderID = getFolderID(filePath);
        try {
            for (MultipartFile multipartFile : multipartFiles) {
                File fileMetadata = new File();
                fileMetadata.setParents(Collections.singletonList(folderID));
                fileMetadata.setName(multipartFile.getOriginalFilename());
                drive.files()
                        .create(fileMetadata,
                                new InputStreamContent(multipartFile.getContentType(),
                                        new ByteArrayInputStream(multipartFile.getBytes())))
                        .setFields("id").execute();
                log.info("{} uploaded successfully.", multipartFile.getOriginalFilename());
            }
        } catch (Exception e) {
            log.error("Error during the upload process ", e);
        }
        return listAllByFolderID(folderID);
    }

    public String getFolderID(String path) throws Exception {
        String parentId = null;
        String[] folderNames = path.split("/");

        for (String name : folderNames) {
            parentId = findOrCreateFolder(parentId, name);
        }
        return parentId;
    }

    private String findOrCreateFolder(String parentID, String folderName) throws Exception {
        String folderID = searchFolderId(parentID, folderName);
        if (folderID != null) {
            return folderID;
        }

        File fileMetadata = new File();
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setName(folderName);

        if (parentID != null) {
            fileMetadata.setParents(Collections.singletonList(parentID));
        }
        return drive.files().create(fileMetadata).setFields("id").execute().getId();
    }

    private String searchFolderId(String parentID, String folderName) throws Exception {
        String folderID = null;
        String pageToken = null;
        FileList result = null;

        File fileMetadata = new File();
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setName(folderName);

        do {
            String query = " mimeType = 'application/vnd.google-apps.folder' ";
            if (parentID == null) {
                query = query + " and 'root' in parents";
            } else {
                query = query + " and '" + parentID + "' in parents";
            }
            result = drive.files().list().setQ(query).setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)").setPageToken(pageToken).execute();

            for (File file : result.getFiles()) {
                if (file.getName().equalsIgnoreCase(folderName)) {
                    folderID = file.getId();
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null && folderID == null);

        return folderID;
    }
}
