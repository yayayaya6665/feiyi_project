package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.CertificationCreateDTO;
import org.example.springboot.dto.command.CertificationDTO;
import org.example.springboot.dto.response.CertificationResponseDTO;
import org.example.springboot.service.CertificationService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description: 商家认证表表
 * @Version: 1.0
 */
@Tag(name = "商家认证表管理")
@RestController
@RequestMapping("/user/certification")
@Slf4j
public class CertificationController {
    @Resource
    private CertificationService certificationService;


    @Operation(summary = "商家申请信息")
    @PostMapping
    public Result<Integer> createOrder(@Valid @RequestBody CertificationCreateDTO certificationCreateDTO) {
        int evaluate = certificationService.createOrder(certificationCreateDTO);
        return Result.success(evaluate);
    }
    @Operation(summary = "分页查询信息")
    @GetMapping("/page")
    public Result<Page<CertificationResponseDTO>> getPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "认证名称") @RequestParam(required = false) String name,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone) {

        log.info("分页查询信息列表");
        Page<CertificationResponseDTO> result = certificationService.getPage(current, size,name, phone);
        return Result.success(result);
    }

    @Operation(summary = "管理员审核信息")
    @PutMapping("/review/{id}")
    public Result<Integer> review(@Parameter(description = "ID")@PathVariable String id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        int evaluate = certificationService.review(id, userId);
        return Result.success(evaluate);
    }

    @Operation(summary = "管理员审核拒绝信息")
    @PutMapping("/refuse/{id}")
    public Result<Integer> refuse(@Parameter(description = "ID")@PathVariable String id , CertificationDTO certificationDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        int evaluate = certificationService.refuse(id, userId,certificationDTO.getContent());
        return Result.success(evaluate);
    }
    /**
     * 获取查询信息
     * @param id
     * @return
     */
    @Operation(summary = "获取查询信息")
    @GetMapping("/{id}")
    public Result<CertificationResponseDTO> getCertification( @Parameter(description = "用户ID") @PathVariable String id){
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null){
            return Result.error("请先登录");
        }
        CertificationResponseDTO result = certificationService.getCertification(id, userId);
        return Result.success(result);
    }
}
