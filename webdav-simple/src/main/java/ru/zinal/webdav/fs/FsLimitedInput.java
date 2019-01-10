/*
 * Copyright 2019 zinal.
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
package ru.zinal.webdav.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author zinal
 */
public class FsLimitedInput extends InputStream {
    
    private final FileChannel channel;
    private long remaining;
    private ByteBuffer minibuf = null;
    
    public FsLimitedInput(File f, long start, long total)
            throws IOException {
        if (total > 0) {
            channel = FileChannel.open(f.toPath(), StandardOpenOption.READ);
            channel.position(start);
            remaining = total;
        } else {
            channel = null;
            remaining = 0L;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int read() throws IOException {
        if (remaining<=0L)
            return -1;
        if (minibuf==null)
            minibuf = ByteBuffer.allocate(1);
        if ( channel.read(minibuf) != 1 )
            return -1;
        remaining -= 1L;
        return 0xFF & ((int) minibuf.get());
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining<=0L)
            return -1;
        if (((long)len) > remaining)
            len = (int)remaining;
        int bytes = channel.read(ByteBuffer.wrap(b, off, len));
        if (bytes>0)
            remaining -= bytes;
        return bytes;
    }

}
