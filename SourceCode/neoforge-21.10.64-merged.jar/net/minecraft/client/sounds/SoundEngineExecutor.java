package net.minecraft.client.sounds;

import java.util.concurrent.locks.LockSupport;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.BlockableEventLoop;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The SoundEngineExecutor class is responsible for executing sound-related tasks in a separate thread.
 * <p>
 * It extends the BlockableEventLoop class, providing an event loop for managing and executing tasks.
 */
@OnlyIn(Dist.CLIENT)
public class SoundEngineExecutor extends BlockableEventLoop<Runnable> {
    private Thread thread = this.createThread();
    private volatile boolean shutdown;

    public SoundEngineExecutor() {
        super("Sound executor");
    }

    private Thread createThread() {
        Thread thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.setName("Sound engine");
        thread.setUncaughtExceptionHandler(
            (p_437177_, p_437178_) -> Minecraft.getInstance()
                .delayCrash(CrashReport.forThrowable(p_437178_, "Uncaught exception on thread: " + p_437177_.getName()))
        );
        thread.start();
        return thread;
    }

    /**
     * Wraps the given runnable task. In this case, the original runnable is returned as-is.
     * <p>
     * @return The wrapped runnable task
     *
     * @param runnable The original runnable task
     */
    @Override
    public Runnable wrapRunnable(Runnable runnable) {
        return runnable;
    }

    @Override
    public void schedule(Runnable task) {
        if (!this.shutdown) {
            super.schedule(task);
        }
    }

    /**
     * Determines whether the given runnable task should be run or not.
     * It depends on the shutdown state of the SoundEngineExecutor.
     * <p>
     * @return true if the task should run, false otherwise
     *
     * @param runnable The runnable task
     */
    @Override
    protected boolean shouldRun(Runnable runnable) {
        return !this.shutdown;
    }

    @Override
    protected Thread getRunningThread() {
        return this.thread;
    }

    private void run() {
        while (!this.shutdown) {
            this.managedBlock(() -> this.shutdown);
        }
    }

    @Override
    public void waitForTasks() {
        LockSupport.park("waiting for tasks");
    }

    public void shutDown() {
        this.shutdown = true;
        this.dropAllTasks();
        this.thread.interrupt();

        try {
            this.thread.join();
        } catch (InterruptedException interruptedexception) {
            Thread.currentThread().interrupt();
        }
    }

    public void startUp() {
        this.shutdown = false;
        this.thread = this.createThread();
    }
}
