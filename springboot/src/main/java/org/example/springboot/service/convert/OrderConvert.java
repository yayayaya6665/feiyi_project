package org.example.springboot.service.convert;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.example.springboot.dto.response.*;
import org.example.springboot.entity.*;
import org.example.springboot.enums.FileBusinessTypeEnum;
import org.example.springboot.enums.OrderStatus;
import org.example.springboot.enums.PayType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单转换类
 * @author system
 */
public class OrderConvert {

    public static OrderResponseDTO toImagesResponseDTO(ShopOrder order, UserAddress address, List<ShopOrderItem> items ,List<SysFileInfo> images,List<Evaluate> commentList) {
        if (order == null) {
            return null;
        }

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusName(OrderStatus.getDescription(order.getStatus()));
        dto.setPayType(order.getPayType());
        dto.setPayTypeName(PayType.getDescription(order.getPayType()));
        dto.setPayTime(order.getPayTime());
        dto.setLogisticsNo(order.getLogisticsNo());
        dto.setRemark(order.getRemark());
        dto.setContent(order.getContent());
        dto.setReason(order.getReason());
        dto.setCreateTime(order.getCreateTime());
        dto.setUpdateTime(order.getUpdateTime());
        dto.setArbitration(order.getArbitration());
        dto.setArbitrationContent(order.getArbitrationContent());


        // 转换地址
        if (address != null) {
            dto.setAddress(UserAddressConvert.toResponseDTO(address));
        }

        // 转换订单明细
        if (items != null && !items.isEmpty()) {
            List<OrderItemResponseDTO> itemDTOs = items.stream()
                    .map(OrderConvert::toItemResponseDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }
        // 转换图片
        if (!CollectionUtils.isEmpty(images)) {
            // 提取所有图片路径，而非循环覆盖为单元素列表
            //businessField 用户退款业务
            List<String> cancelRefund = images.stream()
                    .filter(file -> FileBusinessTypeEnum.USER_RETURN_VOUCHER.name().equals(file.getBusinessType()))
                    .filter(file -> file.getBusinessField().equals("cancelRefund"))
                    .map(SysFileInfo::getFilePath)
                    .collect(Collectors.toList());
            //用户换货 业务
            List<String>  cancelReturn = images.stream()
                    .filter(file -> FileBusinessTypeEnum.USER_SLOW_VOUCHER.name().equals(file.getBusinessType()))
                    .filter(file -> file.getBusinessField().equals("cancelReturn"))
                    .map(SysFileInfo::getFilePath)
                    .collect(Collectors.toList());
            //商家拒绝退款图片
            List<String> businessImg = images.stream()
                    .filter(file -> FileBusinessTypeEnum.REFUND_REFUSE.name().equals(file.getBusinessType()))
                    .filter(file -> file.getBusinessField().equals("businessImg"))
                    .map(SysFileInfo::getFilePath)
                    .collect(Collectors.toList());
            //商家换货图片
            List<String> rejectExchangeImg = images.stream()
                    .filter(file -> FileBusinessTypeEnum.REFUND_REFUSE_EXCHANGE.name().equals(file.getBusinessType()))
                    .filter(file -> file.getBusinessField().equals("rejectExchangeImg"))
                    .map(SysFileInfo::getFilePath)
                    .collect(Collectors.toList());
            if (!cancelRefund.isEmpty()){
                dto.setCoverFilePath(cancelRefund);
            }
            if (!cancelReturn.isEmpty()){
                dto.setCoverFilePath(cancelReturn);
            }
            if (!businessImg.isEmpty()){
                dto.setSellerFilePath(businessImg);
            }
            if (!rejectExchangeImg.isEmpty()){
                dto.setSellerFilePath(rejectExchangeImg);
            }
        }
        // 转换评价
        if (commentList != null) {
            List<EvaluateResponseDTO> evaluateDTOs = commentList.stream()
                    .map(evaluate -> {
                        EvaluateResponseDTO evaluateDTO = new EvaluateResponseDTO();
                        evaluateDTO.setRating(evaluate.getRating());    // 评分
                        evaluateDTO.setRemark(evaluate.getRemark());// 评价内容
                        evaluateDTO.setCreateTime(evaluate.getCreateTime());// 评价时间
                        return evaluateDTO;

                    }).collect(Collectors.toList());
            // 赋值到OrderResponseDTO的evaluateList字段
            dto.setEvaluateList(evaluateDTOs);
        }
        return dto;
    }

    /**
     * Entity转ResponseDTO
     */
    public static OrderResponseDTO toResponseDTO(ShopOrder order, UserAddress address, List<ShopOrderItem> items ) {
        if (order == null) {
            return null;
        }
        
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusName(OrderStatus.getDescription(order.getStatus()));
        dto.setPayType(order.getPayType());
        dto.setPayTypeName(PayType.getDescription(order.getPayType()));
        dto.setPayTime(order.getPayTime());
        dto.setLogisticsNo(order.getLogisticsNo());
        dto.setRemark(order.getRemark());
        dto.setCreateTime(order.getCreateTime());
        dto.setUpdateTime(order.getUpdateTime());
        dto.setContent(order.getContent());
        dto.setReason(order.getReason());
        dto.setArbitration(order.getArbitration());
        dto.setArbitrationContent(order.getArbitrationContent());
        
        // 转换地址
        if (address != null) {
            dto.setAddress(UserAddressConvert.toResponseDTO(address));
        }
        
        // 转换订单明细
        if (items != null && !items.isEmpty()) {
            List<OrderItemResponseDTO> itemDTOs = items.stream()
                    .map(OrderConvert::toItemResponseDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }
        
        return dto;
    }

    /**
     * OrderItem Entity转ResponseDTO
     */
    public static OrderItemResponseDTO toItemResponseDTO(ShopOrderItem item) {
        if (item == null) {
            return null;
        }
        
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setSkuId(item.getSkuId());
        dto.setTitle(item.getTitle());
        dto.setSkuTitle(item.getSkuTitle());
        dto.setFullTitle(item.getFullTitle());
        dto.setPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getSubtotal());
        
        return dto;
    }

    /**
     * Entity转ListResponseDTO
     */
    public static OrderListResponseDTO toListResponseDTO(ShopOrder order, List<ShopOrderItem> items) {
        if (order == null) {
            return null;
        }
        
        OrderListResponseDTO dto = new OrderListResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setStatus(order.getStatus());
        dto.setStatusName(OrderStatus.getDescription(order.getStatus()));
        dto.setPayTime(order.getPayTime());
        dto.setCreateTime(order.getCreateTime());
        dto.setUpdateTime(order.getUpdateTime());
        dto.setContent(order.getContent());
        dto.setArbitration(order.getArbitration());
        dto.setArbitrationContent(order.getArbitrationContent());

        
        // 订单商品数量和主商品标题
        if (items != null && !items.isEmpty()) {
            dto.setItemCount(items.size());
            dto.setMainProductTitle(items.get(0).getTitle());
            dto.setProductId(items.get(0).getProductId());
        } else {
            dto.setItemCount(0);
            dto.setMainProductTitle("");
        }
        
        return dto;
    }
}

