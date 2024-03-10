package jtorrent.domain.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

/**
 * A ContinuousMergedInputStream represents an InputStream that is a concatenation of multiple input streams. When the
 * end of a stream is reached, the next stream is used. This allows for continuous reading of data from multiple
 * streams. This is similar to {@link java.io.SequenceInputStream} except that a single read operation can span multiple
 * streams.
 */
public class ContinuousMergedInputStream extends InputStream {

    private final InputStream[] inputStreams;
    private int currentStreamIndex;
    private boolean eofReached;

    public ContinuousMergedInputStream(Collection<InputStream> inputStreams) {
        this(inputStreams.toArray(new InputStream[0]));
    }

    public ContinuousMergedInputStream(InputStream... inputStreams) {
        this.inputStreams = inputStreams;
        this.currentStreamIndex = 0;
        this.eofReached = false;
    }

    @Override
    public int read() throws IOException {
        if (eofReached) {
            return -1;
        }

        int byteRead = inputStreams[currentStreamIndex].read();
        if (byteRead == -1) {
            currentStreamIndex++;
            if (currentStreamIndex >= inputStreams.length) {
                eofReached = true;
                return -1;
            }
            return read(); // Recursively move to the next stream
        }
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }

        if (eofReached) {
            return -1; // End of all input streams
        }

        int bytesRead = inputStreams[currentStreamIndex].read(b, off, len);
        if (bytesRead < len) {
            inputStreams[currentStreamIndex].close();
            currentStreamIndex++;
            if (currentStreamIndex >= inputStreams.length) {
                eofReached = true;
                return bytesRead;
            }

            // Recursively move to the next stream
            if (bytesRead == -1) {
                return read(b, off, len);
            } else {
                return bytesRead + Math.max(read(b, off + bytesRead, len - bytesRead), 0);
            }
        }
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        for (InputStream is : inputStreams) {
            is.close();
        }
    }
}
