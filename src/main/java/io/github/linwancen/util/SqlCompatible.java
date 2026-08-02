package io.github.linwancen.util;

import com.intellij.ide.util.RunOnceUtil;
import com.intellij.psi.PsiElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class SqlCompatible {
    private static final Logger LOG = LoggerFactory.getLogger(SqlCompatible.class);
    public static Class<?> sqlNavigationUtils;
    public static Method findRelatedDbElements;

    static {
        try {
            // new version new Class
            sqlNavigationUtils = Class.forName("com.intellij.sql.SqlNavigationUtils");
        } catch (Throwable e) {
            try {
                // old version
                sqlNavigationUtils = Class.forName("com.intellij.sql.SqlDocumentationProvider");
            } catch (Throwable e2) {
                RunOnceUtil.runOnceForApp("io.github.linwancen.util.SqlCompatible.sqlNavigationUtils", () ->
                        LOG.warn("SqlCompatible.sqlNavigationUtils not found:", e2));
            }
        }
        try {
            findRelatedDbElements = sqlNavigationUtils.getMethod("findRelatedDbElements", PsiElement.class, boolean.class);
        } catch (Throwable e) {
            RunOnceUtil.runOnceForApp("io.github.linwancen.util.SqlCompatible.findRelatedDbElements", () ->
                    LOG.warn("SqlCompatible.findRelatedDbElements not found:", e));
        }
    }
}
