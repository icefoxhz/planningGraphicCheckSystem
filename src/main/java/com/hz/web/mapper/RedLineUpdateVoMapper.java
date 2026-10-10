package com.hz.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hz.web.entity.vo.RedLineUpdateVoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RedLineUpdateVoMapper extends BaseMapper<RedLineUpdateVoEntity> {
}
