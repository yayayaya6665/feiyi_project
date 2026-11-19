package org.example.springboot.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *留言创建DTO
 * @author system
 */
@Data
@Schema(description = "留言创建DTO")
public class CulturalCreateDTO {
    @Schema(description = "描述说明")
    private String describe;

    @Schema(description = "商品ID")
    private String productId;

}
