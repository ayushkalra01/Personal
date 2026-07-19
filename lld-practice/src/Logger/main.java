import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// ==========================================
// 1. Core Data Structures
// ==========================================
enum LogLevel {
    DEBUG(1), INFO(2), WARN(3), ERROR(4);
    public final int severity;
    LogLevel(int severity) { this.severity = severity; }
}

class LogEvent {
    public final LogLevel level;
    public final String message;
    public final LocalDateTime timestamp;

    public LogEvent(LogLevel level, String message, LocalDateTime timestamp) {
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }
}

// ==========================================
// 2. Formatting Strategies (Layouts)
// ==========================================
interface LogFormatter {
    String format(LogLevel level, String message, LocalDateTime timestamp);
}

class TextFormatter implements LogFormatter {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Override
    public String format(LogLevel level, String message, LocalDateTime timestamp) {
        return String.format("[%s] [%s] - %s", timestamp.format(fmt), level.name(), message);
    }
}

class JsonFormatter implements LogFormatter {
    @Override
    public String format(LogLevel level, String message, LocalDateTime timestamp) {
        return String.format("{\"timestamp\": \"%s\", \"level\": \"%s\", \"message\": \"%s\"}",
                timestamp.toString(), level.name(), message);
    }
}

// ==========================================
// 3. Appenders (Destinations)
// ==========================================
abstract class LogAppender {
    protected LogLevel threshold;
    protected LogFormatter formatter;

    public LogAppender(LogLevel threshold, LogFormatter formatter) {
        this.threshold = threshold;
        this.formatter = formatter;
    }

    public void process(LogLevel incomingLevel, String message, LocalDateTime timestamp) {
        if (incomingLevel.severity >= this.threshold.severity) {
            String formattedMessage = formatter.format(incomingLevel, message, timestamp);
            write(formattedMessage);
        }
    }

    protected abstract void write(String formattedMessage);

    // Hook for graceful shutdown
    public void shutdown() {}
}

class ConsoleAppender extends LogAppender {
    public ConsoleAppender(LogLevel threshold, LogFormatter formatter) { super(threshold, formatter); }
    @Override
    protected void write(String message) { System.out.println(message); }
}

class FileAppender extends LogAppender {
    private String filePath;
    public FileAppender(LogLevel threshold, LogFormatter formatter, String filePath) {
        super(threshold, formatter);
        this.filePath = filePath;
    }
    @Override
    protected void write(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("File write failed: " + e.getMessage());
        }
    }
}

// ==========================================
// 4. The Async Wrapper (Decorator)
// ==========================================

class AsyncAppender extends LogAppender {
    private final LogAppender delegate;
    private final BlockingQueue<LogEvent> queue;
    private volatile boolean isRunning = true;
    private final Thread worker;

    public AsyncAppender(LogAppender delegate) {
        super(delegate.threshold, delegate.formatter);
        this.delegate = delegate;
        this.queue = new ArrayBlockingQueue<>(10000);

        this.worker = new Thread(()->consumeLogs());
        this.worker.setName("Async-Log-Thread");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void process(LogLevel incomingLevel, String message, LocalDateTime timestamp) {
        if (incomingLevel.severity >= delegate.threshold.severity) {
            queue.offer(new LogEvent(incomingLevel, message, timestamp));
        }
    }

    @Override
    protected void write(String formattedMessage) { /* Not used directly */ }

    private void consumeLogs() {
        // Runs as long as the app is active OR there are still logs left to write
        while (isRunning || !queue.isEmpty()) {
            try {
                // Poll instead of take() so the thread wakes up frequently to check isRunning
                LogEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    delegate.process(event.level, event.message, event.timestamp);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        this.isRunning = false; // Trigger the thread to stop accepting new work
        try {
            worker.join(3000); // Wait up to 3 seconds for the queue to flush to disk
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        delegate.shutdown();
    }
}

// ==========================================
// 5. The Logger API
// ==========================================
class Logger {
    private List<LogAppender> appenders = new ArrayList<>();

    public void addAppender(LogAppender appender) { appenders.add(appender); }

    public void debug(String msg) { log(LogLevel.DEBUG, msg); }
    public void info(String msg)  { log(LogLevel.INFO, msg); }
    public void warn(String msg)  { log(LogLevel.WARN, msg); }
    public void error(String msg) { log(LogLevel.ERROR, msg); }

    private void log(LogLevel level, String message) {
        LocalDateTime now = LocalDateTime.now();
        for (LogAppender appender : appenders) {
            appender.process(level, message, now);
        }
    }

    public void shutdown() {
        for (LogAppender appender : appenders) {
            appender.shutdown();
        }
    }
}

// ==========================================
// 6. Execution (Main)
// ==========================================
public class main {
    public static void main(String[] args) {
        Logger logger = new Logger();

        // Setup Console (Synchronous, Plain Text, All Levels)
        logger.addAppender(new ConsoleAppender(LogLevel.DEBUG, new TextFormatter()));

        // Setup File (Asynchronous, JSON, Errors Only)
        FileAppender fileAppender = new FileAppender(LogLevel.ERROR, new JsonFormatter(), "errors.json");
        logger.addAppender(new AsyncAppender(fileAppender));

        // Generate Logs
        System.out.println("--- Generating Logs ---");
        logger.debug("Checking cache configuration...");
        logger.info("User 'alice_99' logged in successfully.");
        logger.warn("API response time exceeded 500ms.");
        logger.error("Failed to connect to database cluster!");

        // Shut down gracefully to ensure the background thread writes the file before JVM exits
        logger.shutdown();
        System.out.println("--- Application Shut Down ---");
    }
}