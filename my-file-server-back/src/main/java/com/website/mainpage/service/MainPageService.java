package com.website.mainpage.service;

import com.website.common.Tool;
import com.website.forum.repository.CommentRepository;
import com.website.forum.repository.ForumRepository;
import com.website.mainpage.dto.UserPageDTO;
import com.website.mainpage.entity.FileEntity;
import com.website.mainpage.entity.FolderEntity;
import com.website.mainpage.entity.MainUserEntity;
import com.website.mainpage.repository.FileRepository;
import com.website.mainpage.repository.FolderRepository;
import com.website.mainpage.repository.MainUserRepository;
import com.website.security.dto.CustomUserDetails;
import com.website.security.entity.User;
import com.website.security.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MainPageService {
    @Value("${file.download-url}")
    private String downloadUrl;
    private final Tool tool;
    private final UserRepository userRepository;
    private final MainUserRepository mainUserRepository;
    private final FileRepository fileRepository;
    private final ForumRepository forumRepository;
    private final CommentRepository commentRepository;
    private final FolderRepository folderRepository;
    public MainPageService(Tool tool, UserRepository userRepository, MainUserRepository mainUserRepository, FileRepository fileRepository, ForumRepository forumRepository, CommentRepository commentRepository, FolderRepository folderRepository) {
        this.tool = tool;
        this.userRepository = userRepository;
        this.mainUserRepository = mainUserRepository;
        this.fileRepository = fileRepository;
        this.forumRepository = forumRepository;
        this.commentRepository = commentRepository;
        this.folderRepository = folderRepository;
    }
    @Transactional
    public String uploadFile(MultipartFile file, String description, CustomUserDetails user, boolean isPrivate) {
        try{
            FileEntity fileEntity =  new FileEntity();
            fileEntity.setChangedName(tool.upload(file));
            fileEntity.setUploadedAt(LocalDateTime.now());
            fileEntity.setDescription(description);
            fileEntity.setPrivate(isPrivate);
            fileEntity.setSize(file.getSize());
            fileEntity.setFileFullPath(downloadUrl+fileEntity.getChangedName());
            fileEntity.setUploadedByUser(mainUserRepository.findById(user.getUserCode()).orElseThrow());
            fileEntity.setDownload_count(0);
            if(file.getOriginalFilename() != null){
                fileEntity.setOriginalName(StringUtils.cleanPath(file.getOriginalFilename()));
            } else {
                fileEntity.setOriginalName("파일");
            }
            fileRepository.save(fileEntity);
            return "업로드 성공";
        } catch (Exception e){
            return e.getMessage();
        }
    }
    public Page<FileEntity> getPublicFiles(int page) {
        Pageable pageable = PageRequest.of(page,10,Sort.by("uploadedAt").descending());
        return fileRepository.getPublicFile(pageable);
    }
    public Page<FileEntity> getMyFile(CustomUserDetails user, int page) {
        Pageable pageable = PageRequest.of(page,10,  Sort.by("uploadedAt").descending());
        return fileRepository.getMyFile(user.getUserCode(),pageable);
    }
    @Transactional
    public void increaseDownloadCount(Long fileCode) {
        FileEntity fileEntity = fileRepository.findById(fileCode).orElseThrow();
        fileEntity.setDownload_count(fileEntity.getDownload_count() + 1);
    }

    public boolean deleteFile(Long fileCode) {
        FileEntity fileEntity = fileRepository.findById(fileCode).orElseThrow();
        try{
            if(!tool.deleteFile(fileEntity.getChangedName())){
                return false;
            }
            fileRepository.deleteById(fileEntity.getFileCode());
            return true;
        } catch (Exception e){
            return false;
        }

    }

    public MainUserEntity getUser(CustomUserDetails user) {
        return mainUserRepository.findById(user.getUserCode()).orElseThrow();
    }

    public UserPageDTO getOtherUser(Long userCode) {
        MainUserEntity user = mainUserRepository.findById(userCode).orElseThrow();
        int writtenPostCount = forumRepository.getUserWrittenPostCount(userCode);
        int writtenCommentCount = commentRepository.getUserWrittenCommentCount(userCode);
        int uploadCount = fileRepository.getUserFileUploadCount(userCode);
        return new UserPageDTO(
                user.getUserCode(),
                user.getId(),
                writtenPostCount,
                writtenCommentCount,
                uploadCount,
                "/icon.png"
        );
    }

    @Transactional
    public void modifyUser(UserPageDTO user) {
        User userEntity = userRepository.findByUserCode(user.getUserCode());
        userEntity.setId(user.getUserId());
        userRepository.save(userEntity);
    }

    @Transactional
    public Long getUserRootFolder(Long userCode) {
        Long userRootFolderCode = folderRepository.getUserRootFolderCode(userCode);
        if(userRootFolderCode==null){   //처음에 루트 폴더 만들어주기
            FolderEntity newFolderEntity = new FolderEntity();
            newFolderEntity.setUser(userCode);
            newFolderEntity.setFolderName("rootFolder"+userCode);
            folderRepository.save(newFolderEntity);
            userRootFolderCode = folderRepository.getUserRootFolderCode(userCode);
        }
        return userRootFolderCode;
    }

    public FolderEntity getFileInFolder(Long folderCode, Long userCode) {
        try{
            FolderEntity files =  folderRepository.getFileInFolder(folderCode, userCode);
            //로직 비효율적임 고치자 나중에
            List<FolderEntity> folders = folderRepository.getFolderInFolder(folderCode,userCode);
            for(FolderEntity f : folders){
                f.setFiles(null);
            }
            files.setFolders(folders);
            return files;
        } catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }
}
