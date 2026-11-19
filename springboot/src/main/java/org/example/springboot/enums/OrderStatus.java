package org.example.springboot.enums;

/**
 * 订单状态枚举
 * @author system
 */
public enum OrderStatus {
    
    /**
     * 待支付
     */
    PENDING(0, "待支付"),
    
    /**
     * 已支付
     */
    PAID(1, "已支付"),
    
    /**
     * 已发货
     */
    SHIPPED(2, "已发货"),
    
    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),
    
    /**
     * 已关闭
     */
    CLOSED(4, "已关闭"),
    /**
     * 申请退款
     */
    REFUNDING(5, "申请退款"),
    /**
     * 退款成功
     */
    REFUND_SUCCESS(6, "退款成功"),
    /**
     * 退款失败
     */
    REFUND_FAIL(7, "退款失败"),
    /**
     * 申请换货
     */
    EXCHANGE_APPLY(8, "申请换货"),
    /**
     * 换货成功
     */
    EXCHANGE_SUCCESS(9, "换货成功"),
    /**
     * 换货失败
     */
    EXCHANGE_FAIL(10, "换货失败"),
//    /**
//     * 待评价
//     */
//    EVALUATE(3, "待评价"),
    /**
     * 已评价
     */
    EVALUATED(12, "已评价"),
    /**
     * 我的仲裁
     */
    ARBITRATION(13, "申请仲裁");



    private final Integer code;
    private final String description;

    OrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : OrderStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 验证状态流转是否合法
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        
        switch (this) {
            case PENDING:
                // 待支付 -> 已支付/已关闭
                return target == PAID || target == CLOSED;
            case PAID:
                // 已支付 -> 已发货/已关闭
                return target == SHIPPED || target == CLOSED;
            case SHIPPED:
                // 已发货 -> 已完成
                return target == COMPLETED;
            case COMPLETED:
            case CLOSED:
                // 已完成/已关闭 不能再流转
                return false;
            default:
                return false;
        }
    }

    /**
     * 获取状态描述
     */
    public static String getDescription(Integer code) {
        OrderStatus status = fromCode(code);
        return status != null ? status.description : "未知";
    }
}

