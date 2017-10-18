package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	
	private Lock lock;
	private Condition2 listener,speaker;
	private int ssn,sln,wsn,wln;
	private LinkedList wd;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	lock=new Lock();
    	wd=new LinkedList();
    	listener=new Condition2(lock);
    	speaker=new Condition2(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     * 等待一个线程通过这个communicator倾听，然后传达语句给倾听者。
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     * 在这个线程与一个正在听的线程配对前不要返回。
     * 只有一个线程应该收到语句。
     * @param	word	the integer to transfer.
     * word 要传达的整数
     */
    public void speak(int word) {
    	boolean status=Machine.interrupt().disable();
    	lock.acquire();
    	wsn++;
    	System.out.println(KThread.currentThread().getName()+"打南边来了个speaker");
    	wd.addLast((Object)new Integer(word));
    	if(wsn>1){
    		System.out.println(KThread.currentThread().getName()+"有speaker醒着，这只speaker快要睡了");
    		ssn++;wsn--;
//    		System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    		speaker.sleep();
    	}else if(sln==0 && wln==0){
    		System.out.println(KThread.currentThread().getName()+"没有listener，这只speaker快要睡了");
    		ssn++;wsn--;
//    		System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    		speaker.sleep();
    	}else{
    		wln++;sln--;
    		listener.wake();
    		System.out.println(KThread.currentThread().getName()+"listener醒了,speaker说了"+word);
//    		System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    	}
    	wln--;
    	System.out.println(KThread.currentThread().getName()+"listener注销了,准备听完就离开");
    	lock.release();
//    	System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    	Machine.interrupt().restore(status);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     * 等待一个线程通过这个communicator表达，然后返回传递给speak()的语句。
     * @return	the integer transferred.
     */    
    public int listen() {
    	boolean status=Machine.interrupt().disable();
    	lock.acquire();
    	System.out.println(KThread.currentThread().getName()+"打北边来了个listener");
    	wln++;
    	if(wln>1){
    		System.out.println(KThread.currentThread().getName()+",有listener醒着,这只listener快要睡了");
    		wln--;sln++;
//    		System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    		listener.sleep();
    	}else if(ssn==0 && wsn==0){
    		System.out.println(KThread.currentThread().getName()+"没有speaker,这只listener快要睡了");
    		wln--;sln++;
//    		System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    		listener.sleep();
    	}else{
    		wsn++;ssn--;
    		speaker.wake();
    		System.out.println(KThread.currentThread().getName()+"有一个speaker醒了");
//    		System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    	}
    	wsn--;
    	System.out.println(KThread.currentThread().getName()+"speaker注销了,准备说完就离开");
    	lock.release();
//    	System.out.println("现在有 "+wsn+" 个清醒的Speaker,有 "+wln+" 个清醒的Listener,有 "+ssn+" 个沉睡的Speaker,有 "+sln+" 个沉睡的Listener");
    	Machine.interrupt().restore(status);
    	Integer t= new Integer(wd.getFirst().toString());
    	wd.removeFirst();
    	return t.intValue();
    }
    
    
}
