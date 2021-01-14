package job_scheduler.util;

import job_scheduler.core.PipelineSettings;

public class Util {

    public static String convertTag(String tag) {
        return tag.isEmpty() ? "#Default" : tag;
    }

    public static String convertTagHash(String tag) {
        return tag.isEmpty() ? "" + ("#Default".hashCode()) : tag;
    }

    public static String convertMessageNewLines(String message)
    {
        if(message == null)
        {
            return "";
        }
        return message.replace("\n", "<br>");
    }

    public static String convertFactorToColor(double factor)
    {
        if(factor == -1) {
            return "background-color: #3232ff";
        }

        int r1, g1, b1, r2, g2, b2;

        factor = factor * 100;

        if(factor >= 0 && factor < 33) {
            r1 = 255;
            g1 = 0;
            b1 = 0;

            r2 = 255;
            g2 = 140;
            b2 = 0;

            factor = factor / 33.0;
        }
        else if(factor >= 33 && factor < 66) {

            r1 = 255;
            g1 = 140;
            b1 = 0;

            r2 = 255;
            g2 = 215;
            b2 = 0;

            factor = (factor - 33) / (66.0 - 33.0);
        }
        else {

            r1 = 255;
            g1 = 215;
            b1 = 0;

            r2 = 0;
            g2 = 153;
            b2 = 0;

            factor = (factor - 66) / (100.0 - 66.0);
        }

        int red = (int)(r1 + (r2 - r1) * factor + 0.5);
        int green = (int)(g1 + (g2 - g1) * factor + 0.5);
        int blue = (int)(b1 + (b2 - b1) * factor + 0.5);

        String rr = Integer.toHexString(red);
        String bb = Integer.toHexString(blue);
        String gg = Integer.toHexString(green);

        rr = rr.length() == 1 ? "0" + rr : rr;
        gg = gg.length() == 1 ? "0" + gg : gg;
        bb = bb.length() == 1 ? "0" + bb : bb;

        return "background-color: #" + rr+gg+bb;
    }

    public static String formatJobCount(long count) {
        if(count == 1) {
            return "" + count + " Job";
        }

        return "" + count + " Jobs";
    }

    public static String formatVariableCount(long count) {
        if(count == 1) {
            return "" + count + " Variable";
        }

        return "" + count + " Variables";
    }

    public static String formatTagCount(long count) {
        if(count == 1) {
            return "" + count + " Tag";
        }

        return "" + count + " Tags";
    }

    public static String formatPipelineCount(long count) {
        if(count == 1) {
            return "" + count + " Pipeline";
        }

        return "" + count + " Pipelines";
    }

    public static String calculateTime(double duration) {

        if(duration < 1000)
        {
            return duration == 1 ? (int)duration + " millisecond" : (int)duration + " milliseconds";
        }

        if(duration >= 1000 && duration < 60 * 1000)
        {
            double value = duration / 1000.0;

            String strVal = String.format("%.2f", value);

            return strVal.equals("1.00") ? strVal + " second" : strVal + " seconds";
        }

        if(duration >= 60 * 1000 && duration < 60 * 60 * 1000)
        {
            double value = duration / (1000.0 * 60);

            String strVal = String.format("%.2f", value);

            return strVal.equals("1.00") ? strVal + " minute" : strVal + " minutes";
        }

        double value = duration / (60 * 60 * 1000.0);

        String strVal = String.format("%.2f", value);

        return strVal.equals("1.00") ? strVal + " hour" : strVal + " hours";

    }

    public static String getPercentageFormatted(double factor) {

        if(factor == -1) {
            return "N/A";
        }

        return String.format("%6.2f" , 100 * factor) + "%";

    }

    public static String getPercentageValue(double factor) {

        if(factor == -1) {
            return "N/A";
        }

        return "" + (int)(100 * factor);

    }

    public static String getStatusClass(int status) {
        if(status == PipelineSettings.UNINITIALIZED) {
            return "warning";
        }
        else if(status == PipelineSettings.OK) {
            return "success";
        }
        else if(status == PipelineSettings.FAILED) {
            return "danger";
        }

        return "";
    }
}
