package org.example.springboot.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.command.CulturalCreateDTO;
import org.example.springboot.dto.command.MessageCreateDTO;
import org.example.springboot.dto.response.CulturalResponseDTO;
import org.example.springboot.dto.response.MessageResponseDTO;
import org.example.springboot.dto.response.ShopProductDetailResponseDTO;
import org.example.springboot.dto.response.ShopProductImageResponseDTO;
import org.example.springboot.entity.Cultural;
import org.example.springboot.entity.Message;
import org.example.springboot.entity.ShopProduct;
import org.example.springboot.entity.SysFileInfo;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.enums.ShopProductStatus;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.CulturalMapper;
import org.example.springboot.mapper.MessageMapper;
import org.example.springboot.mapper.ShopProductMapper;
import org.example.springboot.mapper.SysFileInfoMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 合规与文化认证
 * @author system
 */
@Slf4j
@Service
public class CulturalService {
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private ShopProductMapper shopProductMapper;
    @Resource
    private SysFileInfoMapper sysFileInfoMapper;
    @Resource
    private CulturalMapper culturalMapper;
    /**
     * 创建留言
     * @param culturalCreateDTO
     * @param userId
     * @return
     */

    public int createCultural(@Valid CulturalCreateDTO culturalCreateDTO, Long userId) {
        //校验
        if (culturalCreateDTO.getProductId() == null || culturalCreateDTO.getProductId().trim().isEmpty()) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (userId == null){
            throw new RuntimeException("该账号无权限！ 请重新登录");
        }
        Cultural cultural = new Cultural();
        cultural.setCulturalStatus(0);//默认审核中
        BeanUtils.copyProperties(culturalCreateDTO, cultural);
        return culturalMapper.insert(cultural);
    }
    /**
     * 获取留言列表
     * @param current
     * @param size
     * @return
     */
    public Page<CulturalResponseDTO> getCategoryPage(Long current, Long size, String productId) {
        //1.校验参数
        if (current == null || current <= 0){
            throw new BusinessException("4001","当前页不能为空");
        }
        if (size == null || size <= 0){
            throw new BusinessException("4001","每页大小不能为空");
        }
        log.info("获取信息列表，页码: {}, 大小: {}", current, size);
        //2.构建分页对象
        Page<Cultural> page = new Page<>(current, size);
        //3.查询
        LambdaQueryWrapper<Cultural> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Cultural::getCreateTime);
        if (productId != null){
            queryWrapper.like(Cultural::getProductId, productId);
        }
        Page<Cultural> culturalPage = culturalMapper.selectPage(page, queryWrapper);
        //获取商品详情
        Set<String> productIds = new HashSet<>();
        Set<String> culturalIds = new HashSet<>();
        for (Cultural cultural : culturalPage.getRecords()){
            productIds.add(cultural.getProductId());
            culturalIds.add(cultural.getId());
        }
        if (productIds.isEmpty()){
            return new Page<>();
        }
        //批量商品信息查询
        List<ShopProduct> shopProducts = shopProductMapper.selectBatchIds(productIds);
        //批量查询文件
        QueryWrapper<SysFileInfo> queryWrapperLife = new QueryWrapper<>();
        queryWrapperLife.in("business_id",productIds);
        queryWrapperLife.eq("business_type", FileBusinessTypeEnum.CULTURAL_AUTHENTICATION.name());
        queryWrapperLife.eq("status",1);
        List<SysFileInfo> images = sysFileInfoMapper.selectList(queryWrapperLife);
        // 提前按 business_id 分组图片，避免在循环中重复过滤
        Map<String, List<ShopProductImageResponseDTO>> productImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        SysFileInfo::getBusinessId,
                        Collectors.mapping(sysFileInfo -> {
                            ShopProductImageResponseDTO imageDTO = new ShopProductImageResponseDTO();
                            BeanUtils.copyProperties(sysFileInfo, imageDTO);
                            return imageDTO;
                        }, Collectors.toList())
                ));
        //填充数据
        HashMap<String, ShopProduct> shopMap = new HashMap<>();
        for (ShopProduct shopProduct : shopProducts){
            shopMap.put(shopProduct.getId(), shopProduct);
        }
        //4.返回
        Page<CulturalResponseDTO> resultPage = new Page<>(culturalPage.getCurrent(), culturalPage.getSize(), culturalPage.getTotal());
        List<CulturalResponseDTO> responseDTOS = culturalPage.getRecords().stream()
                .map(cultural -> {
                    CulturalResponseDTO culturalResponseDTO = new CulturalResponseDTO();
                    BeanUtils.copyProperties(cultural, culturalResponseDTO);
                    ShopProduct product = shopMap.get(cultural.getProductId());
                    if (product != null) {
                        ShopProductDetailResponseDTO productDto = new ShopProductDetailResponseDTO();
                        BeanUtils.copyProperties(product, productDto);

                        // 从 Map 中获取对应的图片列表，如果没有则为空列表
                        List<ShopProductImageResponseDTO> imageList =
                                productImagesMap.getOrDefault(cultural.getProductId(),
                                        Collections.emptyList());
                        productDto.setImageList(imageList);

                        culturalResponseDTO.setShopProductDetailResponseDTO(productDto);
                    }
                    return culturalResponseDTO;
                }).collect(Collectors.toList());
        resultPage.setRecords(responseDTOS);
        return resultPage;
    }
    /**
     * 审核
     * @param id
     * @param status
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public int createReview(String id, Integer status) {
        if (id == null || id.trim().isEmpty()) {
            throw new BusinessException("ID不能为空");
        }
        if (status == null) {
            throw new BusinessException("审核状态不能为空");
        }
        //根据id查询
        Cultural culturalById = culturalMapper.selectById(id);
        culturalById.setCulturalStatus( status);
        culturalById.setOutcome("审核通过");
        ShopProduct shopProduct = shopProductMapper.selectById(culturalById.getProductId());
        if (shopProduct == null) {
            throw new BusinessException("商品不存在");
        }
        shopProduct.setCulturalType(ShopProductStatus.AUDIT_PASS.getCode());
        shopProductMapper.updateById(shopProduct);
        //更新
        return culturalMapper.updateById(culturalById);
    }
    /**
     * 拒绝
     * @param id
     * @param status
     * @return
     */
    public int refuse(String id, Integer status ,String outcome) {
        if (id == null || id.trim().isEmpty()) {
            throw new BusinessException("ID不能为空");
        }
        if (status == null) {
            throw new BusinessException("审核状态不能为空");
        }
        //根据id查询
        Cultural culturalById = culturalMapper.selectById(id);
        if (culturalById == null){
            throw new BusinessException("该信息不存在");
        }
        culturalById.setCulturalStatus( status);//拒绝
        culturalById.setOutcome(outcome);//拒绝原因
        //更新
        return culturalMapper.updateById(culturalById);
    }
    /**
     * 获取详情
     * @param id
     * @return
     */

    public CulturalResponseDTO getDetail(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new BusinessException("ID不能为空");
        }
        Cultural culturalById = culturalMapper.selectById(id);
        if (culturalById == null){
           throw new BusinessException("该信息不存在");
        }
        ShopProduct shopProduct = shopProductMapper.selectById(culturalById.getProductId());
        if (shopProduct == null){
            throw new BusinessException("商品不存在");
        }
        //批量查询文件
        QueryWrapper<SysFileInfo> queryWrapperLife = new QueryWrapper<>();
        queryWrapperLife.in("business_id",shopProduct.getId());
        queryWrapperLife.eq("business_type", FileBusinessTypeEnum.CULTURAL_AUTHENTICATION.name());
        queryWrapperLife.eq("status",1);
        List<SysFileInfo> images = sysFileInfoMapper.selectList(queryWrapperLife);
        CulturalResponseDTO culturalResponseDTO = new CulturalResponseDTO();
        BeanUtils.copyProperties(culturalById, culturalResponseDTO);

        ShopProductDetailResponseDTO shopProductDetailResponseDTO = new ShopProductDetailResponseDTO();
        BeanUtils.copyProperties(shopProduct, shopProductDetailResponseDTO);
        culturalResponseDTO.setShopProductDetailResponseDTO(shopProductDetailResponseDTO);


        // 7. 转换文件列表为ShopProductImageResponseDTO集合（核心修正部分）
        List<ShopProductImageResponseDTO> imageDTOList = images.stream()
                .map(sysFileInfo -> {
                    ShopProductImageResponseDTO imageDTO = new ShopProductImageResponseDTO();
                    BeanUtils.copyProperties(sysFileInfo, imageDTO);
                    return imageDTO;
                }).collect(Collectors.toList());
        shopProductDetailResponseDTO.setImageList(imageDTOList);
        return culturalResponseDTO;
    }
}

