package org.example.springboot.dto.response;

import lombok.Data;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description:
 * @Version: 1.0
 */
@Data
public class OrderArbitrationParams {
    /**
     * 仲裁内容
     */
    private String arbitrationContent;
    /**
     * 0:待处理 1:支持用户 2:支持商家
     */
    private Integer arbitration;
}
