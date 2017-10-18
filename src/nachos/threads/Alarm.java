package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	LinkedList waitingList;
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    waitingList=new LinkedList();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
//    KThread.currentThread().yield();
    boolean status=Machine.interrupt().disable();
	for(Iterator it=waitingList.iterator();it.hasNext();){
		Waitings wait=(Waitings) it.next();
		if(Machine.timer().getTime()>=wait.getDueTime()){
			it.remove();
			wait.getThread().ready();
		}
		
	}
	Machine.interrupt().restore(status);
	
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
    boolean status=Machine.interrupt().disable();
	long wakeTime = Machine.timer().getTime() + x;
	Waitings w=new Waitings(wakeTime,KThread.currentThread());
	waitingList.add(w);
	KThread.sleep();
	Machine.interrupt().restore(status);
//	while (wakeTime > Machine.timer().getTime())
//	    KThread.yield();
	
    }
    
    class Waitings{
    	private long dueTime;
    	private KThread thread;
    	Waitings(long dt,KThread th){
    		dueTime=dt;
    		thread=th;
    	}
		public long getDueTime() {
			return dueTime;
		}
		public void setDueTime(long dueTime) {
			this.dueTime = dueTime;
		}
		public KThread getThread() {
			return thread;
		}
		public void setThread(KThread thread) {
			this.thread = thread;
		}
    }
}
