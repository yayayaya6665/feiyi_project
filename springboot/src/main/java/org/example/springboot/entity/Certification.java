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
 * @Description: 商家认证表实体类
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_certification")
@Schema(description = "商家认证表实体类")
public class Certification {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "ID")
    private String id;


    @Schema(description = "用户ID")
    @NotBlank(message = "用户ID不能为空")
    @Size(max = 64, message = "用户ID长度不能超过64个字符")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "商家认证名称")
    @NotNull(message = "商家认证名称不能为空")
    @TableField("name")
    private String name;

    @Schema(description = "认证状态")
    @TableField("certification")
    private Integer certification;

    @Schema(description = "手机号留言信息")
    @NotBlank(message = "手机号信息不能为空")
    @TableField("phone")
    private String phone;

    @Schema(description = "拒绝原因")
    @TableField("content")
    private String content;

    @Schema(description = "创建时间")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
