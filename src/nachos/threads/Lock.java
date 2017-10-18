package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 * 锁是一个有两种状态busy与free的同步原语，仅有两种操作
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.等待free,设为busy
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.设为free，如果可能唤醒一个等待的进程
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 * 只有要求锁的进程可能释放锁。
 * 与信号量一样，API不允许读锁的状态(因为你读过后值可能已经改变)
 */
public class Lock {
    /**
     * Allocate a new lock. The lock will initially be <i>free</i>.
     * 分配新锁，初始化为free
     */
    public Lock() {
    }

    /**
     * Atomically acquire this lock. The current thread must not already hold
     * this lock.
     * 原子的要求锁，当前线程不能已经拥有锁
     */
    public void acquire() {
	Lib.assert(!isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();
	KThread thread = KThread.currentThread();

	//如果锁被持有，加入等待队列并sleep
	if (lockHolder != null) {
	    waitQueue.waitForAccess(thread);
	    KThread.sleep();
	}
	//如果锁未被持有，获得锁，设置lockHolder线程
	else {
	    waitQueue.acquire(thread);
	    lockHolder = thread;
	}

	Lib.assert(lockHolder == thread);

	Machine.interrupt().restore(intStatus);
    }

    /**
     * Atomically release this lock, allowing other threads to acquire it.
     * 原子的释放锁，允许其他线程获取。
     */
    public void release() {
	Lib.assert(isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();

	//如果等待队列的下一个线程不为空，使之就绪
	if ((lockHolder = waitQueue.nextThread()) != null)
	    lockHolder.ready();
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Test if the current thread holds this lock.
     * 测试当前线程是否持有锁
     *
     * @return	true if the current thread holds this lock.
     */
    public boolean isHeldByCurrentThread() {
	return (lockHolder == KThread.currentThread());
    }

    private KThread lockHolder = null;
    private ThreadQueue waitQueue =
	ThreadedKernel.scheduler.newThreadQueue(true);
}
