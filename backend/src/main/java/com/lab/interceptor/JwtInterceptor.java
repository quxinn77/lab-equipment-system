package com.lab.interceptor;

import cn.hutool.core.util.StrUtil;
import com.lab.annotation.RequireRole;
import com.lab.enums.RoleEnum;
import com.lab.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        // 登录/注册放行
        if ("/api/auth/login".equals(uri) || "/api/auth/register".equals(uri)) {
            return true;
        }
        // 文件静态访问（GET）放行
        if (uri.startsWith("/api/files/") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 解析 token
        String auth = request.getHeader("Authorization");
        if (StrUtil.isBlank(auth) || !auth.startsWith("Bearer ")) {
            return writeError(response, 401, "未登录或token已失效");
        }
        String token = auth.substring(7).trim();
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            return writeError(response, 401, "未登录或token已失效");
        }
        // B4：登出黑名单校验
        if (jwtUtil.isBlacklisted(token)) {
            return writeError(response, 401, "登录已失效，请重新登录");
        }
        Number userId = claims.get("userId", Number.class);
        String username = claims.get("username", String.class);
        Number roleId = claims.get("roleId", Number.class);
        RoleEnum role = RoleEnum.fromId(roleId == null ? null : roleId.longValue());
        if (userId == null || role == null) {
            return writeError(response, 401, "未登录或token已失效");
        }
        UserContext.set(new UserContext.LoginUser(userId.longValue(), username, claims.getSubject(),
                roleId.longValue(), role.getCode(), role.getName()));

        // 角色权限校验
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = hm.getBeanType().getAnnotation(RequireRole.class);
            }
            if (requireRole != null) {
                boolean allowed = Arrays.asList(requireRole.value()).contains(role);
                if (!allowed) {
                    return writeError(response, 403, "无权限执行该操作");
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean writeError(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
        writer.flush();
        return false;
    }
}
