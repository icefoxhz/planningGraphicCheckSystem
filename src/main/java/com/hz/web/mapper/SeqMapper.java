package com.hz.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeqMapper extends BaseMapper<Object> {

    @Select("SELECT seq_layer_id.NEXTVAL FROM dual")
    Integer getNextIdFromSequence();
}
