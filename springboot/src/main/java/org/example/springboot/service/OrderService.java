package org.example.springboot.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.command.OrderCreateCommandDTO;
import org.example.springboot.dto.response.OrderArbitrationParams;
import org.example.springboot.dto.response.OrderListResponseDTO;
import org.example.springboot.dto.response.OrderResponseDTO;
import org.example.springboot.dto.response.UserDetailResponseDTO;
import org.example.springboot.entity.*;
import org.example.springboot.enums.ArbitrationStatus;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.enums.OrderStatus;
import org.example.springboot.enums.UserType;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.*;
import org.example.springboot.service.convert.OrderConvert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务
 * @author system
 */
@Slf4j
@Service
public class OrderService {

    @Resource
    private ShopOrderMapper shopOrderMapper;

    @Resource
    private ShopOrderItemMapper shopOrderItemMapper;

    @Resource
    private ShopProductMapper shopProductMapper;

    @Resource
    private UserAddressService userAddressService;
    @Resource
    private CartService cartService;
    @Resource
    private SysFileInfoMapper sysFileInfoMapper;
    @Resource
    private EvaluateMapper evaluateMapper;
    @Resource
    private UserService userService;

    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseDTO createOrder(OrderCreateCommandDTO dto, Long userId) {
        log.info("创建订单，用户ID: {}, 商品ID: {}, 数量: {}", userId, dto.getProductId(), dto.getQuantity());

        // 0. 验证用户
        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        // 1. 验证商品
        ShopProduct product = shopProductMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.canPurchase()) {
            throw new BusinessException("商品已下架或库存不足");
        }
        if (!product.checkStock(dto.getQuantity())) {
            throw new BusinessException("商品库存不足");
        }

        // 2. 验证收货地址
        UserAddress address = userAddressService.getAddressEntityById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException("收货地址不存在，请先添加地址");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权使用该收货地址");
        }
        
        // 3. 计算订单金额
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
        BigDecimal payAmount = totalAmount; // 暂不考虑优惠
        
        // 4. 生成订单号
        String orderNo = generateOrderNo(userId);
        
        // 5. 创建订单主表
        ShopOrder order = ShopOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .totalAmount(totalAmount)
                .payAmount(payAmount)
                .status(OrderStatus.PENDING.getCode())
                .receiverAddressId(address.getId())
                .remark(dto.getRemark())
                .build();
        shopOrderMapper.insert(order);
        
        // 6. 创建订单明细
        ShopOrderItem orderItem = ShopOrderItem.builder()
                .orderId(order.getId())
                .productId(product.getId())
                .skuId(product.getId()) // 暂无SKU，使用商品ID
                .title(product.getTitle())
                .skuTitle(product.getSubtitle())
                .price(product.getPrice())
                .quantity(dto.getQuantity())
                .subtotal(totalAmount)
                .build();
        shopOrderItemMapper.insert(orderItem);
        
        // 7. 扣减库存
        boolean stockReduced = product.reduceStock(dto.getQuantity());
        if (!stockReduced) {
            throw new BusinessException("库存扣减失败");
        }
        shopProductMapper.updateById(product);
        
        log.info("订单创建成功，订单号: {}", orderNo);
        //删除购物车信息
        cartService.deleteCart(dto.getProductId());
        // 8. 返回订单详情
        List<ShopOrderItem> items = new ArrayList<>();
        items.add(orderItem);
        return OrderConvert.toResponseDTO(order, address, items);
    }

    /**
     * 获取订单详情
     */
    public OrderResponseDTO getOrderDetail(Long orderId, Long userId, boolean isAdmin) {
        log.info("获取订单详情，订单ID: {}, 用户ID: {}, 是否管理员: {}", orderId, userId, isAdmin);
        
        // 查询订单
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 权限校验（管理员可以查看所有订单）
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        //获取用户信息
        UserDetailResponseDTO userDetailResponseDTO = userService.getUserById(userId);
        if (userDetailResponseDTO == null){
            throw new BusinessException("用户不存在");
        }
        
        // 查询订单明细
        LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(ShopOrderItem::getOrderId, orderId);
        List<ShopOrderItem> items = shopOrderItemMapper.selectList(itemWrapper);
        
        // 查询收货地址
        UserAddress address = null;
        if (order.getReceiverAddressId() != null) {
            address = userAddressService.getAddressEntityById(order.getReceiverAddressId());
        }

        //查询文件信息
        QueryWrapper<SysFileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("business_id",order.getOrderNo());
        queryWrapper.in("business_type",
                FileBusinessTypeEnum.USER_RETURN_VOUCHER.name(),//用户退货凭证
                FileBusinessTypeEnum.USER_SLOW_VOUCHER.name(),//用户退货凭证
                FileBusinessTypeEnum.REFUND_REFUSE.name(),
                FileBusinessTypeEnum.REFUND_REFUSE_EXCHANGE.name());
        queryWrapper.eq("status",1);
        List<SysFileInfo> images = sysFileInfoMapper.selectList(queryWrapper);
        //查看评价信息
        QueryWrapper<Evaluate> commentQueryWrapper = new QueryWrapper<>();
        if (userDetailResponseDTO.getUserType().equals(UserType.USER.getCode())){
            commentQueryWrapper.eq("user_id",userId);
        }
        commentQueryWrapper.eq("order_no",order.getOrderNo());
        List<Evaluate> commentList = evaluateMapper.selectList(commentQueryWrapper);



        return OrderConvert.toImagesResponseDTO(order, address, items, images,commentList);
    }

    /**
     * 分页查询订单列表（用户）
     */
    public Page<OrderListResponseDTO> getUserOrderPage(Long current, Long size, Integer status, Long userId,String orderNo) {
        log.info("分页查询用户订单，用户ID: {}, 页码: {}, 大小: {}, 状态: {}", userId, current, size, status);
        
        // 构建分页对象
        Page<ShopOrder> page = new Page<>(current, size);
        
        // 构建查询条件
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId);
        if (status != null) {
            wrapper.eq(ShopOrder::getStatus, status);
        }
        if (orderNo != null){
            wrapper.like(ShopOrder::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(ShopOrder::getCreateTime);
        
        // 查询订单
        Page<ShopOrder> orderPage = shopOrderMapper.selectPage(page, wrapper);
        
        // 查询所有订单的明细
        List<Long> orderIds = orderPage.getRecords().stream()
                .map(ShopOrder::getId)
                .collect(Collectors.toList());
        
        Map<Long, List<ShopOrderItem>> itemsMap = getOrderItemsMap(orderIds);
//        List<ShopOrderItem> shopOrderItems = shopOrderItemMapper.selectBatchIds(orderIds);

        // 转换为DTO
        Page<OrderListResponseDTO> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderListResponseDTO> records = orderPage.getRecords().stream()
                .map(order ->

                        OrderConvert.toListResponseDTO(order, itemsMap.get(order.getId())

                        ))
                .collect(Collectors.toList());
        resultPage.setRecords(records);
        
        return resultPage;
    }

    /**
     * 分页查询订单列表（管理员）
     */
    public Page<OrderListResponseDTO> getAdminOrderPage(Long current, Long size, Integer status, String orderNo, Integer arbitration) {
        log.info("分页查询管理员订单，页码: {}, 大小: {}, 状态: {}, 订单号: {}", current, size, status, orderNo);
        
        // 构建分页对象
        Page<ShopOrder> page = new Page<>(current, size);
        
        // 构建查询条件
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ShopOrder::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.like(ShopOrder::getOrderNo, orderNo);
        }
        if (arbitration != null){
            wrapper.eq(ShopOrder::getArbitration, arbitration);
        }
        wrapper.orderByDesc(ShopOrder::getCreateTime);
        
        // 查询订单
        Page<ShopOrder> orderPage = shopOrderMapper.selectPage(page, wrapper);
        
        // 查询所有订单的明细
        List<Long> orderIds = orderPage.getRecords().stream()
                .map(ShopOrder::getId)
                .collect(Collectors.toList());
        
        Map<Long, List<ShopOrderItem>> itemsMap = getOrderItemsMap(orderIds);
        
        // 转换为DTO
        Page<OrderListResponseDTO> resultPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderListResponseDTO> records = orderPage.getRecords().stream()
                .map(order -> OrderConvert.toListResponseDTO(order, itemsMap.get(order.getId())))
                .collect(Collectors.toList());
        resultPage.setRecords(records);
        
        return resultPage;
    }

    /**
     * 支付订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId, String payType) {
        log.info("支付订单，订单ID: {}, 用户ID: {}, 支付方式: {}", orderId, userId, payType);
        
        // 查询订单
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 权限校验
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        
        // 状态校验
        if (!OrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        
        log.info("订单支付成功，订单号: {}", order.getOrderNo());
    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId, boolean isAdmin) {
        log.info("取消订单，订单ID: {}, 用户ID: {}, 是否管理员: {}", orderId, userId, isAdmin);
        
        // 查询订单
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 权限校验
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        
        // 状态校验
        if (!order.canCancel()) {
            throw new BusinessException("订单状态不允许取消");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatus.CLOSED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        
        // 恢复库存
        restoreOrderStock(orderId);
        
        log.info("订单取消成功，订单号: {}", order.getOrderNo());
    }

    /**
     * 发货
     */
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String logisticsNo) {
        log.info("订单发货，订单ID: {}, 物流单号: {}", orderId, logisticsNo);
        
        // 查询订单
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 状态校验
        if (!order.canShip()) {
            throw new BusinessException("订单状态不允许发货");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatus.SHIPPED.getCode());
        order.setLogisticsNo(logisticsNo);
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        
        log.info("订单发货成功，订单号: {}", order.getOrderNo());
    }

    /**
     * 确认收货
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId, Long userId) {
        log.info("确认收货，订单ID: {}, 用户ID: {}", orderId, userId);
        
        // 查询订单
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 权限校验
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        
        // 状态校验
        if (!order.canConfirm()) {
            throw new BusinessException("订单状态不允许确认收货");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);

        
        log.info("确认收货成功，订单号: {}", order.getOrderNo());
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo(Long userId) {
        // 格式：时间戳(yyyyMMddHHmmss) + 用户ID后4位 + 4位随机数
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userIdSuffix = String.format("%04d", userId % 10000);
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return timestamp + userIdSuffix + randomSuffix;
    }

    /**
     * 批量查询订单明细
     */
    private Map<Long, List<ShopOrderItem>> getOrderItemsMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        
        LambdaQueryWrapper<ShopOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ShopOrderItem::getOrderId, orderIds);
        List<ShopOrderItem> items = shopOrderItemMapper.selectList(wrapper);
        
        return items.stream().collect(Collectors.groupingBy(ShopOrderItem::getOrderId));
    }

    /**
     * 恢复订单库存
     */
    private void restoreOrderStock(Long orderId) {
        // 查询订单明细
        LambdaQueryWrapper<ShopOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrderItem::getOrderId, orderId);
        List<ShopOrderItem> items = shopOrderItemMapper.selectList(wrapper);
        
        // 恢复库存
        for (ShopOrderItem item : items) {
            ShopProduct product = shopProductMapper.selectById(item.getProductId());
            if (product != null) {
                product.addStock(item.getQuantity());
                shopProductMapper.updateById(product);
            }
        }
    }
    /**
     * 退款
     */
    public void refundOrder(Long id, Long userId, String description) {
        //退款
        completeReturnOrder(id, userId, description,OrderStatus.REFUNDING.getCode());
    }

    public void returnOrder(Long id, Long userId, String description) {
        //退货
        completeReturnOrder(id, userId, description,OrderStatus.EXCHANGE_APPLY.getCode());
    }
    public void agreeOrder(Long id, Long userId) {
        //1.校验
        if(id== null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.REFUND_SUCCESS.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
    }
    /**
     * 拒绝退货
     */
    public void rejectReturnOrder(Long id, Long userId, String description) {
        //1.校验
        if(id== null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setReason(description);
        order.setStatus(OrderStatus.EXCHANGE_FAIL.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
    }
    public void rejectOrder(Long id, Long userId, String description) {
        //1.校验
        if(id== null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setReason(description);
        order.setStatus(OrderStatus.REFUND_FAIL.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
    }
    public void completeReturnOrder(Long id, Long userId, String description,Integer  status) {
        //1.校验
        if(id== null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setContent(description);
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
    }
    /**
     * 取消退款
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelRefund(Long id, Long userId) {
        if (id == null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        //删除图片信息
        LambdaQueryWrapper<SysFileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysFileInfo::getBusinessId, order.getOrderNo());
        queryWrapper.eq(SysFileInfo::getBusinessField, "cancelRefund");
        int delete = sysFileInfoMapper.delete(queryWrapper);
        if (delete == 0) {
            throw new BusinessException("未找到可删除的图片信息，订单号: {}", order.getOrderNo());
        }
    }
    /**
     * 取消退货
     */
    public void cancelReturn(Long id, Long userId) {
        if (id == null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        //删除图片信息
        LambdaQueryWrapper<SysFileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysFileInfo::getBusinessId, order.getOrderNo());
        queryWrapper.eq(SysFileInfo::getBusinessField, "cancelReturn");
        int delete = sysFileInfoMapper.delete(queryWrapper);
        if (delete == 0) {
            throw new BusinessException("未找到可删除的图片信息，订单号: {}", order.getOrderNo());
        }
    }

    public void ReturnOrder(Long id, Long userId, Integer code) {
        //1.校验
        if(id== null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(code);
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);

    }

    public void arbitrateOrder(Long id, Long userId) {
        if (id == null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(OrderStatus.ARBITRATION.getCode());//待仲裁
        order.setArbitration(ArbitrationStatus.ARBITRATION.getCode());//仲裁
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
    }

    public void handleArbitrateOrder(Long id, Long userId, OrderArbitrationParams orderArbitrationParams) {
        if (id == null || userId == null){
            throw new BusinessException("参数错误");
        }
        ShopOrder order = shopOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        //支持用户
        if (orderArbitrationParams.getArbitration().equals( ArbitrationStatus.ARBITRATION_SUCCESS.getCode())){
            order.setStatus(OrderStatus.REFUND_SUCCESS.getCode());
            order.setArbitration(ArbitrationStatus.ARBITRATION_SUCCESS.getCode());
            order.setUpdateTime(LocalDateTime.now());
            order.setArbitrationContent(orderArbitrationParams.getArbitrationContent());
            shopOrderMapper.updateById(order);
        }
        //支持商家 仲裁失败
        if (orderArbitrationParams.getArbitration().equals( ArbitrationStatus.ARBITRATION_FAIL.getCode())){
            order.setStatus(OrderStatus.REFUND_FAIL.getCode());
            order.setArbitration(ArbitrationStatus.ARBITRATION_FAIL.getCode());
            order.setUpdateTime(LocalDateTime.now());
            order.setArbitrationContent(orderArbitrationParams.getArbitrationContent());
            shopOrderMapper.updateById(order);
        }
        //协商解决
        if (orderArbitrationParams.getArbitration().equals( ArbitrationStatus.DISPUTE.getCode())){
            order.setStatus(OrderStatus.REFUNDING.getCode());
            order.setArbitration(ArbitrationStatus.DISPUTE.getCode());
            order.setUpdateTime(LocalDateTime.now());
            order.setArbitrationContent(orderArbitrationParams.getArbitrationContent());
            shopOrderMapper.updateById(order);
        }
    }
}

