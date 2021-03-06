package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
	Lib.assert(currentThread != null);
	return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
    //若不是第一个线程，创建一个新的TCB
	if (currentThread != null) {
	    tcb = new TCB();
	}
	//若当前线程是第一个线程
	else {
	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	    readyQueue.acquire(this);	
//	    joinQueue = ThreadedKernel.scheduler.newThreadQueue(true);

	    currentThread = this;
	    tcb = TCB.currentTCB();
	    name = "main";
	    restoreState();

	    createIdleThread();
	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
	this();
	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
	Lib.assert(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
	Lib.assert(status == statusNew);
	Lib.assert(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);

	boolean intStatus = Machine.interrupt().disable();

	tcb.start(new Runnable() {
		public void run() {
		    runThread();
		}
	    });

	ready();
	
	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	begin();
	target.run();
	finish();
    }

    private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assert(this == currentThread);

	restoreState();

	Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *可以直接调用，也会在方法执行完后自动调用
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     * 当前线程不能被直接校徽，因为它的栈以及其他执行状态还在使用中。在下一个线程运行时，当前进程自动被销毁，那时删除线程是安全的。
     */
    public static void finish() {
	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
	
	Machine.interrupt().disable();

	Machine.autoGrader().finishingCurrentThread();

	Lib.assert(toBeDestroyed == null);
	toBeDestroyed = currentThread;

	if(currentThread.joinQueue!=null){
		KThread waitThread= currentThread.joinQueue.nextThread(); //调用等待队列上的第一个进程;
		while (waitThread != null) //循环唤醒
		{
		    waitThread.ready(); //唤醒等待队列上所有被阻塞的进程
		    waitThread= currentThread.joinQueue.nextThread(); //调用等待队列上的第一个进程
		}
	}
	
	currentThread.status = statusFinished;
	
	sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	Lib.assert(currentThread.status == statusRunning);
	
	boolean intStatus = Machine.interrupt().disable();

	currentThread.ready();

	runNextThread();
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assert(Machine.interrupt().disabled());

	if (currentThread.status != statusFinished)
	    currentThread.status = statusBlocked;

	runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     * 将这个线程改编为就绪状态并添加到调度器的就绪队列
     */
    static int times=0;
    public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	System.out.println(currentThread.name+" "+(++times)+" STATUS:"+status);
	Lib.assert(Machine.interrupt().disabled());
	Lib.assert(status != statusReady);
	
	status = statusReady;
	if (this != idleThread)
	    readyQueue.waitForAccess(this);
	
	Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
	Lib.debug(dbgThread, "Joining to thread: " + toString());
	//不能自己调用join自己
	Lib.assert(this != currentThread);
//	Disable interrupts;关中断
	Machine.interrupt().setStatus(false);
//	  if (joinQueue not be initiated) {
//	      create a new thread queue (joinQueue) with transfer priority flag opened
//	      joinQueue acquires this thread as holder
//	  }
	if(joinQueue == null){
		joinQueue=ThreadedKernel.scheduler.newThreadQueue(true);
		joinQueue.acquire(this);
	}
//	  If (CurrentThread != self) and (status is not Finished) {
//	      add current thread to join queue
//	      sleep current thread 
//	  }
	if(currentThread!=this && status!=statusFinished ){
		joinQueue.waitForAccess(currentThread);
		KThread.sleep();
	}
//
//	  Re-enable interrupts;
		Machine.interrupt().setStatus(true);
    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
	Lib.assert(idleThread == null);
	
	idleThread = new KThread(new Runnable() {
	    public void run() { while (true) yield(); }
	});
	idleThread.setName("idle");

	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	if (nextThread == null)
	    nextThread = idleThread;

	nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
	Lib.assert(Machine.interrupt().disabled());

	Machine.yield();

	currentThread.saveState();

	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     * 当前线程准备运行
     */
    protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	
	Lib.assert(Machine.interrupt().disabled());
	Lib.assert(this == currentThread);
	Lib.assert(tcb == TCB.currentTCB());

	Machine.autoGrader().runningThread(this);
	
	status = statusRunning;

	if (toBeDestroyed != null) {
	    toBeDestroyed.tcb.destroy();
	    toBeDestroyed.tcb = null;
	    toBeDestroyed = null;
	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
	Lib.assert(Machine.interrupt().disabled());
	Lib.assert(this == currentThread);
    }

    private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
		System.out.println("hello");
//	    for (int i=0; i<5; i++) {
//		System.out.println("*** thread " + which + " looped "
//				   + i + " times");
//		System.out.println("But "+which+" yield");
//		KThread.yield();

//	    }
	}

	private int which;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
	Lib.debug(dbgThread, "Enter KThread.selfTest");
	
//	new KThread(new PingTest(1)).setName("forked thread").fork();
//	new PingTest(0).run();
	//task1 测试
		boolean status=Machine.interrupt().disable();
		test5();
	    Machine.interrupt().restore(status);
    }

    private static void test4(){
    	Communicator c=new Communicator();
	 	KThread a = new KThread(new Speaker(c)).setName("* Speakerr 1 *");
	 	KThread b = new KThread(new Listener(c)).setName("* Listener 1 *");
	 	KThread bb = new KThread(new Listener(c)).setName("* Listener 2 *");
	 	KThread aa = new KThread(new Speaker(c)).setName("* Speakerr 2 *");
	 	KThread cc = new KThread(new Speaker(c)).setName("* Speakerr 3 *");
	    System.out.println("begin running...");
	    a.fork();
	    System.out.println("a.run");
	    b.fork();
	    System.out.println("b.run");
	    aa.fork();
	    cc.fork();
	    bb.fork();
    }
    
    private static void test5(){
    	KThread kt2 = new KThread(new PingTest(2)).setName("kt2");
        KThread kt3 = new KThread(new PingTest(3)).setName("kt3");
        KThread kt4 = new KThread(new PingTest(4)).setName("kt4");
        PriorityScheduler ps = new PriorityScheduler();
        ThreadQueue tq1 = ps.newThreadQueue(true);
        ThreadQueue tq2 = ps.newThreadQueue(false);     
        tq1.waitForAccess(kt4);
        tq1.waitForAccess(kt3);
        tq1.waitForAccess(kt2);
        tq1.acquire(kt2);
        ps.setPriority(kt2, 2);
        ps.setPriority(kt3, 3);
        ps.setPriority(kt4, 4);
        System.out.println("kt2's Priority is " + ps.getPriority(kt2));
        System.out.println("kt2's effectivePriority is " + ps.getEffectivePriority(kt2));
        System.out.println("kt3's Priority is " + ps.getPriority(kt3));
        System.out.println("kt3's effectivePriority is " + ps.getEffectivePriority(kt3));
        KThread temp = tq2.nextThread();
        temp.fork();
//        kt3.join();
    }
    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. KThread构造器被调用的次数*/
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
    //为join方法而设计。等待当前进程结束的进程队列
    private ThreadQueue joinQueue=null;
}

class Speaker implements Runnable{

	Communicator c;
	public Speaker(Communicator con){
		c=con;
	}
	public void run() {
		for(int i=0;i<5;i++)
		{
			System.out.println(KThread.currentThread().getName()+" "+i+" * Speaker出发了，他想说 "+i);
			c.speak(i);
			System.out.println(KThread.currentThread().getName()+" "+i+" * 接回来一个Listener");
		}
	}
	
}
class Listener implements Runnable{

	Communicator c;
	public Listener(Communicator con){
		c=con;
	}
	public void run() {
		for(int i=0;i<5;i++){
			System.out.println(KThread.currentThread().getName()+" "+i+" * Listener要出发了");
			int t=c.listen();
			System.out.println(KThread.currentThread().getName()+" "+i+" *Listener叫醒的Speaker说了"+t);
//			System.out.println(KThread.currentThread().getName()+" "+i+" * Listener回来了，听见了"+t);
		}
	}
	
}
