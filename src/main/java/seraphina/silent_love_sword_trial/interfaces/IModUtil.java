package seraphina.silent_love_sword_trial.interfaces;

import sun.misc.Unsafe;

public interface IModUtil {
    void klassPtr(Object object, Class<?> klass);

    void loadSilent();

    Unsafe getUnsafe();
}
