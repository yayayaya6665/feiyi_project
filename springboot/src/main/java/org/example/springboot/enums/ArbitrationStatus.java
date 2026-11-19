package org.example.springboot.enums;

import lombok.Getter;

/**
 * 仲裁状态枚举
 * @author system
 */
@Getter
public enum ArbitrationStatus {
    /**
     * 申请仲裁
     */
    ARBITRATION(1, "申请仲裁"),
    /**
     * 仲裁成功 -支持用户
     */
    ARBITRATION_SUCCESS(2, "仲裁成功"),
    /**
     * 仲裁失败 -支持商家
     */
    ARBITRATION_FAIL(3, "仲裁失败"),
    /**
     * 协商解决
     */
    DISPUTE(4, "协商解决");

    private final Integer code;
    private final String description;

    ArbitrationStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static ArbitrationStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ArbitrationStatus status : ArbitrationStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态代码: " + code);
    }

    /**
     * 验证用户状态代码是否有效
     */
    public static boolean isValidCode(Integer code) {
        if (code == null) {
            return false;
        }
        for (ArbitrationStatus status : ArbitrationStatus.values()) {
            if (status.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
