package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 * ����һ��������״̬busy��free��ͬ��ԭ��������ֲ���
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.�ȴ�free,��Ϊbusy
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.��Ϊfree��������ܻ���һ���ȴ��Ľ���
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 * ֻ��Ҫ�����Ľ��̿����ͷ�����
 * ���ź���һ����API�����������״̬(��Ϊ�������ֵ�����Ѿ��ı�)
 */
public class Lock {
    /**
     * Allocate a new lock. The lock will initially be <i>free</i>.
     * ������������ʼ��Ϊfree
     */
    public Lock() {
    }

    /**
     * Atomically acquire this lock. The current thread must not already hold
     * this lock.
     * ԭ�ӵ�Ҫ��������ǰ�̲߳����Ѿ�ӵ����
     */
    public void acquire() {
	Lib.assert(!isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();
	KThread thread = KThread.currentThread();

	//����������У�����ȴ����в�sleep
	if (lockHolder != null) {
	    waitQueue.waitForAccess(thread);
	    KThread.sleep();
	}
	//�����δ�����У������������lockHolder�߳�
	else {
	    waitQueue.acquire(thread);
	    lockHolder = thread;
	}

	Lib.assert(lockHolder == thread);

	Machine.interrupt().restore(intStatus);
    }

    /**
     * Atomically release this lock, allowing other threads to acquire it.
     * ԭ�ӵ��ͷ��������������̻߳�ȡ��
     */
    public void release() {
	Lib.assert(isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();

	//����ȴ����е���һ���̲߳�Ϊ�գ�ʹ֮����
	if ((lockHolder = waitQueue.nextThread()) != null)
	    lockHolder.ready();
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Test if the current thread holds this lock.
     * ���Ե�ǰ�߳��Ƿ������
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
