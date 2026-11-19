package org.example.springboot.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.FileInfoDTO;
import org.example.springboot.dto.command.CertificationCreateDTO;
import org.example.springboot.dto.command.MessageCreateDTO;
import org.example.springboot.dto.response.CertificationResponseDTO;
import org.example.springboot.dto.response.MessageResponseDTO;
import org.example.springboot.dto.response.ShopProductDetailResponseDTO;
import org.example.springboot.dto.response.UserDetailResponseDTO;
import org.example.springboot.entity.*;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.enums.UserStatus;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商家认证表
 *
 * @author system
 */
@Slf4j
@Service
public class CertificationService {
    @Resource
    private SysFileInfoMapper sysFileInfoMapper;
    @Resource
    private CertificationMapper certificationMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 创建商家认证
     *
     * @param certificationCreateDTO
     * @param
     * @return
     */

    public int createOrder(@Valid CertificationCreateDTO certificationCreateDTO) {
        //校验
        if (certificationCreateDTO.getName() == null || certificationCreateDTO.getName().trim().isEmpty()) {
            throw new BusinessException("4001", "商家认证名称不能为空");
        }
        LambdaQueryWrapper<Certification> certificationLambdaQueryWrapper = new LambdaQueryWrapper<>();
        certificationLambdaQueryWrapper.eq(Certification::getPhone, certificationCreateDTO.getPhone());
        certificationLambdaQueryWrapper.eq(Certification::getCertification, 0);
        Long nums = certificationMapper.selectCount(certificationLambdaQueryWrapper);
        if (nums > 0) {
            throw new BusinessException("4001", "该用户已经申请认证，平台正在审核中...");
        }
        Certification certification = new Certification();
        certification.setUserId(certificationCreateDTO.getUserId());
        BeanUtils.copyProperties(certificationCreateDTO, certification);
        return certificationMapper.insert(certification);
    }

    /**
     * 获取留言列表
     *
     * @param current
     * @param size
     * @return
     */
    public Page<CertificationResponseDTO> getPage(Long current, Long size, String name, String phone) {
        //1.校验参数
        if (current == null || current <= 0) {
            throw new BusinessException("4001", "当前页不能为空");
        }
        if (size == null || size <= 0) {
            throw new BusinessException("4001", "每页大小不能为空");
        }
        log.info("获取信息列表，页码: {}, 大小: {}", current, size);
        //2.构建分页对象
        Page<Certification> page = new Page<>(current, size);
        //3.查询
        LambdaQueryWrapper<Certification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Certification::getCreatedAt);
        if (name != null) {
            queryWrapper.like(Certification::getName, name);
        }
        Page<Certification> certificationPage = certificationMapper.selectPage(page, queryWrapper);
        //获取账号信息详情
        Set<Long> userIds = new HashSet<>();
        for (Certification certification : certificationPage.getRecords()) {
            userIds.add(certification.getUserId());
        }
        if (userIds.isEmpty()) {
            return new Page<>();
        }
        //批量查询
        List<User> users = userMapper.selectBatchIds(userIds);
        //填充数据
        HashMap<Long, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }
        //4.返回
        Page<CertificationResponseDTO> resultPage = new Page<>(certificationPage.getCurrent(), certificationPage.getSize(), certificationPage.getTotal());
        List<CertificationResponseDTO> responseDTOS = certificationPage.getRecords().stream()
                .map(certification -> {
                    CertificationResponseDTO certificationResponseDTO = new CertificationResponseDTO();
                    BeanUtils.copyProperties(certification, certificationResponseDTO);
                    User user = userMap.get(certification.getUserId());
                    UserDetailResponseDTO userDetailResponseDTO = new UserDetailResponseDTO();
                    BeanUtils.copyProperties(user, userDetailResponseDTO);
                    certificationResponseDTO.setUserDetailResponseDTO(userDetailResponseDTO);
                    return certificationResponseDTO;
                }).collect(Collectors.toList());
        resultPage.setRecords(responseDTOS);
        return resultPage;
    }

    /**
     * 审核
     *
     * @param id
     * @param userId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public int review(String id, Long userId) {
        //1.校验
        if (id == null) {
            throw new BusinessException("4001", "ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException("4001", "用户ID不能为空");
        }
        Certification certification = certificationMapper.selectById(id);
        if (certification == null) {
            throw new BusinessException("4001", "商家认证不存在");
        }
        certification.setCertification(1);//1审核通过

        User user = userMapper.selectById(certification.getUserId());
        user.setStatus(1);//1正常
        user.setUserType("BUSINESS");
        userMapper.updateById(user);
        return certificationMapper.updateById(certification);
    }

    /**
     * 拒绝
     *
     * @param id
     * @param userId
     * @return
     */
    public int refuse(String id, Long userId, String content) {
        //1.校验
        if (id == null) {
            throw new BusinessException("4001", "ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException("4001", "用户ID不能为空");
        }
        Certification certification = certificationMapper.selectById(id);
        if (certification == null) {
            throw new BusinessException("4001", "商家认证不存在");
        }
        certification.setCertification(2);//1审核拒绝
        certification.setContent(content);//拒绝理由
        return certificationMapper.updateById(certification);
    }

    /**
     * 获取商家认证信息
     *
     * @param id
     * @param userId
     * @return
     */
    public CertificationResponseDTO getCertification(String id, Long userId) {
        if (id == null) {
            throw new BusinessException("4001", "ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException("4001", "用户ID不能为空");
        }
        Certification certification = certificationMapper.selectById(id);
        if (certification == null) {
            throw new BusinessException("4001", "商家认证不存在");
        }
        //查询文件信息
        QueryWrapper<SysFileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("business_id", certification.getUserId());
        queryWrapper.in("business_type",
                FileBusinessTypeEnum.BUSINESS_SETTLEMENT.name());//认证凭证
        queryWrapper.eq("status", 1);
        queryWrapper.eq("business_field", "qualificationImages");
        List<SysFileInfo> images = sysFileInfoMapper.selectList(queryWrapper);
        List<FileInfoDTO> fileInfoDTOList = images.stream().map(sysFileInfo -> {
            FileInfoDTO fileInfoDTO = new FileInfoDTO();
            BeanUtils.copyProperties(sysFileInfo, fileInfoDTO);
            return fileInfoDTO;
        }).collect(Collectors.toList());

        // 4. 构建认证响应DTO
        CertificationResponseDTO certificationResponseDTO = new CertificationResponseDTO();
        BeanUtils.copyProperties(certification, certificationResponseDTO);

        User user = userMapper.selectById(certification.getUserId());
        UserDetailResponseDTO userDetailResponseDTO = new UserDetailResponseDTO();
        if (user != null) { // 空指针防护
            BeanUtils.copyProperties(user, userDetailResponseDTO);
        }
        //赋值文件
        certificationResponseDTO.setUserDetailResponseDTO(userDetailResponseDTO);
        certificationResponseDTO.setFileInfoDTOS(fileInfoDTOList);
        return certificationResponseDTO;

    }
}

