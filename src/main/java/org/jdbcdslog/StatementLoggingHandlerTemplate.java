package org.jdbcdslog;

import static org.jdbcdslog.LogUtils.*;
import static org.jdbcdslog.Loggers.*;
import static org.jdbcdslog.ProxyUtils.wrap;

import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Template for statement logging handler, which includes slow query handling, and
 * before/after statement logging
 *
 * @author a511990
 */
public abstract class StatementLoggingHandlerTemplate<T extends Statement> extends LoggingHandlerSupport<T> {
    protected LogMetaData logMetaData;

    public StatementLoggingHandlerTemplate(T target) {
        super(target);
    }

    public StatementLoggingHandlerTemplate(LogMetaData logMetaData, T target) {
        super(target);
        this.logMetaData = logMetaData;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

        Map<String, String> oldMdc = LogUtils.setMdc(logMetaData);
        try {
            boolean needsLog = needsLogging(proxy, method, args);
            long startTimeInNano = 0;
            final boolean isAddBatch = isAddBatch(proxy, method, args);
            final boolean isExecuteBatch = isExecuteBatch(proxy, method, args);
            LazyStringBuilder sb = null;

            if (isAddBatch) {
                if (!ConfigurationParameters.logAddBatch) {
                    needsLog = false;
                }
                if (ConfigurationParameters.logExecuteBatchDetail) {
                    doAddBatch(proxy, method, args);
                }
            }

            if (needsLog) {
                startTimeInNano = System.nanoTime();

                sb = new LazyStringBuilder();
                sb.append(new Consumer<StringBuilder>() {
                    public void accept(final StringBuilder sb) {
                        if (ConfigurationParameters.logBeforeStatement) {
                            sb.append("START: ");      // Reserve space for START: and END:
                        }
                        sb.append(method.getDeclaringClass().getName()).append(".").append(method.getName()).append(": ");

                        if (isExecuteBatch) {
                            if (ConfigurationParameters.logExecuteBatchDetail) {
                                appendBatchStatements(sb);
                            }
                        } else if (isAddBatch) {
                            if (ConfigurationParameters.logAddBatchDetail) {
                                appendStatement(sb, proxy, method, args);
                            }
                        } else {
                            appendStatement(sb, proxy, method, args);
                        }

                        appendStackTrace(sb);
                    }
                });

                logBeforeInvoke(proxy, method, args, sb);
            }

            Object result = method.invoke(target, args);

            result = doAfterInvoke(proxy, method, args, result);

            if (needsLog) {
                final long elapsedTimeInNano = System.nanoTime() - startTimeInNano;

                sb.append(new Consumer<StringBuilder>() {
                    public void accept(final StringBuilder sb) {
                        if (ConfigurationParameters.logBeforeStatement) {
                            sb.replace(0, ("START: ".length()), "END: ");
                        }

                        appendElapsedTime(sb, elapsedTimeInNano);
                    }
                });

                logAfterInvoke(proxy, method, args, result, elapsedTimeInNano, sb);
            }
            return result;

        } catch (Throwable t) {
            handleException(t, proxy, method, args);
        } finally {
            LogUtils.resetMdc(oldMdc);
        }
        return null;
    }

    protected abstract void doAddBatch(Object proxy, Method method, Object[] args);

    protected abstract void appendBatchStatements(StringBuilder sb);

    protected boolean isExecuteBatch(Object proxy, Method method, Object[] args) {
        return method.getName().equals("executeBatch");
    }


    protected boolean isAddBatch(Object proxy, Method method, Object[] args) {
        return method.getName().equals("addBatch");
    }


    protected abstract void appendStatement(StringBuilder sb, Object proxy, Method method, Object[] args) ;

    protected boolean needsLogging(Object proxy, Method method, Object[] args) {
        return false;
    }

    protected void logBeforeInvoke(Object proxy, Method method, Object[] args, LazyStringBuilder message) {
        if (ConfigurationParameters.logBeforeStatement) {
            logInfoOn(message, getLogger());
        }
    }

    protected Object doAfterInvoke(Object proxy, Method method, Object[] args, Object result) {
        return wrap(logMetaData, result);
    }

    protected void logAfterInvoke(Object proxy, final Method method, Object[] args, Object result, final long elapsedTimeInNano, LazyStringBuilder message) {

        LazyStringBuilder endMessage = message;
        if ( ! ConfigurationParameters.logDetailAfterStatement) {
            // replace the log message to a simple message

            endMessage = new LazyStringBuilder();
            endMessage.append(new Consumer<StringBuilder>() {
                public void accept(final StringBuilder endMessage) {
                    endMessage.append("END: ")
                        .append(method.getDeclaringClass().getName()).append(".").append(method.getName())
                        .append(": ");
                    appendStackTrace(endMessage);
                    appendElapsedTime(endMessage, elapsedTimeInNano);
                }
            });
        }

        logInfoOn(endMessage, getLogger());

        if (elapsedTimeInNano >= ConfigurationParameters.slowQueryThresholdInNano) {
            logInfoOn(message, getSlowQueryLogger());       // log the original message
        }

    }

    protected boolean needsSlowOperationLogging(Object proxy, Method method, Object[] args, Object result, long elapsedTimeInNano) {
        return true;
    }

    protected void handleException(Throwable t, Object proxy, Method method, Object[] args) throws Throwable {
        LogUtils.handleException(t, getLogger(), LogUtils.createLogEntry(method, null, null, null));
    }

    private void logInfoOn(Supplier<String> msgSupplier, Logger logger) {
        if (logger.isInfoEnabled()) {
            logger.info(msgSupplier.get());
        }
    }

    protected Logger getLogger() {
        return statementLogger;
    }
    protected Logger getSlowQueryLogger() {
        return slowQueryLogger;
    }

}
