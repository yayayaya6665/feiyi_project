package org.example.springboot.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价创建DTO
 * @author system
 */
@Data
@Schema(description = "评价创建DTO")
public class EvaluateCreateDTO {
    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品ID")
    private String productId;

    @Schema(description = "商品评分")
    private String rating;

    @Schema(description = "评价信息")
    private String remark;

}
