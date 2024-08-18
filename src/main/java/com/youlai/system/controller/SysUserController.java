package com.youlai.system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.system.common.result.PageResult;
import com.youlai.system.common.result.Result;
import com.youlai.system.enums.ContactType;
import com.youlai.system.model.form.PasswordChangeForm;
import com.youlai.system.model.form.UserProfileForm;
import com.youlai.system.model.vo.UserProfileVO;
import com.youlai.system.security.util.SecurityUtils;
import com.youlai.system.util.ExcelUtils;
import com.youlai.system.enums.LogModuleEnum;
import com.youlai.system.model.dto.UserImportDTO;
import com.youlai.system.plugin.norepeat.annotation.PreventRepeatSubmit;
import com.youlai.system.plugin.easyexcel.UserImportListener;
import com.youlai.system.model.form.UserForm;
import com.youlai.system.model.entity.SysUser;
import com.youlai.system.model.query.UserPageQuery;
import com.youlai.system.model.dto.UserExportDTO;
import com.youlai.system.model.vo.UserInfoVO;
import com.youlai.system.model.vo.UserPageVO;
import com.youlai.system.plugin.syslog.annotation.LogAnnotation;
import com.youlai.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户控制层
 *
 * @author Ray
 * @since 2022/10/16
 */
@Tag(name = "02.用户接口")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping("/page")
    @LogAnnotation(value = "用户分页列表", module = LogModuleEnum.USER)
    public PageResult<UserPageVO> listPagedUsers(
            UserPageQuery queryParams
    ) {
        IPage<UserPageVO> result = userService.listPagedUsers(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:user:add')")
    @PreventRepeatSubmit
    public Result<?> saveUser(
            @RequestBody @Valid UserForm userForm
    ) {
        boolean result = userService.saveUser(userForm);
        return Result.judge(result);
    }

    @Operation(summary = "用户表单数据")
    @GetMapping("/{userId}/form")
    public Result<UserForm> getUserForm(
            @Parameter(description = "用户ID") @PathVariable Long userId
    ) {
        UserForm formData = userService.getUserFormData(userId);
        return Result.success(formData);
    }

    @Operation(summary = "修改用户")
    @PutMapping(value = "/{userId}")
    @PreAuthorize("@ss.hasPerm('sys:user:edit')")
    public Result<?> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody @Validated UserForm userForm) {
        boolean result = userService.updateUser(userId, userForm);
        return Result.judge(result);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:user:delete')")
    public Result<?> deleteUsers(
            @Parameter(description = "用户ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = userService.deleteUsers(ids);
        return Result.judge(result);
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping(value = "/{userId}/status")
    public Result<?> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "用户状态(1:启用;0:禁用)") @RequestParam Integer status
    ) {
        boolean result = userService.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getStatus, status)
        );
        return Result.judge(result);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUserInfo() {
        UserInfoVO userInfoVO = userService.getCurrentUserInfo();
        return Result.success(userInfoVO);
    }

    @Operation(summary = "用户导入模板下载")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String fileName = "用户导入模板.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String fileClassPath = "templates" + File.separator + "excel" + File.separator + fileName;
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileClassPath);

        ServletOutputStream outputStream = response.getOutputStream();
        ExcelWriter excelWriter = EasyExcel.write(outputStream).withTemplate(inputStream).build();

        excelWriter.finish();
    }

    @Operation(summary = "导入用户")
    @PostMapping("/import")
    public Result<?> importUsers(MultipartFile file) throws IOException {
        UserImportListener listener = new UserImportListener();
        String msg = ExcelUtils.importExcel(file.getInputStream(), UserImportDTO.class, listener);
        return Result.success(msg);
    }

    @Operation(summary = "导出用户")
    @GetMapping("/export")
    public void exportUsers(UserPageQuery queryParams, HttpServletResponse response) throws IOException {
        String fileName = "用户列表.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        List<UserExportDTO> exportUserList = userService.listExportUsers(queryParams);
        EasyExcel.write(response.getOutputStream(), UserExportDTO.class).sheet("用户列表")
                .doWrite(exportUserList);
    }

    @Operation(summary = "获取个人中心用户信息")
    @GetMapping("/{userId}/profile")
    public Result<UserProfileVO> getUserProfile(
            @Parameter(description = "用户ID") @PathVariable Long userId
    ) {
        UserProfileVO userProfile = userService.getUserProfile(userId);
        return Result.success(userProfile);
    }

    @Operation(summary = "修改个人中心用户信息")
    @PutMapping("/{userId}/profile")
    public Result<?> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UserProfileForm formData
    ) {
        boolean result = userService.updateUserProfile(formData);
        return Result.judge(result);
    }

    @Operation(summary = "重置用户密码")
    @PutMapping(value = "/{userId}/password/reset")
    @PreAuthorize("@ss.hasPerm('sys:user:password:reset')")
    public Result<?> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestParam String password
    ) {
        boolean result = userService.resetPassword(userId, password);
        return Result.judge(result);
    }

    @Operation(summary = "修改密码")
    @PutMapping(value = "/password")
    public Result<?> changePassword(
            @RequestBody PasswordChangeForm data
    ) {
        Long currUserId = SecurityUtils.getUserId();
        boolean result = userService.changePassword(currUserId, data);
        return Result.judge(result);
    }

    @Operation(summary = "发送短信/邮箱验证码")
    @PostMapping(value = "/send-verification-code")
    public Result<?> sendVerificationCode(
            @Parameter(description = "联系方式（手机号码或邮箱地址）", required = true) @RequestParam String contact,
            @Parameter(description = "联系方式类型（Mobile或Email）", required = true) @RequestParam ContactType contactType
    ) {
        boolean result = userService.sendVerificationCode(contact, contactType);
        return Result.judge(result);
    }


}
