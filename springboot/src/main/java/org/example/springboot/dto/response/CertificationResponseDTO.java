package org.example.springboot.dto.response;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.springboot.dto.FileInfoDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 响应DTO
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "响应DTO")
public class CertificationResponseDTO {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商家认证名称")
    private String name;

    @Schema(description = "认证状态")
    private Integer certification;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "拒绝原因")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "用户信息")
    private UserDetailResponseDTO userDetailResponseDTO;
    @Schema(description = "用户图片信息")
    private List<String> userFilePath;
    @Schema(description = "商家图片信息")
    private List<FileInfoDTO> fileInfoDTOS;
}

