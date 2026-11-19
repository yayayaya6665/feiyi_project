package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.NoticeCreateDTO;
import org.example.springboot.dto.response.NoticeResponseDTO;
import org.example.springboot.service.NoticeService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-19
 * @Description: 通知公告表
 * @Version: 1.0
 */
@Tag(name = "留言管理")
@RestController
@RequestMapping("/notice")
@Slf4j
public class NoticeController {
    @Resource
    private NoticeService noticeService;


    @Operation(summary = "创建公告信息")
    @PostMapping
    public Result<Integer> createNotice(@Valid @RequestBody NoticeCreateDTO noticeCreateDTO) {
        int evaluate = noticeService.createNotice(noticeCreateDTO);
        return Result.success(evaluate);
    }
    @Operation(summary = "分页查询信息")
    @GetMapping("/page")
    public Result<Page<NoticeResponseDTO>> getCategoryPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "是否置顶")  Integer noticeStatus,
            @Parameter(description = "公告内容") String noticeContent) {

        log.info("分页查询信息列表");
        Page<NoticeResponseDTO> result = noticeService.getCategoryPage(current, size, noticeStatus,noticeContent);
        return Result.success(result);
    }

    @Operation(summary = "是否置顶")
    @PutMapping("/{id}/noticeStatus")
    public Result<Integer> noticeStatus( @PathVariable String id,Integer noticeStatus) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null){
            return Result.error("请先登录");
        }
        int evaluate = noticeService.noticeStatus(id ,userId,noticeStatus);
        return Result.success(evaluate);
    }
    /**
     * 删除信息
     */
    @Operation(summary = "删除信息")
    @DeleteMapping("/{id}")
    public Result<Integer> delete(@PathVariable String id) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null){
            return Result.error("请先登录");
        }
        int evaluate = noticeService.delete(id,userId);
        return Result.success(evaluate);
    }
    /**
     * 根据ID查看信息
     */
    @Operation(summary = "根据ID查看信息")
    @GetMapping("/{id}")
    public Result<NoticeResponseDTO> getById(@PathVariable String id) {
        NoticeResponseDTO evaluate = noticeService.getById(id);
        return Result.success(evaluate);
    }
    /**
     * 修改信息
     */
    @Operation(summary = "修改信息")
    @PutMapping("/{id}")
    public Result<Integer> update(@PathVariable String id, @Valid @RequestBody NoticeCreateDTO noticeCreateDTO) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null){
            return Result.error("请先登录");
        }
        int evaluate = noticeService.update(id,userId,noticeCreateDTO);
        return Result.success( evaluate);
    }
    /**
     * 获取所有公告信息
     * @return
     */
    @Operation(summary = "获取所有公告信息")
    @GetMapping("/all")
    public Result<List<NoticeResponseDTO>> getAll() {
        List<NoticeResponseDTO> result = noticeService.getAll();
        return Result.success(result);
    }

}
