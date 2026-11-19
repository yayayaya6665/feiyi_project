package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description: 合规与文化认证实体类
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("shop_cultural")
@Schema(description = "合规与文化认证实体类")
public class Cultural {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;


    @Schema(description = "商品ID")
    @NotBlank(message = "商品ID不能为空")
    @Size(max = 64, message = "商品ID长度不能超过64个字符")
    @TableField("product_id")
    private String productId;


    @Schema(description = "审核")
    @NotBlank(message = "审核不能为空")
    @TableField("cultural_status")
    private Integer culturalStatus;

    @Schema(description = "审核结果")
    @TableField("outcome")
    private String outcome;

    @Schema(description = "描述说明")
    @TableField("`describe`")
    private String describe;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
