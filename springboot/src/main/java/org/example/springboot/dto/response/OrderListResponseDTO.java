package org.example.springboot.dto.response;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表响应DTO
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单列表响应DTO")
public class OrderListResponseDTO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品id")
    private String productId;
    @Schema(description = "是否仲裁")
    private Integer arbitration;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "订单状态码")
    private Integer status;

    @Schema(description = "订单状态名称")
    private String statusName;

    @Schema(description = "商品数量")
    private Integer itemCount;

    @Schema(description = "主商品标题")
    private String mainProductTitle;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "申请内容")
    private String content;
    @Schema(description = "仲裁内容")
    private String arbitrationContent;

}

