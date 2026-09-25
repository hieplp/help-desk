package dev.hieplp.helpdesk.config;

import dev.hieplp.helpdesk.security.principal.Caller;
import dev.hieplp.helpdesk.security.principal.CurrentCaller;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC wiring: registers the resolver behind {@link CurrentCaller}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Adds {@link CallerArgumentResolver} to the resolver chain.
     */
    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CallerArgumentResolver());
    }

    /**
     * Resolves {@code @CurrentCaller Caller} parameters from the
     * {@link SecurityContextHolder} authentication.
     */
    static class CallerArgumentResolver implements HandlerMethodArgumentResolver {

        /**
         * Supports only parameters annotated {@link CurrentCaller} of type {@link Caller}.
         */
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentCaller.class)
                    && parameter.getParameterType() == Caller.class;
        }

        /**
         * Builds the {@link Caller} from the current authentication.
         */
        @Override
        public @Nullable Object resolveArgument(
                @NonNull MethodParameter parameter,
                @Nullable ModelAndViewContainer mavContainer,
                @NonNull NativeWebRequest webRequest,
                @Nullable WebDataBinderFactory binderFactory
        ) {
            return Caller.from(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
