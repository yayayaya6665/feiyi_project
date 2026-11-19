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
 * @Description: 通知公告表
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("shop_order_notice")
@Schema(description = "通知公告表")
public class Notice {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "分类ID")
    private String id;

    @Schema(description = "是否置顶")
    @NotBlank(message = "是否置顶不能为空")
    @TableField("notice_status")
    private Integer noticeStatus;

    @Schema(description = "公告内容")
    @NotNull(message = "公告内容不能为空")
    @TableField("notice_content")
    private String noticeContent;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
