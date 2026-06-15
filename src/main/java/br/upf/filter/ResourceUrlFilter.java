package br.upf.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ResourceUrlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            ResponseWrapper wrapper = new ResponseWrapper(httpResponse);
            chain.doFilter(request, wrapper);
            
            String content = wrapper.getContent();
            // Remove .xhtml from resource URLs
            content = content.replaceAll(
                "(/jakarta\\.faces\\.resource/[^\"'\\s]+)\\.xhtml(\\?[^\"'\\s]*)",
                "$1$2"
            );
            
            response.setContentLength(content.length());
            response.getWriter().write(content);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {}

    @Override
    public void destroy() {}

    private static class ResponseWrapper extends HttpServletResponseWrapper {
        private StringWriter sw = new StringWriter();
        private PrintWriter pw = new PrintWriter(sw);

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return pw;
        }

        public String getContent() {
            pw.flush();
            return sw.toString();
        }
    }
}
