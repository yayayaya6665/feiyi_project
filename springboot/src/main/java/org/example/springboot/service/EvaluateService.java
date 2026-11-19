package org.example.springboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.command.EvaluateCreateDTO;
import org.example.springboot.dto.response.CartListResponseDTO;
import org.example.springboot.dto.response.EvaluateResponseDTO;
import org.example.springboot.dto.response.ShopProductDetailResponseDTO;
import org.example.springboot.dto.response.ShopProductImageResponseDTO;
import org.example.springboot.entity.*;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.enums.OrderStatus;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.EvaluateMapper;
import org.example.springboot.mapper.ShopOrderMapper;
import org.example.springboot.mapper.ShopProductMapper;
import org.example.springboot.mapper.SysFileInfoMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价服务
 * @author system
 */
@Slf4j
@Service
public class EvaluateService {
    @Resource
    private EvaluateMapper evaluateMapper;
    @Resource
    private ShopProductMapper shopProductMapper;
    @Resource
    private SysFileInfoMapper sysFileInfoMapper;
    @Resource
    private ShopOrderMapper shopOrderMapper;
    @Transactional(rollbackFor = Exception.class)
    public int createEvaluate(@Valid EvaluateCreateDTO evaluateCreateDTO, Long userId) {
        //1.校验
        if (StrUtil.isEmpty(evaluateCreateDTO.getOrderNo())|| StrUtil.isEmpty(evaluateCreateDTO.getProductId())){
            throw new BusinessException("4001","参数错误");
        }

        log.info("创建评价，用户ID: {}, 订单号: {}, 商品ID: {}, 商品评分: {}, 评价信息: {}", userId, evaluateCreateDTO.getOrderNo(), evaluateCreateDTO.getProductId(), evaluateCreateDTO.getRating(), evaluateCreateDTO.getRemark());
        Evaluate evaluate = new Evaluate();
        evaluate.setUserId(userId);
        BeanUtils.copyProperties(evaluateCreateDTO, evaluate);
        //修改订单状态
        LambdaQueryWrapper<ShopOrder> shopOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shopOrderLambdaQueryWrapper.eq(ShopOrder::getOrderNo, evaluateCreateDTO.getOrderNo());
        ShopOrder shopOrder = shopOrderMapper.selectOne(shopOrderLambdaQueryWrapper);
        shopOrder.setStatus(OrderStatus.EVALUATED.getCode());
        shopOrderMapper.updateById(shopOrder);
        return evaluateMapper.insert(evaluate);
    }
    /**
     * 获取评价列表
     * @param current
     * @param size
     * @return
     */
    public Page<EvaluateResponseDTO> getCategoryPage(Long current, Long size,String productId) {
        //1.校验参数
        if (current == null || current <= 0){
            throw new BusinessException("4001","当前页不能为空");
        }
        if (size == null || size <= 0){
            throw new BusinessException("4001","每页大小不能为空");
        }
        log.info("获取评价列表，页码: {}, 大小: {}", current, size);
        //2.构建分页对象
        Page<Evaluate> page = new Page<>(current, size);
        //3.查询
        LambdaQueryWrapper<Evaluate> evaluateLambdaQueryWrapper = new LambdaQueryWrapper<>();
        evaluateLambdaQueryWrapper.orderByDesc(Evaluate::getCreateTime);
        evaluateLambdaQueryWrapper.eq(Evaluate::getProductId, productId);
        Page<Evaluate> evaluatePage = evaluateMapper.selectPage(page, evaluateLambdaQueryWrapper);
        //4.转换
        Page<EvaluateResponseDTO> resultPage = new Page<>(evaluatePage.getCurrent(), evaluatePage.getSize(), evaluatePage.getTotal());
        List<EvaluateResponseDTO> evaluateResponseDTOList = evaluatePage.getRecords().stream()
                .map(evaluate -> {
                    EvaluateResponseDTO evaluateResponseDTO = new EvaluateResponseDTO();
                    BeanUtils.copyProperties(evaluate, evaluateResponseDTO);
                    return evaluateResponseDTO;
                }).collect(Collectors.toList());
        resultPage.setRecords(evaluateResponseDTOList);
        return resultPage;
    }

    public void deleteEvaluate(Long evaluateId) {
        evaluateMapper.deleteById(evaluateId);
    }

    public Page<EvaluateResponseDTO> getEvaluatePage(Long current, Long size ,String rating,String  orderNo) {
        //1.校验参数
        if (current == null || current <= 0){
            throw new BusinessException("4001","当前页不能为空");
        }
        if (size == null || size <= 0){
            throw new BusinessException("4001","每页大小不能为空");
        }
        log.info("获取评价列表，页码: {}, 大小: {}", current, size);
        //2.构建分页对象
        Page<Evaluate> page = new Page<>(current, size);
        //3.查询
        LambdaQueryWrapper<Evaluate> evaluateLambdaQueryWrapper = new LambdaQueryWrapper<>();
        evaluateLambdaQueryWrapper.orderByDesc(Evaluate::getCreateTime);
        if (rating != null){
            evaluateLambdaQueryWrapper.eq(Evaluate:: getRating ,rating);
        }
        if (orderNo != null){
            evaluateLambdaQueryWrapper.like(Evaluate:: getOrderNo ,orderNo);
        }
        Page<Evaluate> evaluatePage = evaluateMapper.selectPage(page, evaluateLambdaQueryWrapper);
        //获取商品详情
        Set<String> productIds = new HashSet<>(); // 商品
        Set<String> productIdFile = new HashSet<>(); // 图片
        for ( Evaluate  evaluate : evaluatePage.getRecords()){
            productIds.add(evaluate.getProductId());
            productIdFile.add(evaluate.getProductId());
        }
        if (productIds.isEmpty()){
            return new Page<>();
        }
        //批量商品信息查询
        List<ShopProduct> shopProducts = shopProductMapper.selectBatchIds(productIds);
        //批量查询文件
        QueryWrapper<SysFileInfo> queryWrapperLife = new QueryWrapper<>();
        queryWrapperLife.in("business_id",productIds);
        queryWrapperLife.eq("business_type", FileBusinessTypeEnum.SHOP_PRODUCT.name());
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
        //4.转换
        Page<EvaluateResponseDTO> resultPage = new Page<>(evaluatePage.getCurrent(), evaluatePage.getSize(), evaluatePage.getTotal());
        List<EvaluateResponseDTO> evaluateResponseDTOList = evaluatePage.getRecords().stream()
                .map(evaluate -> {
                    EvaluateResponseDTO evaluateResponseDTO = new EvaluateResponseDTO();
                    BeanUtils.copyProperties(evaluate, evaluateResponseDTO);
                    ShopProduct product = shopMap.get(evaluate.getProductId());
                    if (product != null) {
                        ShopProductDetailResponseDTO productDto = new ShopProductDetailResponseDTO();
                        BeanUtils.copyProperties(product, productDto);

                        // 从 Map 中获取对应的图片列表，如果没有则为空列表
                        List<ShopProductImageResponseDTO> imageList =
                                productImagesMap.getOrDefault(evaluate.getProductId(),
                                        Collections.emptyList());
                        productDto.setImageList(imageList);

                        evaluateResponseDTO.setShopProductDetailResponseDTO(productDto);
                    }
                    return evaluateResponseDTO;
                }).collect(Collectors.toList());
        resultPage.setRecords(evaluateResponseDTOList);
        return resultPage;
    }
/**
 * 获取详情
 * @param evaluateId
 * @return
 */
    public EvaluateResponseDTO getDetail(Long evaluateId) {
        Evaluate evaluate = evaluateMapper.selectById(evaluateId);
        EvaluateResponseDTO evaluateResponseDTO = new EvaluateResponseDTO();
        if (evaluate != null) {
            BeanUtils.copyProperties(evaluate, evaluateResponseDTO);
        }
        return evaluateResponseDTO;
    }
}

