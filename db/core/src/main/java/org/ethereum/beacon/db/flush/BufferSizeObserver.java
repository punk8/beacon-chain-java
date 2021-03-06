package org.ethereum.beacon.db.flush;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.db.source.WriteBuffer;

/**
 * Flushing strategy that observes a size of given buffer and emits a flush whenever size limit is
 * exceeded.
 */
public class BufferSizeObserver implements DatabaseFlusher {

  private static final Logger logger = LogManager.getLogger(BufferSizeObserver.class);

  /** A buffer. */
  private final WriteBuffer buffer;
  /** A commit track. Aids forced flushes consistency. */
  private final WriteBuffer commitTrack;
  /** A limit of buffer size in bytes. */
  private final long bufferSizeLimit;

  BufferSizeObserver(WriteBuffer buffer, WriteBuffer commitTrack, long bufferSizeLimit) {
    this.buffer = buffer;
    this.commitTrack = commitTrack;
    this.bufferSizeLimit = bufferSizeLimit;
  }

  public static <K, V> BufferSizeObserver create(WriteBuffer<K, V> buffer, long bufferSizeLimit) {
    WriteBuffer<K, V> commitTrack = new WriteBuffer<>(buffer, false);
    return new BufferSizeObserver(buffer, commitTrack, bufferSizeLimit);
  }

  @Override
  public void flush() {
    buffer.flush();
  }

  @Override
  public void commit() {
    commitTrack.flush();
    if (buffer.evaluateSize() >= bufferSizeLimit) {
      logger.debug(
          "Flush db buffer due to size limit: {} >= {}", buffer.evaluateSize(), bufferSizeLimit);
      flush();
    }
  }
}
