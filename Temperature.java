import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class Temperature {

    private static final int THREAD_COUNT = 8;
    private static final int MINUTES = 60;
    private static final int HOURS = 72;
    private static Lock lock = new ReentrantLock();

    private static boolean allSensorsReady(int caller, List<Boolean> sensors) {
        for (int i = 0; i < sensors.size(); i++) {
            if (!sensors.get(i) && caller != i) {
                return false;
            }
        }
        return true;
    }

    private static void printLargestDifference(List<Integer> sensorReadings) {
        int step = 10;
        int startInterval = 0;
        int maxDifference = Integer.MIN_VALUE;

        for (int threadIndex = 0; threadIndex < THREAD_COUNT; threadIndex++) {
            int offset = threadIndex * MINUTES;

            for (int i = offset; i < MINUTES - step + 1; i++) {
                List<Integer> subList = sensorReadings.subList(i, i + step);
                int max = Collections.max(subList);
                int min = Collections.min(subList);
                int diff = max - min;

                if (diff > maxDifference) {
                    maxDifference = diff;
                    startInterval = i;
                }
            }
        }

        System.out.println("Largest temperature difference: " + maxDifference + "F"
                + " starting at minute " + startInterval
                + " and ending at minute " + (startInterval + 10));
    }

    private static void printHighestTemperatures(List<Integer> sensorReadings) {
        Set<Integer> temperatures = new HashSet<>();

        for (int i = sensorReadings.size() - 1; i >= 0; i--) {
            if (temperatures.add(sensorReadings.get(i)) && temperatures.size() == 5) {
                break;
            }
        }

        System.out.println("Highest temperatures: " + temperatures);
    }

    private static void printLowestTemperatures(List<Integer> sensorReadings) {
        Set<Integer> temperatures = new HashSet<>();

        for (Integer sensorReading : sensorReadings) {
            if (temperatures.add(sensorReading) && temperatures.size() == 5) {
                break;
            }
        }

        System.out.println("Lowest temperatures: " + temperatures);
    }

    private static void generateReport(int hour, List<Integer> sensorReadings) {
        System.out.println("Hour " + (hour + 1) + " report:");

        printLargestDifference(sensorReadings);

        Collections.sort(sensorReadings);

        printHighestTemperatures(sensorReadings);
        printLowestTemperatures(sensorReadings);

        System.out.println();
    }

    private static void measureTemperature(int threadId, List<Integer> sensorReadings, List<Boolean> sensorsReady) {
        for (int hour = 0; hour < HOURS; hour++) {
            for (int minute = 0; minute < MINUTES; minute++) {
                sensorsReady.set(threadId, false);
                sensorReadings.set(minute + (threadId * MINUTES), Util.generateRandomNumber(-100, 70));
                sensorsReady.set(threadId, true);

                while (!allSensorsReady(threadId, sensorsReady)) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (threadId == 0) {
                lock.lock();
                try {
                    generateReport(hour, sensorReadings);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static class Util {
        private static final Random random = new Random();

        public static int generateRandomNumber(int min, int max) {
            return random.nextInt((max - min) + 1) + min;
        }
    }

    public static void main(String[] args) {
        List<Integer> sensorReadings = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(THREAD_COUNT * MINUTES, 0)));
        List<Boolean> sensorsReady = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(THREAD_COUNT, false)));
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int finalI = i;
            Thread thread = new Thread(() -> measureTemperature(finalI, sensorReadings, sensorsReady));
            threads.add(thread);
            thread.start();
        }

        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}