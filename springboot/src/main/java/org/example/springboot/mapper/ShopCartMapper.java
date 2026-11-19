package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.ShopCart;
import org.example.springboot.entity.ShopOrder;

/**
 * 购物车Mapper
 * @author system
 */
@Mapper
public interface ShopCartMapper extends BaseMapper<ShopCart> {
}

