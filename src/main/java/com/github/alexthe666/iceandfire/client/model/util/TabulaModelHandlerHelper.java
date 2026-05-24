package com.github.alexthe666.iceandfire.client.model.util;

import com.github.alexthe666.citadel.client.model.TabulaModelHandler;
import com.github.alexthe666.citadel.client.model.container.TabulaModelContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TabulaModelHandlerHelper {

    public static TabulaModelContainer loadTabulaModel(String path) throws IOException {
        String normalizedPath = path;

        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        if (!normalizedPath.endsWith(".tbl")) {
            normalizedPath = normalizedPath + ".tbl";
        }

        InputStream stream = null;

        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        if (contextCl != null) {
            stream = contextCl.getResourceAsStream(normalizedPath);
        }

        if (stream == null) {
            stream = TabulaModelHandlerHelper.class.getClassLoader().getResourceAsStream(normalizedPath);
        }

        if (stream == null) {
            stream = TabulaModelHandler.INSTANCE.getClass().getClassLoader().getResourceAsStream(normalizedPath);
        }

        if (stream == null) {
            String classPath = path.startsWith("/") ? path : "/" + path;
            if (!classPath.endsWith(".tbl")) {
                classPath = classPath + ".tbl";
            }
            stream = TabulaModelHandlerHelper.class.getResourceAsStream(classPath);
        }

        if (stream == null) {
            throw new IOException("找不到 Tabula 模型资源: " + normalizedPath
                    + " (已尝试 ContextClassLoader / IaF ClassLoader / Citadel ClassLoader / Class.getResource)");
        }

        return TabulaModelHandler.INSTANCE.loadTabulaModel(getModelJsonStream(normalizedPath, stream));
    }

    private static InputStream getModelJsonStream(String name, InputStream file) throws IOException {
        ZipInputStream zip = new ZipInputStream(file);

        ZipEntry entry;
        do {
            if ((entry = zip.getNextEntry()) == null) {
                throw new RuntimeException("No model.json present in " + name);
            }
        } while (!entry.getName().equals("model.json"));

        return zip;
    }
}
