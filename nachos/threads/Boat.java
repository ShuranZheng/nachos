package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		//System.out.println("\n ***Testing Boats with only 2 children***");
		//begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		 begin(2, 9, b);
	}
	
	
	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		
		for (int i = 0; i < children; i++){
			KThread kidi = new KThread(new Runnable(){
				public void run() {
					ChildItinerary();
				}
			}).setName("Kid "+i);
			kidi.fork();
		}
		
		for (int i = 0; i < adults; i++){
			KThread adulti = new KThread(new Runnable(){
				public void run() {
					AdultItinerary();
				}
			}).setName("Adult "+i);
			adulti.fork();
		}
		
		while (oahu.adultsNum+oahu.kidsNum < adults+children)
			KThread.yield();
		
		lock.acquire();
		if (oahu.kidsNum >= 2 || oahu.adultsNum == 0)
			oahu.kid.wake();
		else
			oahu.adult.wake();
		lock.release();
		
		while (molokai.adultsNum+molokai.kidsNum < adults+children){
			lock.acquire();
			arrived.wake();
			lock.release();
		}

//		Lib.assertTrue(bg.isFinished());

	}
		
	static void AdultItinerary() {
		bg.initializeAdult(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.

		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
		
		lock.acquire();
		oahu.adultsNum++;
		oahu.adult.sleep();
		while (oahu.kidsNum >= 2)
			oahu.adult.sleep();
		oahu.adultsNum--;
		bg.AdultRowToMolokai();
		molokai.adultsNum++;
		arrived.sleep();
		molokai.kid.wake();
		lock.release();
	}

	static void ChildItinerary() {
		bg.initializeChild(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.
		
		oahu.kidsNum++;
		for (;;) {
			lock.acquire();
			oahu.kid.sleep();
			while (oahu.kidsNum < 2 && oahu.adultsNum > 0 && !waiting)
				oahu.kid.sleep();
			if (waiting){
				oahu.kidsNum--;
				bg.ChildRideToMolokai();
				molokai.kidsNum++;
				waiting = false;
				arrived.sleep();
				waitingCond.wake();
			}
			else {
				oahu.kidsNum--;
				bg.ChildRowToMolokai();
				if (oahu.kidsNum > 0) {
					waiting = true;
					oahu.kid.wake();
					waitingCond.sleep();
					molokai.kidsNum++;
					arrived.sleep();
					molokai.kid.wake();
				}
			}
			molokai.kid.sleep();
			molokai.kidsNum--;
			bg.ChildRowToOahu();
			oahu.kidsNum++;
			if (oahu.kidsNum >= 2 || oahu.adultsNum == 0)
				oahu.kid.wake();
			else
				oahu.adult.wake();
			lock.release();
		}
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

	private static Lock lock = new Lock();
	
	private static class Location{
		int adultsNum = 0, kidsNum = 0;
		Condition adult = new Condition(lock), kid = new Condition(lock);
	}
	
	private static Location oahu = new Location(), molokai = new Location();
	
	private static boolean waiting = false;
	
	private static Condition waitingCond = new Condition(lock), arrived = new Condition(lock);

	
}
