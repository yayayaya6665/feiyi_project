package org.example.springboot.dto.response;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
import java.util.List;

/**
 * 购物车响应DTO
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车响应DTO")
public class CartResponseDTO {

    @Schema(description = "ID")
    private Long id;
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String userId;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID")
    private String productId;

    /**
     * 商品数量
     */
    @Schema(description = "商品数量")
    private Integer productNum;

    /**
     * 价格快照（加入购物车时的商品价格）
     */
    @Schema(description = "价格快照")
    private BigDecimal productPrice;

    /**
     * 是否选中：0未选 1选中
     */
    @Schema(description = "是否选中：0未选 1选中（结算时默认选中")
    private Integer checked;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;


}

