package org.example.springboot.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *通知公告创建DTO
 * @author system
 */
@Data
@Schema(description = "通知公告创建DTO")
public class NoticeCreateDTO {
    @Schema(description = "id")
    private String id;
    @Schema(description = "是否置顶")
    private Integer noticeStatus;

    @Schema(description = "公告内容")
    private String noticeContent;

}
