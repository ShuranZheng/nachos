package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Total # Queue Allocated
	 */
	private int numQueue = 0;

	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority, numQueue++);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

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

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue implements Comparable<PriorityQueue> {
		/**
		 * The ID of this queue.
		 */
		private int ID;
		/**
		 * Maximum priority of threads who are waiting for this resource.
		 */
		private int maxPriority = priorityMinimum;
		/**
		 * Total # thread
		 */
		private long numThread = 0;
		/**
		 * A Tree Set consisted of all threads who are waiting for this resource. 
		 */
		private TreeSet<ThreadState> threads = new TreeSet<ThreadState>();
		/**
		 * Owner of this resource
		 */
		private KThread owner = null;
		PriorityQueue(boolean transferPriority, int ID) {
			this.transferPriority = transferPriority;
			this.ID = ID;
		}
		private void update()
		{
			int oldMax = maxPriority; 
			int newMax = priorityMinimum;
			if (threads.isEmpty() == false && transferPriority)
				newMax = threads.first().ePriority;
			if (oldMax == newMax)
				return;
			if (owner != null){
				ThreadState ownerState = getThreadState(owner);
				ownerState.owningResource.remove(this);
				maxPriority = newMax;
				ownerState.owningResource.add(this);
				ownerState.update();
			}
			else
				maxPriority = newMax;
		}
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState current = getThreadState(thread);
			current.waitForAccess(this);
			current.index = numThread++;
			threads.add(current);
			update();
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (owner != null){
				ThreadState ownerState = getThreadState(owner);
				ownerState.owningResource.remove(this);
				ownerState.update();
				owner = null;
			}
			getThreadState(thread).acquire(this);
			owner = thread;
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (owner != null){
				ThreadState ownerState = getThreadState(owner);
				ownerState.owningResource.remove(this);
				ownerState.update();
				owner = null;
			}
			if (threads.isEmpty())
				return null;
			
			ThreadState newOwner = threads.pollFirst();
			newOwner.waitingResource = null;
			newOwner.owningResource.add(this);
			owner = newOwner.thread;
			update();
			return owner;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			return threads.first();
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			// Unfortunately, I do NOT want to implement you. 
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		@Override
		public int compareTo(PriorityQueue o) {
			if (maxPriority != o.maxPriority)
				return o.maxPriority > maxPriority ? 1 : -1;
			else if (ID != o.ID)
				return ID < o.ID ? -1 : 1;
			else
				return 0;
		}
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState implements Comparable<ThreadState>{
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.priority = this.ePriority = priorityDefault;
		}

		public void update() {
			int previousPriority = ePriority;
			int newPriority = this.priority;
			if (owningResource.isEmpty() == false)
				newPriority = Math.max(newPriority, owningResource.first().maxPriority);
			if (previousPriority != newPriority){
				if (waitingResource != null){
					waitingResource.threads.remove(this);
					ePriority = newPriority;
					waitingResource.threads.add(this);
					waitingResource.update();
				}
				else
					ePriority = newPriority;
			}
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			return ePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			update();
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 *
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			waitingResource = waitQueue;
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			owningResource.add(waitQueue);
			update();
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/**
		 * Effective priority of this thread.
		 */
		protected int ePriority; 
		/**
		 * Index of added.
		 */
		protected long index; 
		@Override
		public int compareTo(ThreadState o) {
			if (ePriority != o.ePriority)
				return o.ePriority > ePriority ? 1 : -1;
				else if (index != o.index)
					return index < o.index ? -1 : 1;
				else 
					return 0;
		}
		protected PriorityQueue waitingResource = null;
		protected TreeSet<PriorityQueue> owningResource = new TreeSet<PriorityQueue>();
		
	}
}
