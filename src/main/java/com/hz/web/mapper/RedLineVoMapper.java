package com.hz.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hz.web.entity.vo.RedLineVoEntity;
import com.hz.web.mapper.provider.RedLineVoProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author saber
 * @since 2025-05-11
 */
@Mapper
@Repository
public interface RedLineVoMapper extends BaseMapper<RedLineVoEntity> {
    @SelectProvider(type = RedLineVoProvider.class, method = "queryRedLine")
    List<RedLineVoEntity> queryRedLine(@Param("redLineVoEntity") RedLineVoEntity redLineVoEntity, @Param("tableName") String tableName);
}
