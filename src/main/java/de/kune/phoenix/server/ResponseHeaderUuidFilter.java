package de.kune.phoenix.server;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static java.util.UUID.randomUUID;

/**
 * Created by alexander on 25.01.2017.
 */
@WebFilter
public class ResponseHeaderUuidFilter implements Filter {

    private static final UUID UUID = randomUUID();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ((HttpServletResponse)response).addHeader("X-Server-Identifier", UUID.toString());
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
