package org.example.springboot.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *商家认证表创建DTO
 * @author system
 */
@Data
@Schema(description = "商家认证表创建DTO")
public class CertificationCreateDTO {
    @Schema(description = "id")
    private String id;
    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "商家认证名称")
    private String name;

    @Schema(description = "认证状态")
    private Integer certification;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "拒绝原因")
    private String content;

}
