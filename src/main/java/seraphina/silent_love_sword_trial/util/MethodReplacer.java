package seraphina.silent_love_sword_trial.util;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import seraphina.silent_love_sword_trial.jave.Target;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("all")
public final class MethodReplacer {
    private static final Unsafe UNSAFE = ModUtil.INSTANCE.getUnsafe();

    public static final class Config {
        public long methodAccessFlags = 0x18L;
        public long methodFromInterpreted = 0x30L;
        public long methodCode = 0x38L;
        public long methodFromCompiled = 0x40L;
        public boolean useNativeFlag = false;
        public int nativeFlag = 0x0100;
    }

    public static final Config CONFIG = new Config();

    private static volatile boolean jniReady = false;
    private static Pointer javaVM = null;
    private static long fromReflectedMethodAddr = 0;

    private static final MethodHandles.Lookup FULL_LOOKUP = getFullLookup();

    private static final VarHandle VH_METHOD_ACCESSOR;
    private static final VarHandle VH_METHOD_ROOT;
    private static final VarHandle VH_DMH_MEMBER;
    private static final VarHandle VH_MEMBER_VMINDEX;

    private static final Map<String, Hook> HOOKS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<Hook> HOOK_LIST = new CopyOnWriteArrayList<>();
    private static volatile boolean daemonRunning = false;

    static {
        VarHandle acc = null, root = null, dmhMember = null, vmindex = null;
        try {
            Class<?> accessorClass = Class.forName("jdk.internal.reflect.MethodAccessor");
            acc = FULL_LOOKUP.findVarHandle(Method.class, "methodAccessor", accessorClass);
        } catch (Throwable e) {
            System.err.println("[MethodReplacer] VH methodAccessor: " + e);
        }
        try {
            root = FULL_LOOKUP.findVarHandle(Method.class, "root", Method.class);
        } catch (Throwable e) {
            System.err.println("[MethodReplacer] VH root: " + e);
        }
        try {
            Class<?> dmhClass = Class.forName("java.lang.invoke.DirectMethodHandle");
            Class<?> memberNameClass = Class.forName("java.lang.invoke.MemberName");
            dmhMember = FULL_LOOKUP.findVarHandle(dmhClass, "member", memberNameClass);
            vmindex = FULL_LOOKUP.findVarHandle(memberNameClass, "vmindex", long.class);
        } catch (Throwable e) {
            System.err.println("[MethodReplacer] VH MemberName: " + e);
        }

        VH_METHOD_ACCESSOR = acc;
        VH_METHOD_ROOT = root;
        VH_DMH_MEMBER = dmhMember;
        VH_MEMBER_VMINDEX = vmindex;

        initJni();
    }

    private static void initJni() {
        try {
            Function getVMs = Function.getFunction("jvm", "JNI_GetCreatedJavaVMs");
            Memory vmBuf = new Memory(Native.POINTER_SIZE);
            IntByReference nVMs = new IntByReference();
            int result = getVMs.invokeInt(new Object[]{vmBuf, 1, nVMs});
            if (result != 0 || nVMs.getValue() == 0) {
                System.err.println("[MethodReplacer] JNI_GetCreatedJavaVMs failed, result=" + result);
                return;
            }
            javaVM = vmBuf.getPointer(0);

            Pointer vtable = javaVM.getPointer(0);
            Pointer getEnvPtr = vtable.getPointer(6 * Native.POINTER_SIZE);
            Function getEnv = Function.getFunction(getEnvPtr);
            PointerByReference envRef = new PointerByReference();
            result = getEnv.invokeInt(new Object[]{javaVM, envRef, 0x00010008});
            if (result != 0) {
                System.err.println("[MethodReplacer] GetEnv failed, result=" + result);
                return;
            }
            Pointer env = envRef.getValue();

            Pointer jniFunctions = env.getPointer(0);
            Pointer frmPtr = jniFunctions.getPointer(7 * Native.POINTER_SIZE);
            fromReflectedMethodAddr = Pointer.nativeValue(frmPtr);

            jniReady = true;
            System.out.println("[MethodReplacer] JNI initialized. FromReflectedMethod=0x" + Long.toHexString(fromReflectedMethodAddr));

        } catch (Throwable e) {
            System.err.println("[MethodReplacer] JNA JNI init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Pointer getCurrentJniEnv() {
        if (javaVM == null) throw new IllegalStateException("JavaVM not initialized");
        try {
            Pointer vtable = javaVM.getPointer(0);
            Pointer getEnvPtr = vtable.getPointer(6 * Native.POINTER_SIZE);
            Function getEnv = Function.getFunction(getEnvPtr);
            PointerByReference envRef = new PointerByReference();
            int result = getEnv.invokeInt(new Object[]{javaVM, envRef, 0x00010008});
            if (result == 0) return envRef.getValue();

            if (result == -2) {
                Pointer attachPtr = vtable.getPointer(4 * Native.POINTER_SIZE);
                Function attach = Function.getFunction(attachPtr);
                result = attach.invokeInt(new Object[]{javaVM, envRef, null});
                if (result == 0) return envRef.getValue();
            }
            throw new Error("GetEnv/AttachCurrentThread failed with code " + result);
        } catch (Throwable e) {
            throw new Error("Failed to obtain JNI environment for current thread", e);
        }
    }

    public static void replaceMethod(Class<?> targetClass, Class<?> replacementClass,
                                     String methodName, Class<?>... paramTypes) {
        try {
            Method replacement = replacementClass.getDeclaredMethod(methodName, paramTypes);
            replacement.setAccessible(true);

            Method target = resolveTarget(targetClass, replacement);
            if (target == null) {
                throw new NoSuchMethodException(
                        "Cannot find target in " + targetClass.getName() +
                                " for " + replacementClass.getName() + "." + methodName);
            }

            registerAndApply(target, replacement);
        } catch (Throwable e) {
            throw new Error("[MethodReplacer] replaceMethod failed: " + e.getMessage(), e);
        }
    }

    public static void replaceMethods(Class<?> targetClass, Class<?> replacementClass) {
        for (Method replacement : replacementClass.getDeclaredMethods()) {
            Target anno = replacement.getAnnotation(Target.class);
            if (anno == null || anno.isField()) continue;

            replacement.setAccessible(true);
            Method target = findTargetByAnnotation(targetClass, anno);
            if (target == null) continue;

            try {
                registerAndApply(target, replacement);
            } catch (Throwable e) {
                System.err.println("[MethodReplacer] Failed to hook " + replacement.getName());
                e.printStackTrace();
            }
        }
    }

    public static void reapplyAll() {
        for (Hook hook : HOOK_LIST) {
            try {
                hook.apply();
            } catch (Throwable ignored) {}
        }
    }

    public static synchronized void startDaemon(long intervalMs) {
        if (daemonRunning) return;
        daemonRunning = true;
        Thread t = new Thread(() -> {
            while (!Thread.interrupted()) {
                reapplyAll();
                try { Thread.sleep(intervalMs); } catch (InterruptedException e) { break; }
            }
        }, "MethodReplaceDaemon");
        t.setDaemon(true);
        t.start();
    }

    private static void registerAndApply(Method target, Method replacement) throws Throwable {
        String key = target.getDeclaringClass().getName() + "." + target.getName()
                + "->" + replacement.getDeclaringClass().getName() + "." + replacement.getName();
        Hook hook = HOOKS.computeIfAbsent(key, k -> {
            try {
                return new Hook(target, replacement);
            } catch (Throwable e) {
                throw new Error(e);
            }
        });
        if (!HOOK_LIST.contains(hook)) {
            HOOK_LIST.add(hook);
        }
        hook.apply();
    }

    private static final class Hook {
        final Method target;
        final Method replacement;
        final long targetPtr;
        final long trampAddr;
        final int origAccessFlags;

        Hook(Method target, Method replacement) throws Throwable {
            this.target = target;
            this.replacement = replacement;

            warmUp(target);
            warmUp(replacement);

            this.targetPtr = getMethodPointer(target);
            if (targetPtr == 0 || targetPtr < 0x10000) {
                throw new Error("Invalid target Method* pointer: 0x" + Long.toHexString(targetPtr));
            }

            long replPtr = getMethodPointer(replacement);
            long replEntry = UNSAFE.getLong(replPtr + CONFIG.methodFromInterpreted);
            if (replEntry == 0L) {
                replEntry = UNSAFE.getLong(replPtr + CONFIG.methodFromCompiled);
            }
            if (replEntry == 0L) {
                throw new Error("Replacement method entry is zero. Call it once before hooking.");
            }

            this.origAccessFlags = UNSAFE.getInt(targetPtr + CONFIG.methodAccessFlags);
            this.trampAddr = generateTrampoline(replEntry);
        }

        void apply() {
            if (targetPtr == 0) return;

            UNSAFE.putLong(targetPtr + CONFIG.methodCode, 0L);
            UNSAFE.putLong(targetPtr + CONFIG.methodFromInterpreted, trampAddr);
            UNSAFE.putLong(targetPtr + CONFIG.methodFromCompiled, trampAddr);

            if (CONFIG.useNativeFlag) {
                UNSAFE.putInt(targetPtr + CONFIG.methodAccessFlags, origAccessFlags | CONFIG.nativeFlag);
            }

            replaceAccessor(target, replacement);
        }
    }

    private static long getMethodPointer(Method method) {
        if (VH_DMH_MEMBER != null && VH_MEMBER_VMINDEX != null) {
            try {
                return getMethodPointerViaMemberName(method);
            } catch (Throwable e) {
                System.err.println("[MethodReplacer] MemberName path failed:");
                e.printStackTrace();
            }
        }

        if (!jniReady) {
            throw new IllegalStateException("JNI not initialized and MemberName path failed");
        }
        try {
            long oop = getObjectAddress(method);
            Memory handleMem = new Memory(Native.POINTER_SIZE);
            if (Native.POINTER_SIZE == 8) {
                handleMem.setLong(0, oop);
            } else {
                handleMem.setInt(0, (int) oop);
            }

            Pointer env = getCurrentJniEnv();
            Function frm = Function.getFunction(new Pointer(fromReflectedMethodAddr));
            long ptr = frm.invokeLong(new Object[]{ env, handleMem });

            if (ptr == 0 || ptr < 0x10000) {
                throw new Error("FromReflectedMethod returned invalid pointer: 0x" + Long.toHexString(ptr));
            }
            return ptr;
        } catch (Throwable e) {
            throw new Error("getMethodPointer failed for " + method, e);
        }
    }

    private static long getMethodPointerViaMemberName(Method method) {
        try {
            MethodHandle mh = FULL_LOOKUP.unreflect(method);

            Class<?> dmhClass = Class.forName("java.lang.invoke.DirectMethodHandle");
            if (!dmhClass.isInstance(mh)) {
                throw new Error("Not a DirectMethodHandle: " + mh.getClass());
            }

            Object member = VH_DMH_MEMBER.get(mh);
            if (member == null) {
                throw new Error("MemberName is null");
            }

            long vmindex = (long) VH_MEMBER_VMINDEX.get(member);
            if (vmindex == 0 || vmindex < 0x10000) {
                throw new Error("Invalid vmindex: 0x" + Long.toHexString(vmindex));
            }

            return vmindex;
        } catch (Throwable e) {
            throw new Error("getMethodPointerViaMemberName failed", e);
        }
    }

    private static long getObjectAddress(Object obj) {
        if (obj == null) return 0;
        Object[] holder = new Object[]{obj};
        long baseOffset = UNSAFE.arrayBaseOffset(Object[].class);
        int scale = UNSAFE.arrayIndexScale(Object[].class);

        if (scale == 4) {
            int compressed = UNSAFE.getInt(holder, baseOffset);
            return (compressed & 0xFFFFFFFFL) << 3;
        } else if (scale == 8) {
            return UNSAFE.getLong(holder, baseOffset);
        } else {
            throw new Error("Unsupported OOP size: " + scale);
        }
    }

    private static void replaceAccessor(Method target, Method replacement) {
        if (VH_METHOD_ACCESSOR == null) return;
        try {
            Object bridge = createBridge(target, replacement);
            VH_METHOD_ACCESSOR.set(target, bridge);

            if (VH_METHOD_ROOT != null) {
                Method root = (Method) VH_METHOD_ROOT.get(target);
                if (root != null && root != target) {
                    VH_METHOD_ACCESSOR.set(root, bridge);
                }
            }
        } catch (Throwable e) {
            System.err.println("[MethodReplacer] replaceAccessor failed: " + e.getMessage());
        }
    }

    private static Object createBridge(Method target, Method replacement) throws Throwable {
        MethodHandle mh = FULL_LOOKUP.unreflect(replacement);
        Class<?> acc = Class.forName("jdk.internal.reflect.MethodAccessor");

        return Proxy.newProxyInstance(
                MethodReplacer.class.getClassLoader(),
                new Class<?>[]{acc},
                (proxy, m, a) -> {
                    if (!"invoke".equals(m.getName())) {
                        if ("equals".equals(m.getName())) return proxy == a[0];
                        if ("hashCode".equals(m.getName())) return System.identityHashCode(proxy);
                        return "BridgeAccessor";
                    }
                    Object obj = a[0];
                    Object[] args = (Object[]) a[1];
                    Object[] fin;

                    boolean ts = Modifier.isStatic(target.getModifiers());
                    boolean rs = Modifier.isStatic(replacement.getModifiers());

                    if (ts && rs) {
                        fin = args;
                    } else if (!ts && !rs) {
                        fin = new Object[args.length + 1];
                        fin[0] = obj;
                        System.arraycopy(args, 0, fin, 1, args.length);
                    } else if (!ts && rs) {
                        Class<?>[] rp = replacement.getParameterTypes();
                        if (rp.length == target.getParameterCount() + 1
                                && (obj == null || rp[0].isInstance(obj))) {
                            fin = new Object[args.length + 1];
                            fin[0] = obj;
                            System.arraycopy(args, 0, fin, 1, args.length);
                        } else {
                            fin = args;
                        }
                    } else {
                        Class<?>[] rp = replacement.getParameterTypes();
                        if (args.length > 0 && (args[0] == null || rp[0].isInstance(args[0]))) {
                            fin = args;
                        } else {
                            fin = new Object[args.length + 1];
                            fin[0] = obj;
                            System.arraycopy(args, 0, fin, 1, args.length);
                        }
                    }

                    try {
                        return mh.invokeWithArguments(fin);
                    } catch (Throwable t) {
                        if (t instanceof RuntimeException) throw (RuntimeException) t;
                        if (t instanceof Error) throw (Error) t;
                        throw new InvocationTargetException(t);
                    }
                }
        );
    }

    private static MethodHandles.Lookup getFullLookup() {
        try {
            java.lang.reflect.Field impl = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            return (MethodHandles.Lookup) UNSAFE.getObject(
                    UNSAFE.staticFieldBase(impl), UNSAFE.staticFieldOffset(impl));
        } catch (Throwable e1) {
            try {
                Constructor<MethodHandles.Lookup> c =
                        MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                c.setAccessible(true);
                return c.newInstance(Object.class, -1);
            } catch (Throwable e2) {
                return MethodHandles.lookup();
            }
        }
    }

    private static long generateTrampoline(long destEntry) {
        long mem = UNSAFE.allocateMemory(32);
        UNSAFE.setMemory(mem, 32, (byte) 0xCC);

        int p = 0;
        UNSAFE.putByte(mem + p++, (byte) 0x48);
        UNSAFE.putByte(mem + p++, (byte) 0xB8);
        UNSAFE.putLong(mem + p, destEntry);
        p += 8;
        UNSAFE.putByte(mem + p++, (byte) 0xFF);
        UNSAFE.putByte(mem + p++, (byte) 0xE0);
        UNSAFE.putByte(mem + p++, (byte) 0xC3);

        markExecutable(mem, 32);
        return mem;
    }

    private static void markExecutable(long addr, int size) {
        try {
            Function virtualProtect = Function.getFunction("kernel32", "VirtualProtect");
            Memory oldProt = new Memory(4);
            virtualProtect.invokeLong(new Object[]{
                    new Pointer(addr),
                    new BaseTSD.SIZE_T(size),
                    0x40,
                    oldProt
            });
        } catch (Throwable e) {
            System.err.println("[MethodReplacer] WARN: VirtualProtect failed: " + e.getMessage());
        }
    }

    private static void warmUp(Method method) {
        try {
            Object[] dummies = dummyArgs(method.getParameterTypes());
            if (Modifier.isStatic(method.getModifiers())) {
                method.invoke(null, dummies);
            } else {
                Object inst = UNSAFE.allocateInstance(method.getDeclaringClass());
                method.invoke(inst, dummies);
            }
        } catch (Throwable ignored) {}
    }

    private static Object[] dummyArgs(Class<?>[] types) {
        Object[] a = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (!t.isPrimitive()) a[i] = null;
            else if (t == boolean.class) a[i] = false;
            else if (t == int.class || t == short.class || t == byte.class || t == char.class) a[i] = 0;
            else if (t == long.class) a[i] = 0L;
            else if (t == float.class) a[i] = 0.0f;
            else if (t == double.class) a[i] = 0.0;
        }
        return a;
    }

    private static Method resolveTarget(Class<?> targetClass, Method replacement) {
        Target anno = replacement.getAnnotation(Target.class);
        if (anno != null && !anno.isField()) {
            Method m = findTargetByAnnotation(targetClass, anno);
            if (m != null) return m;
        }
        try {
            Class<?>[] oldParams = inferOldParams(replacement, targetClass);
            return targetClass.getDeclaredMethod(replacement.getName(), oldParams);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findTargetByAnnotation(Class<?> targetClass, Target anno) {
        String obf = anno.obfuscated();
        String desc = anno.desc();
        for (Method m : targetClass.getDeclaredMethods()) {
            if (m.getName().equals(obf) && getDescriptor(m).equals(desc)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static String getDescriptor(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Class<?> p : m.getParameterTypes()) sb.append(desc(p));
        sb.append(')');
        sb.append(desc(m.getReturnType()));
        return sb.toString();
    }

    private static String desc(Class<?> c) {
        if (c == void.class) return "V";
        if (c == boolean.class) return "Z";
        if (c == byte.class) return "B";
        if (c == char.class) return "C";
        if (c == short.class) return "S";
        if (c == int.class) return "I";
        if (c == long.class) return "J";
        if (c == float.class) return "F";
        if (c == double.class) return "D";
        if (c.isArray()) return "[" + desc(c.getComponentType());
        return "L" + c.getName().replace('.', '/') + ";";
    }

    private static Class<?>[] inferOldParams(Method replacement, Class<?> targetClass) {
        Class<?>[] rp = replacement.getParameterTypes();
        if (Modifier.isStatic(replacement.getModifiers()) && rp.length > 0
                && (rp[0] == targetClass || targetClass.isAssignableFrom(rp[0]))) {
            Class<?>[] op = new Class<?>[rp.length - 1];
            System.arraycopy(rp, 1, op, 0, op.length);
            return op;
        }
        return rp;
    }
}