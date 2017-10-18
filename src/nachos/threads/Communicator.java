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
     * �ȴ�һ���߳�ͨ�����communicator������Ȼ�󴫴����������ߡ�
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     * ������߳���һ�����������߳����ǰ��Ҫ���ء�
     * ֻ��һ���߳�Ӧ���յ���䡣
     * @param	word	the integer to transfer.
     * word Ҫ���������
     */
    public void speak(int word) {
    	boolean status=Machine.interrupt().disable();
    	lock.acquire();
    	wsn++;
    	System.out.println(KThread.currentThread().getName()+"���ϱ����˸�speaker");
    	wd.addLast((Object)new Integer(word));
    	if(wsn>1){
    		System.out.println(KThread.currentThread().getName()+"��speaker���ţ���ֻspeaker��Ҫ˯��");
    		ssn++;wsn--;
//    		System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    		speaker.sleep();
    	}else if(sln==0 && wln==0){
    		System.out.println(KThread.currentThread().getName()+"û��listener����ֻspeaker��Ҫ˯��");
    		ssn++;wsn--;
//    		System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    		speaker.sleep();
    	}else{
    		wln++;sln--;
    		listener.wake();
    		System.out.println(KThread.currentThread().getName()+"listener����,speaker˵��"+word);
//    		System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    	}
    	wln--;
    	System.out.println(KThread.currentThread().getName()+"listenerע����,׼��������뿪");
    	lock.release();
//    	System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    	Machine.interrupt().restore(status);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     * �ȴ�һ���߳�ͨ�����communicator��Ȼ�󷵻ش��ݸ�speak()����䡣
     * @return	the integer transferred.
     */    
    public int listen() {
    	boolean status=Machine.interrupt().disable();
    	lock.acquire();
    	System.out.println(KThread.currentThread().getName()+"�򱱱����˸�listener");
    	wln++;
    	if(wln>1){
    		System.out.println(KThread.currentThread().getName()+",��listener����,��ֻlistener��Ҫ˯��");
    		wln--;sln++;
//    		System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    		listener.sleep();
    	}else if(ssn==0 && wsn==0){
    		System.out.println(KThread.currentThread().getName()+"û��speaker,��ֻlistener��Ҫ˯��");
    		wln--;sln++;
//    		System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    		listener.sleep();
    	}else{
    		wsn++;ssn--;
    		speaker.wake();
    		System.out.println(KThread.currentThread().getName()+"��һ��speaker����");
//    		System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    	}
    	wsn--;
    	System.out.println(KThread.currentThread().getName()+"speakerע����,׼��˵����뿪");
    	lock.release();
//    	System.out.println("������ "+wsn+" �����ѵ�Speaker,�� "+wln+" �����ѵ�Listener,�� "+ssn+" ����˯��Speaker,�� "+sln+" ����˯��Listener");
    	Machine.interrupt().restore(status);
    	Integer t= new Integer(wd.getFirst().toString());
    	wd.removeFirst();
    	return t.intValue();
    }
    
    
}
