package org.example.springboot.dto.command;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建购物车命令DTO
 * @author system
 */
@Data
@Schema(description = "创建购物车命令DTO")
public class CartCreateCommandDTO {
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private String userId;

    @Schema(description = "商品ID", example = "SP-2026-001")
    @NotBlank(message = "商品ID不能为空")
    @Size(max = 50, message = "商品ID长度不能超过50个字符")
    private String productId;

    @Schema(description = "商品数量", example = "1")
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量至少为1")
    private Integer productNum;

    //商品价格
    @Schema(description = "价格快照")
    @NotNull(message = "价格快照不能为空")
    @DecimalMin(value = "0.00", message = "价格快照不能小于0")
    @TableField("product_price")
    private BigDecimal productPrice;

    /**
     * 是否选中：0未选 1选中
     */
    @Schema(description = "是否选中：0未选 1选中（结算时默认选中")
    private Integer checked;
}

