package com.student.boot.shared.auth.controller;

import com.student.boot.common.enums.LogModuleEnum;
import com.student.boot.common.result.Result;
import com.student.boot.shared.auth.model.RefreshTokenRequest;
import com.student.boot.shared.auth.service.AuthService;
import com.student.boot.shared.auth.model.CaptchaResponse;
import com.student.boot.shared.auth.model.AuthTokenResponse;
import com.student.boot.common.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制层
 *
 * @author Ray
 * @since 2022/10/16
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    @Log(value = "登录", module = LogModuleEnum.LOGIN)
    public Result<AuthTokenResponse> login(
            @Parameter(description = "用户名", example = "admin") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password
    ) {
        AuthTokenResponse authTokenResponse = authService.login(username, password);
        return Result.success(authTokenResponse);
    }

    @Operation(summary = "注销")
    @DeleteMapping("/logout")
    @Log(value = "注销", module = LogModuleEnum.LOGIN)
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaResponse> getCaptcha() {
        CaptchaResponse captcha = authService.getCaptcha();
        return Result.success(captcha);
    }

    @Operation(summary = "刷新token")
    @PostMapping("/refresh-token")
    public Result<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthTokenResponse authTokenResponse =  authService.refreshToken(request);
        return Result.success(authTokenResponse);
    }
}
