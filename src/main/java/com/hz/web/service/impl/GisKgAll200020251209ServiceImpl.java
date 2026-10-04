package com.hz.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hz.constant.MyConstant;
import com.hz.web.entity.GisKgAll200020251209Entity;
import com.hz.web.mapper.GisKgAll200020251209Mapper;
import com.hz.web.service.IGisKgAll200020251209Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 控规全要素 GIS_KG_ALL_2000_20251209
 *
 * @author saber
 */
@Service
@Slf4j
public class GisKgAll200020251209ServiceImpl extends ServiceImpl<GisKgAll200020251209Mapper, GisKgAll200020251209Entity>
        implements IGisKgAll200020251209Service {
}
