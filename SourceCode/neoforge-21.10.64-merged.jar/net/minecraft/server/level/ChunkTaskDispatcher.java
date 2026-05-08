package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class ChunkTaskDispatcher implements ChunkHolder.LevelChangeListener, AutoCloseable {
    public static final int DISPATCHER_PRIORITY_COUNT = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ChunkTaskPriorityQueue queue;
    private final TaskScheduler<Runnable> executor;
    private final PriorityConsecutiveExecutor dispatcher;
    protected boolean sleeping;

    public ChunkTaskDispatcher(TaskScheduler<Runnable> executor, Executor dispatcher) {
        this.queue = new ChunkTaskPriorityQueue(executor.name() + "_queue");
        this.executor = executor;
        this.dispatcher = new PriorityConsecutiveExecutor(4, dispatcher, "dispatcher");
        this.sleeping = true;
    }

    public boolean hasWork() {
        return this.dispatcher.hasWork() || this.queue.hasWork();
    }

    @Override
    public void onLevelChange(ChunkPos chunkPos, IntSupplier queueLevelGetter, int ticketLevel, IntConsumer queueLevelSetter) {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(0, () -> {
            int i = queueLevelGetter.getAsInt();
            if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                LOGGER.debug("RES {} {} -> {}", chunkPos, i, ticketLevel);
            }

            this.queue.resortChunkTasks(i, chunkPos, ticketLevel);
            queueLevelSetter.accept(ticketLevel);
        }));
    }

    public void release(long chunkPos, Runnable afterRelease, boolean fullClear) {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(1, () -> {
            this.queue.release(chunkPos, fullClear);
            this.onRelease(chunkPos);
            if (this.sleeping) {
                this.sleeping = false;
                this.pollTask();
            }

            afterRelease.run();
        }));
    }

    public void submit(Runnable task, long chunkPos, IntSupplier queueLevelSupplier) {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(2, () -> {
            int i = queueLevelSupplier.getAsInt();
            if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                LOGGER.debug("SUB {} {} {} {}", new ChunkPos(chunkPos), i, this.executor, this.queue);
            }

            this.queue.submit(task, chunkPos, i);
            if (this.sleeping) {
                this.sleeping = false;
                this.pollTask();
            }
        }));
    }

    protected void pollTask() {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(3, () -> {
            ChunkTaskPriorityQueue.TasksForChunk chunktaskpriorityqueue$tasksforchunk = this.popTasks();
            if (chunktaskpriorityqueue$tasksforchunk == null) {
                this.sleeping = true;
            } else {
                this.scheduleForExecution(chunktaskpriorityqueue$tasksforchunk);
            }
        }));
    }

    protected void scheduleForExecution(ChunkTaskPriorityQueue.TasksForChunk tasks) {
        CompletableFuture.allOf(tasks.tasks().stream().map(p_371614_ -> this.executor.scheduleWithResult(p_371506_ -> {
            p_371614_.run();
            p_371506_.complete(Unit.INSTANCE);
        })).toArray(CompletableFuture[]::new)).thenAccept(p_371810_ -> this.pollTask());
    }

    protected void onRelease(long chunkPos) {
    }

    @Nullable
    protected ChunkTaskPriorityQueue.TasksForChunk popTasks() {
        return this.queue.pop();
    }

    @Override
    public void close() {
        this.executor.close();
    }
}
