package autoloupe;

import java.nio.file.Path;

public record WatchConfig(
        Path folder,
        long settleDelayMillis
) {
}