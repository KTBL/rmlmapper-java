package be.ugent.rml.functions.lib;

import be.ugent.rml.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.slugify.Slugify;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class IDLabFunctions {

    private static final Logger logger = LoggerFactory.getLogger(IDLabFunctions.class);

    public static boolean stringContainsOtherString(String str, String otherStr, String delimiter) {
        String[] split = str.split(delimiter);
        List<String> list = Arrays.asList(split);

        return list.contains(otherStr);
    }

    public static boolean listContainsElement(List list, String str) {
        if (list != null) {
            return list.contains(str);
        } else {
            return false;
        }
    }

    public static List<String> dbpediaSpotlight(String text, String endpoint) {
        if (!text.equals("")) {
            try {
                URL url = new URL(endpoint + "/annotate?text=" + URLEncoder.encode(text, "UTF-8"));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Accept", "application/json");
                con.setInstanceFollowRedirects(true);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                con.disconnect();

                Object document = Configuration.defaultConfiguration().jsonProvider().parse(content.toString());
                return JsonPath.parse(document).read("$.Resources[*].@URI");
            } catch (PathNotFoundException e) {
                // that means no result was found, so that is fine
                logger.info(e.getMessage(), e);
            } catch (Exception e) {
                // that probably means smth is wrong with the DBpedia Spotlight endpoint, so that is fine: log and continue
                logger.warn(e.getMessage(), e);
            }
        }

        return new ArrayList<>();
    }

    public static Object trueCondition(String bool, String value) {
        if (bool == null || !bool.equals("true")) {
            return null;
        } else {
            return value;
        }
    }

    public static String decide(String input, String expected, String result) {
        if (input != null && input.equals(expected)) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Tests whether a given input is null or not.
     * @param input String to evaluate
     * @return whether it's null or not
     */
    public static boolean isNull(String input) {
        return input == null;
    }

    public static String getMIMEType(String filename) {
        if (filename == null) {
            return null;
        } else {
            HashMap<String, String> map = new HashMap<>();

            // Put elements in the hashMap
            map.put("csv", "text/csv");
            map.put("json", "application/json");
            map.put("xml", "application/xml");
            map.put("nt", "application/n-triples");
            map.put("ttl", "text/turtle");
            map.put("nq", "application/n-quads");
            map.put("sql", "application/sql");

            String extension = FilenameUtils.getExtension(filename);

            if (map.containsKey(extension)) {
                return map.get(extension);
            } else {
                return null;
            }
        }
    }

    public static String readFile(String path) {
        try {
            logger.debug(Utils.getFile(path).toString());
            return Utils.fileToString(Utils.getFile(path));
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
            return null;
        }
    }

    public static String random() {
        return UUID.randomUUID().toString();

    }

    public static String toUpperCaseURL(String test) {
        String upperTest = test.toUpperCase();
        if (!upperTest.startsWith("HTTP://")) {
            upperTest = "HTTP://" + upperTest;
        }
        return upperTest;
    }

    /**
     * Tests whether a certain number is in a certain range.
     * Everything is optional:
     *   - function returns false when testNumber is null
     *   - function only tests from constraint when to is null
     *   - function only tests to constraint when from is null
     *   - function returns true when from and to are null.
     * @param testNumber The number put under the test. Optional (function returns false when is null)
     * @param from The number from where (inclusive)
     * @param to The number until where (exclusive)
     * @return whether it's in range or not
     */
    public static boolean inRange(Double testNumber, Double from, Double to) {
        if (testNumber == null) {
            return false;
        }
        if (from == null && to == null) {
            return true;
        }

        if (from == null) {
            return testNumber < to;
        }
        if (to == null) {
            return testNumber >= from;
        }
        return testNumber >= from && testNumber < to;
    }

    /**
     * Convert a string to its slugified equivalent.
     * @param str The String to slugify
     * @return the slugified string. Returns null if the input was also null.
     */
    public static String slugify(String str) {
        if (str != null) {
            Slugify slg = new Slugify();
            return slg.slugify(str);
        }
        return null;
    }

    // TODO The functions below are currently untested and undefined as idlab-fn functions.
    // (They however do no belong to GREL functions either)

    // TODO check whether this is the right place for this
    public static boolean isSet(String valueParameter) {
        return !StringUtils.isEmpty(valueParameter);
    }

    // TODO check whether this is the right place for this
    public static boolean booleanMatch(String valueParameter, String regexParameter) {
        return valueParameter.matches(regexParameter);
    }

    ///////////////////////////////
    // Date formatting functions //
    ///////////////////////////////
    // TODO check whether this is the right place for this

    /**
     * Returns a given date(time) string as an ISO-8601 formatted date(time) string.
     *
     * @param dateStr     Input string representing a parsable date or dateTime, e.g. "01 April 22"
     * @param pattern     DateTime format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uu"
     * @param language    The language of dateStr, as defined in {@link Locale}
     * @param includeTime If <code>true</code>, include the time part in the output.
     * @return A normalized date string in the ISO-8601 format uuuu-MM-dd (xs:date) or uuuu-MM-ddTHH:mm:ss,
     * or null if parsing the input fails.
     */
    private static String normalizeDateTimeStr(String dateStr, String pattern, String language, boolean includeTime) {
        try {
            Locale locale = new Locale(language);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
            if (includeTime) {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            } else {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.toString();
            }
        } catch (Throwable e) {
            logger.error("{}; format pattern: \"{}\", input: \"{}\", language: \"{}\"", e.getMessage(), pattern, dateStr, language);
            return null;
        }
    }

    /**
     * Returns `dateStr` as a normalized xs:date string, using `pattern` as current date form.
     *
     * @param dateStr  Input string representing a parsable date, e.g. "01 April 22"
     * @param pattern  Date format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uu"
     * @param language The language of dateStr, as defined in {@link Locale}
     * @return A normalized date string in the ISO-8601 format uuuu-MM-dd (xs:date), or null if parsing the input fails.
     */
    public static String normalizeDateWithLang(String dateStr, String pattern, String language) {
        return normalizeDateTimeStr(dateStr, pattern, language, false);
    }

    /**
     * Returns `dateStr` as a normalized xs:date string, using `pattern` as current date form. It uses the language of
     * the current locale of the JVM to parse certain input strings, like names of months.
     *
     * @param dateStr Input string representing a parsable date, e.g. "01 April 22"
     * @param pattern Date format pattern used to parse the given dateStr as defined in {@link SimpleDateFormat}, e.g. "dd LLL y"
     * @return A normalized date string in the ISO-8601 format uuuu-MM-dd (xs:date), or null if parsing the input fails.
     */
    public static String normalizeDate(String dateStr, String pattern) {
        return normalizeDateWithLang(dateStr, pattern, Locale.getDefault().getLanguage());
    }

    /**
     * Returns `dateTimeStr` as a normalized xs:date string, using `pattern` as current datetime form.
     *
     * @param dateTimeStr Input string representing a parsable datetime, e.g. "01 April 22T11:44:00"
     * @param pattern     Date format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uuTHH:mm:ss"
     * @param language    The language of dateStr, as defined in {@link Locale}
     * @return A normalized date string in the ISO-8601 format uuuu-MM-ddTHH:mm:ss (xs:datetime), or null if parsing the input fails.
     */
    public static String normalizeDateTimeWithLang(String dateTimeStr, String pattern, String language) {
        return normalizeDateTimeStr(dateTimeStr, pattern, language, true);
    }

    /**
     * Returns `dateTimeStr` as a normalized xs:date string, using `pattern` as current datetime form.
     * It uses the language of the current locale of the JVM to parse certain input strings, like names of months.
     *
     * @param dateTimeStr Input string representing a parsable datetime, e.g. "01 April 22T11:44:00"
     * @param pattern     Date format pattern used to parse the given dateStr as defined in {@link DateTimeFormatter}, e.g. "dd LLLL uuTHH:mm:ss"
     * @return A normalized date string in the ISO-8601 format uuuu-MM-ddTHH:mm:ss (xs:datetime), or null if parsing the input fails.
     */
    public static String normalizeDateTime(String dateTimeStr, String pattern) {
        return normalizeDateTimeWithLang(dateTimeStr, pattern, Locale.getDefault().getLanguage());
    }



    // TODO check whether this is the right place for this
    public static String jsonize(Object s) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(s);
    }
}
