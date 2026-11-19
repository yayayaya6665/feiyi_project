package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车实体类
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("shop_cart")
@Schema(description = "购物车实体类")
public class ShopCart {

    @TableId(type = IdType.INPUT)
    @Schema(description = "主键ID")
    private Long id;
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private String userId;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID")
    @NotBlank(message = "商品ID不能为空")
    @Size(max = 64, message = "商品ID长度不能超过64个字符")
    @TableField("product_id")
    private String productId;

    /**
     * 商品数量
     */
    @Schema(description = "商品数量")
    @NotNull(message = "商品数量不能为空")
    @TableField("product_num")
    private Integer productNum;

    /**
     * 价格快照（加入购物车时的商品价格）
     */
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

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

