package com.hz.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hz.constant.MyConstant;
import com.hz.web.entity.BkqXmfwMEntity;
import com.hz.web.mapper.BkqXmfwMMapper;
import com.hz.web.service.IBkqXmfwMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 项目范围面 BKQ_XMFW_M
 *
 * @author saber
 */
@Service
@Slf4j
public class BkqXmfwMServiceImpl extends ServiceImpl<BkqXmfwMMapper, BkqXmfwMEntity> implements IBkqXmfwMService {
}
