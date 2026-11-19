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
 * @Description: 留言实体类
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("shop_order_message")
@Schema(description = "留言实体类")
public class Message {
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID")
    private Long id;


    @Schema(description = "商品ID")
    @NotBlank(message = "商品ID不能为空")
    @Size(max = 64, message = "商品ID长度不能超过64个字符")
    @TableField("product_id")
    private String productId;

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "商家回复信息（可为空，用户留言后由商家填写）")
    @TableField("message")
    private String message;

    @Schema(description = "用户留言信息")
    @TableField("remark")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
