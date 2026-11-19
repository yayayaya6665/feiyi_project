package org.example.springboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 响应DTO
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "响应DTO")
public class MessageResponseDTO {
    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "商品ID")
    private String productId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "留言用户显示名")
    private String userName;

    @Schema(description = "留言用户头像")
    private String userAvatar;

    @Schema(description = "商家回复信息")
    private String message;

    @Schema(description = "用户留言信息")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "商品信息")
    private ShopProductDetailResponseDTO shopProductDetailResponseDTO;
}

