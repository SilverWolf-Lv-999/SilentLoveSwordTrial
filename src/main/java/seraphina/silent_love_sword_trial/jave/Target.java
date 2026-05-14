package seraphina.silent_love_sword_trial.jave;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Target {
    String obfuscated();

    String desc();

    boolean isField() default false;
}
