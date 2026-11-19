package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.command.PostCreateCommandDTO;
import org.example.springboot.dto.response.PostResponseDTO;
import org.example.springboot.entity.PostComment;
import org.example.springboot.service.PostService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "论坛帖子管理", description = "发帖、列表、点赞、热门帖子等")
@RequestMapping("/post")
@RestController
@Slf4j
@Validated
public class PostController {

    @Resource
    private PostService postService;

    @Operation(summary = "发布帖子")
    @PostMapping("/add")
    public Result<PostResponseDTO> add(@Valid @RequestBody PostCreateCommandDTO dto) {
        return Result.success("发帖成功", postService.createPost(dto));
    }

    @Operation(summary = "帖子列表")
    @GetMapping("/list")
    public Result<Page<PostResponseDTO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(postService.listPosts(page, size));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/detail/{postId}")
    public Result<PostResponseDTO> detail(@PathVariable Long postId) {
        return Result.success(postService.getDetail(postId));
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like/{postId}")
    public Result<Boolean> like(@PathVariable Long postId) {
        return Result.success(postService.toggleLike(postId));
    }

    @Operation(summary = "热门帖子")
    @GetMapping("/hot")
    public Result<List<PostResponseDTO>> hot() {
        return Result.success(postService.hotPosts());
    }

    @Operation(summary = "添加评论")
    @PostMapping("/comment")
    public Result<PostComment> comment(@RequestParam Long postId, @RequestParam String content) {
        return Result.success(postService.addComment(postId, content));
    }

    @Operation(summary = "获取评论列表")
    @GetMapping("/comments/{postId}")
    public Result<List<PostComment>> comments(@PathVariable Long postId) {
        return Result.success(postService.getComments(postId));
    }

    @Operation(summary = "管理员删除帖子")
    @DeleteMapping("/admin/delete/{postId}")
    public Result<Void> adminDelete(@PathVariable Long postId) {
        postService.adminDelete(postId);
        return Result.success();
    }
}
