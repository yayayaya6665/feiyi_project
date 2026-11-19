package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.CulturalCreateDTO;
import org.example.springboot.dto.command.MessageCreateDTO;
import org.example.springboot.dto.response.CulturalResponseDTO;
import org.example.springboot.dto.response.MessageResponseDTO;
import org.example.springboot.service.CulturalService;
import org.example.springboot.service.MessageService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description: 合规与文化认证信息表
 * @Version: 1.0
 */
@Tag(name = "合规与文化认证管理")
@RestController
@RequestMapping("/shop/order/cultural")
@Slf4j
public class CulturalController {
    @Resource
    private CulturalService culturalService;


    @Operation(summary = "用户创建信息")
    @PostMapping
    public Result<Integer> createCultural(@Valid @RequestBody CulturalCreateDTO  culturalCreateDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("创建合规与文化认证，用户ID: {}, 选择商品ID: {}", userId, culturalCreateDTO.getProductId());
        int evaluate = culturalService.createCultural(culturalCreateDTO, userId);
        return Result.success(evaluate);
    }
    @Operation(summary = "分页查询信息")
    @GetMapping("/page")
    public Result<Page<CulturalResponseDTO>> getCategoryPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "商品ID") @RequestParam(required = false) String productId) {

        log.info("分页查询信息列表");
        Page<CulturalResponseDTO> result = culturalService.getCategoryPage(current, size,productId);
        return Result.success(result);
    }

    @Operation(summary = "管理员审核信息")
    @PutMapping("/review")
    public Result<Integer> createReview( @RequestParam String id,@RequestParam Integer status) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null){
            return Result.error("无权限！");
        }
        int evaluate = culturalService.createReview(id, status);
        return Result.success(evaluate);
    }
    /**
     * 审核拒绝
     * @param
     * @return
     */
    @Operation(summary = "审核拒绝")
    @PutMapping("/refuse")
    public Result<Integer> refuse(@RequestParam String id,@RequestParam Integer status ,@RequestParam String outcome) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null){
            return Result.error("无权限！");
        }
        int evaluate = culturalService.refuse(id, status,outcome);
        return Result.success(evaluate);
    }
    /**
     * 查看详情
     * @param
     * @return
     */
    @Operation(summary = "查看详情")
    @GetMapping("/{id}")
    public Result<CulturalResponseDTO> getDetail(@PathVariable String id) {
        CulturalResponseDTO detail = culturalService.getDetail(id);
        return Result.success(detail);
    }
}
