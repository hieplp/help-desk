package dev.hieplp.helpdesk.security.principal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated {@link Caller} (user id + role) into a controller parameter.
 * Usage: {@code @GetMapping ... list(@CurrentCaller Caller caller)}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentCaller {
}
