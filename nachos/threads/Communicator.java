package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	
	Lock lock = new Lock();
	Condition waitingListener = new Condition(lock);
	Condition activeListener = new Condition(lock);
	Condition activeSpeaker = new Condition(lock);
	Condition waitingSpeaker = new Condition(lock);
	int word = 0, listener = 0, speaker = 0;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	while (speaker > 0){
    		waitingSpeaker.sleep();
    	}
    	speaker ++;
    	this.word = word;
    	if (listener > 0) activeListener.wake();
    	else activeSpeaker.sleep();
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	int ret = 0;
    	lock.acquire();
    	while (listener > 0){
    		waitingListener.sleep();
    	}
    	listener ++;
    	while (speaker <= 0){
    		activeListener.sleep();
    	}
    	ret = word;
    	speaker --;
    	listener --;
    	waitingSpeaker.wake();
    	waitingListener.wake();
    	activeSpeaker.wake();
    	lock.release();
    	return ret;
    }
}
