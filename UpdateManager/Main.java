public class Main {
    public static void main(String[] args) throws Exception {
        String[] userNames = new String[] { "Anga205", "munish42" };

        LeetCodeApi api = new LeetCodeApi("https://leetcode-api-faisalshohag.vercel.app");
        ReadingsFile readingsFile = new ReadingsFile("ProgressViewer/public/readings.json");

        java.util.Map<String, java.util.List<Reading>> allReadings = readingsFile.load();
        allReadings = readingsFile.ensureKeys(allReadings, userNames);

        for (String name : userNames) {
            boolean finishedUser = false;
            while (!finishedUser) {
                try {
                    String json = api.getUserJson(name); // throws if HTTP != 200
                    Integer solved = api.parseTotalSolved(json);
                    if (solved == null) {
                        System.out.println("Unparsable response for " + name + ", retrying in 7s...");
                        Thread.sleep(7000);
                        continue; // retry
                    }

                    String key = name.toLowerCase();
                    java.util.List<Reading> userReadings = allReadings.get(key);
                    boolean shouldAppend = false;
                    if (userReadings == null || userReadings.isEmpty()) {
                        shouldAppend = true;
                    } else {
                        Reading last = userReadings.get(userReadings.size() - 1);
                        shouldAppend = solved > last.solvedCount;
                    }
                    if (shouldAppend) {
                        if (userReadings == null) {
                            userReadings = new java.util.ArrayList<>();
                            allReadings.put(key, userReadings);
                        }
                        double nowTs = TimeUtils.nowUtcSeconds();
                        userReadings.add(new Reading(solved, nowTs));
                    }
                    finishedUser = true; // valid response received (append or not)
                } catch (Exception ex) {
                    System.out.println("Error for " + name + ": " + ex.getMessage() + ". Retrying in 7s...");
                    try { Thread.sleep(7000); } catch (InterruptedException ie) { /* ignore */ }
                }
            }
        }

        readingsFile.save(allReadings);
    }
}
