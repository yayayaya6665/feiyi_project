package org.example.springboot.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发帖命令")
public class PostCreateCommandDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    @Schema(description = "帖子标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "图片URL列表(逗号分隔)")
    private String images;
}
