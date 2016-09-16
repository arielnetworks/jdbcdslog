package org.jdbcdslog;

import java.util.List;
import java.util.ArrayList;

public class LazyStringBuilder implements Supplier<String> {
    private List<Consumer<StringBuilder>> processes = new ArrayList<Consumer<StringBuilder>>();
    private String result = "";
    private boolean dirty = false;

    public LazyStringBuilder append(Consumer<StringBuilder> proc) {
        processes.add(proc);
        dirty = true;
        return this;
    }

    public LazyStringBuilder append(final CharSequence s) {
        return append(newCharSequenceAppendConsumer(s));
    }

    private Consumer<StringBuilder> newCharSequenceAppendConsumer(final CharSequence s) {
        return new Consumer<StringBuilder>() {
            @Override
            public void accept(final StringBuilder sb){
                sb.append(s);
            }
        };
    }

    public String get() {
        if (dirty) {
            StringBuilder sb = new StringBuilder(result);
            for (Consumer<StringBuilder> proc : processes) {
                proc.accept(sb);
            }
            result = sb.toString();
            processes.clear();
            dirty = false;
        }
        return result;
    }
}

