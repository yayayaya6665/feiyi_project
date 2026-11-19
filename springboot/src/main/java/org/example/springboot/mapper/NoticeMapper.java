package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Notice;

/**
 * 通知公告Mapper
 * @author system
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}

