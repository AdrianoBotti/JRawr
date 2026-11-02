package rawr.web;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rawr.base.character.Character;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Background runner for the optimizer-grade upgrade list. The synchronous upgrade computation can
 * take minutes on a full pool, so the web UI starts one job per session and polls for progress;
 * the computation itself is still serialized by {@link CalculationService#withCalculationLock}.
 */
@Service
public class UpgradeJobService {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeJobService.class);
    private static final int POOL_SIZE = 2;
    private static final int QUEUE_CAPACITY = 4;

    private final UpgradeListService upgradeListService;
    private final ThreadPoolExecutor executor;
    private final Map<String, Job> jobsBySession = new ConcurrentHashMap<String, Job>();

    public UpgradeJobService(UpgradeListService upgradeListService) {
        this.upgradeListService = upgradeListService;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "upgrade-list-job");
            thread.setDaemon(true);
            return thread;
        };
        this.executor = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY), factory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public record Status(String state, int progress, String item, String error, boolean active) {
        static Status idle() {
            return new Status("idle", 0, "", null, false);
        }
    }

    public final class Job {
        private final String sessionId;
        private final Character source;
        private final UpgradeListService.Options options;
        private volatile String state = "queued";
        private volatile int progress;
        private volatile String item = "";
        private volatile String error;
        private volatile boolean cancelRequested;
        private volatile UpgradeListService.UpgradeResult result;

        private Job(String sessionId, Character source, UpgradeListService.Options options) {
            this.sessionId = sessionId;
            this.source = source;
            this.options = options;
        }

        public String state() {
            return state;
        }

        public int progress() {
            return progress;
        }

        public String item() {
            return item;
        }

        public String error() {
            return error;
        }

        public UpgradeListService.UpgradeResult result() {
            return result;
        }

        public boolean active() {
            return "queued".equals(state) || "running".equals(state);
        }

        public Status status() {
            return new Status(state, progress, item, error, active());
        }

        private void run() {
            if (cancelRequested) {
                state = "cancelled";
                return;
            }
            state = "running";
            try {
                UpgradeListService.UpgradeResult value = upgradeListService.compute(source, options,
                        new UpgradeListService.Progress() {
                            @Override
                            public void update(int percent, String currentItem) {
                                progress = Math.max(0, Math.min(100, percent));
                                if (currentItem != null) {
                                    item = currentItem;
                                }
                                if (cancelRequested) {
                                    throw new UpgradeListService.CancelledException();
                                }
                            }

                            @Override
                            public boolean cancelled() {
                                return cancelRequested;
                            }
                        });
                if (cancelRequested) {
                    state = "cancelled";
                } else if (value == null) {
                    state = "cancelled";
                } else {
                    result = value;
                    progress = 100;
                    state = "done";
                }
            } catch (UpgradeListService.CancelledException e) {
                state = "cancelled";
            } catch (Throwable t) {
                LOG.warn("Upgrade list job {} failed", sessionId, t);
                error = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                state = "failed";
            }
        }
    }

    /**
     * Starts a job for the session. A finished job is replaced; an active one is rejected. The
     * source character is cloned at start so later editor changes do not alter the running scan.
     */
    public synchronized Job start(HttpSession session, Character source,
            UpgradeListService.Options options) {
        String sessionId = session.getId();
        Job existing = jobsBySession.get(sessionId);
        if (existing != null && existing.active()) {
            throw new IllegalStateException("An upgrade list is already being built for this character.");
        }
        Character clone = source.cloneForCalculation();
        OptimizerJobService.copyOptimizerState(source, clone);
        Job job = new Job(sessionId, clone, options);
        jobsBySession.put(sessionId, job);
        try {
            executor.execute(job::run);
        } catch (RejectedExecutionException e) {
            jobsBySession.remove(sessionId);
            throw new IllegalStateException("The upgrade queue is full; try again in a moment.");
        }
        return job;
    }

    public Optional<Job> currentJob(HttpSession session) {
        return Optional.ofNullable(jobsBySession.get(session.getId()));
    }

    public void cancel(HttpSession session) {
        Job job = jobsBySession.get(session.getId());
        if (job != null && job.active()) {
            job.cancelRequested = true;
        }
    }

    public void dismissFinished(HttpSession session) {
        Job job = jobsBySession.get(session.getId());
        if (job != null && !job.active()) {
            jobsBySession.remove(session.getId());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
