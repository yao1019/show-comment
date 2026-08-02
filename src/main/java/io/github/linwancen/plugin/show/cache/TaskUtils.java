package io.github.linwancen.plugin.show.cache;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.EdtExecutorService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TaskUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TaskUtils.class);

    public static final Map<Object, AtomicBoolean> running = new ConcurrentHashMap<>();

    public static <T> void init(
            @NotNull Map<Project, ScheduledFuture<?>> taskMap,
            @NotNull Project project,
            @NotNull Map<Project, T> cache,
            @NotNull Consumer<T> func
    ) {
        taskMap.computeIfAbsent(project,
                project1 -> AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
                    try {
                        T t = cache.get(project);
                        if (t == null) {
                            return;
                        }
                        EdtExecutorService.getInstance().execute(() -> {
                            JFrame frame = WindowManager.getInstance().getFrame(project);
                            if (frame != null && !frame.isActive()) {
                                return;
                            }
                            AtomicBoolean flag = running.computeIfAbsent(t, k -> new AtomicBoolean());
                            if (!flag.compareAndSet(false, true)) {
                                return;
                            }
                            ReadAction.nonBlocking(() -> {
                                        if (project.isDisposed()) {
                                            running.remove(t);
                                            cache.remove(project);
                                            ScheduledFuture<?> task = taskMap.remove(project);
                                            if (task != null) {
                                                task.cancel(true);
                                            }
                                            return;
                                        }
                                        try {
                                            func.accept(t);
                                        } catch (Exception e) {
                                            flag.set(false);
                                        }
                                    })
                                    .inSmartMode(project)
                                    .submit(AppExecutorUtil.getAppExecutorService());
                        });
                    } catch (ProcessCanceledException ignored) {
                    } catch (Throwable e) {
                        LOG.info("TaskUtils init catch Throwable but log to record.", e);
                    }
                }, 0L, 1L, TimeUnit.SECONDS));
    }
}
