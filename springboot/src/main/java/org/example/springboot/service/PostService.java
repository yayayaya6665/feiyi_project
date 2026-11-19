package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.command.PostCreateCommandDTO;
import org.example.springboot.dto.response.PostResponseDTO;
import org.example.springboot.entity.Post;
import org.example.springboot.entity.PostComment;
import org.example.springboot.entity.PostLike;
import org.example.springboot.entity.User;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.PostCommentMapper;
import org.example.springboot.mapper.PostLikeMapper;
import org.example.springboot.mapper.PostMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostService {

    @Resource
    private PostMapper postMapper;
    @Resource
    private PostLikeMapper postLikeMapper;
    @Resource
    private PostCommentMapper postCommentMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 发帖
     */
    @Transactional
    public PostResponseDTO createPost(PostCreateCommandDTO dto) {
        String userIdStr = JwtTokenUtils.getCurrentUserIdAsString();
        if (userIdStr == null) throw new BusinessException("请先登录");
        Long userId = Long.parseLong(userIdStr);

        Post post = Post.builder()
                .userId(userId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .images(dto.getImages())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .status(0)
                .build();
        postMapper.insert(post);
        return toDTO(post, userId);
    }

    /**
     * 帖子列表（分页）
     */
    public Page<PostResponseDTO> listPosts(Integer page, Integer size) {
        Page<Post> postPage = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<>();
        qw.eq(Post::getStatus, 0).orderByDesc(Post::getCreateTime);
        Page<Post> result = postMapper.selectPage(postPage, qw);

        Long currentUserId = getCurrentUserId();

        Page<PostResponseDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(p -> toDTO(p, currentUserId))
                .collect(Collectors.toList()));
        return dtoPage;
    }

    /**
     * 帖子详情
     */
    public PostResponseDTO getDetail(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 1) throw new BusinessException("帖子不存在");

        // 浏览量+1
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        postMapper.updateById(post);

        return toDTO(post, getCurrentUserId());
    }

    /**
     * 点赞/取消点赞
     */
    @Transactional
    public boolean toggleLike(Long postId) {
        Long userId = getCurrentUserId();
        if (userId == null) throw new BusinessException("请先登录");

        LambdaQueryWrapper<PostLike> qw = new LambdaQueryWrapper<>();
        qw.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        PostLike existing = postLikeMapper.selectOne(qw);

        Post post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException("帖子不存在");

        if (existing != null) {
            postLikeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) - 1));
            postMapper.updateById(post);
            return false; // 取消点赞
        } else {
            postLikeMapper.insert(PostLike.builder().postId(postId).userId(userId).build());
            post.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + 1);
            postMapper.updateById(post);
            return true; // 已点赞
        }
    }

    /**
     * 热门帖子 Top10
     */
    public List<PostResponseDTO> hotPosts() {
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<>();
        qw.eq(Post::getStatus, 0).orderByDesc(Post::getViewCount).last("LIMIT 10");
        return postMapper.selectList(qw).stream()
                .map(p -> toDTO(p, getCurrentUserId()))
                .collect(Collectors.toList());
    }

    /**
     * 添加评论
     */
    @Transactional
    public PostComment addComment(Long postId, String content) {
        Long userId = getCurrentUserId();
        if (userId == null) throw new BusinessException("请先登录");
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 1) throw new BusinessException("帖子不存在");
        PostComment comment = PostComment.builder().postId(postId).userId(userId).content(content).build();
        postCommentMapper.insert(comment);
        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        postMapper.updateById(post);
        return comment;
    }

    /**
     * 获取帖子评论列表
     */
    public List<PostComment> getComments(Long postId) {
        LambdaQueryWrapper<PostComment> qw = new LambdaQueryWrapper<>();
        qw.eq(PostComment::getPostId, postId).orderByAsc(PostComment::getCreateTime);
        return postCommentMapper.selectList(qw);
    }

    /**
     * 管理员删除帖子
     */
    @Transactional
    public void adminDelete(Long postId) {
        if (!JwtTokenUtils.isAdmin()) throw new BusinessException("无权限，仅管理员可删除");
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException("帖子不存在");
        post.setStatus(1);
        postMapper.updateById(post);
    }

    private PostResponseDTO toDTO(Post post, Long currentUserId) {
        String username = "匿名用户";
        String avatar = null;
        if (post.getUserId() != null) {
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                username = user.getName() != null ? user.getName() : user.getUsername();
                avatar = user.getAvatar();
            }
        }

        boolean isLiked = false;
        if (currentUserId != null) {
            LambdaQueryWrapper<PostLike> lq = new LambdaQueryWrapper<>();
            lq.eq(PostLike::getPostId, post.getId()).eq(PostLike::getUserId, currentUserId);
            isLiked = postLikeMapper.selectCount(lq) > 0;
        }

        return PostResponseDTO.builder()
                .id(post.getId()).userId(post.getUserId())
                .username(username).avatar(avatar)
                .title(post.getTitle()).content(post.getContent())
                .images(post.getImages())
                .viewCount(post.getViewCount()).likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isLiked(isLiked).createTime(post.getCreateTime())
                .build();
    }

    private Long getCurrentUserId() {
        String uid = JwtTokenUtils.getCurrentUserIdAsString();
        try { return uid != null ? Long.parseLong(uid) : null; }
        catch (NumberFormatException e) { return null; }
    }
}
