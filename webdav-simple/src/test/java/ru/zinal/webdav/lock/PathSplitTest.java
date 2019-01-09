/*
 * Copyright 2018 zinal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.zinal.webdav.lock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import ru.zinal.webdav.util.SmallT;

/**
 *
 * @author zinal
 */
public class PathSplitTest {
    
    public PathSplitTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void pathSplitTest() {
        String[] result;
        result = LockEntry.splitPath("/");
        assertArrayEquals(new String[] {}, result);
        result = LockEntry.splitPath("/////");
        assertArrayEquals(new String[] {}, result);
        result = LockEntry.splitPath("a/b");
        assertArrayEquals(new String[] {"a", "b"}, result);
        result = LockEntry.splitPath("/a/b");
        assertArrayEquals(new String[] {"a", "b"}, result);
        result = LockEntry.splitPath("/a/b/");
        assertArrayEquals(new String[] {"a", "b"}, result);
        result = LockEntry.splitPath("x");
        assertArrayEquals(new String[] {"x"}, result);
        result = LockEntry.splitPath("y///");
        assertArrayEquals(new String[] {"y"}, result);
    }

    @Test
    public void tokenExtractorTest() {
        String arg, result;
        arg = "aa:e71d4fae>bb";
        result = SmallT.extractToken(arg);
        assertEquals("e71d4fae", result);
        arg = "(<opaquelocktoken:e71d4fae-5dec-22d6-fea5-00a0c91e6be4>)";
        result = SmallT.extractToken(arg);
        assertEquals("e71d4fae-5dec-22d6-fea5-00a0c91e6be4", result);
    }
}
