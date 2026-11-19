package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Evaluate;
import org.example.springboot.entity.ShopOrder;

/**
 * 评价Mapper
 * @author system
 */
@Mapper
public interface EvaluateMapper extends BaseMapper<Evaluate> {
}

