package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.CartCreateCommandDTO;
import org.example.springboot.dto.command.OrderCreateCommandDTO;
import org.example.springboot.dto.response.CartListResponseDTO;
import org.example.springboot.dto.response.CartResponseDTO;
import org.example.springboot.dto.response.OrderListResponseDTO;
import org.example.springboot.dto.response.OrderResponseDTO;
import org.example.springboot.enums.UserType;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.service.CartService;
import org.example.springboot.service.OrderService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车控制器
 * @author system
 */
@Tag(name = "购物车管理")
@RestController
@RequestMapping("/shop/cart")
@Slf4j
public class CartController {

    @Resource
    private OrderService orderService;
    @Resource
    private CartService cartService;

    @Operation(summary = "新增购物车")
    @PostMapping
    public Result<CartResponseDTO> createCart(@Valid @RequestBody CartCreateCommandDTO cartCreateCommandDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("新增购物车，用户ID: {}, 商品ID: {}", userId, cartCreateCommandDTO.getProductId());
        CartResponseDTO result = cartService.createCart(cartCreateCommandDTO, userId);
        return Result.success(result);
    }


    @Operation(summary = "分页查询用户购物车列表")
    @GetMapping("/page")
    public Result<Page<CartListResponseDTO>> getUserCartPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("分页查询用户订单，用户ID: {}, 页码: {}, 大小: {}", userId, current, size);
        Page<CartListResponseDTO> result = cartService.getUserCartPage(current, size, userId);
        return Result.success(result);
    }

    @Operation(summary = "取消购物车信息")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@Parameter(description = "购物车ID") @PathVariable Long id) {
        log.info("取消订单，订单ID: {}", id);
        cartService.cancelCart(id);
        return Result.success();
    }
    @Operation(summary = "添加商品数量")
    @PutMapping("/{id}/quantity")
    public Result<Void> updateCart(@Parameter(description = "购物车ID") @PathVariable Long id ,@Parameter(description = "数量") @RequestParam Integer quantity) {
        if (quantity < 1){
           throw  new BusinessException("4001","数量不能小于1");
        }
        cartService.updateCart(id, quantity);
        return Result.success();
    }
    //购物车结算
    @Operation(summary = "购物车结算")
    @PostMapping("/settlement")
    public Result<OrderResponseDTO> settlement(@Valid @RequestBody OrderCreateCommandDTO orderCreateCommandDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        return null;
    }
    //取消选中
    @Operation(summary = "取消选中")
    @PutMapping("/{id}/checked")
    public Result<Void> cancelChecked(@Parameter(description = "购物车ID") @PathVariable Long id ,@Parameter(description = "取消选中") @RequestParam Integer checked) {
        cartService.cancelChecked(id, checked);
        return Result.success();
    }
}

