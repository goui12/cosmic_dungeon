package net.minecraft.server.jsonrpc.security;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;

@Sharable
public class AuthenticationHandler extends ChannelInboundHandlerAdapter {
    private final Logger LOGGER = LogUtils.getLogger();
    private static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf("authenticated");
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private final SecurityConfig securityConfig;

    public AuthenticationHandler(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object request) throws Exception {
        String s = this.getClientIp(context);
        if (request instanceof HttpRequest httprequest) {
            AuthenticationHandler.SecurityCheckResult authenticationhandler$securitycheckresult = this.performSecurityChecks(httprequest);
            if (!authenticationhandler$securitycheckresult.isAllowed()) {
                this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", s, authenticationhandler$securitycheckresult.getReason());
                context.channel().attr(AUTHENTICATED_KEY).set(false);
                this.sendUnauthorizedResponse(context, authenticationhandler$securitycheckresult.getReason());
                return;
            }

            context.channel().attr(AUTHENTICATED_KEY).set(true);
        }

        Boolean obool = context.channel().attr(AUTHENTICATED_KEY).get();
        if (Boolean.TRUE.equals(obool)) {
            super.channelRead(context, request);
        } else {
            this.LOGGER.debug("Dropping unauthenticated connection with ip {}", s);
            context.close();
        }
    }

    private AuthenticationHandler.SecurityCheckResult performSecurityChecks(HttpRequest request) {
        return !this.validateAuthentication(request)
            ? AuthenticationHandler.SecurityCheckResult.denied("Invalid or missing API key")
            : AuthenticationHandler.SecurityCheckResult.allowed();
    }

    private boolean validateAuthentication(HttpRequest request) {
        String s = request.headers().get("Authorization");
        if (s == null || s.trim().isEmpty()) {
            return false;
        } else if (s.startsWith("Bearer ")) {
            String s1 = s.substring("Bearer ".length()).trim();
            return this.isValidApiKey(s1);
        } else {
            return false;
        }
    }

    public boolean isValidApiKey(String apiKey) {
        if (apiKey != null && !apiKey.isEmpty()) {
            byte[] abyte = apiKey.getBytes(StandardCharsets.UTF_8);
            byte[] abyte1 = this.securityConfig.secretKey().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(abyte, abyte1);
        } else {
            return false;
        }
    }

    private String getClientIp(ChannelHandlerContext context) {
        InetSocketAddress inetsocketaddress = (InetSocketAddress)context.channel().remoteAddress();
        return inetsocketaddress.getAddress().getHostAddress();
    }

    private void sendUnauthorizedResponse(ChannelHandlerContext context, String message) {
        String s = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        byte[] abyte = s.getBytes(StandardCharsets.UTF_8);
        DefaultFullHttpResponse defaultfullhttpresponse = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(abyte)
        );
        defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, abyte.length);
        defaultfullhttpresponse.headers().set(HttpHeaderNames.CONNECTION, "close");
        context.writeAndFlush(defaultfullhttpresponse).addListener(p_449756_ -> context.close());
    }

    static class SecurityCheckResult {
        private final boolean allowed;
        private final String reason;

        private SecurityCheckResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static AuthenticationHandler.SecurityCheckResult allowed() {
            return new AuthenticationHandler.SecurityCheckResult(true, null);
        }

        public static AuthenticationHandler.SecurityCheckResult denied(String reason) {
            return new AuthenticationHandler.SecurityCheckResult(false, reason);
        }

        public boolean isAllowed() {
            return this.allowed;
        }

        public String getReason() {
            return this.reason;
        }
    }
}
