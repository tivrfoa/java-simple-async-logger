package org.tivrfoa;


public class InterruptUtil {

    final boolean previouslyInterrupted;

    public InterruptUtil() {
        previouslyInterrupted = Thread.currentThread().isInterrupted();
    }

    public void maskInterruptFlag() {
        if (previouslyInterrupted) {
            Thread.interrupted();
        }
    }

    public void unmaskInterruptFlag() {
        if (previouslyInterrupted) {
            try {
                Thread.currentThread().interrupt();
            } catch (SecurityException se) {
                se.printStackTrace();
                System.err.println("Failed to intrreupt current thread");
            }
        }
    }

}
