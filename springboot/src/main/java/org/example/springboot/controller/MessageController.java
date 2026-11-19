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
import org.example.springboot.dto.command.MessageCreateDTO;
import org.example.springboot.dto.response.EvaluateResponseDTO;
import org.example.springboot.dto.response.MessageResponseDTO;
import org.example.springboot.service.EvaluateService;
import org.example.springboot.service.MessageService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description: 评价信息表
 * @Version: 1.0
 */
@Tag(name = "留言管理")
@RestController
@RequestMapping("/shop/order/message")
@Slf4j
public class MessageController {
    @Resource
    private MessageService messageService;


    @Operation(summary = "用户创建信息")
    @PostMapping
    public Result<Integer> createOrder(@Valid @RequestBody MessageCreateDTO messageCreateDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        log.info("创建留言信息，用户ID: {}, 商品ID: {}", userId, messageCreateDTO.getProductId());
        int evaluate = messageService.createMessage(messageCreateDTO, userId);
        return Result.success(evaluate);
    }
    @Operation(summary = "分页查询信息")
    @GetMapping("/page")
    public Result<Page<MessageResponseDTO>> getCategoryPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "商品ID") @RequestParam(required = false) String productId) {

        log.info("分页查询信息列表");
        Page<MessageResponseDTO> result = messageService.getCategoryPage(current, size,productId);
        return Result.success(result);
    }

    @Operation(summary = "商家创建信息")
    @PutMapping("/add")
    public Result<Integer> createMessas( @RequestBody MessageCreateDTO messageCreateDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        int evaluate = messageService.addMessage(messageCreateDTO, userId);
        return Result.success(evaluate);
    }
}
