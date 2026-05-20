import java.util.Map;

public class ReportDTO {

    private long totalEmployees;
    private double attritionRate;
    private double avgAttendance;
    private double productivityScore;

    private Map<String, Integer> headcountTrend;
    private Map<String, Integer> productivityTrend;
    private Map<String, Integer> attendanceByDept;

}
