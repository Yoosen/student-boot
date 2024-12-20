package com.student.boot.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.student.boot.common.annotation.DataPermission;
import com.student.boot.system.model.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    @DataPermission(deptIdColumnName = "id")
    @Override
    List<Dept> selectList(@Param(Constants.WRAPPER) Wrapper<Dept> queryWrapper);
}
