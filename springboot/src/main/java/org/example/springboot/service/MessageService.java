package org.example.springboot.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.example.springboot.dto.command.MessageCreateDTO;
import org.example.springboot.dto.response.MessageResponseDTO;
import org.example.springboot.dto.response.ShopProductDetailResponseDTO;
import org.example.springboot.entity.Message;
import org.example.springboot.entity.ShopProduct;
import org.example.springboot.entity.SysFileInfo;
import org.example.springboot.entity.User;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.MessageMapper;
import org.example.springboot.mapper.ShopProductMapper;
import org.example.springboot.mapper.SysFileInfoMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 留言服务
 * @author system
 */
@Slf4j
@Service
public class MessageService {
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private ShopProductMapper shopProductMapper;
    @Resource
    private SysFileInfoMapper sysFileInfoMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 创建留言
     * @param messageCreateDTO
     * @param userId
     * @return
     */
    public int createMessage(@Valid MessageCreateDTO messageCreateDTO, Long userId) {
        //校验
        if (messageCreateDTO.getProductId() == null || messageCreateDTO.getProductId().trim().isEmpty()) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (messageCreateDTO.getRemark() == null || messageCreateDTO.getRemark().trim().isEmpty()) {
            throw new RuntimeException("留言内容不能为空");
        }
        Message message = new Message();
        message.setUserId(userId);
        BeanUtils.copyProperties(messageCreateDTO, message);
        return messageMapper.insert(message);
    }
    /**
     * 获取留言列表
     * @param current
     * @param size
     * @return
     */
    public Page<MessageResponseDTO> getCategoryPage(Long current, Long size, String productId) {
        //1.校验参数
        if (current == null || current <= 0){
            throw new BusinessException("4001","当前页不能为空");
        }
        if (size == null || size <= 0){
            throw new BusinessException("4001","每页大小不能为空");
        }
        log.info("获取信息列表，页码: {}, 大小: {}", current, size);
        //2.构建分页对象
        Page<Message> page = new Page<>(current, size);
        //3.查询
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Message::getCreateTime);
        if (StringUtils.hasText(productId)) {
            queryWrapper.eq(Message::getProductId, productId.trim());
        }
        Page<Message> messagePage = messageMapper.selectPage(page, queryWrapper);
        //获取商品详情
        Set<String> productIds = new HashSet<>();
        for (Message message : messagePage.getRecords()){
            productIds.add(message.getProductId());
        }
        if (productIds.isEmpty()){
            Page<MessageResponseDTO> emptyPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }
        //批量查询
        List<ShopProduct> shopProducts = shopProductMapper.selectBatchIds(productIds);
        //批量查询文件
        QueryWrapper<SysFileInfo> queryWrapperLife = new QueryWrapper<>();
        queryWrapperLife.in("business_id",productIds);
        queryWrapperLife.eq("business_type", FileBusinessTypeEnum.SHOP_PRODUCT.name());
        queryWrapperLife.eq("status",1);
        List<SysFileInfo> images = sysFileInfoMapper.selectList(queryWrapperLife);
        //填充数据
        HashMap<String, ShopProduct> shopMap = new HashMap<>();
        for (ShopProduct shopProduct : shopProducts){
            shopMap.put(shopProduct.getId(), shopProduct);
        }
        HashMap<String, SysFileInfo> filesMap = new HashMap<>();
        for (SysFileInfo sysFileInfo : images){
            filesMap.put(sysFileInfo.getBusinessId(), sysFileInfo);
        }
        Set<Long> userIds = messagePage.getRecords().stream()
                .map(Message::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                userMap.put(u.getId(), u);
            }
        }
        //4.返回
        Page<MessageResponseDTO> resultPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        List<MessageResponseDTO> responseDTOS = messagePage.getRecords().stream()
                .map(message -> {
                    MessageResponseDTO messageResponseDTO = new MessageResponseDTO();
                    BeanUtils.copyProperties(message, messageResponseDTO);
                    User user = userMap.get(message.getUserId());
                    if (user != null) {
                        String name = StringUtils.hasText(user.getName()) ? user.getName() : user.getUsername();
                        messageResponseDTO.setUserName(name);
                        messageResponseDTO.setUserAvatar(user.getAvatar());
                    }
                    ShopProduct product = shopMap.get(message.getProductId());
                    ShopProductDetailResponseDTO productDto = new ShopProductDetailResponseDTO();
                    if (product != null) {
                        BeanUtils.copyProperties(product, productDto);
                        SysFileInfo coverFile = filesMap.get(product.getId());
                        if (coverFile != null) {
                            productDto.setCoverFilePath(coverFile.getFilePath());
                        }
                    } else {
                        productDto.setTitle("商品不存在或已删除");
                    }
                    messageResponseDTO.setShopProductDetailResponseDTO(productDto);
                    return messageResponseDTO;
                }).collect(Collectors.toList());
        resultPage.setRecords(responseDTOS);
        return resultPage;
    }
    /**
     * 添加留言
     * @param messageCreateDTO
     * @param userId
     * @return
     */
    public int addMessage(@Valid MessageCreateDTO messageCreateDTO, Long userId) {
        if (messageCreateDTO.getId() == null) {
            throw new RuntimeException("留言ID不能为空");
        }
        if (messageCreateDTO.getProductId() == null || messageCreateDTO.getProductId().trim().isEmpty()) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (messageCreateDTO.getMessage() == null || messageCreateDTO.getMessage().trim().isEmpty()) {
            throw new RuntimeException("回复内容不能为空");
        }
        log.info("商家回复留言，操作者用户ID: {}, 留言ID: {}", userId, messageCreateDTO.getId());
        // 仅更新商家回复字段，避免把 user_id 覆盖为商家账号
        LambdaUpdateWrapper<Message> uw = new LambdaUpdateWrapper<>();
        uw.eq(Message::getId, messageCreateDTO.getId())
                .eq(Message::getProductId, messageCreateDTO.getProductId().trim())
                .set(Message::getMessage, messageCreateDTO.getMessage().trim());
        return messageMapper.update(null, uw);
    }
}

