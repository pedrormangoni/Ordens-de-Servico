package br.upf.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(urlPatterns = "/*")
public class AuthFilter implements Filter {

    public static final String AUTH_USER_ID = "authUserId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String path = req.getRequestURI().substring(contextPath.length());

        if (isPublicPath(path) || isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (isAuthenticated(req.getSession(false))) {
            chain.doFilter(request, response);
            return;
        }

        if (path.endsWith(".xhtml") || path.isEmpty() || "/".equals(path)) {
            res.sendRedirect(contextPath + "/login.xhtml");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute(AUTH_USER_ID) != null;
    }

    private boolean isPublicPath(String path) {
        return "/login.xhtml".equals(path)
                || "/register.xhtml".equals(path)
                || "/index.html".equals(path);
    }

    private boolean isStaticResource(String path) {
        return path.contains("/jakarta.faces.resource/")
                || path.contains("/resources/")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".woff2")
                || path.endsWith(".woff")
                || path.endsWith(".ttf")
                || path.endsWith(".eot")
                || path.endsWith(".ico")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".gif")
                || path.endsWith(".svg");
    }
}
