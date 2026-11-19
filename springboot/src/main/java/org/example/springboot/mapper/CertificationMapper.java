package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Certification;

/**
 * 评价Mapper
 * @author system
 */
@Mapper
public interface CertificationMapper extends BaseMapper<Certification> {
}

