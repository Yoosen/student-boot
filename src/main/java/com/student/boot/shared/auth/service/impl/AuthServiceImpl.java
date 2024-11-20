package com.student.boot.shared.auth.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.student.boot.common.constant.SecurityConstants;
import com.student.boot.common.exception.BusinessException;
import com.student.boot.common.result.ResultCode;
import com.student.boot.core.security.util.SecurityUtils;
import com.student.boot.shared.auth.enums.CaptchaTypeEnum;
import com.student.boot.shared.auth.model.RefreshTokenRequest;
import com.student.boot.shared.auth.service.AuthService;
import com.student.boot.shared.auth.model.CaptchaResponse;
import com.student.boot.shared.auth.model.AuthTokenResponse;
import com.student.boot.config.property.CaptchaProperties;
import com.student.boot.shared.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 *
 * @author haoxr
 * @since 2.4.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CodeGenerator codeGenerator;
    private final Font captchaFont;
    private final CaptchaProperties captchaProperties;
    private final TokenService tokenService;

    /**
     * 登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @Override
    public AuthTokenResponse login(String username, String password) {
        // 创建认证令牌对象
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username.toLowerCase().trim(), password);
        // 执行用户认证，认证成功返回的Authentication是SysUserDetailsService#loadUserByUsername获取到的的UserDetails
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        // 认证成功后生成JWT令牌
        AuthTokenResponse authTokenResponse = tokenService.generateToken(authentication);
        // 将认证信息存入Security上下文，便于在AOP（如日志记录）中获取当前用户信息
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 返回包含JWT令牌的登录结果
        return authTokenResponse;
    }

    /**
     * 注销
     */
    @Override
    public void logout() {
        String token = SecurityUtils.getTokenFromRequest();
        if (StrUtil.isNotBlank(token) && token.startsWith(SecurityConstants.JWT_TOKEN_PREFIX)) {
            token = token.substring(SecurityConstants.JWT_TOKEN_PREFIX.length());
            // 将JWT令牌加入黑名单
            tokenService.blacklistToken(token);
            // 清除Security上下文
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 获取验证码
     *
     * @return 验证码
     */
    @Override
    public CaptchaResponse getCaptcha() {

        String captchaType = captchaProperties.getType();
        int width = captchaProperties.getWidth();
        int height = captchaProperties.getHeight();
        int interfereCount = captchaProperties.getInterfereCount();
        int codeLength = captchaProperties.getCode().getLength();

        AbstractCaptcha captcha;
        if (CaptchaTypeEnum.CIRCLE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createCircleCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.GIF.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createGifCaptcha(width, height, codeLength);
        } else if (CaptchaTypeEnum.LINE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.SHEAR.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createShearCaptcha(width, height, codeLength, interfereCount);
        } else {
            throw new IllegalArgumentException("Invalid captcha type: " + captchaType);
        }
        captcha.setGenerator(codeGenerator);
        captcha.setTextAlpha(captchaProperties.getTextAlpha());
        captcha.setFont(captchaFont);

        String captchaCode = captcha.getCode();
        String imageBase64Data = captcha.getImageBase64Data();

        // 验证码文本缓存至Redis，用于登录校验
        String captchaKey = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(SecurityConstants.CAPTCHA_CODE_PREFIX + captchaKey, captchaCode,
                captchaProperties.getExpireSeconds(), TimeUnit.SECONDS);

        return CaptchaResponse.builder()
                .captchaKey(captchaKey)
                .captchaBase64(imageBase64Data)
                .build();
    }

    /**
     * 刷新令牌
     *
     * @param request 刷新令牌请求参数
     * @return 新的访问令牌
     */
    @Override
    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        // 验证刷新令牌

        String refreshToken = request.getRefreshToken();

        boolean isValidate = tokenService.validateToken(refreshToken);

        if (!isValidate) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        return tokenService.refreshToken(refreshToken);
    }

}
