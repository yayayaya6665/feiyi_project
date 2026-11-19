package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.OrderCreateCommandDTO;
import org.example.springboot.dto.response.OrderArbitrationParams;
import org.example.springboot.dto.response.OrderListResponseDTO;
import org.example.springboot.dto.response.OrderParams;
import org.example.springboot.dto.response.OrderResponseDTO;
import org.example.springboot.enums.OrderStatus;
import org.example.springboot.enums.UserType;
import org.example.springboot.service.OrderService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 * @author system
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/shop/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderResponseDTO> createOrder(@Valid @RequestBody OrderCreateCommandDTO dto) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("创建订单，用户ID: {}, 商品ID: {}", userId, dto.getProductId());
        OrderResponseDTO result = orderService.createOrder(dto, userId);
        return Result.success(result);
    }
    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<OrderResponseDTO> getOrderDetail(@Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String userType = JwtTokenUtils.getCurrentUserType();
        boolean isAdmin = UserType.ADMIN.getCode().equals(userType) || UserType.BUSINESS.getCode().equals(userType);

        
        log.info("获取订单详情，订单ID: {}, 用户ID: {}, 角色: {}", id, userId, userType);
        OrderResponseDTO result = orderService.getOrderDetail(id, userId, isAdmin);
        return Result.success(result);
    }

    @Operation(summary = "分页查询用户订单列表")
    @GetMapping("/page")
    public Result<Page<OrderListResponseDTO>> getUserOrderPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("分页查询用户订单，用户ID: {}, 页码: {}, 大小: {}, 状态: {}", userId, current, size, status);
        Page<OrderListResponseDTO> result = orderService.getUserOrderPage(current, size, status, userId,orderNo);
        return Result.success(result);
    }

    @Operation(summary = "分页查询管理员订单列表")
    @GetMapping("/admin/page")
    public Result<Page<OrderListResponseDTO>> getAdminOrderPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "是否仲裁") @RequestParam(required = false) Integer arbitration) {
        log.info("分页查询管理员订单，页码: {}, 大小: {}, 状态: {}, 订单号: {},是否仲裁:{}", current, size, status, orderNo, arbitration);
        Page<OrderListResponseDTO> result = orderService.getAdminOrderPage(current, size, status, orderNo , arbitration);
        return Result.success(result);
    }

    @Operation(summary = "支付订单")
    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "支付方式") @RequestParam String payType) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("支付订单，订单ID: {}, 用户ID: {}, 支付方式: {}", id, userId, payType);
        orderService.payOrder(id, userId, payType);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String userType = JwtTokenUtils.getCurrentUserType();
        boolean isAdmin = UserType.ADMIN.getCode().equals(userType) || UserType.BUSINESS.getCode().equals(userType);
        
        log.info("取消订单，订单ID: {}, 用户ID: {}, 是否管理员: {}", id, userId, isAdmin);
        orderService.cancelOrder(id, userId, isAdmin);
        return Result.success();
    }

    @Operation(summary = "发货（管理员）")
    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "物流单号") @RequestParam String logisticsNo) {
        log.info("订单发货，订单ID: {}, 物流单号: {}", id, logisticsNo);
        orderService.shipOrder(id, logisticsNo);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmOrder(@Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("确认收货，订单ID: {}, 用户ID: {}", id, userId);
        orderService.confirmOrder(id, userId);
        return Result.success();
    }
    @Operation(summary = "发起退款")
    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(@Parameter(description = "订单ID") @PathVariable Long id,@RequestBody OrderParams orderParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("发起退款，订单ID: {}, 用户ID: {}", id, userId);
        String description = orderParams.getDescription();
        orderService.refundOrder(id, userId,description);
        return Result.success();
    }
    @Operation(summary = "发起退货")
    @PutMapping("/{id}/return")
    public Result<Void> returnOrder(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody OrderParams orderParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String description = orderParams.getDescription();
        orderService.returnOrder(id, userId,description);
        return Result.success();
    }

    @Operation(summary = "同意退款")
    @PutMapping("/{id}/agree")
    public Result<Void> agreeOrder(@Parameter(description = "订单ID") @PathVariable Long id,@RequestBody OrderParams orderParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        orderService.agreeOrder(id,userId);
        return Result.success();
    }
    @Operation(summary = "拒绝退款")
    @PutMapping("/{id}/reject")
    public Result<Void> rejectOrder(@Parameter(description = "订单ID") @PathVariable Long id,@RequestBody OrderParams orderParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String description = orderParams.getDescription();
        orderService.rejectOrder(id,userId,description);
        return Result.success();
    }
    @Operation(summary = "取消退款")
    @PutMapping("/{id}/cancelRefund")
    public Result<Void> cancelRefund(@Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("取消退款，订单ID: {}, 用户ID: {}", id, userId);
        orderService.cancelRefund(id, userId);
        return Result.success();
    }
    @Operation(summary = "取消换货")
    @PutMapping("/{id}/cancelReturn")
    public Result<Void> cancelReturn(@Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("取消换货，订单ID: {}, 用户ID: {}", id, userId);
        orderService.cancelReturn(id, userId);
        return Result.success();
    }
    @Operation(summary = "同意换货")
    @PutMapping("/{id}/completeReturn")
    public Result<Void> completeReturnOrder(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody OrderParams orderParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("同意换货，订单ID: {}, 用户ID: {}", id, userId);
        orderService.ReturnOrder(id, userId, OrderStatus.PAID.getCode());
        return Result.success();
    }
    @Operation(summary = "拒绝换货")
    @PutMapping("/{id}/rejectReturn")
    public Result<Void> rejectReturnOrder(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody OrderParams orderParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        String description = orderParams.getDescription();
        log.info("拒绝换货，订单ID: {}, 用户ID: {}, 描述: {}", id, userId, description);
        orderService.rejectReturnOrder(id, userId, description);
        return Result.success();
    }
    /**
     * 申请仲裁
    */
    @Operation(summary = "申请仲裁")
    @PutMapping("/{id}/arbitrate")
    public Result<Void> arbitrateOrder(@Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("申请仲裁，订单ID: {}, 用户ID: {}", id, userId);
        orderService.arbitrateOrder(id, userId);
        return Result.success();
    }
    @Operation(summary = "处理仲裁")
    @PutMapping("/{id}/handleArbitrate")
    public Result<Void> handleArbitrateOrder(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody OrderArbitrationParams orderArbitrationParams) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("处理仲裁，订单ID: {}, 用户ID: {}", id, userId);
        orderService.handleArbitrateOrder(id, userId, orderArbitrationParams);
        return Result.success();
    }

}

