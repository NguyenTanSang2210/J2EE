package nhom2.NguyenTanSang.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý upload và quản lý file ảnh sách
 * Hỗ trợ upload vào thư mục static/images/books/
 */
@Service
@Slf4j
public class FileUploadService {
    
    // Thư mục upload ảnh (relative to project root)
    private static final String UPLOAD_DIR = "src/main/resources/static/images/books/";
    
    // Các định dạng file được hỗ trợ
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif"
    );
    
    // Kích thước file tối đa (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    /**
     * Upload ảnh sách và trả về URL path
     * 
     * @param file MultipartFile cần upload
     * @return String URL path của ảnh (VD: /images/books/abc123.jpg)
     * @throws IOException nếu có lỗi upload
     * @throws IllegalArgumentException nếu file không hợp lệ
     */
    public String uploadBookImage(MultipartFile file) throws IOException {
        // Validate file
        validateImageFile(file);
        
        // Tạo thư mục nếu chưa có
        createUploadDirectoryIfNotExists();
        
        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // Đường dẫn lưu file
        Path uploadPath = Paths.get(UPLOAD_DIR + uniqueFilename);
        
        // Lưu file vào filesystem
        Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("📁 Successfully uploaded image: {}", uniqueFilename);
        
        // Trả về URL path cho web (relative từ static folder)
        return "/images/books/" + uniqueFilename;
    }
    
    /**
     * Xóa ảnh từ filesystem (chỉ xóa ảnh upload local, không xóa external URL)
     * 
     * @param imageUrl URL của ảnh cần xóa
     * @return true nếu xóa thành công, false nếu không xóa được hoặc là external URL
     */
    public boolean deleteBookImage(String imageUrl) {
        // Không xóa nếu là external URL (http/https) hoặc default image
        if (imageUrl == null || imageUrl.isEmpty() || 
            imageUrl.startsWith("http") || 
            imageUrl.contains("default-book.jpg") || 
            imageUrl.contains("default-book.svg")) {
            log.debug("❌ Skipping delete for external URL or default image: {}", imageUrl);
            return false;
        }
        
        try {
            // Extract filename from URL path (VD: /images/books/abc123.jpg → abc123.jpg)
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("🗑️ Successfully deleted image: {}", filename);
                return true;
            } else {
                log.warn("⚠️ Image file not found for deletion: {}", filename);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error deleting image: {}", imageUrl, e);
            return false;
        }
    }
    
    /**
     * Validate file upload
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file quá lớn. Tối đa 5MB");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Định dạng file không hỗ trợ. Chỉ chấp nhận: jpg, jpeg, png, gif");
        }
        
        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !isValidImageExtension(filename)) {
            throw new IllegalArgumentException("Phần mở rộng file không hỗ trợ. Chỉ chấp nhận: jpg, jpeg, png, gif");
        }
    }
    
    /**
     * Kiểm tra extension file có hợp lệ không
     */
    private boolean isValidImageExtension(String filename) {
        String extension = getFileExtension(filename);
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * Lấy extension từ filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
    
    /**
     * Tạo thư mục upload nếu chưa có
     */
    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("📁 Created upload directory: {}", UPLOAD_DIR);
        }
    }
    
    /**
     * Kiểm tra xem imageUrl có phải là external URL không
     */
    public boolean isExternalUrl(String imageUrl) {
        return imageUrl != null && 
               (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"));
    }
}