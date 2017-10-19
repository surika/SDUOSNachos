package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 * 一个根据线程优先级选择线程的调度器
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *线程调度器将每个线程与一个优先级联系在一起。下一个要出队的线程永远是优先级不低于其他等待线程的。就像RR轮转调度器，
 *要出队的线程，在所有拥有相同（最高）的优先级的线程中，这个要出队的是等待时间最长的那个。
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 * 从本质上讲，一个优先级调度器可以用轮转的方式调度所有的有最高优先级的线程，并无视其他所有线程。
 *如果永远有一个线程等待着优先级更高的线程，这有饿死线程的可能性。
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 * 优先级调度器必须部分解决优先级转换问题。特别的，优先级必须通过锁与joins来捐赠
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     * 建立新的优先级线程队列
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    //获取优先级
    public int getPriority(KThread thread) {
	Lib.assert(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }
    
    //获取有效优先级
    public int getEffectivePriority(KThread thread) {
	Lib.assert(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    //设置优先级
    public void setPriority(KThread thread, int priority) {
	Lib.assert(Machine.interrupt().disabled());
		       
	Lib.assert(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }
    //提高优先级
    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }
    //降低优先级
    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    
    
    //返回特定线程的状态
    /**
     * Return the scheduling state of the specified thread.
     *获取特定线程的调度状态
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     * 
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }
    //类 线程队列
    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     * 类 优先级队列
     */
    protected class PriorityQueue extends ThreadQueue {
    	
    	/** The queue  waiting on this resource */
        private LinkedList waitQueue = new LinkedList();

        /** The ThreadState corresponds to the holder of the resource */
        private ThreadState holder = null; 

        /** Set to true when a new thread is added to the queue, 
         *  or any of the queues in the waitQueue flag themselves as dirty */
        private boolean dirty;               
        /** The cached highest of the effective priorities in the waitQueue. 
         *  This value is invalidated while dirty is true */
        private int effectivePriority; 
    	/**
    	 * <tt>true</tt> if this queue should transfer priority from waiting
    	 * threads to the owning thread.
    	 * 队列是否可以从等待的线程中转换优先级到这个线程
    	 */
    	public boolean transferPriority;
    	
    	//构造函数
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}
	//入队
	public void waitForAccess(KThread thread) {
	    Lib.assert(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}
	
	public void acquire(KThread thread) {
		Lib.assert(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assert(Machine.interrupt().disabled());
	    // TODO:implement me
//	    if I have a holder and I transfer priority, remove myself from the holder's resource list
//	    if waitQueue is empty, return null
//	       ThreadState firstThread = pickNextThread();
//	       remove firstThread from waitQueue
//	       firstThread.acquire(this);
//	    return firstThread
	    if(holder!=null && transferPriority){
	    	holder.res.remove(this);
	    }
	    if(waitQueue.isEmpty())
	    	return null;
	    ThreadState firstThread=pickNextThread();
	    waitQueue.remove(firstThread);
	    firstThread.acquire(this);
	    return firstThread.thread;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *返回nextThread()将会返回的下一个线程
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
		 // TODO:implement me
//		ThreadState ret = null
//				for each ThreadState ts in waitQueue
//				   if ret is null OR ts has higher priority/time ranking than ret
//				      set ret to ts
//				return ret;
		ThreadState ret=null;
		for (Iterator it = waitQueue.iterator(); it.hasNext();) {  
            ThreadState ts = getThreadState((KThread) it.next());
            if(ret==null || ts.getEffectivePriority()>ts.getEffectivePriority() )
            	ret=ts;
        }
	    return ret;
	}
	
	public int getEffectivePriority() {

        // System.out.print("[Inside getEffectivePriority] transferPriority: " + transferPriority + "\n"); // debug

        // if do not transfer priority, return minimum priority
        if (transferPriority == false) {
        // System.out.print("Inside 'getEffectivePriority:' false branch\n" ); // debug
            return priorityMinimum;
        }

        if (dirty) {
            effectivePriority = priorityMinimum; 
            for (Iterator it = waitQueue.iterator(); it.hasNext();) {  
                KThread thread = (KThread) it.next(); 
                int priority = getThreadState(thread).getEffectivePriority();
                if ( priority > effectivePriority) { 
                    effectivePriority = priority;
                }
            }
            dirty = false;
        }

        return effectivePriority;
    }

    public void setDirty() {
        if (transferPriority == false) {
            return;
        }

        dirty = true;

        if (holder != null) {
            holder.setDirty();
        }
    }

    public void print() {
        Lib.assert(Machine.interrupt().disabled());
        // implement me (if you want)
        for (Iterator it = waitQueue.iterator(); it.hasNext();) {  
            KThread currentThread = (KThread) it.next(); 
            int  priority = getThreadState(currentThread).getPriority();

            System.out.print("Thread: " + currentThread 
                                + "\t  Priority: " + priority + "\n");
        }
    }
	
//	public void print() {
//	    Lib.assert(Machine.interrupt().disabled());
//	    // TODO:implement me
//	    // implement me (if you want)
//	}

	
    }
    //
    /**
     * 封装好的 用于优先级调度器的 线程状态 类
     * 线程的调度状态。包括线程优先级、有效优先级、拥有的对象、等待的队列。
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState 
    {
    	/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;
		//成员为PriorityQueue类型，为等待该线程持有的资源的线程队列
		protected LinkedList res;
		private boolean dirty=false;
		//该线程正在等待的资源
		private ThreadQueue waitingOn;
		
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *构造函数，为指定线程关联线程状态
		 * @param	thread	the thread this state belongs to.
		 */
    	public ThreadState(KThread thread) {
	    this.thread = thread;
	    res=new LinkedList();
	    //构造时使用默认的优先级
	    setPriority(priorityDefault);
    	}

	/**
	 	* Return the priority of the associated thread.
	 *返回线程优先级
	 * @return	the priority of the associated thread.
	 */
    	public int getPriority() {
	    return priority;
	}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
    	public int getEffectivePriority() {
    		//该线程的有效优先级，设置为当前优先级
    		int maxEffective=priority;
    		//如果有效优先级改动过，则把有效优先级设置为等待这个线程资源的所有线程中优先级最高的值
    		if(dirty){
    			for(Iterator it=res.iterator();it.hasNext();){
    				PriorityQueue pg=(PriorityQueue)(it.next());
    				int effective=pg.getEffectivePriority();
    				if(maxEffective<effective){
    					maxEffective=effective;
    				}
    			}
    		}
    		return maxEffective;
    	}
		
	/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
		    if (this.priority == priority)
			return;
		    
		    this.priority = priority;
		    //优先级改变了，有效优先级随之修改
		    setDirty();
		}
	
		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 * 特定优先级队列的waitForAccess(thread)方法被调用时调用此方法
		 * 因此，相关线程等待获取waitQueue保护的资源的方法。这个方法只能在关联线程不能立即获取资源时调用。
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assert(Machine.interrupt().disabled());
			//判断要加入的队列中有没有我，即是否还是我正在等待的这个资源，如果不是就继续，是则不用重复操作
//			Lib.assert(waitQueue.waitQueue.indexOf(thread)==-1);
			if(waitQueue.waitQueue.indexOf(thread)!=-1)return;
			
			waitQueue.waitQueue.add(thread);
			//申请新资源，加入了新队列，刚加入的队列可能因为我的加入而改变优先级
			waitQueue.setDirty();
			
			waitingOn=waitQueue;
			//若等待该线程的队列中存在这次要加入的队列
			if(res.indexOf(waitQueue)!=-1){
				//从等待该线程的队列中移走
				res.remove(waitQueue);
				//要加入的队列没有持有者
				waitQueue.holder=null;
			}
		}
	
		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 当关联线程获得了waitQueue持有的资源后调用。
		 * 可以作为waitQueue(其中的线程是当前对象相关线程)的acquire(thread)被调用的结果
		 * 或waitQueue中nextThread()被调用的结果
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
		    //当前线程现在持有这个资源，故原来所在的队列现在在等待我
			res.add(waitQueue);
			//已经获得了资源，不再等待资源了，故不再处于某个队列中
			if(waitQueue==waitingOn)
				waitingOn=null;
			//资源持有情况发生改变后，优先级状况也会相应改变
			setDirty();
		}	
		
		//标记这个线程的优先级修改过了，所以优先级需要重新获取
		public void setDirty(){
			//如果已经标记为修改过，返回
			if(dirty)return;
			dirty=true;
			//获取当前正在等待的队列。因为当前线程处在此队列中，当前线程优先级的改变可能导致
			//其所在队列优先级也发生改变。
			PriorityQueue pg=(PriorityQueue) waitingOn;
			if(pg!=null)
				pg.setDirty();
		}
	
		
    }
}
