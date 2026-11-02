package rawr.web;

/** Per-user calculation preferences, persisted in browser cookies like the other settings. */
public record GlobalSettings(boolean hideProfEnchants, int procEffectMode,
        int combinationEffectMode, boolean useMultithreading, int maxThreads, int topGemmings) {
    public static final int PROC_SIMPLE = 1;
    public static final int PROC_ADVANCED = 2;
    public static final int PROC_INTERPOLATION = 3;
    public static final int COMBINATION_LINEAR = 0;
    public static final int COMBINATION_INTERPOLATED = 1;
    public static final int COMBINATION_EXACT = 2;
    public static final int MIN_THREADS = 1;
    public static final int MAX_THREADS = 32;
    public static final int DEFAULT_TOP_GEMMINGS = 3;
    public static final int MAX_TOP_GEMMINGS = 25;

    public GlobalSettings(boolean hideProfEnchants, int procEffectMode,
            int combinationEffectMode, boolean useMultithreading, int maxThreads) {
        this(hideProfEnchants, procEffectMode, combinationEffectMode, useMultithreading,
                maxThreads, DEFAULT_TOP_GEMMINGS);
    }

    public static GlobalSettings defaults() {
        return new GlobalSettings(true, PROC_INTERPOLATION, COMBINATION_INTERPOLATED,
                true, OptimizerJobService.defaultMaxThreads(), DEFAULT_TOP_GEMMINGS);
    }
}
