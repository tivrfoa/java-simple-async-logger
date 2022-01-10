package org.tivrfoa;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class AsyncAppenderBase extends UnsynchronizedAppenderBase {

	BlockingQueue<String> blockingQueue;
	FileAppender fileAppender;

	public AsyncAppenderBase(FileAppender fileAppender) {
		this.fileAppender = fileAppender;
	}

	/**
	 * The default buffer size.
	 */
	public static final int DEFAULT_QUEUE_SIZE = 256;
	int queueSize = DEFAULT_QUEUE_SIZE;

	static final int UNDEFINED = -1;
	int discardingThreshold = UNDEFINED;
	boolean neverBlock = false;

	Worker worker = new Worker();

	/**
	 * The default maximum queue flush time allowed during appender stop. If the
	 * worker takes longer than this time it will exit, discarding any remaining
	 * items in the queue
	 */
	public static final int DEFAULT_MAX_FLUSH_TIME = 1000;
	int maxFlushTime = DEFAULT_MAX_FLUSH_TIME;

	@Override
	public void start() {
		if (isStarted())
			return;

		if (queueSize < 1) {
			System.err.println("Invalid queue size [" + queueSize + "]");
			return;
		}
		blockingQueue = new ArrayBlockingQueue<String>(queueSize);

		if (discardingThreshold == UNDEFINED)
			discardingThreshold = queueSize / 5;
		System.out.println("Setting discardingThreshold to " + discardingThreshold);
		worker.setDaemon(true);
		worker.setName("AsyncAppender-Worker-" + getName());
		// make sure this instance is marked as "started" before staring the worker
		// Thread
		super.start();
		worker.start();
	}

	@Override
	public void stop() {
		if (!isStarted())
			return;

		// mark this appender as stopped so that Worker can also processPriorToRemoval
		// if it is invoking
		// aii.appendLoopOnAppenders
		// and sub-appenders consume the interruption
		super.stop();

		// interrupt the worker thread so that it can terminate. Note that the
		// interruption can be consumed by sub-appenders
		worker.interrupt();

		InterruptUtil interruptUtil = new InterruptUtil();

		try {
			interruptUtil.maskInterruptFlag();

			worker.join(maxFlushTime);

			// check to see if the thread ended and if not add a warning message
			if (worker.isAlive()) {
				System.out.println("Max queue flush timeout (" + maxFlushTime + " ms) exceeded. Approximately "
						+ blockingQueue.size() + " queued events were possibly discarded.");
			} else {
				System.out.println("Queue flush finished successfully within timeout.");
			}

		} catch (InterruptedException e) {
			int remaining = blockingQueue.size();
			System.err.println("Failed to join worker thread. " + remaining + " queued events may be discarded.");
		} finally {
			interruptUtil.unmaskInterruptFlag();
		}
	}

	@Override
	protected void append(String msg) {
		/*if (isQueueBelowDiscardingThreshold() && isDiscardable(eventObject)) {
			return;
		}*/
		put(msg);
	}

	private boolean isQueueBelowDiscardingThreshold() {
		return (blockingQueue.remainingCapacity() < discardingThreshold);
	}

	private void put(String msg) {
		if (neverBlock) {
			blockingQueue.offer(msg);
		} else {
			putUninterruptibly(msg);
		}
	}

	private void putUninterruptibly(String msg) {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					blockingQueue.put(msg);
					break;
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getDiscardingThreshold() {
		return discardingThreshold;
	}

	public void setDiscardingThreshold(int discardingThreshold) {
		this.discardingThreshold = discardingThreshold;
	}

	public int getMaxFlushTime() {
		return maxFlushTime;
	}

	public void setMaxFlushTime(int maxFlushTime) {
		this.maxFlushTime = maxFlushTime;
	}

	/**
	 * Returns the number of elements currently in the blocking queue.
	 *
	 * @return number of elements currently in the queue.
	 */
	public int getNumberOfElementsInQueue() {
		return blockingQueue.size();
	}

	public void setNeverBlock(boolean neverBlock) {
		this.neverBlock = neverBlock;
	}

	public boolean isNeverBlock() {
		return neverBlock;
	}

	/**
	 * The remaining capacity available in the blocking queue.
	 * <p>
	 * See also {@link java.util.concurrent.BlockingQueue#remainingCapacity()
	 * BlockingQueue#remainingCapacity()}
	 *
	 * @return the remaining capacity
	 * 
	 */
	public int getRemainingCapacity() {
		return blockingQueue.remainingCapacity();
	}

	class Worker extends Thread {

		public void run() {
			AsyncAppenderBase parent = AsyncAppenderBase.this;

			// loop while the parent is started
			while (parent.isStarted()) {
				try {
					List<String> elements = new ArrayList<>();
					String e0 = parent.blockingQueue.take();
					elements.add(e0);
					parent.blockingQueue.drainTo(elements);
					for (var e : elements) {
						fileAppender.doAppend(e);
					}
				} catch (InterruptedException e1) {
					// exit if interrupted
					break;
				}
			}

			System.out.println("Worker thread will flush remaining events before exiting. ");

			for (String e : parent.blockingQueue) {
				fileAppender.doAppend(e);
				parent.blockingQueue.remove(e);
			}

			fileAppender.stop();
		}
	}
}

