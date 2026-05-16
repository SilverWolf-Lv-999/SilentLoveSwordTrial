package seraphina.silent_love_sword_trial.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class PreciseFieldBackTrackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreciseFieldBackTrackManager.class);

    private final Map<String, Map<String, CapturedValue>> capturedData = new ConcurrentHashMap<>();

    private final Set<Class<?>> trackedClasses = ConcurrentHashMap.newKeySet();

    private final Set<String> trackedClassNames = ConcurrentHashMap.newKeySet();

    private final Set<String> scannedModIds = ConcurrentHashMap.newKeySet();

    private static final String[] BLACKLIST_PREFIXES = {
            "seraphina.silent_love_sword_trail."
    };

    public PreciseFieldBackTrackManager() {
        Thread thread = new Thread(this::delayedAutoScan, "ModAutoScanner");
        thread.start();
    }

    private void delayedAutoScan() {
        try {
            Thread.sleep(2000);

            Object modList = Class.forName("net.minecraftforge.fml.ModList")
                    .getMethod("get").invoke(null);

            if (modList == null) {
                LOGGER.error("[PreciseBackTrack] ModList 尚未初始化");
                return;
            }

            List<?> modFiles = (List<?>) modList.getClass()
                    .getMethod("getModFiles").invoke(modList);

            LOGGER.info("[PreciseBackTrack] 发现 {} 个 Mod 文件", modFiles.size());

            if (modFiles.size() > 30) {
                LOGGER.warn("[PreciseBackTrack] Mod 数量超过 30 个 ({})，跳过类扫描记录", modFiles.size());
                startRuntimeMonitoring();
                return;
            }

            int totalClasses = 0;
            for (Object modFileInfo : modFiles) {
                try {
                    totalClasses += scanModFile(modFileInfo);
                } catch (Exception e) {
                    LOGGER.debug("[PreciseBackTrack] 扫描 Mod 文件失败: {}", e.getMessage());
                }
            }

            LOGGER.info("[PreciseBackTrack] 自动扫描完成，共捕获 {} 个类的静态字段", totalClasses);
            startRuntimeMonitoring();

        } catch (Exception e) {
            LOGGER.error("[PreciseBackTrack] 自动扫描失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private int scanModFile(Object modFileInfo) throws Exception {
        Object modFile = modFileInfo.getClass()
                .getMethod("getFile").invoke(modFileInfo);

        if (modFile == null) return 0;

        String modId = "unknown";
        try {
            List<?> mods = (List<?>) modFileInfo.getClass()
                    .getMethod("getMods").invoke(modFileInfo);
            if (!mods.isEmpty()) {
                modId = (String) mods.get(0).getClass()
                        .getMethod("getModId").invoke(mods.get(0));
            }
        } catch (Exception ignored) {}

        if ("minecraft".equals(modId) || "forge".equals(modId) || "test_sword".equals(modId)) {
            LOGGER.debug("[PreciseBackTrack] 跳过核心 Mod [{}]", modId);
            return 0;
        }

        if (scannedModIds.contains(modId)) return 0;
        scannedModIds.add(modId);

        LOGGER.debug("[PreciseBackTrack] 扫描 Mod [{}]", modId);

        int forgeScanCount = scanViaForgeData(modFile, modId);
        if (forgeScanCount > 0) return forgeScanCount;
        return scanViaJarFile(modFile, modId);
    }

    private int scanViaForgeData(Object modFile, String modId) {
        try {
            Object scanData = modFile.getClass()
                    .getMethod("getScanResult").invoke(modFile);
            if (scanData == null) return 0;

            Set<?> classes = (Set<?>) scanData.getClass()
                    .getMethod("getClasses").invoke(scanData);
            if (classes == null || classes.isEmpty()) return 0;

            LOGGER.debug("[PreciseBackTrack] Mod [{}] 有 {} 个预扫描类", modId, classes.size());

            int count = 0;
            ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();

            for (Object classData : classes) {
                try {
                    String className = (String) classData.getClass().getMethod("getClassName").invoke(classData);
                    if (className == null || className.contains("$")) continue;
                    if (isBlacklisted(className)) continue;

                    Class<?> clazz = Class.forName(className, false, ctxLoader);
                    if (captureClassFromModScan(clazz)) count++;
                } catch (Throwable ignored) {}
            }

            LOGGER.info("[PreciseBackTrack] Mod [{}] 通过 Forge 数据捕获 {} 个类", modId, count);
            return count;

        } catch (Exception e) {
            LOGGER.debug("[PreciseBackTrack] Forge 数据扫描失败: {}", e.getMessage());
            return 0;
        }
    }

    private int scanViaJarFile(Object modFile, String modId) {
        try {
            Path filePath = resolveModFilePath(modFile);

            if (filePath == null) {
                // Fallback: try to scan via SecureJar's root path (virtual filesystem)
                return scanViaSecureJar(modFile, modId);
            }

            if (!filePath.toFile().exists()) {
                LOGGER.debug("[PreciseBackTrack] Mod [{}] 文件不存在: {}", modId, filePath);
                return scanViaSecureJar(modFile, modId);
            }

            int count = 0;
            ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();

            try (JarFile jar = new JarFile(filePath.toFile())) {
                Enumeration<?> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String name = entry.getName();

                    if (!name.endsWith(".class") || name.contains("$")) continue;

                    String className = name.replace('/', '.').replace(".class", "");
                    if (isBlacklisted(className)) continue;

                    try {
                        Class<?> clazz = Class.forName(className, false, ctxLoader);
                        if (captureClassFromModScan(clazz)) count++;
                    } catch (Throwable ignored) {}
                }
            }

            LOGGER.info("[PreciseBackTrack] Mod [{}] 通过 Jar 扫描捕获 {} 个类", modId, count);
            return count;

        } catch (Exception e) {
            LOGGER.debug("[PreciseBackTrack] Jar 扫描失败: {}", e.getMessage());
            return scanViaSecureJar(modFile, modId);
        }
    }

    private Path resolveModFilePath(Object modFile) {
        // Strategy 1: getFilePath() (older Forge)
        try {
            Path path = (Path) modFile.getClass().getMethod("getFilePath").invoke(modFile);
            if (path != null && path.toFile().exists()) return path;
        } catch (Exception ignored) {}

        // Strategy 2: getSecureJar().getPrimaryPath() (newer Forge 1.20.x)
        try {
            Object secureJar = modFile.getClass().getMethod("getSecureJar").invoke(modFile);
            if (secureJar != null) {
                Path path = (Path) secureJar.getClass().getMethod("getPrimaryPath").invoke(secureJar);
                if (path != null) {
                    try {
                        if (path.toFile().exists()) return path;
                    } catch (UnsupportedOperationException e) {
                        // Virtual filesystem path - can't convert to File
                        // Try to extract real jar path from the virtual filesystem
                        Path extracted = extractRealJarPath(path);
                        if (extracted != null) return extracted;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Strategy 3: Extract jar path from SecureJar's UnionFileSystem via reflection
        try {
            Object secureJar = modFile.getClass().getMethod("getSecureJar").invoke(modFile);
            if (secureJar != null) {
                Path extracted = extractJarPathFromSecureJar(secureJar);
                if (extracted != null) return extracted;
            }
        } catch (Exception ignored) {}

        // Strategy 4: getSecureJar().getRootPath() -> resolve to actual jar
        try {
            Object secureJar = modFile.getClass().getMethod("getSecureJar").invoke(modFile);
            if (secureJar != null) {
                Path rootPath = (Path) secureJar.getClass().getMethod("getRootPath").invoke(secureJar);
                if (rootPath != null) {
                    Path extracted = extractRealJarPath(rootPath);
                    if (extracted != null) return extracted;
                }
            }
        } catch (Exception ignored) {}

        // Strategy 5: Try ProtectionDomain code source location
        try {
            String fileName = null;
            try {
                fileName = (String) modFile.getClass().getMethod("getFileName").invoke(modFile);
            } catch (Exception ignored) {}
            if (fileName != null) {
                LOGGER.debug("[PreciseBackTrack] 无法解析路径，文件名: {}", fileName);
                // Try to find the jar in known mod directories
                Path modsDir = Path.of("mods");
                if (modsDir.toFile().isDirectory()) {
                    Path candidate = modsDir.resolve(fileName);
                    if (candidate.toFile().exists()) return candidate;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Extract the real jar file path from a virtual filesystem path (UnionFileSystem).
     * UnionFileSystem wraps real jar paths as its backing store.
     */
    private Path extractRealJarPath(Path virtualPath) {
        if (virtualPath == null) return null;

        try {
            // Try URI-based extraction: jar:file:///path/to/mod.jar!/
            java.net.URI uri = virtualPath.toUri();
            String uriStr = uri.toString();
            Path jarPath = parseJarUri(uriStr);
            if (jarPath != null) return jarPath;
        } catch (Exception ignored) {}

        try {
            // Try to get the FileSystem and extract backing paths via reflection
            java.nio.file.FileSystem fs = virtualPath.getFileSystem();
            return extractJarPathFromFileSystem(fs);
        } catch (Exception ignored) {}

        // Try string-based parsing
        try {
            String pathStr = virtualPath.toString();
            // Some virtual paths contain the jar path
            if (pathStr.contains(".jar")) {
                int idx = pathStr.indexOf(".jar");
                String jarPart = pathStr.substring(0, idx + 4);
                // Remove leading separators or prefixes
                jarPart = jarPart.replaceAll("^[/\\\\]+", "");
                Path candidate = Path.of(jarPart);
                try {
                    if (candidate.toFile().exists()) return candidate;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Parse a jar: URI to extract the real file path.
     * e.g. "jar:file:///C:/mods/mod.jar!/" -> Path("C:/mods/mod.jar")
     */
    private Path parseJarUri(String uriStr) {
        if (uriStr == null) return null;
        try {
            // Handle jar:file:///path!/entry format
            if (uriStr.startsWith("jar:file:")) {
                String filePart = uriStr.substring(4); // remove "jar:"
                int bangIdx = filePart.indexOf('!');
                if (bangIdx > 0) {
                    filePart = filePart.substring(0, bangIdx);
                }
                java.net.URI fileUri = new java.net.URI(filePart);
                Path path = Path.of(fileUri);
                if (path.toFile().exists()) return path;
            }
            // Handle union:// or other custom schemes
            if (uriStr.contains("file:")) {
                int fileIdx = uriStr.indexOf("file:");
                String filePart = uriStr.substring(fileIdx);
                int bangIdx = filePart.indexOf('!');
                if (bangIdx > 0) {
                    filePart = filePart.substring(0, bangIdx);
                }
                // Remove trailing separators
                filePart = filePart.replaceAll("[/\\\\]+$", "");
                java.net.URI fileUri = new java.net.URI(filePart);
                Path path = Path.of(fileUri);
                if (path.toFile().exists()) return path;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Extract jar path from a FileSystem (likely UnionFileSystem) via reflection.
     * Tries to access internal fields that store the backing jar paths.
     */
    private Path extractJarPathFromFileSystem(java.nio.file.FileSystem fs) {
        if (fs == null) return null;
        String fsClassName = fs.getClass().getName();

        try {
            // Try cpw.mods.niofs.union.UnionFileSystem
            if (fsClassName.contains("UnionFileSystem")) {
                // Try "basepaths" or "basePath" fields
                for (String fieldName : new String[]{"basepaths", "basePath", "primaryPath", "paths"}) {
                    try {
                        Field f = findFieldInHierarchy(fs.getClass(), fieldName);
                        if (f == null) continue;
                        f.setAccessible(true);
                        Object val = f.get(fs);
                        Path result = extractPathFromFieldValue(val);
                        if (result != null) return result;
                    } catch (Exception ignored) {}
                }

                // Try all Path/Path[] fields
                for (Field f : fs.getClass().getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(fs);
                        Path result = extractPathFromFieldValue(val);
                        if (result != null) return result;
                    } catch (Exception ignored) {}
                }
            }

            // Try provider-based extraction
            try {
                var provider = fs.provider();
                if (provider != null) {
                    for (Field f : provider.getClass().getDeclaredFields()) {
                        try {
                            f.setAccessible(true);
                            Object val = f.get(provider);
                            Path result = extractPathFromFieldValue(val);
                            if (result != null) return result;
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            LOGGER.debug("[PreciseBackTrack] FileSystem 反射提取失败: {}", e.getMessage());
        }

        return null;
    }

    private Field findFieldInHierarchy(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Path extractPathFromFieldValue(Object val) {
        if (val == null) return null;

        if (val instanceof Path p) {
            try {
                if (p.toFile().exists() && p.toString().endsWith(".jar")) return p;
            } catch (Exception ignored) {}
        }

        if (val instanceof Path[] paths) {
            for (Path p : paths) {
                try {
                    if (p.toFile().exists() && p.toString().endsWith(".jar")) return p;
                } catch (Exception ignored) {}
            }
        }

        if (val instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item instanceof Path p) {
                    try {
                        if (p.toFile().exists() && p.toString().endsWith(".jar")) return p;
                    } catch (Exception ignored) {}
                }
            }
        }

        if (val instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                Path result = extractPathFromFieldValue(item);
                if (result != null) return result;
            }
        }

        return null;
    }

    /**
     * Extract jar path from SecureJar object via reflection.
     */
    private Path extractJarPathFromSecureJar(Object secureJar) {
        if (secureJar == null) return null;

        try {
            // Try to get the root path and extract from its filesystem
            Path rootPath = (Path) secureJar.getClass().getMethod("getRootPath").invoke(secureJar);
            if (rootPath != null) {
                Path extracted = extractRealJarPath(rootPath);
                if (extracted != null) return extracted;
            }
        } catch (Exception ignored) {}

        // Try reflection on SecureJar's internal fields
        try {
            for (Field f : secureJar.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(secureJar);
                    Path result = extractPathFromFieldValue(val);
                    if (result != null) return result;

                    // If it's a filesystem, try extracting from it
                    if (val instanceof java.nio.file.FileSystem fs) {
                        result = extractJarPathFromFileSystem(fs);
                        if (result != null) return result;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return null;
    }

    private int scanViaSecureJar(Object modFile, String modId) {
        try {
            Object secureJar = modFile.getClass().getMethod("getSecureJar").invoke(modFile);
            if (secureJar == null) return 0;

            Path rootPath = (Path) secureJar.getClass().getMethod("getRootPath").invoke(secureJar);
            if (rootPath == null) return 0;

            int count = 0;
            ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();

            // Walk the virtual filesystem to find .class files
            try (var stream = java.nio.file.Files.walk(rootPath)) {
                var classFiles = stream
                        .filter(p -> p.toString().endsWith(".class"))
                        .filter(p -> !p.toString().contains("$"))
                        .toList();

                for (Path classFile : classFiles) {
                    String relativePath = rootPath.relativize(classFile).toString();
                    String className = relativePath.replace('/', '.').replace('\\', '.').replace(".class", "");
                    if (isBlacklisted(className)) continue;

                    try {
                        Class<?> clazz = Class.forName(className, false, ctxLoader);
                        if (captureClassFromModScan(clazz)) count++;
                    } catch (Throwable ignored) {}
                }
            }

            if (count > 0) {
                LOGGER.info("[PreciseBackTrack] Mod [{}] 通过 SecureJar 扫描捕获 {} 个类", modId, count);
            }
            return count;

        } catch (Exception e) {
            LOGGER.debug("[PreciseBackTrack] SecureJar 扫描失败 [{}]: {}", modId, e.getMessage());
            return 0;
        }
    }

    private boolean isBlacklisted(String className) {
        for (String prefix : BLACKLIST_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        if (className.contains("$$Lambda") || className.contains("$Proxy")) return true;
        if (className.startsWith("[")) return true;
        // Skip mixin classes - they cannot be loaded via Class.forName
        String lower = className.toLowerCase();
        if (lower.contains(".mixin.") || lower.contains(".mixins.") || lower.endsWith("mixin")) return true;
        return false;
    }

    /**
     * Capture a class that is already known to be from a mod jar (e.g., found via mod file scanning).
     * Skips the ProtectionDomain check since ModuleClassLoader may not provide standard URLs.
     */
    private boolean captureClassFromModScan(Class<?> clazz) {
        if (clazz == null || trackedClasses.contains(clazz)) return false;
        if (isBlacklisted(clazz.getName())) return false;
        if (clazz.isArray() || clazz.isPrimitive()) return false;

        try {
            Map<String, CapturedValue> fields = captureStaticFields(clazz);
            if (!fields.isEmpty()) {
                capturedData.put(clazz.getName(), fields);
                trackedClasses.add(clazz);
                trackedClassNames.add(clazz.getName());
                LOGGER.debug("[PreciseBackTrack] 捕获: {} ({} 字段)", clazz.getSimpleName(), fields.size());
                return true;
            } else {
                trackedClassNames.add(clazz.getName());
                return false;
            }
        } catch (Throwable e) {
            LOGGER.debug("[PreciseBackTrack] 捕获类失败 {}: {}", clazz.getName(), e.getMessage());
            return false;
        }
    }

    private boolean captureClass(Class<?> clazz) {
        if (clazz == null || trackedClasses.contains(clazz)) return false;
        if (isBlacklisted(clazz.getName())) return false;
        if (clazz.isArray() || clazz.isPrimitive()) return false;
        if (!ClassUtil.isModClass(clazz)) return false;

        try {
            Map<String, CapturedValue> fields = captureStaticFields(clazz);
            if (!fields.isEmpty()) {
                capturedData.put(clazz.getName(), fields);
                trackedClasses.add(clazz);
                trackedClassNames.add(clazz.getName());
                LOGGER.debug("[PreciseBackTrack] 捕获: {} ({} 字段)", clazz.getSimpleName(), fields.size());
                return true;
            } else {
                trackedClassNames.add(clazz.getName());
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, CapturedValue> captureStaticFields(Class<?> clazz) {
        Map<String, CapturedValue> fields = new HashMap<>();
        for (Field field : getAllFields(clazz)) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers)) continue;
            if (isSensitiveType(field.getType())) continue;

            try {
                Object base = ModUtil.INSTANCE.getUnsafe().staticFieldBase(field);
                long offset = ModUtil.INSTANCE.getUnsafe().staticFieldOffset(field);
                Class<?> type = field.getType();

                Object value;
                if (type == int.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getInt(base, offset);
                } else if (type == long.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getLong(base, offset);
                } else if (type == boolean.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getBoolean(base, offset);
                } else if (type == byte.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getByte(base, offset);
                } else if (type == short.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getShort(base, offset);
                } else if (type == char.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getChar(base, offset);
                } else if (type == float.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getFloat(base, offset);
                } else if (type == double.class) {
                    value = ModUtil.INSTANCE.getUnsafe().getDouble(base, offset);
                } else {
                    value = ModUtil.INSTANCE.getUnsafe().getObject(base, offset);
                }

                fields.put(field.getName(), new CapturedValue(field.getName(), type, deepCopy(value),
                        Modifier.isFinal(modifiers), Modifier.isFinal(modifiers) && type.isPrimitive()));
            } catch (Exception e) {
                LOGGER.debug("[PreciseBackTrack] 捕获字段失败 {}.{}: {}",
                        clazz.getName(), field.getName(), e.getMessage());
            }
        }
        return fields;
    }

    private void startRuntimeMonitoring() {
        Thread monitor = new Thread(() -> {
            Set<String> seen = ConcurrentHashMap.newKeySet();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
                    for (StackTraceElement[] stack : stacks.values()) {
                        for (StackTraceElement elem : stack) {
                            String className = elem.getClassName();
                            if (seen.contains(className)) continue;
                            seen.add(className);

                            if (!trackedClassNames.contains(className) && !isBlacklisted(className)) {
                                try {
                                    Class<?> clazz = Class.forName(className, false,
                                            Thread.currentThread().getContextClassLoader());
                                    captureClass(clazz);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.debug("[PreciseBackTrack] 运行时监控出错: {}", e.getMessage());
                }
            }
        }, "RuntimeMonitor");

        monitor.setDaemon(true);
        monitor.start();
    }

    public void captureAllClasses() {
        if (!scannedModIds.isEmpty()) {
            LOGGER.info("[PreciseBackTrack] 已扫描 {} 个 Mod，追踪 {} 个类",
                    scannedModIds.size(), trackedClasses.size());
            return;
        }
        new Thread(this::delayedAutoScan, "ManualScanner").start();
    }

    public void backTrack() {
        if (trackedClasses.isEmpty()) {
            LOGGER.warn("[PreciseBackTrack] 没有可回溯的类");
            return;
        }

        LOGGER.info("[PreciseBackTrack] 开始回溯 {} 个类...", trackedClasses.size());
        int classCount = 0, fieldCount = 0, skipCount = 0, errorCount = 0;

        for (Class<?> clazz : trackedClasses) {
            Map<String, CapturedValue> fields = capturedData.get(clazz.getName());
            if (fields == null || fields.isEmpty()) continue;

            for (Field field : getAllFields(clazz)) {
                if (!Modifier.isStatic(field.getModifiers())) continue;

                String fieldKey = clazz.getName() + "." + field.getName();
                CapturedValue captured = fields.get(field.getName());
                if (captured == null) continue;

                try {
                    if (isBlacklisted(field.getDeclaringClass().getName())) {
                        LOGGER.debug("[PreciseBackTrack] 跳过 {} - 声明类被拉黑", fieldKey);
                        continue;
                    }

                    if (isSensitiveType(field.getType())) {
                        LOGGER.debug("[PreciseBackTrack] 跳过 {} - 敏感类型字段", fieldKey);
                        continue;
                    }

                    boolean success = modifyField(field, captured);
                    if (success) {
                        fieldCount++;
                    } else {
                        skipCount++;
                    }

                } catch (Throwable t) {
                    errorCount++;
                    LOGGER.warn("[PreciseBackTrack] 回溯 {} 时发生错误: {}", fieldKey, t.getMessage());
                }
            }
            classCount++;
        }

        LOGGER.info("[PreciseBackTrack] 完成: {} 个类, {} 个字段回溯, {} 个跳过, {} 个错误",
                classCount, fieldCount, skipCount, errorCount);
    }

    private boolean modifyField(Field field, CapturedValue captured) {
        int modifiers = field.getModifiers();
        if (!Modifier.isStatic(modifiers)) return false;

        try {
            Class<?> fieldType = field.getType();
            Object original = captured.value;
            if (captured.isFinalPrimitive) {
                LOGGER.debug("[PreciseBackTrack] 跳过 final 原始类型: {}.{}",
                        field.getDeclaringClass().getName(), field.getName());
                return false;
            }

            if (original == null) {
                // 捕获时为 null 的字段：如果当前不为 null，说明是延迟初始化的字段（如 shader、registry 等）
                // 不应将其重置为 null，否则会破坏运行时状态
                Object cur = getFieldValue(field);
                if (cur != null && !(cur instanceof List) && !(cur instanceof Set) && !(cur instanceof Map)) {
                    LOGGER.debug("[PreciseBackTrack] 跳过 {}.{} - 捕获时为null但当前已初始化（疑似延迟初始化字段）",
                            field.getDeclaringClass().getName(), field.getName());
                    return false;
                }
                if (List.class.isAssignableFrom(fieldType)) {
                    return setFieldValue(field, new ArrayList<>());
                }
                if (Set.class.isAssignableFrom(fieldType)) {
                    return setFieldValue(field, new HashSet<>());
                }
                if (Map.class.isAssignableFrom(fieldType)) {
                    return setFieldValue(field, new HashMap<>());
                }
                // 非集合类型且原始值为 null，跳过（避免将延迟初始化字段重置为 null）
                return false;
            }

            if (fieldType.isPrimitive()) {
                if (original == null) {
                    original = getPrimitiveDefaultValue(fieldType);
                }
                return setPrimitiveFieldValue(field, original);
            }

            Object freshCopy = deepCopy(original);
            if (freshCopy == null && original != null) {
                LOGGER.debug("[PreciseBackTrack] deepCopy 返回 null，使用原始值: {}.{}",
                        field.getDeclaringClass().getName(), field.getName());
                freshCopy = original;
            }

            Object current = getFieldValue(field);
            if (current instanceof List && freshCopy instanceof List) {
                if (tryModifyArrayList(field, (List<?>) current, (List<?>) freshCopy)) {
                    return true;
                }
            }

            if (tryModifyCollection(current, freshCopy)) {
                return true;
            }

            if (tryModifyArray(current, freshCopy)) {
                return true;
            }

            if (freshCopy != null && !fieldType.isAssignableFrom(freshCopy.getClass())) {
                LOGGER.warn("[PreciseBackTrack] 类型不兼容，跳过 {}: 期望 {}, 实际 {}",
                        field.getName(), fieldType.getName(), freshCopy.getClass().getName());
                return false;
            }

            if (freshCopy != null && !isSystemClass(fieldType)) {
                try {
                    Class<?> clazz = Class.forName(fieldType.getName(), true,
                            fieldType.getClassLoader() != null ? fieldType.getClassLoader() :
                                    Thread.currentThread().getContextClassLoader());
                    Object newInstance = clazz.getDeclaredConstructor().newInstance();
                    LOGGER.debug("[PreciseBackTrack] 为 {} 创建新实例: {}",
                            field.getName(), fieldType.getName());
                    return setFieldValue(field, newInstance);
                } catch (Exception e) {
                    LOGGER.debug("[PreciseBackTrack] 无法实例化 {}，使用 deepCopy 值: {}",
                            fieldType.getName(), e.getMessage());
                }
            }

            return setFieldValue(field, freshCopy);

        } catch (UnsupportedOperationException e) {
            LOGGER.warn("[PreciseBackTrack] 无法修改字段 {} - 可能是不可修改集合: {}",
                    field.getName(), e.getMessage());
            return false;
        } catch (Throwable e) {
            LOGGER.error("[PreciseBackTrack] 修改字段 {} 时发生未知错误: {}",
                    field.getName(), e.getMessage());
            return false;
        }
    }

    private Object getFieldValue(Field field) {
        try {
            long offset = ModUtil.INSTANCE.getUnsafe().staticFieldOffset(field);
            Object base = ModUtil.INSTANCE.getUnsafe().staticFieldBase(field);
            Class<?> type = field.getType();

            if (type == int.class) return ModUtil.INSTANCE.getUnsafe().getInt(base, offset);
            if (type == long.class) return ModUtil.INSTANCE.getUnsafe().getLong(base, offset);
            if (type == boolean.class) return ModUtil.INSTANCE.getUnsafe().getBoolean(base, offset);
            if (type == byte.class) return ModUtil.INSTANCE.getUnsafe().getByte(base, offset);
            if (type == short.class) return ModUtil.INSTANCE.getUnsafe().getShort(base, offset);
            if (type == char.class) return ModUtil.INSTANCE.getUnsafe().getChar(base, offset);
            if (type == float.class) return ModUtil.INSTANCE.getUnsafe().getFloat(base, offset);
            if (type == double.class) return ModUtil.INSTANCE.getUnsafe().getDouble(base, offset);
            return ModUtil.INSTANCE.getUnsafe().getObject(base, offset);
        } catch (Exception e) {
            try {
                return field.get(null);
            } catch (IllegalAccessException ex) {
                return null;
            }
        }
    }

    private boolean setFieldValue(Field field, Object value) {
        try {
            Class<?> type = field.getType();
            long offset = ModUtil.INSTANCE.getUnsafe().staticFieldOffset(field);
            Object base = ModUtil.INSTANCE.getUnsafe().staticFieldBase(field);

            if (type == int.class) {
                ModUtil.INSTANCE.getUnsafe().putInt(base, offset, value != null ? (Integer) value : 0);
            } else if (type == long.class) {
                ModUtil.INSTANCE.getUnsafe().putLong(base, offset, value != null ? (Long) value : 0L);
            } else if (type == boolean.class) {
                ModUtil.INSTANCE.getUnsafe().putBoolean(base, offset, value != null ? (Boolean) value : false);
            } else if (type == byte.class) {
                ModUtil.INSTANCE.getUnsafe().putByte(base, offset, value != null ? (Byte) value : (byte) 0);
            } else if (type == short.class) {
                ModUtil.INSTANCE.getUnsafe().putShort(base, offset, value != null ? (Short) value : (short) 0);
            } else if (type == char.class) {
                ModUtil.INSTANCE.getUnsafe().putChar(base, offset, value != null ? (Character) value : '\0');
            } else if (type == float.class) {
                ModUtil.INSTANCE.getUnsafe().putFloat(base, offset, value != null ? (Float) value : 0.0f);
            } else if (type == double.class) {
                ModUtil.INSTANCE.getUnsafe().putDouble(base, offset, value != null ? (Double) value : 0.0d);
            } else {
                ModUtil.INSTANCE.getUnsafe().putObject(base, offset, value);
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("[PreciseBackTrack] Unsafe 设置字段失败 {}: {}", field.getName(), e.getMessage());
            try {
                field.set(null, value);
                return true;
            } catch (Exception ex) {
                LOGGER.error("[PreciseBackTrack] 反射设置字段失败 {}: {}", field.getName(), ex.getMessage());
                return false;
            }
        }
    }

    private boolean setPrimitiveFieldValue(Field field, Object value) {
        return setFieldValue(field, value);
    }

    private Object getPrimitiveDefaultValue(Class<?> primitiveType) {
        if (primitiveType == boolean.class) return false;
        if (primitiveType == byte.class) return (byte) 0;
        if (primitiveType == short.class) return (short) 0;
        if (primitiveType == int.class) return 0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == float.class) return 0.0f;
        if (primitiveType == double.class) return 0.0;
        if (primitiveType == char.class) return '\u0000';
        return null;
    }

    private boolean tryModifyArrayList(Field field, List<?> current, List<?> original) {
        if (current == null || original == null) return false;

        try {
            String className = current.getClass().getName();
            if (className.contains("ImmutableCollections")) {
                return false;
            }

            int beforeSize = current.size();
            current.clear();

            if (current.size() == beforeSize && beforeSize > 0) {
                List<Object> newList;
                try {
                    newList = (List<Object>) current.getClass().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    newList = new ArrayList<>();
                }
                newList.addAll(original);
                return setFieldValue(field, newList);
            } else {
                List<Object> copy = new ArrayList<>(original);
                ((List<Object>) current).addAll(copy);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("[PreciseBackTrack] 修改 List 失败: {}", current.getClass().getName(), e);
            return false;
        }
    }

    private boolean tryModifyCollection(Object current, Object original) {
        if (current == null || original == null) return false;

        try {
            if (current instanceof Set && original instanceof Set) {
                ((Set<?>) current).clear();
                Set<?> copy = new HashSet<>((Set<?>) original);
                ((Set<Object>) current).addAll(copy);
                return true;
            }
            if (current instanceof Map && original instanceof Map) {
                ((Map<?, ?>) current).clear();
                Map<?, ?> copy = new HashMap<>((Map<?, ?>) original);
                ((Map<Object, Object>) current).putAll(copy);
                return true;
            }
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("[PreciseBackTrack] 集合不可修改: {}", current.getClass().getName());
            return false;
        } catch (Exception e) {
            LOGGER.error("[PreciseBackTrack] 修改集合失败: {}", current.getClass().getName(), e);
            return false;
        }
        return false;
    }

    private boolean tryModifyArray(Object current, Object original) {
        if (current == null || original == null) return false;
        if (!current.getClass().isArray() || !original.getClass().isArray()) return false;
        if (!current.getClass().getComponentType().equals(original.getClass().getComponentType())) {
            return false;
        }

        try {
            int len = Math.min(Array.getLength(current), Array.getLength(original));
            for (int i = 0; i < len; i++) {
                Array.set(current, i, Array.get(original, i));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Object deepCopy(Object value) {
        if (value == null) return null;

        try {
            Class<?> valueClass = value.getClass();
            String className = valueClass.getName();

            if (className.contains("ImmutableCollections") ||
                    className.contains("DirectByteBuffer") ||
                    className.contains("Unsafe") ||
                    className.contains("MethodHandle") ||
                    className.contains("VarHandle") ||
                    className.contains("$Lambda") ||
                    className.startsWith("jdk.internal") ||
                    className.startsWith("sun.misc") ||
                    className.startsWith("sun.nio")) {
                return value;
            }

            if (value instanceof String || value instanceof Number ||
                    value instanceof Boolean || value instanceof Character) {
                return value;
            }

            if (value instanceof List<?> list) {
                try {
                    if (list.isEmpty()) return new ArrayList<>();

                    String listClassName = list.getClass().getName();
                    if (listClassName.contains("SubList") ||
                            listClassName.contains("Synchronized") ||
                            listClassName.contains("Unmodifiable") ||
                            listClassName.contains("Immutable")) {
                        return new ArrayList<>(list);
                    }

                    try {
                        Constructor<?> copyConstructor = listClassName.getClass()
                                .getConstructor(Collection.class);
                        return copyConstructor.newInstance(list);
                    } catch (NoSuchMethodException e) {
                        List<Object> copy = (List<Object>) list.getClass().getDeclaredConstructor().newInstance();
                        copy.addAll(list);
                        return copy;
                    }
                } catch (Throwable e) {
                    LOGGER.warn("[PreciseBackTrack] List 深拷贝失败，使用 ArrayList: {}",
                            list.getClass().getName());
                    return new ArrayList<>(list);
                }
            }

            if (value instanceof Set<?> set) {
                try {
                    if (set.isEmpty()) return new HashSet<>();
                    return new HashSet<>(set);
                } catch (Throwable e) {
                    LOGGER.error("[PreciseBackTrack] Set 深拷贝失败: {}", set.getClass().getName());
                    return null;
                }
            }

            if (value instanceof Map<?, ?> map) {
                try {
                    if (map.isEmpty()) return new HashMap<>();
                    return new HashMap<>(map);
                } catch (Throwable e) {
                    LOGGER.error("[PreciseBackTrack] Map 深拷贝失败: {}", map.getClass().getName());
                    return null;
                }
            }

            if (valueClass.isArray()) {
                int len = Array.getLength(value);
                Object copy = Array.newInstance(valueClass.getComponentType(), len);
                for (int i = 0; i < len; i++) {
                    Array.set(copy, i, deepCopy(Array.get(value, i)));
                }
                return copy;
            }

            if (isSystemClass(valueClass)) {
                return value;
            }

            try {
                Method cloneMethod = valueClass.getMethod("clone");
                return cloneMethod.invoke(value);
            } catch (Exception e) {
                return value;
            }

        } catch (Throwable t) {
            LOGGER.error("[PreciseBackTrack] deepCopy 失败: {}",
                    value != null ? value.getClass().getName() : "null", t);
            return null;
        }
    }

    private boolean isSystemClass(Class<?> type) {
        String typeName = type.getName();
        return typeName.startsWith("java.") ||
                typeName.startsWith("javax.") ||
                typeName.startsWith("sun.") ||
                typeName.startsWith("com.sun.") ||
                typeName.startsWith("jdk.") ||
                typeName.startsWith("com.mojang.") ||
                typeName.startsWith("net.minecraft.") ||
                typeName.startsWith("net.minecraftforge.") ||
                typeName.startsWith("io.netty.") ||
                typeName.startsWith("org.apache.") ||
                typeName.startsWith("com.google.") ||
                typeName.startsWith("org.lwjgl.") ||
                typeName.startsWith("org.slf4j.");
    }

    private boolean isSensitiveType(Class<?> type) {
        if (type == null) return true;
        if (type.isPrimitive()) return false;

        String name = type.getName();
        if (isBlacklisted(name)) return true;
        if (Thread.class.isAssignableFrom(type)) return true;
        if (Class.class.isAssignableFrom(type)) return true;
        if (ClassLoader.class.isAssignableFrom(type)) return true;
        if (AccessibleObject.class.isAssignableFrom(type)) return true;

        return false;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            if (isBlacklisted(current.getName())) break;

            try {
                Field[] declared = current.getDeclaredFields();
                for (Field f : declared) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        fields.add(f);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[PreciseBackTrack] 获取 {} 字段失败: {}", current.getName(), e.getMessage());
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private record CapturedValue(String name, Class<?> type, Object value, boolean isFinal, boolean isFinalPrimitive) { }
}
