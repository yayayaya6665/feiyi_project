package org.example.springboot.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 *留言创建DTO
 * @author system
 */
@Data
@Schema(description = "留言创建DTO")
public class MessageCreateDTO {
    @Schema(description = "id")
    private Long id;
    @Schema(description = "商品ID")
    private String productId;

    @Schema(description = "商家回复信息")
    @Size(max = 1024, message = "回复长度不能超过1024个字符")
    private String message;

    @Schema(description = "用户留言信息")
    @Size(max = 1024, message = "留言长度不能超过1024个字符")
    private String remark;

}
