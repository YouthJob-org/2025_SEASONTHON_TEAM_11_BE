package com.youthjob.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.common.response.ApiResponse;
import com.youthjob.common.response.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) {
        write(res,
                ErrorStatus.FORBIDDEN_ACCESS_DENIED.getStatusCode(),
                ApiResponse.failOnly(ErrorStatus.FORBIDDEN_ACCESS_DENIED));
    }

    private void write(HttpServletResponse res, int status, Object body) {
        try {
            res.setStatus(status);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.getWriter().write(om.writeValueAsString(body));
        } catch (Exception ignored) {}
    }
}
