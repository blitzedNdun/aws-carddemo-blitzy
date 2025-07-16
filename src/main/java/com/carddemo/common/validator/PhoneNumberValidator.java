/*
 * PhoneNumberValidator.java
 * 
 * Jakarta Bean Validation constraint validator for phone number validation.
 * Implements North American Numbering Plan (NANP) validation using area code lookup
 * from COBOL copybook CSLKPCDY.cpy.
 * 
 * This validator ensures exact functional equivalence with the original COBOL
 * phone validation logic while providing comprehensive error reporting.
 * 
 * Part of CardDemo mainframe modernization - COBOL to Java transformation.
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

package com.carddemo.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Constraint validator for {@link ValidPhoneNumber} annotation.
 * 
 * This validator implements comprehensive North American phone number validation
 * using the area code lookup tables from COBOL copybook CSLKPCDY.cpy.
 * 
 * <p>The validator supports multiple phone number formats and provides detailed
 * validation context for error reporting, maintaining exact functional equivalence
 * with the original COBOL validation logic.
 * 
 * <p>Area codes are categorized into three groups based on CSLKPCDY.cpy:
 * <ul>
 *   <li>Valid phone area codes (VALID-PHONE-AREA-CODE)</li>
 *   <li>General purpose codes (VALID-GENERAL-PURP-CODE)</li>
 *   <li>Easily recognizable codes (VALID-EASY-RECOG-AREA-CODE)</li>
 * </ul>
 * 
 * @see ValidPhoneNumber
 * @since 1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    private ValidPhoneNumber annotation;
    
    // Phone number format patterns
    private static final Pattern PHONE_PATTERN_FORMATTED = Pattern.compile(
        "^\\(?([0-9]{3})\\)?[\\s.-]?([0-9]{3})[\\s.-]?([0-9]{4})$"
    );
    
    private static final Pattern PHONE_PATTERN_DIGITS_ONLY = Pattern.compile(
        "^([0-9]{10})$"
    );
    
    private static final Pattern PHONE_PATTERN_DIGITS_WITH_COUNTRY_CODE = Pattern.compile(
        "^1([0-9]{10})$"
    );
    
    private static final Pattern PHONE_PATTERN_INTERNATIONAL = Pattern.compile(
        "^\\+1[\\s.-]?\\(?([0-9]{3})\\)?[\\s.-]?([0-9]{3})[\\s.-]?([0-9]{4})$"
    );
    
    // Valid North American area codes from CSLKPCDY.cpy - VALID-PHONE-AREA-CODE
    private static final Set<String> VALID_PHONE_AREA_CODES = new HashSet<>();
    
    // General purpose area codes from CSLKPCDY.cpy - VALID-GENERAL-PURP-CODE  
    private static final Set<String> VALID_GENERAL_PURPOSE_CODES = new HashSet<>();
    
    // Easily recognizable area codes from CSLKPCDY.cpy - VALID-EASY-RECOG-AREA-CODE
    private static final Set<String> VALID_EASY_RECOGNIZABLE_CODES = new HashSet<>();
    
    static {
        initializeValidPhoneAreaCodes();
        initializeValidEasyRecognizableCodes();
        initializeValidGeneralPurposeCodes();
    }
    
    /**
     * Initialize valid phone area codes from CSLKPCDY.cpy VALID-PHONE-AREA-CODE
     */
    private static void initializeValidPhoneAreaCodes() {
        // Area codes from lines 30-440 of CSLKPCDY.cpy
        VALID_PHONE_AREA_CODES.add("201");
        VALID_PHONE_AREA_CODES.add("202");
        VALID_PHONE_AREA_CODES.add("203");
        VALID_PHONE_AREA_CODES.add("204");
        VALID_PHONE_AREA_CODES.add("205");
        VALID_PHONE_AREA_CODES.add("206");
        VALID_PHONE_AREA_CODES.add("207");
        VALID_PHONE_AREA_CODES.add("208");
        VALID_PHONE_AREA_CODES.add("209");
        VALID_PHONE_AREA_CODES.add("210");
        VALID_PHONE_AREA_CODES.add("212");
        VALID_PHONE_AREA_CODES.add("213");
        VALID_PHONE_AREA_CODES.add("214");
        VALID_PHONE_AREA_CODES.add("215");
        VALID_PHONE_AREA_CODES.add("216");
        VALID_PHONE_AREA_CODES.add("217");
        VALID_PHONE_AREA_CODES.add("218");
        VALID_PHONE_AREA_CODES.add("219");
        VALID_PHONE_AREA_CODES.add("220");
        VALID_PHONE_AREA_CODES.add("223");
        VALID_PHONE_AREA_CODES.add("224");
        VALID_PHONE_AREA_CODES.add("225");
        VALID_PHONE_AREA_CODES.add("226");
        VALID_PHONE_AREA_CODES.add("228");
        VALID_PHONE_AREA_CODES.add("229");
        VALID_PHONE_AREA_CODES.add("231");
        VALID_PHONE_AREA_CODES.add("234");
        VALID_PHONE_AREA_CODES.add("236");
        VALID_PHONE_AREA_CODES.add("239");
        VALID_PHONE_AREA_CODES.add("240");
        VALID_PHONE_AREA_CODES.add("242");
        VALID_PHONE_AREA_CODES.add("246");
        VALID_PHONE_AREA_CODES.add("248");
        VALID_PHONE_AREA_CODES.add("249");
        VALID_PHONE_AREA_CODES.add("250");
        VALID_PHONE_AREA_CODES.add("251");
        VALID_PHONE_AREA_CODES.add("252");
        VALID_PHONE_AREA_CODES.add("253");
        VALID_PHONE_AREA_CODES.add("254");
        VALID_PHONE_AREA_CODES.add("256");
        VALID_PHONE_AREA_CODES.add("260");
        VALID_PHONE_AREA_CODES.add("262");
        VALID_PHONE_AREA_CODES.add("264");
        VALID_PHONE_AREA_CODES.add("267");
        VALID_PHONE_AREA_CODES.add("268");
        VALID_PHONE_AREA_CODES.add("269");
        VALID_PHONE_AREA_CODES.add("270");
        VALID_PHONE_AREA_CODES.add("272");
        VALID_PHONE_AREA_CODES.add("276");
        VALID_PHONE_AREA_CODES.add("279");
        VALID_PHONE_AREA_CODES.add("281");
        VALID_PHONE_AREA_CODES.add("284");
        VALID_PHONE_AREA_CODES.add("289");
        VALID_PHONE_AREA_CODES.add("301");
        VALID_PHONE_AREA_CODES.add("302");
        VALID_PHONE_AREA_CODES.add("303");
        VALID_PHONE_AREA_CODES.add("304");
        VALID_PHONE_AREA_CODES.add("305");
        VALID_PHONE_AREA_CODES.add("306");
        VALID_PHONE_AREA_CODES.add("307");
        VALID_PHONE_AREA_CODES.add("308");
        VALID_PHONE_AREA_CODES.add("309");
        VALID_PHONE_AREA_CODES.add("310");
        VALID_PHONE_AREA_CODES.add("312");
        VALID_PHONE_AREA_CODES.add("313");
        VALID_PHONE_AREA_CODES.add("314");
        VALID_PHONE_AREA_CODES.add("315");
        VALID_PHONE_AREA_CODES.add("316");
        VALID_PHONE_AREA_CODES.add("317");
        VALID_PHONE_AREA_CODES.add("318");
        VALID_PHONE_AREA_CODES.add("319");
        VALID_PHONE_AREA_CODES.add("320");
        VALID_PHONE_AREA_CODES.add("321");
        VALID_PHONE_AREA_CODES.add("323");
        VALID_PHONE_AREA_CODES.add("325");
        VALID_PHONE_AREA_CODES.add("326");
        VALID_PHONE_AREA_CODES.add("330");
        VALID_PHONE_AREA_CODES.add("331");
        VALID_PHONE_AREA_CODES.add("332");
        VALID_PHONE_AREA_CODES.add("334");
        VALID_PHONE_AREA_CODES.add("336");
        VALID_PHONE_AREA_CODES.add("337");
        VALID_PHONE_AREA_CODES.add("339");
        VALID_PHONE_AREA_CODES.add("340");
        VALID_PHONE_AREA_CODES.add("341");
        VALID_PHONE_AREA_CODES.add("343");
        VALID_PHONE_AREA_CODES.add("345");
        VALID_PHONE_AREA_CODES.add("346");
        VALID_PHONE_AREA_CODES.add("347");
        VALID_PHONE_AREA_CODES.add("351");
        VALID_PHONE_AREA_CODES.add("352");
        VALID_PHONE_AREA_CODES.add("360");
        VALID_PHONE_AREA_CODES.add("361");
        VALID_PHONE_AREA_CODES.add("364");
        VALID_PHONE_AREA_CODES.add("365");
        VALID_PHONE_AREA_CODES.add("367");
        VALID_PHONE_AREA_CODES.add("368");
        VALID_PHONE_AREA_CODES.add("380");
        VALID_PHONE_AREA_CODES.add("385");
        VALID_PHONE_AREA_CODES.add("386");
        VALID_PHONE_AREA_CODES.add("401");
        VALID_PHONE_AREA_CODES.add("402");
        VALID_PHONE_AREA_CODES.add("403");
        VALID_PHONE_AREA_CODES.add("404");
        VALID_PHONE_AREA_CODES.add("405");
        VALID_PHONE_AREA_CODES.add("406");
        VALID_PHONE_AREA_CODES.add("407");
        VALID_PHONE_AREA_CODES.add("408");
        VALID_PHONE_AREA_CODES.add("409");
        VALID_PHONE_AREA_CODES.add("410");
        VALID_PHONE_AREA_CODES.add("412");
        VALID_PHONE_AREA_CODES.add("413");
        VALID_PHONE_AREA_CODES.add("414");
        VALID_PHONE_AREA_CODES.add("415");
        VALID_PHONE_AREA_CODES.add("416");
        VALID_PHONE_AREA_CODES.add("417");
        VALID_PHONE_AREA_CODES.add("418");
        VALID_PHONE_AREA_CODES.add("419");
        VALID_PHONE_AREA_CODES.add("423");
        VALID_PHONE_AREA_CODES.add("424");
        VALID_PHONE_AREA_CODES.add("425");
        VALID_PHONE_AREA_CODES.add("430");
        VALID_PHONE_AREA_CODES.add("431");
        VALID_PHONE_AREA_CODES.add("432");
        VALID_PHONE_AREA_CODES.add("434");
        VALID_PHONE_AREA_CODES.add("435");
        VALID_PHONE_AREA_CODES.add("437");
        VALID_PHONE_AREA_CODES.add("438");
        VALID_PHONE_AREA_CODES.add("440");
        VALID_PHONE_AREA_CODES.add("441");
        VALID_PHONE_AREA_CODES.add("442");
        VALID_PHONE_AREA_CODES.add("443");
        VALID_PHONE_AREA_CODES.add("445");
        VALID_PHONE_AREA_CODES.add("447");
        VALID_PHONE_AREA_CODES.add("448");
        VALID_PHONE_AREA_CODES.add("450");
        VALID_PHONE_AREA_CODES.add("458");
        VALID_PHONE_AREA_CODES.add("463");
        VALID_PHONE_AREA_CODES.add("464");
        VALID_PHONE_AREA_CODES.add("469");
        VALID_PHONE_AREA_CODES.add("470");
        VALID_PHONE_AREA_CODES.add("473");
        VALID_PHONE_AREA_CODES.add("474");
        VALID_PHONE_AREA_CODES.add("475");
        VALID_PHONE_AREA_CODES.add("478");
        VALID_PHONE_AREA_CODES.add("479");
        VALID_PHONE_AREA_CODES.add("480");
        VALID_PHONE_AREA_CODES.add("484");
        VALID_PHONE_AREA_CODES.add("501");
        VALID_PHONE_AREA_CODES.add("502");
        VALID_PHONE_AREA_CODES.add("503");
        VALID_PHONE_AREA_CODES.add("504");
        VALID_PHONE_AREA_CODES.add("505");
        VALID_PHONE_AREA_CODES.add("506");
        VALID_PHONE_AREA_CODES.add("507");
        VALID_PHONE_AREA_CODES.add("508");
        VALID_PHONE_AREA_CODES.add("509");
        VALID_PHONE_AREA_CODES.add("510");
        VALID_PHONE_AREA_CODES.add("512");
        VALID_PHONE_AREA_CODES.add("513");
        VALID_PHONE_AREA_CODES.add("514");
        VALID_PHONE_AREA_CODES.add("515");
        VALID_PHONE_AREA_CODES.add("516");
        VALID_PHONE_AREA_CODES.add("517");
        VALID_PHONE_AREA_CODES.add("518");
        VALID_PHONE_AREA_CODES.add("519");
        VALID_PHONE_AREA_CODES.add("520");
        VALID_PHONE_AREA_CODES.add("530");
        VALID_PHONE_AREA_CODES.add("531");
        VALID_PHONE_AREA_CODES.add("534");
        VALID_PHONE_AREA_CODES.add("539");
        VALID_PHONE_AREA_CODES.add("540");
        VALID_PHONE_AREA_CODES.add("541");
        VALID_PHONE_AREA_CODES.add("548");
        VALID_PHONE_AREA_CODES.add("551");
        VALID_PHONE_AREA_CODES.add("559");
        VALID_PHONE_AREA_CODES.add("561");
        VALID_PHONE_AREA_CODES.add("562");
        VALID_PHONE_AREA_CODES.add("563");
        VALID_PHONE_AREA_CODES.add("564");
        VALID_PHONE_AREA_CODES.add("567");
        VALID_PHONE_AREA_CODES.add("570");
        VALID_PHONE_AREA_CODES.add("571");
        VALID_PHONE_AREA_CODES.add("572");
        VALID_PHONE_AREA_CODES.add("573");
        VALID_PHONE_AREA_CODES.add("574");
        VALID_PHONE_AREA_CODES.add("575");
        VALID_PHONE_AREA_CODES.add("579");
        VALID_PHONE_AREA_CODES.add("580");
        VALID_PHONE_AREA_CODES.add("581");
        VALID_PHONE_AREA_CODES.add("582");
        VALID_PHONE_AREA_CODES.add("585");
        VALID_PHONE_AREA_CODES.add("586");
        VALID_PHONE_AREA_CODES.add("587");
        VALID_PHONE_AREA_CODES.add("601");
        VALID_PHONE_AREA_CODES.add("602");
        VALID_PHONE_AREA_CODES.add("603");
        VALID_PHONE_AREA_CODES.add("604");
        VALID_PHONE_AREA_CODES.add("605");
        VALID_PHONE_AREA_CODES.add("606");
        VALID_PHONE_AREA_CODES.add("607");
        VALID_PHONE_AREA_CODES.add("608");
        VALID_PHONE_AREA_CODES.add("609");
        VALID_PHONE_AREA_CODES.add("610");
        VALID_PHONE_AREA_CODES.add("612");
        VALID_PHONE_AREA_CODES.add("613");
        VALID_PHONE_AREA_CODES.add("614");
        VALID_PHONE_AREA_CODES.add("615");
        VALID_PHONE_AREA_CODES.add("616");
        VALID_PHONE_AREA_CODES.add("617");
        VALID_PHONE_AREA_CODES.add("618");
        VALID_PHONE_AREA_CODES.add("619");
        VALID_PHONE_AREA_CODES.add("620");
        VALID_PHONE_AREA_CODES.add("623");
        VALID_PHONE_AREA_CODES.add("626");
        VALID_PHONE_AREA_CODES.add("628");
        VALID_PHONE_AREA_CODES.add("629");
        VALID_PHONE_AREA_CODES.add("630");
        VALID_PHONE_AREA_CODES.add("631");
        VALID_PHONE_AREA_CODES.add("636");
        VALID_PHONE_AREA_CODES.add("639");
        VALID_PHONE_AREA_CODES.add("640");
        VALID_PHONE_AREA_CODES.add("641");
        VALID_PHONE_AREA_CODES.add("646");
        VALID_PHONE_AREA_CODES.add("647");
        VALID_PHONE_AREA_CODES.add("649");
        VALID_PHONE_AREA_CODES.add("650");
        VALID_PHONE_AREA_CODES.add("651");
        VALID_PHONE_AREA_CODES.add("656");
        VALID_PHONE_AREA_CODES.add("657");
        VALID_PHONE_AREA_CODES.add("658");
        VALID_PHONE_AREA_CODES.add("659");
        VALID_PHONE_AREA_CODES.add("660");
        VALID_PHONE_AREA_CODES.add("661");
        VALID_PHONE_AREA_CODES.add("662");
        VALID_PHONE_AREA_CODES.add("664");
        VALID_PHONE_AREA_CODES.add("667");
        VALID_PHONE_AREA_CODES.add("669");
        VALID_PHONE_AREA_CODES.add("670");
        VALID_PHONE_AREA_CODES.add("671");
        VALID_PHONE_AREA_CODES.add("672");
        VALID_PHONE_AREA_CODES.add("678");
        VALID_PHONE_AREA_CODES.add("680");
        VALID_PHONE_AREA_CODES.add("681");
        VALID_PHONE_AREA_CODES.add("682");
        VALID_PHONE_AREA_CODES.add("683");
        VALID_PHONE_AREA_CODES.add("684");
        VALID_PHONE_AREA_CODES.add("689");
        VALID_PHONE_AREA_CODES.add("701");
        VALID_PHONE_AREA_CODES.add("702");
        VALID_PHONE_AREA_CODES.add("703");
        VALID_PHONE_AREA_CODES.add("704");
        VALID_PHONE_AREA_CODES.add("705");
        VALID_PHONE_AREA_CODES.add("706");
        VALID_PHONE_AREA_CODES.add("707");
        VALID_PHONE_AREA_CODES.add("708");
        VALID_PHONE_AREA_CODES.add("709");
        VALID_PHONE_AREA_CODES.add("712");
        VALID_PHONE_AREA_CODES.add("713");
        VALID_PHONE_AREA_CODES.add("714");
        VALID_PHONE_AREA_CODES.add("715");
        VALID_PHONE_AREA_CODES.add("716");
        VALID_PHONE_AREA_CODES.add("717");
        VALID_PHONE_AREA_CODES.add("718");
        VALID_PHONE_AREA_CODES.add("719");
        VALID_PHONE_AREA_CODES.add("720");
        VALID_PHONE_AREA_CODES.add("721");
        VALID_PHONE_AREA_CODES.add("724");
        VALID_PHONE_AREA_CODES.add("725");
        VALID_PHONE_AREA_CODES.add("726");
        VALID_PHONE_AREA_CODES.add("727");
        VALID_PHONE_AREA_CODES.add("731");
        VALID_PHONE_AREA_CODES.add("732");
        VALID_PHONE_AREA_CODES.add("734");
        VALID_PHONE_AREA_CODES.add("737");
        VALID_PHONE_AREA_CODES.add("740");
        VALID_PHONE_AREA_CODES.add("742");
        VALID_PHONE_AREA_CODES.add("743");
        VALID_PHONE_AREA_CODES.add("747");
        VALID_PHONE_AREA_CODES.add("753");
        VALID_PHONE_AREA_CODES.add("754");
        VALID_PHONE_AREA_CODES.add("757");
        VALID_PHONE_AREA_CODES.add("758");
        VALID_PHONE_AREA_CODES.add("760");
        VALID_PHONE_AREA_CODES.add("762");
        VALID_PHONE_AREA_CODES.add("763");
        VALID_PHONE_AREA_CODES.add("765");
        VALID_PHONE_AREA_CODES.add("767");
        VALID_PHONE_AREA_CODES.add("769");
        VALID_PHONE_AREA_CODES.add("770");
        VALID_PHONE_AREA_CODES.add("771");
        VALID_PHONE_AREA_CODES.add("772");
        VALID_PHONE_AREA_CODES.add("773");
        VALID_PHONE_AREA_CODES.add("774");
        VALID_PHONE_AREA_CODES.add("775");
        VALID_PHONE_AREA_CODES.add("778");
        VALID_PHONE_AREA_CODES.add("779");
        VALID_PHONE_AREA_CODES.add("780");
        VALID_PHONE_AREA_CODES.add("781");
        VALID_PHONE_AREA_CODES.add("782");
        VALID_PHONE_AREA_CODES.add("784");
        VALID_PHONE_AREA_CODES.add("785");
        VALID_PHONE_AREA_CODES.add("786");
        VALID_PHONE_AREA_CODES.add("787");
        VALID_PHONE_AREA_CODES.add("801");
        VALID_PHONE_AREA_CODES.add("802");
        VALID_PHONE_AREA_CODES.add("803");
        VALID_PHONE_AREA_CODES.add("804");
        VALID_PHONE_AREA_CODES.add("805");
        VALID_PHONE_AREA_CODES.add("806");
        VALID_PHONE_AREA_CODES.add("807");
        VALID_PHONE_AREA_CODES.add("808");
        VALID_PHONE_AREA_CODES.add("809");
        VALID_PHONE_AREA_CODES.add("810");
        VALID_PHONE_AREA_CODES.add("812");
        VALID_PHONE_AREA_CODES.add("813");
        VALID_PHONE_AREA_CODES.add("814");
        VALID_PHONE_AREA_CODES.add("815");
        VALID_PHONE_AREA_CODES.add("816");
        VALID_PHONE_AREA_CODES.add("817");
        VALID_PHONE_AREA_CODES.add("818");
        VALID_PHONE_AREA_CODES.add("819");
        VALID_PHONE_AREA_CODES.add("820");
        VALID_PHONE_AREA_CODES.add("825");
        VALID_PHONE_AREA_CODES.add("826");
        VALID_PHONE_AREA_CODES.add("828");
        VALID_PHONE_AREA_CODES.add("829");
        VALID_PHONE_AREA_CODES.add("830");
        VALID_PHONE_AREA_CODES.add("831");
        VALID_PHONE_AREA_CODES.add("832");
        VALID_PHONE_AREA_CODES.add("838");
        VALID_PHONE_AREA_CODES.add("839");
        VALID_PHONE_AREA_CODES.add("840");
        VALID_PHONE_AREA_CODES.add("843");
        VALID_PHONE_AREA_CODES.add("845");
        VALID_PHONE_AREA_CODES.add("847");
        VALID_PHONE_AREA_CODES.add("848");
        VALID_PHONE_AREA_CODES.add("849");
        VALID_PHONE_AREA_CODES.add("850");
        VALID_PHONE_AREA_CODES.add("854");
        VALID_PHONE_AREA_CODES.add("856");
        VALID_PHONE_AREA_CODES.add("857");
        VALID_PHONE_AREA_CODES.add("858");
        VALID_PHONE_AREA_CODES.add("859");
        VALID_PHONE_AREA_CODES.add("860");
        VALID_PHONE_AREA_CODES.add("862");
        VALID_PHONE_AREA_CODES.add("863");
        VALID_PHONE_AREA_CODES.add("864");
        VALID_PHONE_AREA_CODES.add("865");
        VALID_PHONE_AREA_CODES.add("867");
        VALID_PHONE_AREA_CODES.add("868");
        VALID_PHONE_AREA_CODES.add("869");
        VALID_PHONE_AREA_CODES.add("870");
        VALID_PHONE_AREA_CODES.add("872");
        VALID_PHONE_AREA_CODES.add("873");
        VALID_PHONE_AREA_CODES.add("876");
        VALID_PHONE_AREA_CODES.add("878");
        VALID_PHONE_AREA_CODES.add("901");
        VALID_PHONE_AREA_CODES.add("902");
        VALID_PHONE_AREA_CODES.add("903");
        VALID_PHONE_AREA_CODES.add("904");
        VALID_PHONE_AREA_CODES.add("905");
        VALID_PHONE_AREA_CODES.add("906");
        VALID_PHONE_AREA_CODES.add("907");
        VALID_PHONE_AREA_CODES.add("908");
        VALID_PHONE_AREA_CODES.add("909");
        VALID_PHONE_AREA_CODES.add("910");
        VALID_PHONE_AREA_CODES.add("912");
        VALID_PHONE_AREA_CODES.add("913");
        VALID_PHONE_AREA_CODES.add("914");
        VALID_PHONE_AREA_CODES.add("915");
        VALID_PHONE_AREA_CODES.add("916");
        VALID_PHONE_AREA_CODES.add("917");
        VALID_PHONE_AREA_CODES.add("918");
        VALID_PHONE_AREA_CODES.add("919");
        VALID_PHONE_AREA_CODES.add("920");
        VALID_PHONE_AREA_CODES.add("925");
        VALID_PHONE_AREA_CODES.add("928");
        VALID_PHONE_AREA_CODES.add("929");
        VALID_PHONE_AREA_CODES.add("930");
        VALID_PHONE_AREA_CODES.add("931");
        VALID_PHONE_AREA_CODES.add("934");
        VALID_PHONE_AREA_CODES.add("936");
        VALID_PHONE_AREA_CODES.add("937");
        VALID_PHONE_AREA_CODES.add("938");
        VALID_PHONE_AREA_CODES.add("939");
        VALID_PHONE_AREA_CODES.add("940");
        VALID_PHONE_AREA_CODES.add("941");
        VALID_PHONE_AREA_CODES.add("943");
        VALID_PHONE_AREA_CODES.add("945");
        VALID_PHONE_AREA_CODES.add("947");
        VALID_PHONE_AREA_CODES.add("948");
        VALID_PHONE_AREA_CODES.add("949");
        VALID_PHONE_AREA_CODES.add("951");
        VALID_PHONE_AREA_CODES.add("952");
        VALID_PHONE_AREA_CODES.add("954");
        VALID_PHONE_AREA_CODES.add("956");
        VALID_PHONE_AREA_CODES.add("959");
        VALID_PHONE_AREA_CODES.add("970");
        VALID_PHONE_AREA_CODES.add("971");
        VALID_PHONE_AREA_CODES.add("972");
        VALID_PHONE_AREA_CODES.add("973");
        VALID_PHONE_AREA_CODES.add("978");
        VALID_PHONE_AREA_CODES.add("979");
        VALID_PHONE_AREA_CODES.add("980");
        VALID_PHONE_AREA_CODES.add("983");
        VALID_PHONE_AREA_CODES.add("984");
        VALID_PHONE_AREA_CODES.add("985");
        VALID_PHONE_AREA_CODES.add("986");
        VALID_PHONE_AREA_CODES.add("989");
        
        // Easily recognizable codes from lines 441-520
        VALID_PHONE_AREA_CODES.add("200");
        VALID_PHONE_AREA_CODES.add("211");
        VALID_PHONE_AREA_CODES.add("222");
        VALID_PHONE_AREA_CODES.add("233");
        VALID_PHONE_AREA_CODES.add("244");
        VALID_PHONE_AREA_CODES.add("255");
        VALID_PHONE_AREA_CODES.add("266");
        VALID_PHONE_AREA_CODES.add("277");
        VALID_PHONE_AREA_CODES.add("288");
        VALID_PHONE_AREA_CODES.add("299");
        VALID_PHONE_AREA_CODES.add("300");
        VALID_PHONE_AREA_CODES.add("311");
        VALID_PHONE_AREA_CODES.add("322");
        VALID_PHONE_AREA_CODES.add("333");
        VALID_PHONE_AREA_CODES.add("344");
        VALID_PHONE_AREA_CODES.add("355");
        VALID_PHONE_AREA_CODES.add("366");
        VALID_PHONE_AREA_CODES.add("377");
        VALID_PHONE_AREA_CODES.add("388");
        VALID_PHONE_AREA_CODES.add("399");
        VALID_PHONE_AREA_CODES.add("400");
        VALID_PHONE_AREA_CODES.add("411");
        VALID_PHONE_AREA_CODES.add("422");
        VALID_PHONE_AREA_CODES.add("433");
        VALID_PHONE_AREA_CODES.add("444");
        VALID_PHONE_AREA_CODES.add("455");
        VALID_PHONE_AREA_CODES.add("466");
        VALID_PHONE_AREA_CODES.add("477");
        VALID_PHONE_AREA_CODES.add("488");
        VALID_PHONE_AREA_CODES.add("499");
        VALID_PHONE_AREA_CODES.add("500");
        VALID_PHONE_AREA_CODES.add("511");
        VALID_PHONE_AREA_CODES.add("522");
        VALID_PHONE_AREA_CODES.add("533");
        VALID_PHONE_AREA_CODES.add("544");
        VALID_PHONE_AREA_CODES.add("555");
        VALID_PHONE_AREA_CODES.add("566");
        VALID_PHONE_AREA_CODES.add("577");
        VALID_PHONE_AREA_CODES.add("588");
        VALID_PHONE_AREA_CODES.add("599");
        VALID_PHONE_AREA_CODES.add("600");
        VALID_PHONE_AREA_CODES.add("611");
        VALID_PHONE_AREA_CODES.add("622");
        VALID_PHONE_AREA_CODES.add("633");
        VALID_PHONE_AREA_CODES.add("644");
        VALID_PHONE_AREA_CODES.add("655");
        VALID_PHONE_AREA_CODES.add("666");
        VALID_PHONE_AREA_CODES.add("677");
        VALID_PHONE_AREA_CODES.add("688");
        VALID_PHONE_AREA_CODES.add("699");
        VALID_PHONE_AREA_CODES.add("700");
        VALID_PHONE_AREA_CODES.add("711");
        VALID_PHONE_AREA_CODES.add("722");
        VALID_PHONE_AREA_CODES.add("733");
        VALID_PHONE_AREA_CODES.add("744");
        VALID_PHONE_AREA_CODES.add("755");
        VALID_PHONE_AREA_CODES.add("766");
        VALID_PHONE_AREA_CODES.add("777");
        VALID_PHONE_AREA_CODES.add("788");
        VALID_PHONE_AREA_CODES.add("799");
        VALID_PHONE_AREA_CODES.add("800");
        VALID_PHONE_AREA_CODES.add("811");
        VALID_PHONE_AREA_CODES.add("822");
        VALID_PHONE_AREA_CODES.add("833");
        VALID_PHONE_AREA_CODES.add("844");
        VALID_PHONE_AREA_CODES.add("855");
        VALID_PHONE_AREA_CODES.add("866");
        VALID_PHONE_AREA_CODES.add("877");
        VALID_PHONE_AREA_CODES.add("888");
        VALID_PHONE_AREA_CODES.add("899");
        VALID_PHONE_AREA_CODES.add("900");
        VALID_PHONE_AREA_CODES.add("911");
        VALID_PHONE_AREA_CODES.add("922");
        VALID_PHONE_AREA_CODES.add("933");
        VALID_PHONE_AREA_CODES.add("944");
        VALID_PHONE_AREA_CODES.add("955");
        VALID_PHONE_AREA_CODES.add("966");
        VALID_PHONE_AREA_CODES.add("977");
        VALID_PHONE_AREA_CODES.add("988");
        VALID_PHONE_AREA_CODES.add("999");
    }
    
    /**
     * Initialize valid general purpose codes from CSLKPCDY.cpy VALID-GENERAL-PURP-CODE
     */
    private static void initializeValidGeneralPurposeCodes() {
        // This is a subset of the valid phone area codes for general purpose use
        // From lines 521-930 in CSLKPCDY.cpy
        VALID_GENERAL_PURPOSE_CODES.addAll(VALID_PHONE_AREA_CODES);
        
        // Remove the easily recognizable codes from general purpose codes
        VALID_GENERAL_PURPOSE_CODES.removeAll(VALID_EASY_RECOGNIZABLE_CODES);
    }
    
    /**
     * Initialize valid easily recognizable codes from CSLKPCDY.cpy VALID-EASY-RECOG-AREA-CODE
     */
    private static void initializeValidEasyRecognizableCodes() {
        // Easily recognizable codes from lines 931-1010 in CSLKPCDY.cpy
        VALID_EASY_RECOGNIZABLE_CODES.add("200");
        VALID_EASY_RECOGNIZABLE_CODES.add("211");
        VALID_EASY_RECOGNIZABLE_CODES.add("222");
        VALID_EASY_RECOGNIZABLE_CODES.add("233");
        VALID_EASY_RECOGNIZABLE_CODES.add("244");
        VALID_EASY_RECOGNIZABLE_CODES.add("255");
        VALID_EASY_RECOGNIZABLE_CODES.add("266");
        VALID_EASY_RECOGNIZABLE_CODES.add("277");
        VALID_EASY_RECOGNIZABLE_CODES.add("288");
        VALID_EASY_RECOGNIZABLE_CODES.add("299");
        VALID_EASY_RECOGNIZABLE_CODES.add("300");
        VALID_EASY_RECOGNIZABLE_CODES.add("311");
        VALID_EASY_RECOGNIZABLE_CODES.add("322");
        VALID_EASY_RECOGNIZABLE_CODES.add("333");
        VALID_EASY_RECOGNIZABLE_CODES.add("344");
        VALID_EASY_RECOGNIZABLE_CODES.add("355");
        VALID_EASY_RECOGNIZABLE_CODES.add("366");
        VALID_EASY_RECOGNIZABLE_CODES.add("377");
        VALID_EASY_RECOGNIZABLE_CODES.add("388");
        VALID_EASY_RECOGNIZABLE_CODES.add("399");
        VALID_EASY_RECOGNIZABLE_CODES.add("400");
        VALID_EASY_RECOGNIZABLE_CODES.add("411");
        VALID_EASY_RECOGNIZABLE_CODES.add("422");
        VALID_EASY_RECOGNIZABLE_CODES.add("433");
        VALID_EASY_RECOGNIZABLE_CODES.add("444");
        VALID_EASY_RECOGNIZABLE_CODES.add("455");
        VALID_EASY_RECOGNIZABLE_CODES.add("466");
        VALID_EASY_RECOGNIZABLE_CODES.add("477");
        VALID_EASY_RECOGNIZABLE_CODES.add("488");
        VALID_EASY_RECOGNIZABLE_CODES.add("499");
        VALID_EASY_RECOGNIZABLE_CODES.add("500");
        VALID_EASY_RECOGNIZABLE_CODES.add("511");
        VALID_EASY_RECOGNIZABLE_CODES.add("522");
        VALID_EASY_RECOGNIZABLE_CODES.add("533");
        VALID_EASY_RECOGNIZABLE_CODES.add("544");
        VALID_EASY_RECOGNIZABLE_CODES.add("555");
        VALID_EASY_RECOGNIZABLE_CODES.add("566");
        VALID_EASY_RECOGNIZABLE_CODES.add("577");
        VALID_EASY_RECOGNIZABLE_CODES.add("588");
        VALID_EASY_RECOGNIZABLE_CODES.add("599");
        VALID_EASY_RECOGNIZABLE_CODES.add("600");
        VALID_EASY_RECOGNIZABLE_CODES.add("611");
        VALID_EASY_RECOGNIZABLE_CODES.add("622");
        VALID_EASY_RECOGNIZABLE_CODES.add("633");
        VALID_EASY_RECOGNIZABLE_CODES.add("644");
        VALID_EASY_RECOGNIZABLE_CODES.add("655");
        VALID_EASY_RECOGNIZABLE_CODES.add("666");
        VALID_EASY_RECOGNIZABLE_CODES.add("677");
        VALID_EASY_RECOGNIZABLE_CODES.add("688");
        VALID_EASY_RECOGNIZABLE_CODES.add("699");
        VALID_EASY_RECOGNIZABLE_CODES.add("700");
        VALID_EASY_RECOGNIZABLE_CODES.add("711");
        VALID_EASY_RECOGNIZABLE_CODES.add("722");
        VALID_EASY_RECOGNIZABLE_CODES.add("733");
        VALID_EASY_RECOGNIZABLE_CODES.add("744");
        VALID_EASY_RECOGNIZABLE_CODES.add("755");
        VALID_EASY_RECOGNIZABLE_CODES.add("766");
        VALID_EASY_RECOGNIZABLE_CODES.add("777");
        VALID_EASY_RECOGNIZABLE_CODES.add("788");
        VALID_EASY_RECOGNIZABLE_CODES.add("799");
        VALID_EASY_RECOGNIZABLE_CODES.add("800");
        VALID_EASY_RECOGNIZABLE_CODES.add("811");
        VALID_EASY_RECOGNIZABLE_CODES.add("822");
        VALID_EASY_RECOGNIZABLE_CODES.add("833");
        VALID_EASY_RECOGNIZABLE_CODES.add("844");
        VALID_EASY_RECOGNIZABLE_CODES.add("855");
        VALID_EASY_RECOGNIZABLE_CODES.add("866");
        VALID_EASY_RECOGNIZABLE_CODES.add("877");
        VALID_EASY_RECOGNIZABLE_CODES.add("888");
        VALID_EASY_RECOGNIZABLE_CODES.add("899");
        VALID_EASY_RECOGNIZABLE_CODES.add("900");
        VALID_EASY_RECOGNIZABLE_CODES.add("911");
        VALID_EASY_RECOGNIZABLE_CODES.add("922");
        VALID_EASY_RECOGNIZABLE_CODES.add("933");
        VALID_EASY_RECOGNIZABLE_CODES.add("944");
        VALID_EASY_RECOGNIZABLE_CODES.add("955");
        VALID_EASY_RECOGNIZABLE_CODES.add("966");
        VALID_EASY_RECOGNIZABLE_CODES.add("977");
        VALID_EASY_RECOGNIZABLE_CODES.add("988");
        VALID_EASY_RECOGNIZABLE_CODES.add("999");
    }
    
    @Override
    public void initialize(ValidPhoneNumber annotation) {
        this.annotation = annotation;
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // Handle null and empty values based on annotation configuration
        if (phoneNumber == null) {
            return annotation.allowNull();
        }
        
        if (phoneNumber.trim().isEmpty()) {
            return annotation.allowEmpty();
        }
        
        // Check maximum length constraint (COBOL field is PIC X(15))
        if (phoneNumber.length() > annotation.maxLength()) {
            addCustomMessage(context, "Phone number exceeds maximum length of " + annotation.maxLength() + " characters");
            return false;
        }
        
        // Normalize the phone number by removing all non-digit characters
        String normalizedNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Validate the format and extract area code
        String areaCode = extractAreaCode(phoneNumber);
        if (areaCode == null) {
            if (annotation.message().equals("Invalid phone number format or area code not recognized")) {
                addCustomMessage(context, "Invalid phone number format. Expected North American format (10 digits)");
            } else {
                addCustomMessage(context, annotation.message());
            }
            return false;
        }
        
        // Validate area code against COBOL lookup table
        if (!isValidAreaCode(areaCode)) {
            if (annotation.message().equals("Invalid phone number format or area code not recognized")) {
                addCustomMessage(context, "Area code " + areaCode + " is not a valid North American area code");
            } else {
                addCustomMessage(context, annotation.message());
            }
            return false;
        }
        
        // Check if easily recognizable area codes are allowed
        if (!annotation.allowEasyRecognizableAreaCodes() && VALID_EASY_RECOGNIZABLE_CODES.contains(areaCode)) {
            if (annotation.message().equals("Invalid phone number format or area code not recognized")) {
                addCustomMessage(context, "Area code " + areaCode + " is an easily recognizable code and not allowed");
            } else {
                addCustomMessage(context, annotation.message());
            }
            return false;
        }
        
        // Check if general purpose only mode is enabled
        if (annotation.generalPurposeOnly() && !VALID_GENERAL_PURPOSE_CODES.contains(areaCode)) {
            addCustomMessage(context, "Area code " + areaCode + " is not a valid general purpose area code");
            return false;
        }
        
        // Validate exchange code (second group of 3 digits)
        if (normalizedNumber.length() == 10) {
            String exchangeCode = normalizedNumber.substring(3, 6);
            if (!isValidExchangeCode(exchangeCode)) {
                addCustomMessage(context, "Invalid exchange code " + exchangeCode + ". Exchange code cannot start with 0 or 1, and cannot be N11");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Extract area code from various phone number formats
     * 
     * @param phoneNumber the phone number string
     * @return the area code or null if invalid format
     */
    private String extractAreaCode(String phoneNumber) {
        // Try formatted patterns first
        Matcher formattedMatcher = PHONE_PATTERN_FORMATTED.matcher(phoneNumber);
        if (formattedMatcher.matches()) {
            return formattedMatcher.group(1);
        }
        
        // Try international format
        Matcher internationalMatcher = PHONE_PATTERN_INTERNATIONAL.matcher(phoneNumber);
        if (internationalMatcher.matches()) {
            return internationalMatcher.group(1);
        }
        
        // Try digits only format (10 digits)
        Matcher digitsMatcher = PHONE_PATTERN_DIGITS_ONLY.matcher(phoneNumber);
        if (digitsMatcher.matches()) {
            String digits = digitsMatcher.group(1);
            return digits.substring(0, 3);
        }
        
        // Try digits with country code format (11 digits starting with 1)
        Matcher digitsWithCountryMatcher = PHONE_PATTERN_DIGITS_WITH_COUNTRY_CODE.matcher(phoneNumber);
        if (digitsWithCountryMatcher.matches()) {
            String digits = digitsWithCountryMatcher.group(1);
            return digits.substring(0, 3);
        }
        
        // No fallback - only accept numbers that match explicit patterns
        return null;
    }
    
    /**
     * Validate area code against COBOL lookup table
     * 
     * @param areaCode the area code to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidAreaCode(String areaCode) {
        return VALID_PHONE_AREA_CODES.contains(areaCode);
    }
    
    /**
     * Validate exchange code according to NANP rules
     * 
     * @param exchangeCode the exchange code to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidExchangeCode(String exchangeCode) {
        if (exchangeCode == null || exchangeCode.length() != 3) {
            return false;
        }
        
        // Exchange code cannot start with 0 or 1
        char firstDigit = exchangeCode.charAt(0);
        if (firstDigit == '0' || firstDigit == '1') {
            return false;
        }
        
        // Exchange code cannot be N11 (where N is 2-9)
        if (exchangeCode.endsWith("11") && firstDigit >= '2' && firstDigit <= '9') {
            return false;
        }
        
        return true;
    }
    
    /**
     * Add a custom validation message to the context
     * 
     * @param context the validation context
     * @param message the custom message
     */
    private void addCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        
        // Add context if provided
        String fullMessage = message;
        if (!annotation.context().isEmpty()) {
            fullMessage = annotation.context() + ": " + message;
        }
        
        context.buildConstraintViolationWithTemplate(fullMessage)
               .addConstraintViolation();
    }
}