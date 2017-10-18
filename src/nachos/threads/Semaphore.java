package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Semaphore</tt> is a synchronization primitive with an unsigned value.
 * A semaphore has only two operations:
 *
 * <ul>
 * <li><tt>P()</tt>: waits until the semaphore's value is greater than zero,
 * then decrements it.
 * <li><tt>V()</tt>: increments the semaphore's value, and wakes up one thread
 * waiting in <tt>P()</tt> if possible.
 * </ul>
 *
 * <p>
 * Note that this API does not allow a thread to read the value of the
 * semaphore directly. Even if you did read the value, the only thing you would
 * know is what the value used to be. You don't know what the value is now,
 * because by the time you get the value, a context switch might have occurred,
 * and some other thread might have called <tt>P()</tt> or <tt>V()</tt>, so the
 * true value might now be different.
 * api不允许线程直接读信号量的值，就算你真的想读值，你要知道的唯一一件事是值曾经是什么。你不知道现在的值，因为当你获得值的时候，
 * 上下文切换可能已经发生，并且一些其他的线程调用了P或V，所以真实值现在可能已经变了。
 */
public class Semaphore {
    /**
     * Allocate a new semaphore.
     * 创建信号量，定义初始值
     *
     * @param	initialValue	the initial value of this semaphore.
     */
    public Semaphore(int initialValue) {
	value = initialValue;
    }

    /**
     * Atomically wait for this semaphore to become non-zero and decrement it.
     * 院子的等待信号量变为非0值然后减它
     */
    public void P() {
    //关中断
	boolean intStatus = Machine.interrupt().disable();

	//若信号量值为O,入队，进程sleep
	if (value == 0) {
	    waitQueue.waitForAccess(KThread.currentThread());
	    KThread.sleep();
	}
	//若值不为0，value-1
	else {
	    value--;
	}
	//开中断
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Atomically increment this semaphore and wake up at most one other thread
     * sleeping on this semaphore.
     */
    public void V() {
    //关中断
	boolean intStatus = Machine.interrupt().disable();
	//等待在这个信号量上的第一个线程出队
	KThread thread = waitQueue.nextThread();
	//有进程等待时，通知进程就绪
	if (thread != null) {
		//进程就绪
	    thread.ready();
	}
	//无进程等待时，信号量值+1
	else {
	    value++;
	}
	//开中断
	Machine.interrupt().restore(intStatus);
    }

    private static class PingTest implements Runnable {
	PingTest(Semaphore ping, Semaphore pong) {
	    this.ping = ping;
	    this.pong = pong;
	}
	
	public void run() {
	    for (int i=0; i<10; i++) {
		ping.P();
		pong.V();
	    }
	}

	private Semaphore ping;
	private Semaphore pong;
    }

    /**
     * Test if this module is working.
     */
    public static void selfTest() {
	Semaphore ping = new Semaphore(0);
	Semaphore pong = new Semaphore(0);

	new KThread(new PingTest(ping, pong)).setName("ping").fork();

	for (int i=0; i<10; i++) {
	    ping.V();
	    pong.P();
	}
    }
    //信号量值
    private int value;
    //等待信号量的队列
    private ThreadQueue waitQueue =
	ThreadedKernel.scheduler.newThreadQueue(false);
}
