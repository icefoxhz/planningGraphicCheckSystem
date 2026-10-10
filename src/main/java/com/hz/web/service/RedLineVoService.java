package com.hz.web.service;

import com.hz.web.entity.vo.RedLineUpdateVoEntity;
import com.hz.web.entity.vo.RedLineVoEntity;
import com.hz.web.mapper.RedLineUpdateVoMapper;
import com.hz.web.mapper.RedLineVoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 红线图层查询服务
 *
 * @author saber
 */
@Service
@Slf4j
public class RedLineVoService {
    @Autowired
    RedLineVoMapper redLineVoMapper;

    @Autowired
    RedLineUpdateVoMapper redLineUpdateVoMapper;

    /**
     * 按条件查询红线图层数据
     *
     * @param redLineVoEntity 查询条件
     * @param tableName       红线空间表名
     * @return 红线数据列表
     */
    public List<RedLineVoEntity> queryRedLine(RedLineVoEntity redLineVoEntity, String tableName) {
        return redLineVoMapper.queryRedLine(redLineVoEntity, tableName);
    }

    public void redlineUpdate(RedLineUpdateVoEntity redLineUpdateVoEntity) {
        redLineUpdateVoMapper.updateById(redLineUpdateVoEntity);
    }
}
