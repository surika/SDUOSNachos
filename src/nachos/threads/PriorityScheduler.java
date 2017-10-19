package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 * һ�������߳����ȼ�ѡ���̵߳ĵ�����
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *�̵߳�������ÿ���߳���һ�����ȼ���ϵ��һ����һ��Ҫ���ӵ��߳���Զ�����ȼ������������ȴ��̵߳ġ�����RR��ת��������
 *Ҫ���ӵ��̣߳�������ӵ����ͬ����ߣ������ȼ����߳��У����Ҫ���ӵ��ǵȴ�ʱ������Ǹ���
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 * �ӱ����Ͻ���һ�����ȼ���������������ת�ķ�ʽ�������е���������ȼ����̣߳����������������̡߳�
 *�����Զ��һ���̵߳ȴ������ȼ����ߵ��̣߳����ж����̵߳Ŀ����ԡ�
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 * ���ȼ����������벿�ֽ�����ȼ�ת�����⡣�ر�ģ����ȼ�����ͨ������joins������
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     * �����µ����ȼ��̶߳���
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    //��ȡ���ȼ�
    public int getPriority(KThread thread) {
	Lib.assert(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }
    
    //��ȡ��Ч���ȼ�
    public int getEffectivePriority(KThread thread) {
	Lib.assert(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    //�������ȼ�
    public void setPriority(KThread thread, int priority) {
	Lib.assert(Machine.interrupt().disabled());
		       
	Lib.assert(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }
    //������ȼ�
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
    //�������ȼ�
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
    
    //�����ض��̵߳�״̬
    /**
     * Return the scheduling state of the specified thread.
     *��ȡ�ض��̵߳ĵ���״̬
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     * 
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }
    //�� �̶߳���
    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     * �� ���ȼ�����
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
    	 * �����Ƿ���Դӵȴ����߳���ת�����ȼ�������߳�
    	 */
    	public boolean transferPriority;
    	
    	//���캯��
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}
	//���
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
	 *����nextThread()���᷵�ص���һ���߳�
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
     * ��װ�õ� �������ȼ��������� �߳�״̬ ��
     * �̵߳ĵ���״̬�������߳����ȼ�����Ч���ȼ���ӵ�еĶ��󡢵ȴ��Ķ��С�
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
		//��ԱΪPriorityQueue���ͣ�Ϊ�ȴ����̳߳��е���Դ���̶߳���
		protected LinkedList res;
		private boolean dirty=false;
		//���߳����ڵȴ�����Դ
		private ThreadQueue waitingOn;
		
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *���캯����Ϊָ���̹߳����߳�״̬
		 * @param	thread	the thread this state belongs to.
		 */
    	public ThreadState(KThread thread) {
	    this.thread = thread;
	    res=new LinkedList();
	    //����ʱʹ��Ĭ�ϵ����ȼ�
	    setPriority(priorityDefault);
    	}

	/**
	 	* Return the priority of the associated thread.
	 *�����߳����ȼ�
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
    		//���̵߳���Ч���ȼ�������Ϊ��ǰ���ȼ�
    		int maxEffective=priority;
    		//�����Ч���ȼ��Ķ����������Ч���ȼ�����Ϊ�ȴ�����߳���Դ�������߳������ȼ���ߵ�ֵ
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
		    //���ȼ��ı��ˣ���Ч���ȼ���֮�޸�
		    setDirty();
		}
	
		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 * �ض����ȼ����е�waitForAccess(thread)����������ʱ���ô˷���
		 * ��ˣ�����̵߳ȴ���ȡwaitQueue��������Դ�ķ������������ֻ���ڹ����̲߳���������ȡ��Դʱ���á�
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assert(Machine.interrupt().disabled());
			//�ж�Ҫ����Ķ�������û���ң����Ƿ��������ڵȴ��������Դ��������Ǿͼ������������ظ�����
//			Lib.assert(waitQueue.waitQueue.indexOf(thread)==-1);
			if(waitQueue.waitQueue.indexOf(thread)!=-1)return;
			
			waitQueue.waitQueue.add(thread);
			//��������Դ���������¶��У��ռ���Ķ��п�����Ϊ�ҵļ�����ı����ȼ�
			waitQueue.setDirty();
			
			waitingOn=waitQueue;
			//���ȴ����̵߳Ķ����д������Ҫ����Ķ���
			if(res.indexOf(waitQueue)!=-1){
				//�ӵȴ����̵߳Ķ���������
				res.remove(waitQueue);
				//Ҫ����Ķ���û�г�����
				waitQueue.holder=null;
			}
		}
	
		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * �������̻߳����waitQueue���е���Դ����á�
		 * ������ΪwaitQueue(���е��߳��ǵ�ǰ��������߳�)��acquire(thread)�����õĽ��
		 * ��waitQueue��nextThread()�����õĽ��
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
		    //��ǰ�߳����ڳ��������Դ����ԭ�����ڵĶ��������ڵȴ���
			res.add(waitQueue);
			//�Ѿ��������Դ�����ٵȴ���Դ�ˣ��ʲ��ٴ���ĳ��������
			if(waitQueue==waitingOn)
				waitingOn=null;
			//��Դ������������ı�����ȼ�״��Ҳ����Ӧ�ı�
			setDirty();
		}	
		
		//�������̵߳����ȼ��޸Ĺ��ˣ��������ȼ���Ҫ���»�ȡ
		public void setDirty(){
			//����Ѿ����Ϊ�޸Ĺ�������
			if(dirty)return;
			dirty=true;
			//��ȡ��ǰ���ڵȴ��Ķ��С���Ϊ��ǰ�̴߳��ڴ˶����У���ǰ�߳����ȼ��ĸı���ܵ���
			//�����ڶ������ȼ�Ҳ�����ı䡣
			PriorityQueue pg=(PriorityQueue) waitingOn;
			if(pg!=null)
				pg.setDirty();
		}
	
		
    }
}
