package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Cultural;

/**
 * 合规与文化认证
 * @author system
 */
@Mapper
public interface CulturalMapper extends BaseMapper<Cultural> {
}

