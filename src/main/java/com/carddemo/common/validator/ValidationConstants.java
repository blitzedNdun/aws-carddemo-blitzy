/*
 * ValidationConstants.java
 *
 * Central constants class containing all validation patterns, lookup tables, and constant values
 * extracted from COBOL copybooks. Provides efficient Set-based lookups for area codes, state codes,
 * and other validation data while maintaining exact consistency with original COBOL validation logic.
 *
 * Consolidates validation constants from:
 * - CSLKPCDY.cpy: North American area codes and US state/ZIP code combinations
 * - CSUTLDWY.cpy: Date validation constants and patterns
 * - CUSTREC.cpy: Customer record field constraints
 * - Business rules from technical specification sections
 *
 * Copyright (c) 2024 CardDemo Application
 */
package com.carddemo.common.validator;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.math.RoundingMode;

/**
 * Central repository for all validation constants extracted from COBOL copybooks.
 * Provides efficient lookup collections and compiled regex patterns for consistent
 * data validation across all application components.
 *
 * This class maintains exact consistency with original COBOL validation logic
 * while providing optimized Java collections for high-performance lookups.
 */
public final class ValidationConstants {

    // Prevent instantiation of constants class
    private ValidationConstants() {
        throw new UnsupportedOperationException("ValidationConstants is a utility class and cannot be instantiated");
    }

    // =======================================================================
    // NORTH AMERICAN AREA CODES (from CSLKPCDY.cpy)
    // =======================================================================

    /**
     * Valid North American phone area codes extracted from NANPA registry.
     * Consolidated from COBOL 88-level condition VALID-PHONE-AREA-CODE.
     * Used for phone number validation across customer and account management.
     */
    public static final Set<String> VALID_AREA_CODES = new HashSet<String>() {{
        // Standard area codes from VALID-PHONE-AREA-CODE
        add("201"); add("202"); add("203"); add("204"); add("205"); add("206"); add("207"); add("208"); add("209"); add("210");
        add("212"); add("213"); add("214"); add("215"); add("216"); add("217"); add("218"); add("219"); add("220"); add("223");
        add("224"); add("225"); add("226"); add("228"); add("229"); add("231"); add("234"); add("236"); add("239"); add("240");
        add("242"); add("246"); add("248"); add("249"); add("250"); add("251"); add("252"); add("253"); add("254"); add("256");
        add("260"); add("262"); add("264"); add("267"); add("268"); add("269"); add("270"); add("272"); add("276"); add("279");
        add("281"); add("284"); add("289"); add("301"); add("302"); add("303"); add("304"); add("305"); add("306"); add("307");
        add("308"); add("309"); add("310"); add("312"); add("313"); add("314"); add("315"); add("316"); add("317"); add("318");
        add("319"); add("320"); add("321"); add("323"); add("325"); add("326"); add("330"); add("331"); add("332"); add("334");
        add("336"); add("337"); add("339"); add("340"); add("341"); add("343"); add("345"); add("346"); add("347"); add("351");
        add("352"); add("360"); add("361"); add("364"); add("365"); add("367"); add("368"); add("380"); add("385"); add("386");
        add("401"); add("402"); add("403"); add("404"); add("405"); add("406"); add("407"); add("408"); add("409"); add("410");
        add("412"); add("413"); add("414"); add("415"); add("416"); add("417"); add("418"); add("419"); add("423"); add("424");
        add("425"); add("430"); add("431"); add("432"); add("434"); add("435"); add("437"); add("438"); add("440"); add("441");
        add("442"); add("443"); add("445"); add("447"); add("448"); add("450"); add("458"); add("463"); add("464"); add("469");
        add("470"); add("473"); add("474"); add("475"); add("478"); add("479"); add("480"); add("484"); add("501"); add("502");
        add("503"); add("504"); add("505"); add("506"); add("507"); add("508"); add("509"); add("510"); add("512"); add("513");
        add("514"); add("515"); add("516"); add("517"); add("518"); add("519"); add("520"); add("530"); add("531"); add("534");
        add("539"); add("540"); add("541"); add("548"); add("551"); add("559"); add("561"); add("562"); add("563"); add("564");
        add("567"); add("570"); add("571"); add("572"); add("573"); add("574"); add("575"); add("579"); add("580"); add("581");
        add("582"); add("585"); add("586"); add("587"); add("601"); add("602"); add("603"); add("604"); add("605"); add("606");
        add("607"); add("608"); add("609"); add("610"); add("612"); add("613"); add("614"); add("615"); add("616"); add("617");
        add("618"); add("619"); add("620"); add("623"); add("626"); add("628"); add("629"); add("630"); add("631"); add("636");
        add("639"); add("640"); add("641"); add("646"); add("647"); add("649"); add("650"); add("651"); add("656"); add("657");
        add("658"); add("659"); add("660"); add("661"); add("662"); add("664"); add("667"); add("669"); add("670"); add("671");
        add("672"); add("678"); add("680"); add("681"); add("682"); add("683"); add("684"); add("689"); add("701"); add("702");
        add("703"); add("704"); add("705"); add("706"); add("707"); add("708"); add("709"); add("712"); add("713"); add("714");
        add("715"); add("716"); add("717"); add("718"); add("719"); add("720"); add("721"); add("724"); add("725"); add("726");
        add("727"); add("731"); add("732"); add("734"); add("737"); add("740"); add("742"); add("743"); add("747"); add("753");
        add("754"); add("757"); add("758"); add("760"); add("762"); add("763"); add("765"); add("767"); add("769"); add("770");
        add("771"); add("772"); add("773"); add("774"); add("775"); add("778"); add("779"); add("780"); add("781"); add("782");
        add("784"); add("785"); add("786"); add("787"); add("801"); add("802"); add("803"); add("804"); add("805"); add("806");
        add("807"); add("808"); add("809"); add("810"); add("812"); add("813"); add("814"); add("815"); add("816"); add("817");
        add("818"); add("819"); add("820"); add("825"); add("826"); add("828"); add("829"); add("830"); add("831"); add("832");
        add("838"); add("839"); add("840"); add("843"); add("845"); add("847"); add("848"); add("849"); add("850"); add("854");
        add("856"); add("857"); add("858"); add("859"); add("860"); add("862"); add("863"); add("864"); add("865"); add("867");
        add("868"); add("869"); add("870"); add("872"); add("873"); add("876"); add("878"); add("901"); add("902"); add("903");
        add("904"); add("905"); add("906"); add("907"); add("908"); add("909"); add("910"); add("912"); add("913"); add("914");
        add("915"); add("916"); add("917"); add("918"); add("919"); add("920"); add("925"); add("928"); add("929"); add("930");
        add("931"); add("934"); add("936"); add("937"); add("938"); add("939"); add("940"); add("941"); add("943"); add("945");
        add("947"); add("948"); add("949"); add("951"); add("952"); add("954"); add("956"); add("959"); add("970"); add("971");
        add("972"); add("973"); add("978"); add("979"); add("980"); add("983"); add("984"); add("985"); add("986"); add("989");
        
        // Easily recognizable codes (toll-free and service numbers)
        add("200"); add("211"); add("222"); add("233"); add("244"); add("255"); add("266"); add("277"); add("288"); add("299");
        add("300"); add("311"); add("322"); add("333"); add("344"); add("355"); add("366"); add("377"); add("388"); add("399");
        add("400"); add("411"); add("422"); add("433"); add("444"); add("455"); add("466"); add("477"); add("488"); add("499");
        add("500"); add("511"); add("522"); add("533"); add("544"); add("555"); add("566"); add("577"); add("588"); add("599");
        add("600"); add("611"); add("622"); add("633"); add("644"); add("655"); add("666"); add("677"); add("688"); add("699");
        add("700"); add("711"); add("722"); add("733"); add("744"); add("755"); add("766"); add("777"); add("788"); add("799");
        add("800"); add("811"); add("822"); add("833"); add("844"); add("855"); add("866"); add("877"); add("888"); add("899");
        add("900"); add("911"); add("922"); add("933"); add("944"); add("955"); add("966"); add("977"); add("988"); add("999");
    }};

    // =======================================================================
    // US STATE CODES (from CSLKPCDY.cpy)
    // =======================================================================

    /**
     * Valid US state and territory codes extracted from COBOL 88-level condition VALID-US-STATE-CODE.
     * Used for address validation in customer and account management modules.
     */
    public static final Set<String> VALID_STATE_CODES = new HashSet<String>() {{
        // US States
        add("AL"); add("AK"); add("AZ"); add("AR"); add("CA"); add("CO"); add("CT"); add("DE"); add("FL"); add("GA");
        add("HI"); add("ID"); add("IL"); add("IN"); add("IA"); add("KS"); add("KY"); add("LA"); add("ME"); add("MD");
        add("MA"); add("MI"); add("MN"); add("MS"); add("MO"); add("MT"); add("NE"); add("NV"); add("NH"); add("NJ");
        add("NM"); add("NY"); add("NC"); add("ND"); add("OH"); add("OK"); add("OR"); add("PA"); add("RI"); add("SC");
        add("SD"); add("TN"); add("TX"); add("UT"); add("VT"); add("VA"); add("WA"); add("WV"); add("WI"); add("WY");
        
        // US Territories and Districts
        add("DC"); add("AS"); add("GU"); add("MP"); add("PR"); add("VI");
    }};

    // =======================================================================
    // STATE-ZIP CODE COMBINATIONS (from CSLKPCDY.cpy)
    // =======================================================================

    /**
     * Valid US state and first two ZIP code digit combinations extracted from
     * COBOL 88-level condition VALID-US-STATE-ZIP-CD2-COMBO.
     * Used for comprehensive address validation ensuring state/ZIP consistency.
     */
    public static final Set<String> VALID_STATE_ZIP_COMBINATIONS = new HashSet<String>() {{
        // Military and overseas
        add("AA34"); add("AE90"); add("AE91"); add("AE92"); add("AE93"); add("AE94"); add("AE95"); add("AE96"); add("AE97"); add("AE98");
        
        // US States with their ZIP prefixes
        add("AK99"); add("AL35"); add("AL36"); add("AP96"); add("AR71"); add("AR72"); add("AS96"); add("AZ85"); add("AZ86");
        add("CA90"); add("CA91"); add("CA92"); add("CA93"); add("CA94"); add("CA95"); add("CA96"); add("CO80"); add("CO81");
        add("CT60"); add("CT61"); add("CT62"); add("CT63"); add("CT64"); add("CT65"); add("CT66"); add("CT67"); add("CT68"); add("CT69");
        add("DC20"); add("DC56"); add("DC88"); add("DE19"); add("FL32"); add("FL33"); add("FL34"); add("FM96");
        add("GA30"); add("GA31"); add("GA39"); add("GU96"); add("HI96"); add("IA50"); add("IA51"); add("IA52"); add("ID83");
        add("IL60"); add("IL61"); add("IL62"); add("IN46"); add("IN47"); add("KS66"); add("KS67"); add("KY40"); add("KY41"); add("KY42");
        add("LA70"); add("LA71"); add("MA10"); add("MA11"); add("MA12"); add("MA13"); add("MA14"); add("MA15"); add("MA16"); add("MA17");
        add("MA18"); add("MA19"); add("MA20"); add("MA21"); add("MA22"); add("MA23"); add("MA24"); add("MA25"); add("MA26"); add("MA27"); add("MA55");
        add("MD20"); add("MD21"); add("ME39"); add("ME40"); add("ME41"); add("ME42"); add("ME43"); add("ME44"); add("ME45"); add("ME46");
        add("ME47"); add("ME48"); add("ME49"); add("MH96"); add("MI48"); add("MI49"); add("MN55"); add("MN56"); add("MO63"); add("MO64");
        add("MO65"); add("MO72"); add("MP96"); add("MS38"); add("MS39"); add("MT59"); add("NC27"); add("NC28"); add("ND58");
        add("NE68"); add("NE69"); add("NH30"); add("NH31"); add("NH32"); add("NH33"); add("NH34"); add("NH35"); add("NH36"); add("NH37"); add("NH38");
        add("NJ70"); add("NJ71"); add("NJ72"); add("NJ73"); add("NJ74"); add("NJ75"); add("NJ76"); add("NJ77"); add("NJ78"); add("NJ79");
        add("NJ80"); add("NJ81"); add("NJ82"); add("NJ83"); add("NJ84"); add("NJ85"); add("NJ86"); add("NJ87"); add("NJ88"); add("NJ89");
        add("NM87"); add("NM88"); add("NV88"); add("NV89"); add("NY50"); add("NY54"); add("NY63"); add("NY10"); add("NY11"); add("NY12"); add("NY13"); add("NY14");
        add("OH43"); add("OH44"); add("OH45"); add("OK73"); add("OK74"); add("OR97"); add("PA15"); add("PA16"); add("PA17"); add("PA18"); add("PA19");
        add("PR60"); add("PR61"); add("PR62"); add("PR63"); add("PR64"); add("PR65"); add("PR66"); add("PR67"); add("PR68"); add("PR69");
        add("PR70"); add("PR71"); add("PR72"); add("PR73"); add("PR74"); add("PR75"); add("PR76"); add("PR77"); add("PR78"); add("PR79");
        add("PR90"); add("PR91"); add("PR92"); add("PR93"); add("PR94"); add("PR95"); add("PR96"); add("PR97"); add("PR98");
        add("PW96"); add("RI28"); add("RI29"); add("SC29"); add("SD57"); add("TN37"); add("TN38"); add("TX73"); add("TX75"); add("TX76");
        add("TX77"); add("TX78"); add("TX79"); add("TX88"); add("UT84"); add("VA20"); add("VA22"); add("VA23"); add("VA24");
        add("VI80"); add("VI82"); add("VI83"); add("VI84"); add("VI85"); add("VT50"); add("VT51"); add("VT52"); add("VT53"); add("VT54");
        add("VT56"); add("VT57"); add("VT58"); add("VT59"); add("WA98"); add("WA99"); add("WI53"); add("WI54"); add("WV24"); add("WV25"); add("WV26");
        add("WY82"); add("WY83");
    }};

    // =======================================================================
    // COMPILED REGEX PATTERNS
    // =======================================================================

    /**
     * Pattern for numeric-only fields (digits 0-9).
     * Used for account IDs, customer IDs, and other numeric identifiers.
     */
    public static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");

    /**
     * Pattern for alphabetic-only fields (letters A-Z, a-z).
     * Used for name validation and alphabetic codes.
     */
    public static final Pattern ALPHA_PATTERN = Pattern.compile("^[A-Za-z]+$");

    /**
     * Pattern for alphanumeric fields (letters and digits).
     * Used for general text fields allowing both letters and numbers.
     */
    public static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    /**
     * Pattern for Social Security Number validation (9 digits).
     * Matches exactly 9 consecutive digits as per COBOL CUST-SSN PIC 9(09).
     */
    public static final Pattern SSN_PATTERN = Pattern.compile("^[0-9]{9}$");

    /**
     * Pattern for phone number validation (15 characters max).
     * Allows digits, spaces, hyphens, and parentheses for formatting.
     * Based on COBOL CUST-PHONE-NUM-1/2 PIC X(15).
     */
    public static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\s\\-\\(\\)]{1,15}$");

    /**
     * Pattern for account ID validation (9 digits).
     * Used for account number validation matching COBOL numeric formats.
     */
    public static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[0-9]{9}$");

    /**
     * Pattern for credit card number validation (16 digits).
     * Standard credit card format with exactly 16 digits.
     */
    public static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{16}$");

    /**
     * Pattern for email address validation.
     * Standard email format for customer communication.
     */
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Pattern for ZIP code validation (5 or 9 digits with optional hyphen).
     * Supports both 5-digit and ZIP+4 formats (12345 or 12345-6789).
     */
    public static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^[0-9]{5}(-[0-9]{4})?$");

    // =======================================================================
    // DATE VALIDATION PATTERNS (from CSUTLDWY.cpy)
    // =======================================================================

    /**
     * Date format patterns for validation.
     * Based on COBOL date processing routines in CSUTLDPY.cpy.
     */
    public static final Set<String> DATE_PATTERNS = new HashSet<String>() {{
        add("YYYYMMDD"); // Primary format from COBOL WS-DATE-FORMAT
        add("CCYYMMDD"); // Century/year format used in date validation
        add("MM/DD/YYYY"); // Alternative display format
        add("YYYY-MM-DD"); // ISO format for database storage
    }};

    // =======================================================================
    // BUSINESS VALIDATION LIMITS
    // =======================================================================

    /**
     * Minimum valid account ID value.
     * Based on 9-digit account ID format with reasonable lower bound.
     */
    public static final long MIN_ACCOUNT_ID = 100000000L; // 9-digit minimum

    /**
     * Maximum valid account ID value.
     * Based on 9-digit account ID format with reasonable upper bound.
     */
    public static final long MAX_ACCOUNT_ID = 999999999L; // 9-digit maximum

    /**
     * Minimum credit limit amount.
     * Business rule for minimum allowable credit limit.
     */
    public static final long MIN_CREDIT_LIMIT = 500L; // $500 minimum

    /**
     * Maximum credit limit amount.
     * Business rule for maximum allowable credit limit.
     */
    public static final long MAX_CREDIT_LIMIT = 50000L; // $50,000 maximum

    /**
     * Minimum FICO credit score.
     * Industry standard minimum FICO score value.
     */
    public static final int MIN_FICO_SCORE = 300;

    /**
     * Maximum FICO credit score.
     * Industry standard maximum FICO score value.
     */
    public static final int MAX_FICO_SCORE = 850;

    // =======================================================================
    // CURRENCY AND DECIMAL PRECISION (COBOL COMP-3 equivalents)
    // =======================================================================

    /**
     * Currency decimal scale (2 decimal places).
     * Matches COBOL financial field formats like PIC S9(10)V99 COMP-3.
     */
    public static final int CURRENCY_SCALE = 2;

    /**
     * Currency decimal precision (12 total digits including scale).
     * Matches COBOL COMP-3 precision for financial calculations.
     */
    public static final int CURRENCY_PRECISION = 12;

    /**
     * COBOL-equivalent rounding mode for financial calculations.
     * Uses HALF_UP to match COBOL arithmetic rounding behavior.
     * This ensures exact precision compatibility with original COBOL COMP-3 calculations.
     */
    public static final RoundingMode COBOL_ROUNDING_MODE = RoundingMode.HALF_UP;

    // =======================================================================
    // DATE VALIDATION CONSTANTS (from CSUTLDWY.cpy)
    // =======================================================================

    /**
     * Valid century values for date validation.
     * Based on COBOL 88-level conditions THIS-CENTURY and LAST-CENTURY.
     */
    public static final Set<Integer> VALID_CENTURIES = new HashSet<Integer>() {{
        add(19); // LAST-CENTURY
        add(20); // THIS-CENTURY
    }};

    /**
     * Valid month values (1-12).
     * Based on COBOL 88-level condition WS-VALID-MONTH.
     */
    public static final Set<Integer> VALID_MONTHS = new HashSet<Integer>() {{
        add(1); add(2); add(3); add(4); add(5); add(6);
        add(7); add(8); add(9); add(10); add(11); add(12);
    }};

    /**
     * Months with 31 days.
     * Based on COBOL 88-level condition WS-31-DAY-MONTH.
     */
    public static final Set<Integer> MONTHS_WITH_31_DAYS = new HashSet<Integer>() {{
        add(1); add(3); add(5); add(7); add(8); add(10); add(12);
    }};

    /**
     * February month constant.
     * Based on COBOL 88-level condition WS-FEBRUARY.
     */
    public static final int FEBRUARY = 2;

    /**
     * Valid day values (1-31).
     * Based on COBOL 88-level condition WS-VALID-DAY.
     */
    public static final Set<Integer> VALID_DAYS = new HashSet<Integer>() {{
        for (int i = 1; i <= 31; i++) {
            add(i);
        }
    }};

    /**
     * Valid February days for non-leap years (1-28).
     * Based on COBOL 88-level condition WS-VALID-FEB-DAY.
     */
    public static final Set<Integer> VALID_FEBRUARY_DAYS = new HashSet<Integer>() {{
        for (int i = 1; i <= 28; i++) {
            add(i);
        }
    }};

    // =======================================================================
    // VALIDATION UTILITY METHODS
    // =======================================================================

    /**
     * Validates if a year is a leap year using COBOL-equivalent logic.
     * Based on the leap year calculation in CSUTLDPY.cpy lines 245-271.
     *
     * @param year The year to validate (4-digit format)
     * @return true if the year is a leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        // COBOL logic: IF WS-EDIT-DATE-YY-N = 0 (century year)
        if (year % 100 == 0) {
            // Century years must be divisible by 400
            return year % 400 == 0;
        } else {
            // Non-century years must be divisible by 4
            return year % 4 == 0;
        }
    }

    /**
     * Gets the maximum valid day for a given month and year.
     * Accounts for leap years and varying month lengths.
     *
     * @param month The month (1-12)
     * @param year The year (4-digit format)
     * @return Maximum valid day for the month/year combination
     */
    public static int getMaxDayForMonth(int month, int year) {
        if (MONTHS_WITH_31_DAYS.contains(month)) {
            return 31;
        } else if (month == FEBRUARY) {
            return isLeapYear(year) ? 29 : 28;
        } else {
            return 30; // April, June, September, November
        }
    }
}