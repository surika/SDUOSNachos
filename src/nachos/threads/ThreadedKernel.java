package nachos.threads;

import nachos.machine.*;

/**
 * A multi-threaded OS kernel.
 * 一个多线程操作系统内核
 */
public class ThreadedKernel extends Kernel {
    /**
     * Allocate a new multi-threaded kernel.
     */
    public ThreadedKernel() {
    	super();
    }

    /**
     * Initialize this kernel. Creates a scheduler, the first thread, and an
     * alarm, and enables interrupts. Creates a file system if necessary.   
     * 创建调度程序、第一个线程、时钟、允许中断、
     */
    public void initialize(String[] args) {
	// set scheduler设置调度程序
	String schedulerName = Config.getString("ThreadedKernel.scheduler");
	scheduler = (Scheduler) Lib.constructObject(schedulerName);

	// set fileSystem设置文件系统
	String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
	if (fileSystemName != null)
	    fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
	else if (Machine.stubFileSystem() != null)
	    fileSystem = Machine.stubFileSystem();
	else
	    fileSystem = null;

	// start threading启动线程
	new KThread(null);

	alarm  = new Alarm();

	Machine.interrupt().enable();
    }

    /**
     * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
     * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes. Note that the
     * autograder never calls this method, so it is safe to put additional
     * tests here.
     */	
    public void selfTest() {
	KThread.selfTest();
	Semaphore.selfTest();
	SynchList.selfTest();
	Boat.selfTest();
	if (Machine.bank() != null) {
	    ElevatorBank.selfTest();
	}
    }
    
    /**
     * A threaded kernel does not run user programs, so this method does
     * nothing.
     */
    public void run() {
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	Machine.halt();
    }

    /** Globally accessible reference to the scheduler. */
    public static Scheduler scheduler = null;
    /** Globally accessible reference to the alarm. */
    public static Alarm alarm = null;
    /** Globally accessible reference to the file system. */
    public static FileSystem fileSystem = null;

    // dummy variables to make javac smarter
    private static RoundRobinScheduler dummy1 = null;
    private static PriorityScheduler dummy2 = null;
    private static LotteryScheduler dummy3 = null;
    private static Condition2 dummy4 = null;
    private static Communicator dummy5 = null;
    private static Rider dummy6 = null;
    private static ElevatorController dummy7 = null;
}
