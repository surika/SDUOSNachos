package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *通过开关中断实现条件变量
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *分配一个新的条件变量
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
	boolean preState = Machine.interrupt().disable();//关中断
    if(!waitQueue.isEmpty()){//唤醒waitQueue中的一个线程
        KThread a = (KThread) waitQueue.removeFirst();//取出waitQueue中一个线程
        a.ready();//将取出的线程启动
    }
    Machine.interrupt().restore(preState);//恢复中断
//    System.out.println("Condition2.wake");
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assert(conditionLock.isHeldByCurrentThread());
	boolean preState = Machine.interrupt().disable();//关中断
    while(!waitQueue.isEmpty()){
        KThread a = (KThread) waitQueue.removeFirst();//取出waitQueue中第一个线程
        a.ready();//将取出的线程启动
    }
    Machine.interrupt().restore(preState);//恢复中断
    }

    private Lock conditionLock;
    private LinkedList waitQueue;
}
