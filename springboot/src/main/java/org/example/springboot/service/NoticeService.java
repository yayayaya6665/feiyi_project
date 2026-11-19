package org.example.springboot.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.command.MessageCreateDTO;
import org.example.springboot.dto.command.NoticeCreateDTO;
import org.example.springboot.dto.response.MessageResponseDTO;
import org.example.springboot.dto.response.NoticeResponseDTO;
import org.example.springboot.dto.response.ShopProductDetailResponseDTO;
import org.example.springboot.entity.Message;
import org.example.springboot.entity.Notice;
import org.example.springboot.entity.ShopProduct;
import org.example.springboot.entity.SysFileInfo;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.MessageMapper;
import org.example.springboot.mapper.NoticeMapper;
import org.example.springboot.mapper.ShopProductMapper;
import org.example.springboot.mapper.SysFileInfoMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知公告
 * @author system
 */
@Slf4j
@Service
public class NoticeService {
    @Resource
    private NoticeMapper noticeMapper;
    /**
     * 获取留言列表
     * @param current
     * @param size
     * @return
     */
    public Page<NoticeResponseDTO> getCategoryPage(Long current, Long size, Integer noticeStatus,String noticeContent) {
        //1.校验参数
        if (current == null || current <= 0){
            throw new BusinessException("4001","当前页不能为空");
        }
        if (size == null || size <= 0){
            throw new BusinessException("4001","每页大小不能为空");
        }
        log.info("获取信息列表，页码: {}, 大小: {}", current, size);
        //2.构建分页对象
        Page<Notice> page = new Page<>(current, size);
        //3.查询
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Notice::getNoticeStatus);
        if (noticeStatus != null){
            queryWrapper.eq(Notice::getNoticeStatus, noticeStatus);
        }
        if (noticeContent != null){
            queryWrapper.like(Notice::getNoticeContent, noticeContent);
        }
        Page<Notice> NnticePage = noticeMapper.selectPage(page, queryWrapper);

        //4.返回
        Page<NoticeResponseDTO> resultPage = new Page<>(NnticePage.getCurrent(), NnticePage.getSize(), NnticePage.getTotal());
        List<NoticeResponseDTO> responseDTOS = NnticePage.getRecords().stream()
                .map(notice -> {
                    NoticeResponseDTO noticeResponseDTO = new NoticeResponseDTO();
                    BeanUtils.copyProperties(notice, noticeResponseDTO);
                    return noticeResponseDTO;
                }).collect(Collectors.toList());
        resultPage.setRecords(responseDTOS);
        return resultPage;
    }
    /**
     * 创建留言
     * @param noticeCreateDTO
     * @return
     */
    public int createNotice(@Valid NoticeCreateDTO noticeCreateDTO) {
        if (noticeCreateDTO.getNoticeContent() == null || noticeCreateDTO.getNoticeContent().trim().isEmpty()){
            throw new BusinessException("4001","公告内容不能为空");
        }
        Notice notice = new Notice();
        BeanUtils.copyProperties(noticeCreateDTO, notice);
        return noticeMapper.insert(notice);
    }

    public int noticeStatus(String id, Long userId,Integer noticeStatus) {
        if (id == null || id.trim().isEmpty()){
            throw new BusinessException("4001","ID不能为空");
        }
        if (userId == null){
            throw new BusinessException("4001","用户ID不能为空");
        }
        Notice noticeById = noticeMapper.selectById(id);
        if (noticeById == null){
            throw new BusinessException("4001","公告信息不存在");
        }
        noticeById.setNoticeStatus(noticeStatus);//设置顶置
        return noticeMapper.updateById(noticeById);
    }
    /**
     * 删除留言
     * @param id
     * @param userId
     * @return
     */
    public int delete(String id, Long userId) {
        if (id == null || id.trim().isEmpty()){
            throw new BusinessException("4001","ID不能为空");
        }
        if (userId == null){
            throw new BusinessException("4001","用户ID不能为空");
        }
        Notice noticeById = noticeMapper.selectById(id);
        if (noticeById == null){
            throw new BusinessException("4001","公告信息不存在");
        }
        return noticeMapper.deleteById(id);
    }
/**
     * 根据ID查询
     * @param id
     * @return
     */
    public NoticeResponseDTO getById(String id) {
        if (id == null || id.trim().isEmpty()){
            throw new BusinessException("4001","ID不能为空");
        }
        Notice noticeById = noticeMapper.selectById(id);
        if (noticeById == null){
            throw new BusinessException("4001","公告信息不存在");
        }
        NoticeResponseDTO noticeResponseDTO = new NoticeResponseDTO();
        BeanUtils.copyProperties(noticeById, noticeResponseDTO);
        return noticeResponseDTO;
    }
    /**
     * 修改留言
     * @param id
     * @param userId
     * @param noticeCreateDTO
     * @return
     */
    public int update(String id, Long userId, @Valid NoticeCreateDTO noticeCreateDTO) {
        if (id == null || id.trim().isEmpty()){
            throw new BusinessException("4001","ID不能为空");
        }
        if (userId == null){
            throw new BusinessException("4001","用户ID不能为空");
        }
        Notice noticeById = noticeMapper.selectById(id);
        if (noticeById == null){
            throw new BusinessException("4001","公告信息不存在");
        }
        noticeById.setNoticeContent(noticeCreateDTO.getNoticeContent());
        noticeById.setNoticeStatus(noticeCreateDTO.getNoticeStatus());
        return noticeMapper.updateById(noticeById);
    }
    /**
     * 获取所有公告信息
     * @return
     */
    public List<NoticeResponseDTO> getAll() {
        LambdaQueryWrapper<Notice> noticeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        noticeLambdaQueryWrapper.orderByDesc(Notice::getNoticeStatus).orderByDesc(Notice::getCreateTime);
        noticeLambdaQueryWrapper.last("LIMIT 5");
        List<Notice> notices = noticeMapper.selectList(noticeLambdaQueryWrapper);
        List<NoticeResponseDTO> noticeResponseDTO = notices.stream()
                .map(notice -> {
                    NoticeResponseDTO responseDTO = new NoticeResponseDTO();
                    BeanUtils.copyProperties(notice, responseDTO);
                    return responseDTO;
                }).collect(Collectors.toList());
        return noticeResponseDTO;
    }


}

