package org.example.springboot.dto.response;

import lombok.Data;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description:
 * @Version: 1.0
 */
@Data
public class OrderParams {
    private String description;
    private String orderNo;
    private String requestType;
}
