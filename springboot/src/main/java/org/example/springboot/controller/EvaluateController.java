package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.EvaluateCreateDTO;
import org.example.springboot.dto.command.OrderCreateCommandDTO;
import org.example.springboot.dto.response.EvaluateResponseDTO;
import org.example.springboot.dto.response.OrderResponseDTO;
import org.example.springboot.dto.response.ShopCategoryResponseDTO;
import org.example.springboot.service.EvaluateService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description: 评价信息表
 * @Version: 1.0
 */
@Tag(name = "评价管理")
@RestController
@RequestMapping("/shop/order/evaluate")
@Slf4j
public class EvaluateController {
    @Resource
    private EvaluateService evaluateService;

    @Operation(summary = "创建评价")
    @PostMapping
    public Result<Integer> createOrder(@Valid @RequestBody EvaluateCreateDTO evaluateCreateDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("创建评价信息，用户ID: {}, 商品ID: {}", userId, evaluateCreateDTO.getProductId());
        int evaluate = evaluateService.createEvaluate(evaluateCreateDTO, userId);
        return Result.success(evaluate);
    }
    @Operation(summary = "分页查询评价")
    @GetMapping("/page")
    public Result<Page<EvaluateResponseDTO>> getCategoryPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "商品id") @RequestParam(required = false) String productId) {

        log.info("分页查询商品分类列表");
        Page<EvaluateResponseDTO> result = evaluateService.getCategoryPage(current, size,productId);
        return Result.success(result);
    }
    @Operation(summary = "删除评价")
    @DeleteMapping("/{evaluateId}")
    public Result<Void> deleteCategory(@PathVariable Long evaluateId) {
        log.info("删除评价，ID: {}", evaluateId);
        evaluateService.deleteEvaluate(evaluateId);
        return Result.success();
    }
    @Operation(summary = "分页查询评价（管理员）")
    @GetMapping("/page/list")
    public Result<Page<EvaluateResponseDTO>> getEvaluatePage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false)  String rating,
            @RequestParam(required = false)  String orderNo) {
        log.info("分页查询商品分类列表");
        Page<EvaluateResponseDTO> result = evaluateService.getEvaluatePage(current, size,rating,orderNo);
        return Result.success(result);
    }
    /**
     * 获取评价详情
     * @param evaluateId
     * @return
     */
    @Operation(summary = "获取评价详情")
    @GetMapping("/{evaluateId}")
    public Result<EvaluateResponseDTO> getDetail(@PathVariable Long evaluateId) {
        log.info("获取评价详情，ID: {}", evaluateId);
        EvaluateResponseDTO result = evaluateService.getDetail(evaluateId);
        return Result.success(result);
    }
}
