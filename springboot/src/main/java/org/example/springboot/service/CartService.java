package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.command.CartCreateCommandDTO;
import org.example.springboot.dto.response.CartListResponseDTO;
import org.example.springboot.dto.response.CartResponseDTO;
import org.example.springboot.dto.response.OrderListResponseDTO;
import org.example.springboot.dto.response.ShopProductDetailResponseDTO;
import org.example.springboot.entity.ShopCart;
import org.example.springboot.entity.ShopProduct;
import org.example.springboot.entity.SysFileInfo;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.ShopCartMapper;
import org.example.springboot.mapper.ShopProductMapper;
import org.example.springboot.mapper.SysFileInfoMapper;
import org.example.springboot.service.convert.OrderConvert;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: LZL
 * @CreateTime: 2026-05-18
 * @Description: 购物车服务
 * @Version: 1.0
 */
@Slf4j
@Service
public class CartService {
    @Resource
    private ShopCartMapper shopCartMapper;
    @Resource
    private ShopProductMapper shopProductMapper;
    @Resource
    private SysFileInfoMapper sysFileInfoMapper;
    /**
     * 创建购物车
     *
     * @param cartCreateCommandDTO 创建购物车命令DTO
     * @param userId               用户ID
     * @return 购物车响应DTO
     */
    public CartResponseDTO createCart(@Valid CartCreateCommandDTO cartCreateCommandDTO, Long userId) {
        log.info("创建购物车，用户ID: {}, 商品ID: {}", userId, cartCreateCommandDTO.getProductId());
        //校验
        if (userId == null || userId <= 0) {
            throw new BusinessException("4001","用户ID不能为空");
        }
        if (cartCreateCommandDTO.getProductId() == null || cartCreateCommandDTO.getProductId().isEmpty()) {
            throw new BusinessException("4001","商品ID不能为空");
        }
        if (cartCreateCommandDTO.getProductNum() == null || cartCreateCommandDTO.getProductNum() <= 0) {
            throw new BusinessException("4001","商品数量不能为空");
        }
        if (cartCreateCommandDTO.getProductPrice() == null || cartCreateCommandDTO.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("4001","商品价格不能为空");
        }
        //查询商品id是否存在
        ShopProduct shopProduct = shopProductMapper.selectById(cartCreateCommandDTO.getProductId());
        if (shopProduct == null) {
            throw new BusinessException("4001","商品不存在");
        }
        ShopCart shopCart = new ShopCart();
        BeanUtils.copyProperties(cartCreateCommandDTO, shopCart);
        //查询购物车是否已经添加了
        ShopCart shopCartQuery = shopCartMapper.selectOne(new QueryWrapper<ShopCart>().eq("user_id", userId).eq("product_id", cartCreateCommandDTO.getProductId()));
        if (shopCartQuery == null){
            shopCartMapper.insert(shopCart);
        }else {
            shopCart.setId(shopCartQuery.getId()); // 关键：设置已有购物车的id
            shopCart.setProductNum(shopCartQuery.getProductNum() + cartCreateCommandDTO.getProductNum());
            shopCartMapper.updateById(shopCart);
        }
        CartResponseDTO cartResponseDTO = new CartResponseDTO();
        BeanUtils.copyProperties(shopCart, cartResponseDTO);
        return cartResponseDTO;
    }
    /**
     * 获取用户购物车列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @param userId  用户ID
     * @return 购物车列表响应DTO
     */
    public Page<CartListResponseDTO> getUserCartPage(Long current, Long size, Long userId) {
        //校验
        if (userId == null || userId <= 0) {
            throw new BusinessException("4001","用户ID不能为空");
        }
        if (current == null || current <= 0){
            throw new BusinessException("4001","当前页不能为空");
        }
        if (size == null || size <= 0){
            throw new BusinessException("4001","每页大小不能为空");
        }
        log.info("获取用户购物车列表，用户ID: {}, 页码: {}, 大小: {}", userId, current, size);
        //构建分页对象
        Page<ShopCart> page = new Page<>(current, size);
        //构建查询条件
        LambdaQueryWrapper<ShopCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopCart::getUserId, userId.toString());
        wrapper.orderByDesc(ShopCart::getCreateTime);
        // 查询购物车
        Page<ShopCart> shopCartPage = shopCartMapper.selectPage(page, wrapper);
        // 提取所有的商品id productId
        Set<String> productIds = new HashSet<>();
        for (ShopCart shopCart : shopCartPage.getRecords()){
            productIds.add(shopCart.getProductId());
        }
        if (productIds.isEmpty()){
           return new Page<>();
        }
        //批量查询
        List<ShopProduct> shopProducts = shopProductMapper.selectBatchIds(productIds);
        //批量查询文件
        QueryWrapper<SysFileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("business_id",productIds);
        queryWrapper.eq("business_type", FileBusinessTypeEnum.SHOP_PRODUCT.name());
        queryWrapper.eq("status",1);
        List<SysFileInfo> images = sysFileInfoMapper.selectList(queryWrapper);
        //填充数据
        HashMap<String, ShopProduct> shopMap = new HashMap<>();
        for (ShopProduct shopProduct : shopProducts){
            shopMap.put(shopProduct.getId(), shopProduct);
        }
        HashMap<String, SysFileInfo> filesMap = new HashMap<>();
        for (SysFileInfo sysFileInfo : images){
            filesMap.put(sysFileInfo.getBusinessId(), sysFileInfo);
        }
        // 转换成DTO
        Page<CartListResponseDTO> resultPage = new Page<>(shopCartPage.getCurrent(), shopCartPage.getSize(), shopCartPage.getTotal());
        List<CartListResponseDTO> cartListResponseDTOS = shopCartPage.getRecords().stream()
                .map(shopCart -> {
                    CartListResponseDTO cartListResponseDTO = new CartListResponseDTO();
                    BeanUtils.copyProperties(shopCart, cartListResponseDTO);
                    ShopProduct product = shopMap.get(shopCart.getProductId());
                    if (product == null) {
                        log.warn("购物车商品不存在: {}", shopCart.getProductId());
                        cartListResponseDTO.setShopProductDetailResponseDTO(null);
                        return cartListResponseDTO;
                    }
                    ShopProductDetailResponseDTO productDto = new ShopProductDetailResponseDTO();
                    BeanUtils.copyProperties(product, productDto);
                    cartListResponseDTO.setShopProductDetailResponseDTO(productDto);
                    SysFileInfo coverFile = filesMap.get(product.getId());
                    if (coverFile != null) {
                        productDto.setCoverFilePath(coverFile.getFilePath());
                    }
                    return cartListResponseDTO;
                }).collect(Collectors.toList());
        // 设置结果
        resultPage.setRecords(cartListResponseDTOS);
        return resultPage;
    }

    public void cancelCart(Long id) {
        shopCartMapper.deleteById(id);
    }

    public void updateCart(Long id, Integer quantity) {
        ShopCart shopCart = shopCartMapper.selectById(id);
        shopCart.setProductNum(quantity);
        shopCartMapper.updateById(shopCart);
    }
    /**
     * 取消选中
     * @param id
     * @param checked
     */
    public void cancelChecked(Long id, Integer checked) {
        ShopCart shopCart = shopCartMapper.selectById(id);
        shopCart.setChecked(checked);
        shopCartMapper.updateById(shopCart);
        log.info("取消选中，用户ID: {}, 商品ID: {}, 选中状态: {}", shopCart.getUserId(), shopCart.getProductId(), checked);
    }

    public void deleteCart(@NotBlank(message = "商品ID不能为空") @Size(max = 50, message = "商品ID长度不能超过50个字符") String productId) {
        shopCartMapper.delete(new QueryWrapper<ShopCart>().eq("product_id", productId));
        log.info("删除购物车，商品ID: {}", productId);
    }
}
