package org.jdbcdslog;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LazyStringBuilderTest {
    @Test
    public void testAppend() {
        LazyStringBuilder sb = new LazyStringBuilder();
        sb.append("a");
        sb.append("b");
        assertEquals("ab", sb.get());

        sb.append("c");
        assertEquals("abc", sb.get());

        final boolean[] called = {false};
        sb.append(new Consumer<StringBuilder>() {
            public void accept(final StringBuilder sb) {
                sb.append("d");
                sb.append("e");
                called[0] = true;
            }
        });
        assertEquals(false, called[0]);
        assertEquals("abcde", sb.get());
        assertEquals(true, called[0]);

        sb.append("f");
        assertEquals("abcdef", sb.get());

        sb.append(new Consumer<StringBuilder>() {
            public void accept(final StringBuilder sb) {
                sb.replace(0, 3, "123");
            }
        });
        assertEquals("123def", sb.get());
    }

}
