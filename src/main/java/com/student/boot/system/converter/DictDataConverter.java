package com.student.boot.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.boot.system.model.entity.DictData;
import com.student.boot.system.model.form.DictDataForm;
import com.student.boot.system.model.vo.DictPageVO;
import com.student.boot.common.model.Option;
import com.student.boot.system.model.form.DictForm;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 字典项 对象转换器
 *
 * @author Ray
 * @since 2022/6/8
 */
@Mapper(componentModel = "spring")
public interface DictDataConverter {

    Page<DictPageVO> toPageVo(Page<DictData> page);

    DictDataForm toForm(DictData entity);

    DictData toEntity(DictDataForm formFata);

    Option<Long> toOption(DictData dictData);
    List<Option<Long>> toOption(List<DictData> dictData);
}