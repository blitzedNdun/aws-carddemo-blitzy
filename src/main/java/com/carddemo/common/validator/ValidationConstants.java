/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.carddemo.common.validator;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Central constants class containing all validation patterns, lookup tables, and constant values 
 * extracted from COBOL copybooks. Provides efficient Set-based lookups for area codes, state codes, 
 * and other validation data while maintaining exact consistency with original COBOL validation logic.
 * 
 * This class consolidates validation constants from:
 * - CSLKPCDY.cpy - North American area codes, US state codes, and state-ZIP combinations
 * - CSUTLDPY.cpy - Date validation patterns and procedures
 * - CUSTREC.cpy - Customer record field constraints and data types
 */
public final class ValidationConstants {
    
    // Private constructor to prevent instantiation
    private ValidationConstants() {
        throw new AssertionError("ValidationConstants is a utility class and cannot be instantiated");
    }
    
    // ===========================
    // AREA CODE VALIDATION
    // ===========================
    
    /**
     * Valid North American phone area codes from NANPA (North American Numbering Plan Administrator).
     * Extracted from CSLKPCDY.cpy VALID-PHONE-AREA-CODE 88-level condition.
     * Includes both general purpose and easily recognizable area codes.
     */
    public static final Set<String> VALID_AREA_CODES = new HashSet<>();
    
    static {
        // Initialize area codes from CSLKPCDY.cpy VALID-PHONE-AREA-CODE values
        VALID_AREA_CODES.add("201");
        VALID_AREA_CODES.add("202");
        VALID_AREA_CODES.add("203");
        VALID_AREA_CODES.add("204");
        VALID_AREA_CODES.add("205");
        VALID_AREA_CODES.add("206");
        VALID_AREA_CODES.add("207");
        VALID_AREA_CODES.add("208");
        VALID_AREA_CODES.add("209");
        VALID_AREA_CODES.add("210");
        VALID_AREA_CODES.add("212");
        VALID_AREA_CODES.add("213");
        VALID_AREA_CODES.add("214");
        VALID_AREA_CODES.add("215");
        VALID_AREA_CODES.add("216");
        VALID_AREA_CODES.add("217");
        VALID_AREA_CODES.add("218");
        VALID_AREA_CODES.add("219");
        VALID_AREA_CODES.add("220");
        VALID_AREA_CODES.add("223");
        VALID_AREA_CODES.add("224");
        VALID_AREA_CODES.add("225");
        VALID_AREA_CODES.add("226");
        VALID_AREA_CODES.add("228");
        VALID_AREA_CODES.add("229");
        VALID_AREA_CODES.add("231");
        VALID_AREA_CODES.add("234");
        VALID_AREA_CODES.add("236");
        VALID_AREA_CODES.add("239");
        VALID_AREA_CODES.add("240");
        VALID_AREA_CODES.add("242");
        VALID_AREA_CODES.add("246");
        VALID_AREA_CODES.add("248");
        VALID_AREA_CODES.add("249");
        VALID_AREA_CODES.add("250");
        VALID_AREA_CODES.add("251");
        VALID_AREA_CODES.add("252");
        VALID_AREA_CODES.add("253");
        VALID_AREA_CODES.add("254");
        VALID_AREA_CODES.add("256");
        VALID_AREA_CODES.add("260");
        VALID_AREA_CODES.add("262");
        VALID_AREA_CODES.add("264");
        VALID_AREA_CODES.add("267");
        VALID_AREA_CODES.add("268");
        VALID_AREA_CODES.add("269");
        VALID_AREA_CODES.add("270");
        VALID_AREA_CODES.add("272");
        VALID_AREA_CODES.add("276");
        VALID_AREA_CODES.add("279");
        VALID_AREA_CODES.add("281");
        VALID_AREA_CODES.add("284");
        VALID_AREA_CODES.add("289");
        VALID_AREA_CODES.add("301");
        VALID_AREA_CODES.add("302");
        VALID_AREA_CODES.add("303");
        VALID_AREA_CODES.add("304");
        VALID_AREA_CODES.add("305");
        VALID_AREA_CODES.add("306");
        VALID_AREA_CODES.add("307");
        VALID_AREA_CODES.add("308");
        VALID_AREA_CODES.add("309");
        VALID_AREA_CODES.add("310");
        VALID_AREA_CODES.add("312");
        VALID_AREA_CODES.add("313");
        VALID_AREA_CODES.add("314");
        VALID_AREA_CODES.add("315");
        VALID_AREA_CODES.add("316");
        VALID_AREA_CODES.add("317");
        VALID_AREA_CODES.add("318");
        VALID_AREA_CODES.add("319");
        VALID_AREA_CODES.add("320");
        VALID_AREA_CODES.add("321");
        VALID_AREA_CODES.add("323");
        VALID_AREA_CODES.add("325");
        VALID_AREA_CODES.add("326");
        VALID_AREA_CODES.add("330");
        VALID_AREA_CODES.add("331");
        VALID_AREA_CODES.add("332");
        VALID_AREA_CODES.add("334");
        VALID_AREA_CODES.add("336");
        VALID_AREA_CODES.add("337");
        VALID_AREA_CODES.add("339");
        VALID_AREA_CODES.add("340");
        VALID_AREA_CODES.add("341");
        VALID_AREA_CODES.add("343");
        VALID_AREA_CODES.add("345");
        VALID_AREA_CODES.add("346");
        VALID_AREA_CODES.add("347");
        VALID_AREA_CODES.add("351");
        VALID_AREA_CODES.add("352");
        VALID_AREA_CODES.add("360");
        VALID_AREA_CODES.add("361");
        VALID_AREA_CODES.add("364");
        VALID_AREA_CODES.add("365");
        VALID_AREA_CODES.add("367");
        VALID_AREA_CODES.add("368");
        VALID_AREA_CODES.add("380");
        VALID_AREA_CODES.add("385");
        VALID_AREA_CODES.add("386");
        VALID_AREA_CODES.add("401");
        VALID_AREA_CODES.add("402");
        VALID_AREA_CODES.add("403");
        VALID_AREA_CODES.add("404");
        VALID_AREA_CODES.add("405");
        VALID_AREA_CODES.add("406");
        VALID_AREA_CODES.add("407");
        VALID_AREA_CODES.add("408");
        VALID_AREA_CODES.add("409");
        VALID_AREA_CODES.add("410");
        VALID_AREA_CODES.add("412");
        VALID_AREA_CODES.add("413");
        VALID_AREA_CODES.add("414");
        VALID_AREA_CODES.add("415");
        VALID_AREA_CODES.add("416");
        VALID_AREA_CODES.add("417");
        VALID_AREA_CODES.add("418");
        VALID_AREA_CODES.add("419");
        VALID_AREA_CODES.add("423");
        VALID_AREA_CODES.add("424");
        VALID_AREA_CODES.add("425");
        VALID_AREA_CODES.add("430");
        VALID_AREA_CODES.add("431");
        VALID_AREA_CODES.add("432");
        VALID_AREA_CODES.add("434");
        VALID_AREA_CODES.add("435");
        VALID_AREA_CODES.add("437");
        VALID_AREA_CODES.add("438");
        VALID_AREA_CODES.add("440");
        VALID_AREA_CODES.add("441");
        VALID_AREA_CODES.add("442");
        VALID_AREA_CODES.add("443");
        VALID_AREA_CODES.add("445");
        VALID_AREA_CODES.add("447");
        VALID_AREA_CODES.add("448");
        VALID_AREA_CODES.add("450");
        VALID_AREA_CODES.add("458");
        VALID_AREA_CODES.add("463");
        VALID_AREA_CODES.add("464");
        VALID_AREA_CODES.add("469");
        VALID_AREA_CODES.add("470");
        VALID_AREA_CODES.add("473");
        VALID_AREA_CODES.add("474");
        VALID_AREA_CODES.add("475");
        VALID_AREA_CODES.add("478");
        VALID_AREA_CODES.add("479");
        VALID_AREA_CODES.add("480");
        VALID_AREA_CODES.add("484");
        VALID_AREA_CODES.add("501");
        VALID_AREA_CODES.add("502");
        VALID_AREA_CODES.add("503");
        VALID_AREA_CODES.add("504");
        VALID_AREA_CODES.add("505");
        VALID_AREA_CODES.add("506");
        VALID_AREA_CODES.add("507");
        VALID_AREA_CODES.add("508");
        VALID_AREA_CODES.add("509");
        VALID_AREA_CODES.add("510");
        VALID_AREA_CODES.add("512");
        VALID_AREA_CODES.add("513");
        VALID_AREA_CODES.add("514");
        VALID_AREA_CODES.add("515");
        VALID_AREA_CODES.add("516");
        VALID_AREA_CODES.add("517");
        VALID_AREA_CODES.add("518");
        VALID_AREA_CODES.add("519");
        VALID_AREA_CODES.add("520");
        VALID_AREA_CODES.add("530");
        VALID_AREA_CODES.add("531");
        VALID_AREA_CODES.add("534");
        VALID_AREA_CODES.add("539");
        VALID_AREA_CODES.add("540");
        VALID_AREA_CODES.add("541");
        VALID_AREA_CODES.add("548");
        VALID_AREA_CODES.add("551");
        VALID_AREA_CODES.add("559");
        VALID_AREA_CODES.add("561");
        VALID_AREA_CODES.add("562");
        VALID_AREA_CODES.add("563");
        VALID_AREA_CODES.add("564");
        VALID_AREA_CODES.add("567");
        VALID_AREA_CODES.add("570");
        VALID_AREA_CODES.add("571");
        VALID_AREA_CODES.add("572");
        VALID_AREA_CODES.add("573");
        VALID_AREA_CODES.add("574");
        VALID_AREA_CODES.add("575");
        VALID_AREA_CODES.add("579");
        VALID_AREA_CODES.add("580");
        VALID_AREA_CODES.add("581");
        VALID_AREA_CODES.add("582");
        VALID_AREA_CODES.add("585");
        VALID_AREA_CODES.add("586");
        VALID_AREA_CODES.add("587");
        VALID_AREA_CODES.add("601");
        VALID_AREA_CODES.add("602");
        VALID_AREA_CODES.add("603");
        VALID_AREA_CODES.add("604");
        VALID_AREA_CODES.add("605");
        VALID_AREA_CODES.add("606");
        VALID_AREA_CODES.add("607");
        VALID_AREA_CODES.add("608");
        VALID_AREA_CODES.add("609");
        VALID_AREA_CODES.add("610");
        VALID_AREA_CODES.add("612");
        VALID_AREA_CODES.add("613");
        VALID_AREA_CODES.add("614");
        VALID_AREA_CODES.add("615");
        VALID_AREA_CODES.add("616");
        VALID_AREA_CODES.add("617");
        VALID_AREA_CODES.add("618");
        VALID_AREA_CODES.add("619");
        VALID_AREA_CODES.add("620");
        VALID_AREA_CODES.add("623");
        VALID_AREA_CODES.add("626");
        VALID_AREA_CODES.add("628");
        VALID_AREA_CODES.add("629");
        VALID_AREA_CODES.add("630");
        VALID_AREA_CODES.add("631");
        VALID_AREA_CODES.add("636");
        VALID_AREA_CODES.add("639");
        VALID_AREA_CODES.add("640");
        VALID_AREA_CODES.add("641");
        VALID_AREA_CODES.add("646");
        VALID_AREA_CODES.add("647");
        VALID_AREA_CODES.add("649");
        VALID_AREA_CODES.add("650");
        VALID_AREA_CODES.add("651");
        VALID_AREA_CODES.add("656");
        VALID_AREA_CODES.add("657");
        VALID_AREA_CODES.add("658");
        VALID_AREA_CODES.add("659");
        VALID_AREA_CODES.add("660");
        VALID_AREA_CODES.add("661");
        VALID_AREA_CODES.add("662");
        VALID_AREA_CODES.add("664");
        VALID_AREA_CODES.add("667");
        VALID_AREA_CODES.add("669");
        VALID_AREA_CODES.add("670");
        VALID_AREA_CODES.add("671");
        VALID_AREA_CODES.add("672");
        VALID_AREA_CODES.add("678");
        VALID_AREA_CODES.add("680");
        VALID_AREA_CODES.add("681");
        VALID_AREA_CODES.add("682");
        VALID_AREA_CODES.add("683");
        VALID_AREA_CODES.add("684");
        VALID_AREA_CODES.add("689");
        VALID_AREA_CODES.add("701");
        VALID_AREA_CODES.add("702");
        VALID_AREA_CODES.add("703");
        VALID_AREA_CODES.add("704");
        VALID_AREA_CODES.add("705");
        VALID_AREA_CODES.add("706");
        VALID_AREA_CODES.add("707");
        VALID_AREA_CODES.add("708");
        VALID_AREA_CODES.add("709");
        VALID_AREA_CODES.add("712");
        VALID_AREA_CODES.add("713");
        VALID_AREA_CODES.add("714");
        VALID_AREA_CODES.add("715");
        VALID_AREA_CODES.add("716");
        VALID_AREA_CODES.add("717");
        VALID_AREA_CODES.add("718");
        VALID_AREA_CODES.add("719");
        VALID_AREA_CODES.add("720");
        VALID_AREA_CODES.add("721");
        VALID_AREA_CODES.add("724");
        VALID_AREA_CODES.add("725");
        VALID_AREA_CODES.add("726");
        VALID_AREA_CODES.add("727");
        VALID_AREA_CODES.add("731");
        VALID_AREA_CODES.add("732");
        VALID_AREA_CODES.add("734");
        VALID_AREA_CODES.add("737");
        VALID_AREA_CODES.add("740");
        VALID_AREA_CODES.add("742");
        VALID_AREA_CODES.add("743");
        VALID_AREA_CODES.add("747");
        VALID_AREA_CODES.add("753");
        VALID_AREA_CODES.add("754");
        VALID_AREA_CODES.add("757");
        VALID_AREA_CODES.add("758");
        VALID_AREA_CODES.add("760");
        VALID_AREA_CODES.add("762");
        VALID_AREA_CODES.add("763");
        VALID_AREA_CODES.add("765");
        VALID_AREA_CODES.add("767");
        VALID_AREA_CODES.add("769");
        VALID_AREA_CODES.add("770");
        VALID_AREA_CODES.add("771");
        VALID_AREA_CODES.add("772");
        VALID_AREA_CODES.add("773");
        VALID_AREA_CODES.add("774");
        VALID_AREA_CODES.add("775");
        VALID_AREA_CODES.add("778");
        VALID_AREA_CODES.add("779");
        VALID_AREA_CODES.add("780");
        VALID_AREA_CODES.add("781");
        VALID_AREA_CODES.add("782");
        VALID_AREA_CODES.add("784");
        VALID_AREA_CODES.add("785");
        VALID_AREA_CODES.add("786");
        VALID_AREA_CODES.add("787");
        VALID_AREA_CODES.add("801");
        VALID_AREA_CODES.add("802");
        VALID_AREA_CODES.add("803");
        VALID_AREA_CODES.add("804");
        VALID_AREA_CODES.add("805");
        VALID_AREA_CODES.add("806");
        VALID_AREA_CODES.add("807");
        VALID_AREA_CODES.add("808");
        VALID_AREA_CODES.add("809");
        VALID_AREA_CODES.add("810");
        VALID_AREA_CODES.add("812");
        VALID_AREA_CODES.add("813");
        VALID_AREA_CODES.add("814");
        VALID_AREA_CODES.add("815");
        VALID_AREA_CODES.add("816");
        VALID_AREA_CODES.add("817");
        VALID_AREA_CODES.add("818");
        VALID_AREA_CODES.add("819");
        VALID_AREA_CODES.add("820");
        VALID_AREA_CODES.add("825");
        VALID_AREA_CODES.add("826");
        VALID_AREA_CODES.add("828");
        VALID_AREA_CODES.add("829");
        VALID_AREA_CODES.add("830");
        VALID_AREA_CODES.add("831");
        VALID_AREA_CODES.add("832");
        VALID_AREA_CODES.add("838");
        VALID_AREA_CODES.add("839");
        VALID_AREA_CODES.add("840");
        VALID_AREA_CODES.add("843");
        VALID_AREA_CODES.add("845");
        VALID_AREA_CODES.add("847");
        VALID_AREA_CODES.add("848");
        VALID_AREA_CODES.add("849");
        VALID_AREA_CODES.add("850");
        VALID_AREA_CODES.add("854");
        VALID_AREA_CODES.add("856");
        VALID_AREA_CODES.add("857");
        VALID_AREA_CODES.add("858");
        VALID_AREA_CODES.add("859");
        VALID_AREA_CODES.add("860");
        VALID_AREA_CODES.add("862");
        VALID_AREA_CODES.add("863");
        VALID_AREA_CODES.add("864");
        VALID_AREA_CODES.add("865");
        VALID_AREA_CODES.add("867");
        VALID_AREA_CODES.add("868");
        VALID_AREA_CODES.add("869");
        VALID_AREA_CODES.add("870");
        VALID_AREA_CODES.add("872");
        VALID_AREA_CODES.add("873");
        VALID_AREA_CODES.add("876");
        VALID_AREA_CODES.add("878");
        VALID_AREA_CODES.add("901");
        VALID_AREA_CODES.add("902");
        VALID_AREA_CODES.add("903");
        VALID_AREA_CODES.add("904");
        VALID_AREA_CODES.add("905");
        VALID_AREA_CODES.add("906");
        VALID_AREA_CODES.add("907");
        VALID_AREA_CODES.add("908");
        VALID_AREA_CODES.add("909");
        VALID_AREA_CODES.add("910");
        VALID_AREA_CODES.add("912");
        VALID_AREA_CODES.add("913");
        VALID_AREA_CODES.add("914");
        VALID_AREA_CODES.add("915");
        VALID_AREA_CODES.add("916");
        VALID_AREA_CODES.add("917");
        VALID_AREA_CODES.add("918");
        VALID_AREA_CODES.add("919");
        VALID_AREA_CODES.add("920");
        VALID_AREA_CODES.add("925");
        VALID_AREA_CODES.add("928");
        VALID_AREA_CODES.add("929");
        VALID_AREA_CODES.add("930");
        VALID_AREA_CODES.add("931");
        VALID_AREA_CODES.add("934");
        VALID_AREA_CODES.add("936");
        VALID_AREA_CODES.add("937");
        VALID_AREA_CODES.add("938");
        VALID_AREA_CODES.add("939");
        VALID_AREA_CODES.add("940");
        VALID_AREA_CODES.add("941");
        VALID_AREA_CODES.add("943");
        VALID_AREA_CODES.add("945");
        VALID_AREA_CODES.add("947");
        VALID_AREA_CODES.add("948");
        VALID_AREA_CODES.add("949");
        VALID_AREA_CODES.add("951");
        VALID_AREA_CODES.add("952");
        VALID_AREA_CODES.add("954");
        VALID_AREA_CODES.add("956");
        VALID_AREA_CODES.add("959");
        VALID_AREA_CODES.add("970");
        VALID_AREA_CODES.add("971");
        VALID_AREA_CODES.add("972");
        VALID_AREA_CODES.add("973");
        VALID_AREA_CODES.add("978");
        VALID_AREA_CODES.add("979");
        VALID_AREA_CODES.add("980");
        VALID_AREA_CODES.add("983");
        VALID_AREA_CODES.add("984");
        VALID_AREA_CODES.add("985");
        VALID_AREA_CODES.add("986");
        VALID_AREA_CODES.add("989");
        
        // Add easily recognizable codes (from line 440+ in CSLKPCDY.cpy)
        VALID_AREA_CODES.add("200");
        VALID_AREA_CODES.add("211");
        VALID_AREA_CODES.add("222");
        VALID_AREA_CODES.add("233");
        VALID_AREA_CODES.add("244");
        VALID_AREA_CODES.add("255");
        VALID_AREA_CODES.add("266");
        VALID_AREA_CODES.add("277");
        VALID_AREA_CODES.add("288");
        VALID_AREA_CODES.add("299");
        VALID_AREA_CODES.add("300");
        VALID_AREA_CODES.add("311");
        VALID_AREA_CODES.add("322");
        VALID_AREA_CODES.add("333");
        VALID_AREA_CODES.add("344");
        VALID_AREA_CODES.add("355");
        VALID_AREA_CODES.add("366");
        VALID_AREA_CODES.add("377");
        VALID_AREA_CODES.add("388");
        VALID_AREA_CODES.add("399");
        VALID_AREA_CODES.add("400");
        VALID_AREA_CODES.add("411");
        VALID_AREA_CODES.add("422");
        VALID_AREA_CODES.add("433");
        VALID_AREA_CODES.add("444");
        VALID_AREA_CODES.add("455");
        VALID_AREA_CODES.add("466");
        VALID_AREA_CODES.add("477");
        VALID_AREA_CODES.add("488");
        VALID_AREA_CODES.add("499");
        VALID_AREA_CODES.add("500");
        VALID_AREA_CODES.add("511");
        VALID_AREA_CODES.add("522");
        VALID_AREA_CODES.add("533");
        VALID_AREA_CODES.add("544");
        VALID_AREA_CODES.add("555");
        VALID_AREA_CODES.add("566");
        VALID_AREA_CODES.add("577");
        VALID_AREA_CODES.add("588");
        VALID_AREA_CODES.add("599");
        VALID_AREA_CODES.add("600");
        VALID_AREA_CODES.add("611");
        VALID_AREA_CODES.add("622");
        VALID_AREA_CODES.add("633");
        VALID_AREA_CODES.add("644");
        VALID_AREA_CODES.add("655");
        VALID_AREA_CODES.add("666");
        VALID_AREA_CODES.add("677");
        VALID_AREA_CODES.add("688");
        VALID_AREA_CODES.add("699");
        VALID_AREA_CODES.add("700");
        VALID_AREA_CODES.add("711");
        VALID_AREA_CODES.add("722");
        VALID_AREA_CODES.add("733");
        VALID_AREA_CODES.add("744");
        VALID_AREA_CODES.add("755");
        VALID_AREA_CODES.add("766");
        VALID_AREA_CODES.add("777");
        VALID_AREA_CODES.add("788");
        VALID_AREA_CODES.add("799");
        VALID_AREA_CODES.add("800");
        VALID_AREA_CODES.add("811");
        VALID_AREA_CODES.add("822");
        VALID_AREA_CODES.add("833");
        VALID_AREA_CODES.add("844");
        VALID_AREA_CODES.add("855");
        VALID_AREA_CODES.add("866");
        VALID_AREA_CODES.add("877");
        VALID_AREA_CODES.add("888");
        VALID_AREA_CODES.add("899");
        VALID_AREA_CODES.add("900");
        VALID_AREA_CODES.add("911");
        VALID_AREA_CODES.add("922");
        VALID_AREA_CODES.add("933");
        VALID_AREA_CODES.add("944");
        VALID_AREA_CODES.add("955");
        VALID_AREA_CODES.add("966");
        VALID_AREA_CODES.add("977");
        VALID_AREA_CODES.add("988");
        VALID_AREA_CODES.add("999");
    }
    
    // ===========================
    // STATE CODE VALIDATION  
    // ===========================
    
    /**
     * Valid US state codes including states and territories.
     * Extracted from CSLKPCDY.cpy VALID-US-STATE-CODE 88-level condition.
     */
    public static final Set<String> VALID_STATE_CODES = new HashSet<>();
    
    static {
        // Initialize state codes from CSLKPCDY.cpy VALID-US-STATE-CODE values
        VALID_STATE_CODES.add("AL"); // Alabama
        VALID_STATE_CODES.add("AK"); // Alaska
        VALID_STATE_CODES.add("AZ"); // Arizona
        VALID_STATE_CODES.add("AR"); // Arkansas
        VALID_STATE_CODES.add("CA"); // California
        VALID_STATE_CODES.add("CO"); // Colorado
        VALID_STATE_CODES.add("CT"); // Connecticut
        VALID_STATE_CODES.add("DE"); // Delaware
        VALID_STATE_CODES.add("FL"); // Florida
        VALID_STATE_CODES.add("GA"); // Georgia
        VALID_STATE_CODES.add("HI"); // Hawaii
        VALID_STATE_CODES.add("ID"); // Idaho
        VALID_STATE_CODES.add("IL"); // Illinois
        VALID_STATE_CODES.add("IN"); // Indiana
        VALID_STATE_CODES.add("IA"); // Iowa
        VALID_STATE_CODES.add("KS"); // Kansas
        VALID_STATE_CODES.add("KY"); // Kentucky
        VALID_STATE_CODES.add("LA"); // Louisiana
        VALID_STATE_CODES.add("ME"); // Maine
        VALID_STATE_CODES.add("MD"); // Maryland
        VALID_STATE_CODES.add("MA"); // Massachusetts
        VALID_STATE_CODES.add("MI"); // Michigan
        VALID_STATE_CODES.add("MN"); // Minnesota
        VALID_STATE_CODES.add("MS"); // Mississippi
        VALID_STATE_CODES.add("MO"); // Missouri
        VALID_STATE_CODES.add("MT"); // Montana
        VALID_STATE_CODES.add("NE"); // Nebraska
        VALID_STATE_CODES.add("NV"); // Nevada
        VALID_STATE_CODES.add("NH"); // New Hampshire
        VALID_STATE_CODES.add("NJ"); // New Jersey
        VALID_STATE_CODES.add("NM"); // New Mexico
        VALID_STATE_CODES.add("NY"); // New York
        VALID_STATE_CODES.add("NC"); // North Carolina
        VALID_STATE_CODES.add("ND"); // North Dakota
        VALID_STATE_CODES.add("OH"); // Ohio
        VALID_STATE_CODES.add("OK"); // Oklahoma
        VALID_STATE_CODES.add("OR"); // Oregon
        VALID_STATE_CODES.add("PA"); // Pennsylvania
        VALID_STATE_CODES.add("RI"); // Rhode Island
        VALID_STATE_CODES.add("SC"); // South Carolina
        VALID_STATE_CODES.add("SD"); // South Dakota
        VALID_STATE_CODES.add("TN"); // Tennessee
        VALID_STATE_CODES.add("TX"); // Texas
        VALID_STATE_CODES.add("UT"); // Utah
        VALID_STATE_CODES.add("VT"); // Vermont
        VALID_STATE_CODES.add("VA"); // Virginia
        VALID_STATE_CODES.add("WA"); // Washington
        VALID_STATE_CODES.add("WV"); // West Virginia
        VALID_STATE_CODES.add("WI"); // Wisconsin
        VALID_STATE_CODES.add("WY"); // Wyoming
        VALID_STATE_CODES.add("DC"); // District of Columbia
        VALID_STATE_CODES.add("AS"); // American Samoa
        VALID_STATE_CODES.add("GU"); // Guam
        VALID_STATE_CODES.add("MP"); // Northern Mariana Islands
        VALID_STATE_CODES.add("PR"); // Puerto Rico
        VALID_STATE_CODES.add("VI"); // Virgin Islands
    }
    
    // ===========================
    // STATE-ZIP CODE COMBINATIONS
    // ===========================
    
    /**
     * Valid US state and ZIP code combinations (first 2 digits of ZIP code).
     * Extracted from CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO 88-level condition.
     */
    public static final Set<String> VALID_STATE_ZIP_COMBINATIONS = new HashSet<>();
    
    static {
        // Initialize state-ZIP combinations from CSLKPCDY.cpy VALID-US-STATE-ZIP-CD2-COMBO values
        VALID_STATE_ZIP_COMBINATIONS.add("AA34");
        VALID_STATE_ZIP_COMBINATIONS.add("AE90");
        VALID_STATE_ZIP_COMBINATIONS.add("AE91");
        VALID_STATE_ZIP_COMBINATIONS.add("AE92");
        VALID_STATE_ZIP_COMBINATIONS.add("AE93");
        VALID_STATE_ZIP_COMBINATIONS.add("AE94");
        VALID_STATE_ZIP_COMBINATIONS.add("AE95");
        VALID_STATE_ZIP_COMBINATIONS.add("AE96");
        VALID_STATE_ZIP_COMBINATIONS.add("AE97");
        VALID_STATE_ZIP_COMBINATIONS.add("AE98");
        VALID_STATE_ZIP_COMBINATIONS.add("AK99");
        VALID_STATE_ZIP_COMBINATIONS.add("AL35");
        VALID_STATE_ZIP_COMBINATIONS.add("AL36");
        VALID_STATE_ZIP_COMBINATIONS.add("AP96");
        VALID_STATE_ZIP_COMBINATIONS.add("AR71");
        VALID_STATE_ZIP_COMBINATIONS.add("AR72");
        VALID_STATE_ZIP_COMBINATIONS.add("AS96");
        VALID_STATE_ZIP_COMBINATIONS.add("AZ85");
        VALID_STATE_ZIP_COMBINATIONS.add("AZ86");
        VALID_STATE_ZIP_COMBINATIONS.add("CA90");
        VALID_STATE_ZIP_COMBINATIONS.add("CA91");
        VALID_STATE_ZIP_COMBINATIONS.add("CA92");
        VALID_STATE_ZIP_COMBINATIONS.add("CA93");
        VALID_STATE_ZIP_COMBINATIONS.add("CA94");
        VALID_STATE_ZIP_COMBINATIONS.add("CA95");
        VALID_STATE_ZIP_COMBINATIONS.add("CA96");
        VALID_STATE_ZIP_COMBINATIONS.add("CO80");
        VALID_STATE_ZIP_COMBINATIONS.add("CO81");
        VALID_STATE_ZIP_COMBINATIONS.add("CT60");
        VALID_STATE_ZIP_COMBINATIONS.add("CT61");
        VALID_STATE_ZIP_COMBINATIONS.add("CT62");
        VALID_STATE_ZIP_COMBINATIONS.add("CT63");
        VALID_STATE_ZIP_COMBINATIONS.add("CT64");
        VALID_STATE_ZIP_COMBINATIONS.add("CT65");
        VALID_STATE_ZIP_COMBINATIONS.add("CT66");
        VALID_STATE_ZIP_COMBINATIONS.add("CT67");
        VALID_STATE_ZIP_COMBINATIONS.add("CT68");
        VALID_STATE_ZIP_COMBINATIONS.add("CT69");
        VALID_STATE_ZIP_COMBINATIONS.add("DC20");
        VALID_STATE_ZIP_COMBINATIONS.add("DC56");
        VALID_STATE_ZIP_COMBINATIONS.add("DC88");
        VALID_STATE_ZIP_COMBINATIONS.add("DE19");
        VALID_STATE_ZIP_COMBINATIONS.add("FL32");
        VALID_STATE_ZIP_COMBINATIONS.add("FL33");
        VALID_STATE_ZIP_COMBINATIONS.add("FL34");
        VALID_STATE_ZIP_COMBINATIONS.add("FM96");
        VALID_STATE_ZIP_COMBINATIONS.add("GA30");
        VALID_STATE_ZIP_COMBINATIONS.add("GA31");
        VALID_STATE_ZIP_COMBINATIONS.add("GA39");
        VALID_STATE_ZIP_COMBINATIONS.add("GU96");
        VALID_STATE_ZIP_COMBINATIONS.add("HI96");
        VALID_STATE_ZIP_COMBINATIONS.add("IA50");
        VALID_STATE_ZIP_COMBINATIONS.add("IA51");
        VALID_STATE_ZIP_COMBINATIONS.add("IA52");
        VALID_STATE_ZIP_COMBINATIONS.add("ID83");
        VALID_STATE_ZIP_COMBINATIONS.add("IL60");
        VALID_STATE_ZIP_COMBINATIONS.add("IL61");
        VALID_STATE_ZIP_COMBINATIONS.add("IL62");
        VALID_STATE_ZIP_COMBINATIONS.add("IN46");
        VALID_STATE_ZIP_COMBINATIONS.add("IN47");
        VALID_STATE_ZIP_COMBINATIONS.add("KS66");
        VALID_STATE_ZIP_COMBINATIONS.add("KS67");
        VALID_STATE_ZIP_COMBINATIONS.add("KY40");
        VALID_STATE_ZIP_COMBINATIONS.add("KY41");
        VALID_STATE_ZIP_COMBINATIONS.add("KY42");
        VALID_STATE_ZIP_COMBINATIONS.add("LA70");
        VALID_STATE_ZIP_COMBINATIONS.add("LA71");
        VALID_STATE_ZIP_COMBINATIONS.add("MA10");
        VALID_STATE_ZIP_COMBINATIONS.add("MA11");
        VALID_STATE_ZIP_COMBINATIONS.add("MA12");
        VALID_STATE_ZIP_COMBINATIONS.add("MA13");
        VALID_STATE_ZIP_COMBINATIONS.add("MA14");
        VALID_STATE_ZIP_COMBINATIONS.add("MA15");
        VALID_STATE_ZIP_COMBINATIONS.add("MA16");
        VALID_STATE_ZIP_COMBINATIONS.add("MA17");
        VALID_STATE_ZIP_COMBINATIONS.add("MA18");
        VALID_STATE_ZIP_COMBINATIONS.add("MA19");
        VALID_STATE_ZIP_COMBINATIONS.add("MA20");
        VALID_STATE_ZIP_COMBINATIONS.add("MA21");
        VALID_STATE_ZIP_COMBINATIONS.add("MA22");
        VALID_STATE_ZIP_COMBINATIONS.add("MA23");
        VALID_STATE_ZIP_COMBINATIONS.add("MA24");
        VALID_STATE_ZIP_COMBINATIONS.add("MA25");
        VALID_STATE_ZIP_COMBINATIONS.add("MA26");
        VALID_STATE_ZIP_COMBINATIONS.add("MA27");
        VALID_STATE_ZIP_COMBINATIONS.add("MA55");
        VALID_STATE_ZIP_COMBINATIONS.add("MD20");
        VALID_STATE_ZIP_COMBINATIONS.add("MD21");
        VALID_STATE_ZIP_COMBINATIONS.add("ME39");
        VALID_STATE_ZIP_COMBINATIONS.add("ME40");
        VALID_STATE_ZIP_COMBINATIONS.add("ME41");
        VALID_STATE_ZIP_COMBINATIONS.add("ME42");
        VALID_STATE_ZIP_COMBINATIONS.add("ME43");
        VALID_STATE_ZIP_COMBINATIONS.add("ME44");
        VALID_STATE_ZIP_COMBINATIONS.add("ME45");
        VALID_STATE_ZIP_COMBINATIONS.add("ME46");
        VALID_STATE_ZIP_COMBINATIONS.add("ME47");
        VALID_STATE_ZIP_COMBINATIONS.add("ME48");
        VALID_STATE_ZIP_COMBINATIONS.add("ME49");
        VALID_STATE_ZIP_COMBINATIONS.add("MH96");
        VALID_STATE_ZIP_COMBINATIONS.add("MI48");
        VALID_STATE_ZIP_COMBINATIONS.add("MI49");
        VALID_STATE_ZIP_COMBINATIONS.add("MN55");
        VALID_STATE_ZIP_COMBINATIONS.add("MN56");
        VALID_STATE_ZIP_COMBINATIONS.add("MO63");
        VALID_STATE_ZIP_COMBINATIONS.add("MO64");
        VALID_STATE_ZIP_COMBINATIONS.add("MO65");
        VALID_STATE_ZIP_COMBINATIONS.add("MO72");
        VALID_STATE_ZIP_COMBINATIONS.add("MP96");
        VALID_STATE_ZIP_COMBINATIONS.add("MS38");
        VALID_STATE_ZIP_COMBINATIONS.add("MS39");
        VALID_STATE_ZIP_COMBINATIONS.add("MT59");
        VALID_STATE_ZIP_COMBINATIONS.add("NC27");
        VALID_STATE_ZIP_COMBINATIONS.add("NC28");
        VALID_STATE_ZIP_COMBINATIONS.add("ND58");
        VALID_STATE_ZIP_COMBINATIONS.add("NE68");
        VALID_STATE_ZIP_COMBINATIONS.add("NE69");
        VALID_STATE_ZIP_COMBINATIONS.add("NH30");
        VALID_STATE_ZIP_COMBINATIONS.add("NH31");
        VALID_STATE_ZIP_COMBINATIONS.add("NH32");
        VALID_STATE_ZIP_COMBINATIONS.add("NH33");
        VALID_STATE_ZIP_COMBINATIONS.add("NH34");
        VALID_STATE_ZIP_COMBINATIONS.add("NH35");
        VALID_STATE_ZIP_COMBINATIONS.add("NH36");
        VALID_STATE_ZIP_COMBINATIONS.add("NH37");
        VALID_STATE_ZIP_COMBINATIONS.add("NH38");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ70");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ71");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ72");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ73");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ74");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ75");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ76");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ77");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ78");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ79");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ80");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ81");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ82");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ83");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ84");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ85");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ86");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ87");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ88");
        VALID_STATE_ZIP_COMBINATIONS.add("NJ89");
        VALID_STATE_ZIP_COMBINATIONS.add("NM87");
        VALID_STATE_ZIP_COMBINATIONS.add("NM88");
        VALID_STATE_ZIP_COMBINATIONS.add("NV88");
        VALID_STATE_ZIP_COMBINATIONS.add("NV89");
        VALID_STATE_ZIP_COMBINATIONS.add("NY50");
        VALID_STATE_ZIP_COMBINATIONS.add("NY54");
        VALID_STATE_ZIP_COMBINATIONS.add("NY63");
        VALID_STATE_ZIP_COMBINATIONS.add("NY10");
        VALID_STATE_ZIP_COMBINATIONS.add("NY11");
        VALID_STATE_ZIP_COMBINATIONS.add("NY12");
        VALID_STATE_ZIP_COMBINATIONS.add("NY13");
        VALID_STATE_ZIP_COMBINATIONS.add("NY14");
        VALID_STATE_ZIP_COMBINATIONS.add("OH43");
        VALID_STATE_ZIP_COMBINATIONS.add("OH44");
        VALID_STATE_ZIP_COMBINATIONS.add("OH45");
        VALID_STATE_ZIP_COMBINATIONS.add("OK73");
        VALID_STATE_ZIP_COMBINATIONS.add("OK74");
        VALID_STATE_ZIP_COMBINATIONS.add("OR97");
        VALID_STATE_ZIP_COMBINATIONS.add("PA15");
        VALID_STATE_ZIP_COMBINATIONS.add("PA16");
        VALID_STATE_ZIP_COMBINATIONS.add("PA17");
        VALID_STATE_ZIP_COMBINATIONS.add("PA18");
        VALID_STATE_ZIP_COMBINATIONS.add("PA19");
        VALID_STATE_ZIP_COMBINATIONS.add("PR60");
        VALID_STATE_ZIP_COMBINATIONS.add("PR61");
        VALID_STATE_ZIP_COMBINATIONS.add("PR62");
        VALID_STATE_ZIP_COMBINATIONS.add("PR63");
        VALID_STATE_ZIP_COMBINATIONS.add("PR64");
        VALID_STATE_ZIP_COMBINATIONS.add("PR65");
        VALID_STATE_ZIP_COMBINATIONS.add("PR66");
        VALID_STATE_ZIP_COMBINATIONS.add("PR67");
        VALID_STATE_ZIP_COMBINATIONS.add("PR68");
        VALID_STATE_ZIP_COMBINATIONS.add("PR69");
        VALID_STATE_ZIP_COMBINATIONS.add("PR70");
        VALID_STATE_ZIP_COMBINATIONS.add("PR71");
        VALID_STATE_ZIP_COMBINATIONS.add("PR72");
        VALID_STATE_ZIP_COMBINATIONS.add("PR73");
        VALID_STATE_ZIP_COMBINATIONS.add("PR74");
        VALID_STATE_ZIP_COMBINATIONS.add("PR75");
        VALID_STATE_ZIP_COMBINATIONS.add("PR76");
        VALID_STATE_ZIP_COMBINATIONS.add("PR77");
        VALID_STATE_ZIP_COMBINATIONS.add("PR78");
        VALID_STATE_ZIP_COMBINATIONS.add("PR79");
        VALID_STATE_ZIP_COMBINATIONS.add("PR90");
        VALID_STATE_ZIP_COMBINATIONS.add("PR91");
        VALID_STATE_ZIP_COMBINATIONS.add("PR92");
        VALID_STATE_ZIP_COMBINATIONS.add("PR93");
        VALID_STATE_ZIP_COMBINATIONS.add("PR94");
        VALID_STATE_ZIP_COMBINATIONS.add("PR95");
        VALID_STATE_ZIP_COMBINATIONS.add("PR96");
        VALID_STATE_ZIP_COMBINATIONS.add("PR97");
        VALID_STATE_ZIP_COMBINATIONS.add("PR98");
        VALID_STATE_ZIP_COMBINATIONS.add("PW96");
        VALID_STATE_ZIP_COMBINATIONS.add("RI28");
        VALID_STATE_ZIP_COMBINATIONS.add("RI29");
        VALID_STATE_ZIP_COMBINATIONS.add("SC29");
        VALID_STATE_ZIP_COMBINATIONS.add("SD57");
        VALID_STATE_ZIP_COMBINATIONS.add("TN37");
        VALID_STATE_ZIP_COMBINATIONS.add("TN38");
        VALID_STATE_ZIP_COMBINATIONS.add("TX73");
        VALID_STATE_ZIP_COMBINATIONS.add("TX75");
        VALID_STATE_ZIP_COMBINATIONS.add("TX76");
        VALID_STATE_ZIP_COMBINATIONS.add("TX77");
        VALID_STATE_ZIP_COMBINATIONS.add("TX78");
        VALID_STATE_ZIP_COMBINATIONS.add("TX79");
        VALID_STATE_ZIP_COMBINATIONS.add("TX88");
        VALID_STATE_ZIP_COMBINATIONS.add("UT84");
        VALID_STATE_ZIP_COMBINATIONS.add("VA20");
        VALID_STATE_ZIP_COMBINATIONS.add("VA22");
        VALID_STATE_ZIP_COMBINATIONS.add("VA23");
        VALID_STATE_ZIP_COMBINATIONS.add("VA24");
        VALID_STATE_ZIP_COMBINATIONS.add("VI80");
        VALID_STATE_ZIP_COMBINATIONS.add("VI82");
        VALID_STATE_ZIP_COMBINATIONS.add("VI83");
        VALID_STATE_ZIP_COMBINATIONS.add("VI84");
        VALID_STATE_ZIP_COMBINATIONS.add("VI85");
        VALID_STATE_ZIP_COMBINATIONS.add("VT50");
        VALID_STATE_ZIP_COMBINATIONS.add("VT51");
        VALID_STATE_ZIP_COMBINATIONS.add("VT52");
        VALID_STATE_ZIP_COMBINATIONS.add("VT53");
        VALID_STATE_ZIP_COMBINATIONS.add("VT54");
        VALID_STATE_ZIP_COMBINATIONS.add("VT56");
        VALID_STATE_ZIP_COMBINATIONS.add("VT57");
        VALID_STATE_ZIP_COMBINATIONS.add("VT58");
        VALID_STATE_ZIP_COMBINATIONS.add("VT59");
        VALID_STATE_ZIP_COMBINATIONS.add("WA98");
        VALID_STATE_ZIP_COMBINATIONS.add("WA99");
        VALID_STATE_ZIP_COMBINATIONS.add("WI53");
        VALID_STATE_ZIP_COMBINATIONS.add("WI54");
        VALID_STATE_ZIP_COMBINATIONS.add("WV24");
        VALID_STATE_ZIP_COMBINATIONS.add("WV25");
        VALID_STATE_ZIP_COMBINATIONS.add("WV26");
        VALID_STATE_ZIP_COMBINATIONS.add("WY82");
        VALID_STATE_ZIP_COMBINATIONS.add("WY83");
    }
    
    // ===========================
    // VALIDATION PATTERNS
    // ===========================
    
    /**
     * Pattern for validating numeric fields (digits only).
     * Equivalent to COBOL PIC 9(n) fields.
     */
    public static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    
    /**
     * Pattern for validating alphabetic fields (letters only).
     * Equivalent to COBOL PIC A(n) fields.
     */
    public static final Pattern ALPHA_PATTERN = Pattern.compile("^[A-Za-z]+$");
    
    /**
     * Pattern for validating alphanumeric fields (letters and digits).
     * Equivalent to COBOL PIC X(n) fields with mixed content.
     */
    public static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");
    
    /**
     * Pattern for validating Social Security Numbers.
     * Format: 9 digits (matches CUSTREC.cpy CUST-SSN PIC 9(09)).
     */
    public static final Pattern SSN_PATTERN = Pattern.compile("^[0-9]{9}$");
    
    /**
     * Pattern for validating phone numbers.
     * Format: 10 digits (area code + 7 digits), matches CUSTREC.cpy CUST-PHONE-NUM PIC X(15).
     * Allows common phone number formats.
     */
    public static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$|^[0-9]{3}-[0-9]{3}-[0-9]{4}$|^\\([0-9]{3}\\) [0-9]{3}-[0-9]{4}$");
    
    /**
     * Pattern for validating account IDs.
     * Format: 11 digits based on CardDemo account ID structure.
     */
    public static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[0-9]{11}$");
    
    /**
     * Pattern for validating card numbers.
     * Format: 16 digits for standard credit/debit cards.
     */
    public static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{16}$");
    
    /**
     * Pattern for validating email addresses.
     * Basic email validation pattern for CardDemo application.
     */
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    /**
     * Pattern for validating ZIP codes.
     * Supports both 5-digit and 9-digit (ZIP+4) formats.
     * Matches CUSTREC.cpy CUST-ADDR-ZIP PIC X(10).
     */
    public static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^[0-9]{5}(-[0-9]{4})?$");
    
    // ===========================
    // DATE VALIDATION CONSTANTS
    // ===========================
    
    /**
     * Date format patterns supported by the CardDemo application.
     * Based on CSUTLDPY.cpy date validation procedures.
     */
    public static final String[] DATE_PATTERNS = {
        "yyyyMMdd",     // CCYYMMDD format from CSUTLDPY.cpy
        "yyyy-MM-dd",   // ISO format
        "MM/dd/yyyy",   // US format
        "dd/MM/yyyy"    // European format
    };
    
    // ===========================
    // BUSINESS RULE CONSTANTS
    // ===========================
    
    /**
     * Minimum account ID value based on CardDemo business rules.
     * Account IDs start from 1 and are 11 digits long.
     */
    public static final long MIN_ACCOUNT_ID = 1L;
    
    /**
     * Maximum account ID value based on CardDemo business rules.
     * 11-digit maximum value: 99999999999L
     */
    public static final long MAX_ACCOUNT_ID = 99999999999L;
    
    /**
     * Minimum credit limit for CardDemo credit cards.
     * Based on typical credit card business rules.
     */
    public static final long MIN_CREDIT_LIMIT = 100L;
    
    /**
     * Maximum credit limit for CardDemo credit cards.
     * Set to reasonable maximum for demonstration purposes.
     */
    public static final long MAX_CREDIT_LIMIT = 50000L;
    
    /**
     * Minimum FICO credit score.
     * Based on CUSTREC.cpy CUST-FICO-CREDIT-SCORE PIC 9(03).
     */
    public static final int MIN_FICO_SCORE = 300;
    
    /**
     * Maximum FICO credit score.
     * Based on CUSTREC.cpy CUST-FICO-CREDIT-SCORE PIC 9(03).
     */
    public static final int MAX_FICO_SCORE = 850;
    
    // ===========================
    // CURRENCY AND PRECISION CONSTANTS
    // ===========================
    
    /**
     * Currency scale for financial calculations.
     * Equivalent to COBOL COMP-3 decimal places (typically 2 for currency).
     */
    public static final int CURRENCY_SCALE = 2;
    
    /**
     * Currency precision for financial calculations.
     * Total number of digits including decimal places.
     * Equivalent to COBOL PIC S9(10)V99 COMP-3 (12 total digits).
     */
    public static final int CURRENCY_PRECISION = 12;
    
    /**
     * Rounding mode for financial calculations.
     * Uses HALF_UP to match COBOL arithmetic behavior.
     */
    public static final RoundingMode CURRENCY_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    // ===========================
    // FIELD LENGTH CONSTANTS
    // ===========================
    
    /**
     * Maximum length for customer first name.
     * Based on CUSTREC.cpy CUST-FIRST-NAME PIC X(25).
     */
    public static final int MAX_FIRST_NAME_LENGTH = 25;
    
    /**
     * Maximum length for customer middle name.
     * Based on CUSTREC.cpy CUST-MIDDLE-NAME PIC X(25).
     */
    public static final int MAX_MIDDLE_NAME_LENGTH = 25;
    
    /**
     * Maximum length for customer last name.
     * Based on CUSTREC.cpy CUST-LAST-NAME PIC X(25).
     */
    public static final int MAX_LAST_NAME_LENGTH = 25;
    
    /**
     * Maximum length for address lines.
     * Based on CUSTREC.cpy CUST-ADDR-LINE-1/2/3 PIC X(50).
     */
    public static final int MAX_ADDRESS_LINE_LENGTH = 50;
    
    /**
     * Maximum length for phone numbers.
     * Based on CUSTREC.cpy CUST-PHONE-NUM-1/2 PIC X(15).
     */
    public static final int MAX_PHONE_LENGTH = 15;
    
    /**
     * Maximum length for government issued ID.
     * Based on CUSTREC.cpy CUST-GOVT-ISSUED-ID PIC X(20).
     */
    public static final int MAX_GOVT_ID_LENGTH = 20;
    
    /**
     * Maximum length for EFT account ID.
     * Based on CUSTREC.cpy CUST-EFT-ACCOUNT-ID PIC X(10).
     */
    public static final int MAX_EFT_ACCOUNT_ID_LENGTH = 10;
    
    /**
     * Maximum length for ZIP code.
     * Based on CUSTREC.cpy CUST-ADDR-ZIP PIC X(10).
     */
    public static final int MAX_ZIP_CODE_LENGTH = 10;
    
    /**
     * Length for state code.
     * Based on CUSTREC.cpy CUST-ADDR-STATE-CD PIC X(02).
     */
    public static final int STATE_CODE_LENGTH = 2;
    
    /**
     * Length for country code.
     * Based on CUSTREC.cpy CUST-ADDR-COUNTRY-CD PIC X(03).
     */
    public static final int COUNTRY_CODE_LENGTH = 3;
    
    /**
     * Length for customer ID.
     * Based on CUSTREC.cpy CUST-ID PIC 9(09).
     */
    public static final int CUSTOMER_ID_LENGTH = 9;
    
    /**
     * Length for date of birth field.
     * Based on CUSTREC.cpy CUST-DOB-YYYYMMDD PIC X(10).
     */
    public static final int DATE_OF_BIRTH_LENGTH = 10;
    
    /**
     * Length for primary card holder indicator.
     * Based on CUSTREC.cpy CUST-PRI-CARD-HOLDER-IND PIC X(01).
     */
    public static final int CARD_HOLDER_IND_LENGTH = 1;
    
    /**
     * Length for FICO credit score.
     * Based on CUSTREC.cpy CUST-FICO-CREDIT-SCORE PIC 9(03).
     */
    public static final int FICO_SCORE_LENGTH = 3;
    
    // ===========================
    // DATE VALIDATION CONSTANTS
    // ===========================
    
    /**
     * Valid centuries for date validation.
     * Based on CSUTLDPY.cpy century validation (19xx and 20xx only).
     */
    public static final Set<Integer> VALID_CENTURIES = Set.of(19, 20);
    
    /**
     * Valid months for date validation.
     * Based on CSUTLDPY.cpy month validation (1-12).
     */
    public static final Set<Integer> VALID_MONTHS = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    
    /**
     * Valid days for date validation.
     * Based on CSUTLDPY.cpy day validation (1-31).
     */
    public static final Set<Integer> VALID_DAYS = Set.of(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
    );
    
    /**
     * Months with 31 days.
     * Used for date validation based on CSUTLDPY.cpy logic.
     */
    public static final Set<Integer> MONTHS_WITH_31_DAYS = Set.of(1, 3, 5, 7, 8, 10, 12);
    
    /**
     * Months with 30 days.
     * Used for date validation based on CSUTLDPY.cpy logic.
     */
    public static final Set<Integer> MONTHS_WITH_30_DAYS = Set.of(4, 6, 9, 11);
    
    /**
     * February month constant.
     * Used for leap year validation based on CSUTLDPY.cpy logic.
     */
    public static final int FEBRUARY = 2;
    
    // ===========================
    // UTILITY METHODS
    // ===========================
    
    /**
     * Validates if a given area code is valid.
     * 
     * @param areaCode The area code to validate
     * @return true if the area code is valid, false otherwise
     */
    public static boolean isValidAreaCode(String areaCode) {
        return areaCode != null && VALID_AREA_CODES.contains(areaCode);
    }
    
    /**
     * Validates if a given state code is valid.
     * 
     * @param stateCode The state code to validate
     * @return true if the state code is valid, false otherwise
     */
    public static boolean isValidStateCode(String stateCode) {
        return stateCode != null && VALID_STATE_CODES.contains(stateCode.toUpperCase());
    }
    
    /**
     * Validates if a given state-ZIP combination is valid.
     * 
     * @param stateCode The state code (2 characters)
     * @param zipPrefix The first 2 digits of the ZIP code
     * @return true if the combination is valid, false otherwise
     */
    public static boolean isValidStateZipCombination(String stateCode, String zipPrefix) {
        if (stateCode == null || zipPrefix == null) {
            return false;
        }
        String combination = stateCode.toUpperCase() + zipPrefix;
        return VALID_STATE_ZIP_COMBINATIONS.contains(combination);
    }
    
    /**
     * Validates if a year is a leap year.
     * Based on CSUTLDPY.cpy leap year calculation logic.
     * 
     * @param year The year to validate
     * @return true if the year is a leap year, false otherwise
     */
    public static boolean isLeapYear(int year) {
        // COBOL logic: if year ends in 00, divisible by 400; otherwise divisible by 4
        if (year % 100 == 0) {
            return year % 400 == 0;
        } else {
            return year % 4 == 0;
        }
    }
    
    /**
     * Validates if a given day is valid for the specified month and year.
     * Based on CSUTLDPY.cpy day/month/year validation logic.
     * 
     * @param day The day to validate
     * @param month The month (1-12)
     * @param year The year
     * @return true if the day is valid for the month and year, false otherwise
     */
    public static boolean isValidDayForMonth(int day, int month, int year) {
        if (!VALID_MONTHS.contains(month) || !VALID_DAYS.contains(day)) {
            return false;
        }
        
        // Check for months with 31 days
        if (MONTHS_WITH_31_DAYS.contains(month)) {
            return day <= 31;
        }
        
        // Check for months with 30 days
        if (MONTHS_WITH_30_DAYS.contains(month)) {
            return day <= 30;
        }
        
        // February validation
        if (month == FEBRUARY) {
            if (day <= 28) {
                return true;
            }
            if (day == 29) {
                return isLeapYear(year);
            }
            return false;
        }
        
        return true;
    }
}