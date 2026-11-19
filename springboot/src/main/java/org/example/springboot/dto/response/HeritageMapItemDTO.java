package org.example.springboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 非遗地图省份条目DTO
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "非遗地图条目响应")
public class HeritageMapItemDTO {

    @Schema(description = "作品ID")
    private String id;

    @Schema(description = "非遗项目名称")
    private String title;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "简介")
    private String summary;

    @Schema(description = "封面图片URL")
    private String coverUrl;
}
