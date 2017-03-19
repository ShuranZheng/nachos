package nachos.threads;

import java.util.LinkedList;
import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
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
    boolean intStatus = Machine.interrupt().disable();
    WaitingThread thread = waitingQueue.poll();
    while (thread != null && thread.time <= Machine.timer().getTime()){
    	thread.thread.ready();
    	thread = waitingQueue.poll();
    }
    Machine.interrupt().restore(intStatus);
    KThread.currentThread().yield();
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
    boolean intStatus = Machine.interrupt().disable();
    long wakeTime = Machine.timer().getTime() + x;
	waitingQueue.add(new WaitingThread(wakeTime, KThread.currentThread()));
	KThread.sleep();
    Machine.interrupt().restore(intStatus);
    	
	//while (wakeTime > Machine.timer().getTime())
	 //   KThread.yield();
    }
    
    private class WaitingThread implements Comparable<WaitingThread>{
    	long time;
    	KThread thread;
		@Override
		public int compareTo(WaitingThread arg0) {
			// TODO Auto-generated method stub
			if (time < arg0.time) return -1;
			if (time > arg0.time) return 1;
			return 0;
		}
		
		public WaitingThread(long time, KThread thread){
			this.time = time;
			this.thread = thread;
		}
    }
    
    private PriorityQueue<WaitingThread> waitingQueue;
}
