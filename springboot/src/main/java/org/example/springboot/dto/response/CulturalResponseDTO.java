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
@Schema(description = "合规与文化认证响应DTO")
public class CulturalResponseDTO {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "商品ID")
    private String productId;

    @Schema(description = "审核")
    private Integer culturalStatus;

    @Schema(description = "审核结果")
    private String outcome;

    @Schema(description = "描述说明")
    private String describe;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "商品信息")
    private ShopProductDetailResponseDTO shopProductDetailResponseDTO;
}

