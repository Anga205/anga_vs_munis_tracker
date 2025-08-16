public class Main {
    public static void main(String[] args) throws Exception {
        String[] userNames = new String[] { "Anga205", "munish42" };

        LeetCodeApi api = new LeetCodeApi("https://leetcode-api-faisalshohag.vercel.app");
        ReadingsFile readingsFile = new ReadingsFile("ProgressViewer/public/readings.json");

        java.util.Map<String, java.util.List<Reading>> allReadings = readingsFile.load();
        allReadings = readingsFile.ensureKeys(allReadings, userNames);

        double now = TimeUtils.nowUtcSeconds();

        for (String name : userNames) {
            try {
                String json = api.getUserJson(name);
                Integer solved = api.parseTotalSolved(json);
                if (solved != null) {
                    allReadings.get(name.toLowerCase()).add(new Reading(solved, now));
                }
            } catch (Exception ex) {
                System.out.println("Error for " + name + ": " + ex.getMessage());
            }
        }

        readingsFile.save(allReadings);
    }
}
