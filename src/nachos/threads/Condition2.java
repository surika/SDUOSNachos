package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *ͨ�������ж�ʵ����������
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *����һ���µ���������
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
	waitQueue = new LinkedList();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
//    	release condition lock
//        diable interrupt
//        add current thread to wait queue
//        make current thread sleep
//        restore interrupt 
//        acquire condition lock
	Lib.assert(conditionLock.isHeldByCurrentThread());

	conditionLock.release();
	boolean initState=Machine.interrupt().disable();
	waitQueue.add(KThread.currentThread());
	KThread.sleep();
	Machine.interrupt().restore(initState);
	conditionLock.acquire();
//	System.out.println("Condition2.sleep");
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assert(conditionLock.isHeldByCurrentThread());
	boolean preState = Machine.interrupt().disable();//���ж�
    if(!waitQueue.isEmpty()){//����waitQueue�е�һ���߳�
        KThread a = (KThread) waitQueue.removeFirst();//ȡ��waitQueue��һ���߳�
        a.ready();//��ȡ�����߳�����
    }
    Machine.interrupt().restore(preState);//�ָ��ж�
//    System.out.println("Condition2.wake");
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assert(conditionLock.isHeldByCurrentThread());
	boolean preState = Machine.interrupt().disable();//���ж�
    while(!waitQueue.isEmpty()){
        KThread a = (KThread) waitQueue.removeFirst();//ȡ��waitQueue�е�һ���߳�
        a.ready();//��ȡ�����߳�����
    }
    Machine.interrupt().restore(preState);//�ָ��ж�
    }

    private Lock conditionLock;
    private LinkedList waitQueue;
}
