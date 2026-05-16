package seraphina.silent_love_sword_trial.util;

import seraphina.silent_love_sword_trial.ModSource;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassUtil {
	public static Map<String, Class<?>> hiddenClassMap = new HashMap<>();
	public static MethodHandle defineClass;
	public static MethodHandle defineHiddenClass;
	public static final MethodHandles.Lookup LOOKUP = getLookup();

	public static boolean isModClass(Class<?> clazz) {
		String filePath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
		if (!filePath.contains("/mods/") || !filePath.contains("\\mods\\")) return false;
		if (filePath.contains("/libraries/") || filePath.contains("\\libraries\\")) return false;
		return !clazz.getName().contains("seraphina.silent_love_sword_trial.");
	}

	public enum ClassOption {
		NESTMATE(1), STRONG(4);

		private final int flag;

		ClassOption(int flag) {
			this.flag = flag;
		}

		public static int optionsToFlag(Set<ClassOption> options) {
			int flags = 0;
			for (ClassOption cp : options) {
				flags |= cp.flag;
			}
			return flags;
		}
	}

	public static MethodHandles.Lookup getLookup() {
		try {
			return (MethodHandles.Lookup) ModUtil.INSTANCE.getUnsafe().getObjectVolatile(
					ModUtil.INSTANCE.getUnsafe().staticFieldBase(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")),
					ModUtil.INSTANCE.getUnsafe().staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP"))
			);
		} catch (Exception e) {
			try {
				Constructor<MethodHandles.Lookup> c = MethodHandles.Lookup.class.getDeclaredConstructor();
				c.setAccessible(true);
				return c.newInstance();
			} catch (Throwable var3) {
				var3.printStackTrace();
			}
		}
		return null;
	}

	static {
		try {
			if (LOOKUP != null) {
				defineClass = LOOKUP.findVirtual(ClassLoader.class, "defineClass", MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class));
				defineHiddenClass = LOOKUP.findStatic(ClassLoader.class, "defineClass0", MethodType.methodType(Class.class, ClassLoader.class, Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class, boolean.class, int.class, Object.class));
			}
		} catch (Throwable e) {
			throw new Error("Could not init ClassUtil", e);
		}
	}

	public static Class<?> defineClass(String name, ClassLoader loader, byte[] b, int off, int len, ProtectionDomain pd) {
		try {
			return (Class<?>) defineClass.invoke(loader, name, b, off, len, pd);
		} catch (Throwable e) {
			try {
				Class.forName(name);
				return Class.forName(name);
			} catch (ClassNotFoundException ignored) {
			}
			throw new Error(e);
		}
	}

	public static Class<?> defineClass(ClassLoader loader, String name, byte[] buf) {
		try {
			return (Class<?>) defineClass.invoke(loader, name, buf, 0, buf.length, null, null);
		} catch (Throwable e1) {
			try {
				return Class.forName(name);
			} catch (Exception e) {
				e1.addSuppressed(e);
				throw new RuntimeException(e1);
			}
		}
	}

	public static Class<?> definePackageClass(String name, Class<?> lookup, ClassLoader loader) {
		try {
			InputStream is = lookup.getResourceAsStream("/" + name.replace('.', '/') + ".class");
			byte[] dat = new byte[is.available()];
			is.read(dat);
			is.close();
			Objects.requireNonNull(dat);
			if (loader == null)
				loader = lookup.getClassLoader();
			return (Class<?>) defineClass.invoke(loader, name, dat, 0, dat.length, null);
		} catch (Throwable e) {
			try {
				if (Class.forName(name) != null) {
					return Class.forName(name);
				}
			} catch (ClassNotFoundException e1) {}
			throw new Error(e);
		}
	}

	public static Class<?> defineHiddenClass(String name, ClassLoader loader, Class<?> lookup, byte[] b, int off, int len, boolean initialize, ClassOption... options) {
		try {
			if (hiddenClassMap.containsKey(name) && hiddenClassMap.get(name) != null) {
				return hiddenClassMap.get(name);
			} else {
				Objects.requireNonNull(options);
				int flags = 2 | ClassOption.optionsToFlag(Set.of(options));
				if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
					flags |= 8;
				}
				return (Class<?>) defineHiddenClass.invoke(loader, lookup, name, b, off, len, null, initialize, flags, null);
			}
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

	public static Class<?> defineHiddenPackageClass(String name, ClassLoader loader, Class<?> lookup, boolean initialize, ClassOption... options) {
		try {
			if (hiddenClassMap.containsKey(name) && hiddenClassMap.get(name) != null) {
				return hiddenClassMap.get(name);
			} else {
				InputStream is = lookup.getResourceAsStream("/" + name.replace('.', '/') + ".class");
				byte[] dat = new byte[is.available()];
				is.read(dat);
				is.close();
				Objects.requireNonNull(dat);
				Objects.requireNonNull(options);
				int flags = 2 | ClassOption.optionsToFlag(Set.of(options));
				if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
					flags |= 8;
				}
				Class<?> klass = (Class<?>) defineHiddenClass.invoke(loader, lookup, name, dat, 0, dat.length, null, initialize, flags, null);
				hiddenClassMap.put(name, klass);
				return klass;
			}
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

	public static Class<?> defineHiddenPackageClass(String name, Class<?> lookup, boolean initialize, ClassOption... options) {
		try {
			if (hiddenClassMap.containsKey(name) && hiddenClassMap.get(name) != null) {
				return hiddenClassMap.get(name);
			} else {
				ClassLoader loader = lookup.getClassLoader();
				InputStream is = lookup.getResourceAsStream("/" + name.replace('.', '/') + ".class");
				byte[] dat = new byte[is.available()];
				is.read(dat);
				is.close();
				Objects.requireNonNull(dat);
				Objects.requireNonNull(options);
				int flags = 2 | ClassOption.optionsToFlag(Set.of(options));
				if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
					flags |= 8;
				}
				Class<?> klass = (Class<?>) defineHiddenClass.invoke(loader, lookup, name, dat, 0, dat.length, null, initialize, flags, null);
				hiddenClassMap.put(name, klass);
				return klass;
			}
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

	public static Class<?> defineHiddenPackageClass(String name) {
		return defineHiddenPackageClass(name, ModSource.class, true, ClassOption.STRONG);
	}
}