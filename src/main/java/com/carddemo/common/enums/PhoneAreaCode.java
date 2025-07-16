/**
 * PhoneAreaCode Enum
 * 
 * Converted from COBOL VALID-PHONE-AREA-CODE 88-level condition in CSLKPCDY.cpy
 * Maintains exact COBOL lookup table behavior for customer data integrity validation.
 * 
 * Source: North America Numbering Plan Administrator (NANPA)
 * Original COBOL comment: "North America Phone area codes List obtained from North America 
 * Numbering Plan Administrator *nanpa https://nationalnanpa.com/nanp1/npa_report.csv"
 * 
 * This enum supports:
 * - Jakarta Bean Validation integration for Spring Boot customer data validation
 * - React Hook Form validation for frontend phone number validation
 * - Complete validation methods matching original COBOL behavior
 * 
 * Copyright Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carddemo.common.enums;

import java.util.Optional;

/**
 * Enumeration of valid North American phone area codes.
 * 
 * This enum represents all valid phone area codes as defined in the original
 * COBOL VALID-PHONE-AREA-CODE 88-level condition, maintaining exact validation
 * behavior for customer data integrity.
 * 
 * Usage Examples:
 * - Validation: PhoneAreaCode.isValid("201") returns true
 * - Parsing: PhoneAreaCode.fromCode("201") returns Optional<PhoneAreaCode>
 * - Access: PhoneAreaCode.AREA_201.getCode() returns "201"
 * 
 * Integration:
 * - Jakarta Bean Validation: Use with @Valid annotation
 * - React Hook Form: Validation methods support frontend validation
 * - Spring Boot: Direct integration with REST API validation
 */
public enum PhoneAreaCode {
    
    // Valid North American area codes from COBOL VALID-PHONE-AREA-CODE VALUES clause
    // Original COBOL: lines 30-520 in CSLKPCDY.cpy
    AREA_201("201", "New Jersey"),
    AREA_202("202", "District of Columbia"),
    AREA_203("203", "Connecticut"),
    AREA_204("204", "Manitoba"),
    AREA_205("205", "Alabama"),
    AREA_206("206", "Washington"),
    AREA_207("207", "Maine"),
    AREA_208("208", "Idaho"),
    AREA_209("209", "California"),
    AREA_210("210", "Texas"),
    AREA_212("212", "New York"),
    AREA_213("213", "California"),
    AREA_214("214", "Texas"),
    AREA_215("215", "Pennsylvania"),
    AREA_216("216", "Ohio"),
    AREA_217("217", "Illinois"),
    AREA_218("218", "Minnesota"),
    AREA_219("219", "Indiana"),
    AREA_220("220", "Ohio"),
    AREA_223("223", "Pennsylvania"),
    AREA_224("224", "Illinois"),
    AREA_225("225", "Louisiana"),
    AREA_226("226", "Ontario"),
    AREA_228("228", "Mississippi"),
    AREA_229("229", "Georgia"),
    AREA_231("231", "Michigan"),
    AREA_234("234", "Ohio"),
    AREA_236("236", "British Columbia"),
    AREA_239("239", "Florida"),
    AREA_240("240", "Maryland"),
    AREA_242("242", "Bahamas"),
    AREA_246("246", "Barbados"),
    AREA_248("248", "Michigan"),
    AREA_249("249", "Ontario"),
    AREA_250("250", "British Columbia"),
    AREA_251("251", "Alabama"),
    AREA_252("252", "North Carolina"),
    AREA_253("253", "Washington"),
    AREA_254("254", "Texas"),
    AREA_256("256", "Alabama"),
    AREA_260("260", "Indiana"),
    AREA_262("262", "Wisconsin"),
    AREA_264("264", "Anguilla"),
    AREA_267("267", "Pennsylvania"),
    AREA_268("268", "Antigua and Barbuda"),
    AREA_269("269", "Michigan"),
    AREA_270("270", "Kentucky"),
    AREA_272("272", "Pennsylvania"),
    AREA_276("276", "Virginia"),
    AREA_279("279", "California"),
    AREA_281("281", "Texas"),
    AREA_284("284", "British Virgin Islands"),
    AREA_289("289", "Ontario"),
    AREA_301("301", "Maryland"),
    AREA_302("302", "Delaware"),
    AREA_303("303", "Colorado"),
    AREA_304("304", "West Virginia"),
    AREA_305("305", "Florida"),
    AREA_306("306", "Saskatchewan"),
    AREA_307("307", "Wyoming"),
    AREA_308("308", "Nebraska"),
    AREA_309("309", "Illinois"),
    AREA_310("310", "California"),
    AREA_312("312", "Illinois"),
    AREA_313("313", "Michigan"),
    AREA_314("314", "Missouri"),
    AREA_315("315", "New York"),
    AREA_316("316", "Kansas"),
    AREA_317("317", "Indiana"),
    AREA_318("318", "Louisiana"),
    AREA_319("319", "Iowa"),
    AREA_320("320", "Minnesota"),
    AREA_321("321", "Florida"),
    AREA_323("323", "California"),
    AREA_325("325", "Texas"),
    AREA_326("326", "Ohio"),
    AREA_330("330", "Ohio"),
    AREA_331("331", "Illinois"),
    AREA_332("332", "New York"),
    AREA_334("334", "Alabama"),
    AREA_336("336", "North Carolina"),
    AREA_337("337", "Louisiana"),
    AREA_339("339", "Massachusetts"),
    AREA_340("340", "US Virgin Islands"),
    AREA_341("341", "California"),
    AREA_343("343", "Ontario"),
    AREA_345("345", "Cayman Islands"),
    AREA_346("346", "Texas"),
    AREA_347("347", "New York"),
    AREA_351("351", "Massachusetts"),
    AREA_352("352", "Florida"),
    AREA_360("360", "Washington"),
    AREA_361("361", "Texas"),
    AREA_364("364", "Kentucky"),
    AREA_365("365", "Ontario"),
    AREA_367("367", "Quebec"),
    AREA_368("368", "Alberta"),
    AREA_380("380", "Ohio"),
    AREA_385("385", "Utah"),
    AREA_386("386", "Florida"),
    AREA_401("401", "Rhode Island"),
    AREA_402("402", "Nebraska"),
    AREA_403("403", "Alberta"),
    AREA_404("404", "Georgia"),
    AREA_405("405", "Oklahoma"),
    AREA_406("406", "Montana"),
    AREA_407("407", "Florida"),
    AREA_408("408", "California"),
    AREA_409("409", "Texas"),
    AREA_410("410", "Maryland"),
    AREA_412("412", "Pennsylvania"),
    AREA_413("413", "Massachusetts"),
    AREA_414("414", "Wisconsin"),
    AREA_415("415", "California"),
    AREA_416("416", "Ontario"),
    AREA_417("417", "Missouri"),
    AREA_418("418", "Quebec"),
    AREA_419("419", "Ohio"),
    AREA_423("423", "Tennessee"),
    AREA_424("424", "California"),
    AREA_425("425", "Washington"),
    AREA_430("430", "Texas"),
    AREA_431("431", "Manitoba"),
    AREA_432("432", "Texas"),
    AREA_434("434", "Virginia"),
    AREA_435("435", "Utah"),
    AREA_437("437", "Ontario"),
    AREA_438("438", "Quebec"),
    AREA_440("440", "Ohio"),
    AREA_441("441", "Bermuda"),
    AREA_442("442", "California"),
    AREA_443("443", "Maryland"),
    AREA_445("445", "Pennsylvania"),
    AREA_447("447", "Illinois"),
    AREA_448("448", "Florida"),
    AREA_450("450", "Quebec"),
    AREA_458("458", "Oregon"),
    AREA_463("463", "Indiana"),
    AREA_464("464", "Illinois"),
    AREA_469("469", "Texas"),
    AREA_470("470", "Georgia"),
    AREA_473("473", "Grenada"),
    AREA_474("474", "Saskatchewan"),
    AREA_475("475", "Connecticut"),
    AREA_478("478", "Georgia"),
    AREA_479("479", "Arkansas"),
    AREA_480("480", "Arizona"),
    AREA_484("484", "Pennsylvania"),
    AREA_501("501", "Arkansas"),
    AREA_502("502", "Kentucky"),
    AREA_503("503", "Oregon"),
    AREA_504("504", "Louisiana"),
    AREA_505("505", "New Mexico"),
    AREA_506("506", "New Brunswick"),
    AREA_507("507", "Minnesota"),
    AREA_508("508", "Massachusetts"),
    AREA_509("509", "Washington"),
    AREA_510("510", "California"),
    AREA_512("512", "Texas"),
    AREA_513("513", "Ohio"),
    AREA_514("514", "Quebec"),
    AREA_515("515", "Iowa"),
    AREA_516("516", "New York"),
    AREA_517("517", "Michigan"),
    AREA_518("518", "New York"),
    AREA_519("519", "Ontario"),
    AREA_520("520", "Arizona"),
    AREA_530("530", "California"),
    AREA_531("531", "Nebraska"),
    AREA_534("534", "Wisconsin"),
    AREA_539("539", "Oklahoma"),
    AREA_540("540", "Virginia"),
    AREA_541("541", "Oregon"),
    AREA_548("548", "Ontario"),
    AREA_551("551", "New Jersey"),
    AREA_559("559", "California"),
    AREA_561("561", "Florida"),
    AREA_562("562", "California"),
    AREA_563("563", "Iowa"),
    AREA_564("564", "Washington"),
    AREA_567("567", "Ohio"),
    AREA_570("570", "Pennsylvania"),
    AREA_571("571", "Virginia"),
    AREA_572("572", "Oklahoma"),
    AREA_573("573", "Missouri"),
    AREA_574("574", "Indiana"),
    AREA_575("575", "New Mexico"),
    AREA_579("579", "Quebec"),
    AREA_580("580", "Oklahoma"),
    AREA_581("581", "Quebec"),
    AREA_582("582", "Pennsylvania"),
    AREA_585("585", "New York"),
    AREA_586("586", "Michigan"),
    AREA_587("587", "Alberta"),
    AREA_601("601", "Mississippi"),
    AREA_602("602", "Arizona"),
    AREA_603("603", "New Hampshire"),
    AREA_604("604", "British Columbia"),
    AREA_605("605", "South Dakota"),
    AREA_606("606", "Kentucky"),
    AREA_607("607", "New York"),
    AREA_608("608", "Wisconsin"),
    AREA_609("609", "New Jersey"),
    AREA_610("610", "Pennsylvania"),
    AREA_612("612", "Minnesota"),
    AREA_613("613", "Ontario"),
    AREA_614("614", "Ohio"),
    AREA_615("615", "Tennessee"),
    AREA_616("616", "Michigan"),
    AREA_617("617", "Massachusetts"),
    AREA_618("618", "Illinois"),
    AREA_619("619", "California"),
    AREA_620("620", "Kansas"),
    AREA_623("623", "Arizona"),
    AREA_626("626", "California"),
    AREA_628("628", "California"),
    AREA_629("629", "Tennessee"),
    AREA_630("630", "Illinois"),
    AREA_631("631", "New York"),
    AREA_636("636", "Missouri"),
    AREA_639("639", "Saskatchewan"),
    AREA_640("640", "North Carolina"),
    AREA_641("641", "Iowa"),
    AREA_646("646", "New York"),
    AREA_647("647", "Ontario"),
    AREA_649("649", "Turks and Caicos"),
    AREA_650("650", "California"),
    AREA_651("651", "Minnesota"),
    AREA_656("656", "Florida"),
    AREA_657("657", "California"),
    AREA_658("658", "Jamaica"),
    AREA_659("659", "Alabama"),
    AREA_660("660", "Missouri"),
    AREA_661("661", "California"),
    AREA_662("662", "Mississippi"),
    AREA_664("664", "Montserrat"),
    AREA_667("667", "Maryland"),
    AREA_669("669", "California"),
    AREA_670("670", "Northern Mariana Islands"),
    AREA_671("671", "Guam"),
    AREA_672("672", "British Columbia"),
    AREA_678("678", "Georgia"),
    AREA_680("680", "New York"),
    AREA_681("681", "West Virginia"),
    AREA_682("682", "Texas"),
    AREA_683("683", "Texas"),
    AREA_684("684", "American Samoa"),
    AREA_689("689", "Florida"),
    AREA_701("701", "North Dakota"),
    AREA_702("702", "Nevada"),
    AREA_703("703", "Virginia"),
    AREA_704("704", "North Carolina"),
    AREA_705("705", "Ontario"),
    AREA_706("706", "Georgia"),
    AREA_707("707", "California"),
    AREA_708("708", "Illinois"),
    AREA_709("709", "Newfoundland and Labrador"),
    AREA_712("712", "Iowa"),
    AREA_713("713", "Texas"),
    AREA_714("714", "California"),
    AREA_715("715", "Wisconsin"),
    AREA_716("716", "New York"),
    AREA_717("717", "Pennsylvania"),
    AREA_718("718", "New York"),
    AREA_719("719", "Colorado"),
    AREA_720("720", "Colorado"),
    AREA_721("721", "Sint Maarten"),
    AREA_724("724", "Pennsylvania"),
    AREA_725("725", "Nevada"),
    AREA_726("726", "Texas"),
    AREA_727("727", "Florida"),
    AREA_731("731", "Tennessee"),
    AREA_732("732", "New Jersey"),
    AREA_734("734", "Michigan"),
    AREA_737("737", "Texas"),
    AREA_740("740", "Ohio"),
    AREA_742("742", "Ontario"),
    AREA_743("743", "North Carolina"),
    AREA_747("747", "California"),
    AREA_753("753", "Ontario"),
    AREA_754("754", "Florida"),
    AREA_757("757", "Virginia"),
    AREA_758("758", "Saint Lucia"),
    AREA_760("760", "California"),
    AREA_762("762", "Georgia"),
    AREA_763("763", "Minnesota"),
    AREA_765("765", "Indiana"),
    AREA_767("767", "Dominica"),
    AREA_769("769", "Mississippi"),
    AREA_770("770", "Georgia"),
    AREA_771("771", "District of Columbia"),
    AREA_772("772", "Florida"),
    AREA_773("773", "Illinois"),
    AREA_774("774", "Massachusetts"),
    AREA_775("775", "Nevada"),
    AREA_778("778", "British Columbia"),
    AREA_779("779", "Illinois"),
    AREA_780("780", "Alberta"),
    AREA_781("781", "Massachusetts"),
    AREA_782("782", "Nova Scotia"),
    AREA_784("784", "Saint Vincent and the Grenadines"),
    AREA_785("785", "Kansas"),
    AREA_786("786", "Florida"),
    AREA_787("787", "Puerto Rico"),
    AREA_801("801", "Utah"),
    AREA_802("802", "Vermont"),
    AREA_803("803", "South Carolina"),
    AREA_804("804", "Virginia"),
    AREA_805("805", "California"),
    AREA_806("806", "Texas"),
    AREA_807("807", "Ontario"),
    AREA_808("808", "Hawaii"),
    AREA_809("809", "Dominican Republic"),
    AREA_810("810", "Michigan"),
    AREA_812("812", "Indiana"),
    AREA_813("813", "Florida"),
    AREA_814("814", "Pennsylvania"),
    AREA_815("815", "Illinois"),
    AREA_816("816", "Missouri"),
    AREA_817("817", "Texas"),
    AREA_818("818", "California"),
    AREA_819("819", "Quebec"),
    AREA_820("820", "California"),
    AREA_825("825", "Alberta"),
    AREA_826("826", "Virginia"),
    AREA_828("828", "North Carolina"),
    AREA_829("829", "Dominican Republic"),
    AREA_830("830", "Texas"),
    AREA_831("831", "California"),
    AREA_832("832", "Texas"),
    AREA_838("838", "New York"),
    AREA_839("839", "South Carolina"),
    AREA_840("840", "California"),
    AREA_843("843", "South Carolina"),
    AREA_845("845", "New York"),
    AREA_847("847", "Illinois"),
    AREA_848("848", "New Jersey"),
    AREA_849("849", "Dominican Republic"),
    AREA_850("850", "Florida"),
    AREA_854("854", "South Carolina"),
    AREA_856("856", "New Jersey"),
    AREA_857("857", "Massachusetts"),
    AREA_858("858", "California"),
    AREA_859("859", "Kentucky"),
    AREA_860("860", "Connecticut"),
    AREA_862("862", "New Jersey"),
    AREA_863("863", "Florida"),
    AREA_864("864", "South Carolina"),
    AREA_865("865", "Tennessee"),
    AREA_867("867", "Northwest Territories"),
    AREA_868("868", "Trinidad and Tobago"),
    AREA_869("869", "Saint Kitts and Nevis"),
    AREA_870("870", "Arkansas"),
    AREA_872("872", "Illinois"),
    AREA_873("873", "Quebec"),
    AREA_876("876", "Jamaica"),
    AREA_878("878", "Pennsylvania"),
    AREA_901("901", "Tennessee"),
    AREA_902("902", "Nova Scotia"),
    AREA_903("903", "Texas"),
    AREA_904("904", "Florida"),
    AREA_905("905", "Ontario"),
    AREA_906("906", "Michigan"),
    AREA_907("907", "Alaska"),
    AREA_908("908", "New Jersey"),
    AREA_909("909", "California"),
    AREA_910("910", "North Carolina"),
    AREA_912("912", "Georgia"),
    AREA_913("913", "Kansas"),
    AREA_914("914", "New York"),
    AREA_915("915", "Texas"),
    AREA_916("916", "California"),
    AREA_917("917", "New York"),
    AREA_918("918", "Oklahoma"),
    AREA_919("919", "North Carolina"),
    AREA_920("920", "Wisconsin"),
    AREA_925("925", "California"),
    AREA_928("928", "Arizona"),
    AREA_929("929", "New York"),
    AREA_930("930", "Indiana"),
    AREA_931("931", "Tennessee"),
    AREA_934("934", "New York"),
    AREA_936("936", "Texas"),
    AREA_937("937", "Ohio"),
    AREA_938("938", "Alabama"),
    AREA_939("939", "Puerto Rico"),
    AREA_940("940", "Texas"),
    AREA_941("941", "Florida"),
    AREA_943("943", "Texas"),
    AREA_945("945", "Texas"),
    AREA_947("947", "Michigan"),
    AREA_948("948", "Virginia"),
    AREA_949("949", "California"),
    AREA_951("951", "California"),
    AREA_952("952", "Minnesota"),
    AREA_954("954", "Florida"),
    AREA_956("956", "Texas"),
    AREA_959("959", "Connecticut"),
    AREA_970("970", "Colorado"),
    AREA_971("971", "Oregon"),
    AREA_972("972", "Texas"),
    AREA_973("973", "New Jersey"),
    AREA_978("978", "Massachusetts"),
    AREA_979("979", "Texas"),
    AREA_980("980", "North Carolina"),
    AREA_983("983", "Colorado"),
    AREA_984("984", "North Carolina"),
    AREA_985("985", "Louisiana"),
    AREA_986("986", "Idaho"),
    AREA_989("989", "Michigan"),
    
    // Easily recognizable codes as specified in original COBOL comment
    // "Easily recognizable codes begin here" - line 440 in CSLKPCDY.cpy
    AREA_200("200", "Easily Recognizable Code"),
    AREA_211("211", "Easily Recognizable Code"),
    AREA_222("222", "Easily Recognizable Code"),
    AREA_233("233", "Easily Recognizable Code"),
    AREA_244("244", "Easily Recognizable Code"),
    AREA_255("255", "Easily Recognizable Code"),
    AREA_266("266", "Easily Recognizable Code"),
    AREA_277("277", "Easily Recognizable Code"),
    AREA_288("288", "Easily Recognizable Code"),
    AREA_299("299", "Easily Recognizable Code"),
    AREA_300("300", "Easily Recognizable Code"),
    AREA_311("311", "Easily Recognizable Code"),
    AREA_322("322", "Easily Recognizable Code"),
    AREA_333("333", "Easily Recognizable Code"),
    AREA_344("344", "Easily Recognizable Code"),
    AREA_355("355", "Easily Recognizable Code"),
    AREA_366("366", "Easily Recognizable Code"),
    AREA_377("377", "Easily Recognizable Code"),
    AREA_388("388", "Easily Recognizable Code"),
    AREA_399("399", "Easily Recognizable Code"),
    AREA_400("400", "Easily Recognizable Code"),
    AREA_411("411", "Easily Recognizable Code"),
    AREA_422("422", "Easily Recognizable Code"),
    AREA_433("433", "Easily Recognizable Code"),
    AREA_444("444", "Easily Recognizable Code"),
    AREA_455("455", "Easily Recognizable Code"),
    AREA_466("466", "Easily Recognizable Code"),
    AREA_477("477", "Easily Recognizable Code"),
    AREA_488("488", "Easily Recognizable Code"),
    AREA_499("499", "Easily Recognizable Code"),
    AREA_500("500", "Easily Recognizable Code"),
    AREA_511("511", "Easily Recognizable Code"),
    AREA_522("522", "Easily Recognizable Code"),
    AREA_533("533", "Easily Recognizable Code"),
    AREA_544("544", "Easily Recognizable Code"),
    AREA_555("555", "Easily Recognizable Code"),
    AREA_566("566", "Easily Recognizable Code"),
    AREA_577("577", "Easily Recognizable Code"),
    AREA_588("588", "Easily Recognizable Code"),
    AREA_599("599", "Easily Recognizable Code"),
    AREA_600("600", "Easily Recognizable Code"),
    AREA_611("611", "Easily Recognizable Code"),
    AREA_622("622", "Easily Recognizable Code"),
    AREA_633("633", "Easily Recognizable Code"),
    AREA_644("644", "Easily Recognizable Code"),
    AREA_655("655", "Easily Recognizable Code"),
    AREA_666("666", "Easily Recognizable Code"),
    AREA_677("677", "Easily Recognizable Code"),
    AREA_688("688", "Easily Recognizable Code"),
    AREA_699("699", "Easily Recognizable Code"),
    AREA_700("700", "Easily Recognizable Code"),
    AREA_711("711", "Easily Recognizable Code"),
    AREA_722("722", "Easily Recognizable Code"),
    AREA_733("733", "Easily Recognizable Code"),
    AREA_744("744", "Easily Recognizable Code"),
    AREA_755("755", "Easily Recognizable Code"),
    AREA_766("766", "Easily Recognizable Code"),
    AREA_777("777", "Easily Recognizable Code"),
    AREA_788("788", "Easily Recognizable Code"),
    AREA_799("799", "Easily Recognizable Code"),
    AREA_800("800", "Easily Recognizable Code"),
    AREA_811("811", "Easily Recognizable Code"),
    AREA_822("822", "Easily Recognizable Code"),
    AREA_833("833", "Easily Recognizable Code"),
    AREA_844("844", "Easily Recognizable Code"),
    AREA_855("855", "Easily Recognizable Code"),
    AREA_866("866", "Easily Recognizable Code"),
    AREA_877("877", "Easily Recognizable Code"),
    AREA_888("888", "Easily Recognizable Code"),
    AREA_899("899", "Easily Recognizable Code"),
    AREA_900("900", "Easily Recognizable Code"),
    AREA_911("911", "Easily Recognizable Code"),
    AREA_922("922", "Easily Recognizable Code"),
    AREA_933("933", "Easily Recognizable Code"),
    AREA_944("944", "Easily Recognizable Code"),
    AREA_955("955", "Easily Recognizable Code"),
    AREA_966("966", "Easily Recognizable Code"),
    AREA_977("977", "Easily Recognizable Code"),
    AREA_988("988", "Easily Recognizable Code"),
    AREA_999("999", "Easily Recognizable Code");

    private final String code;
    private final String description;

    /**
     * Constructor for PhoneAreaCode enum values.
     * 
     * @param code The 3-digit area code as a string
     * @param description Human-readable description of the area code location
     */
    PhoneAreaCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the 3-digit area code as a string.
     * 
     * @return The area code string (e.g., "201")
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the human-readable description of the area code location.
     * 
     * @return The description string (e.g., "New Jersey")
     */
    public String getDescription() {
        return description;
    }

    /**
     * Validates if the provided area code string is valid.
     * 
     * This method replicates the original COBOL VALID-PHONE-AREA-CODE 88-level
     * condition behavior, returning true if the area code exists in the
     * enumeration values.
     * 
     * @param areaCode The area code string to validate (e.g., "201")
     * @return true if the area code is valid, false otherwise
     */
    public static boolean isValid(String areaCode) {
        if (areaCode == null || areaCode.length() != 3) {
            return false;
        }
        
        return fromCode(areaCode).isPresent();
    }

    /**
     * Parses an area code string and returns the corresponding enum value.
     * 
     * This method provides null-safe parsing using Optional, enabling
     * robust error handling in validation methods for customer phone
     * number validation.
     * 
     * @param areaCode The area code string to parse (e.g., "201")
     * @return Optional containing the PhoneAreaCode enum value if valid,
     *         empty Optional if invalid
     */
    public static Optional<PhoneAreaCode> fromCode(String areaCode) {
        if (areaCode == null || areaCode.length() != 3) {
            return Optional.empty();
        }
        
        for (PhoneAreaCode phoneAreaCode : values()) {
            if (phoneAreaCode.code.equals(areaCode)) {
                return Optional.of(phoneAreaCode);
            }
        }
        
        return Optional.empty();
    }



    /**
     * Validates area code with Optional result handling.
     * 
     * This method supports React Hook Form validation by providing
     * a validation result that can be used with orElse() for default
     * error handling.
     * 
     * @param areaCode The area code string to validate
     * @return Optional containing the valid PhoneAreaCode or empty if invalid
     */
    public static Optional<PhoneAreaCode> validate(String areaCode) {
        return fromCode(areaCode);
    }

    /**
     * Gets a valid area code or returns a default value.
     * 
     * This method supports the external import requirement for Optional.orElse()
     * method usage in validation scenarios.
     * 
     * @param areaCode The area code string to parse
     * @param defaultValue The default PhoneAreaCode to return if parsing fails
     * @return The parsed PhoneAreaCode or the default value
     */
    public static PhoneAreaCode getOrDefault(String areaCode, PhoneAreaCode defaultValue) {
        return fromCode(areaCode).orElse(defaultValue);
    }

    /**
     * String representation of the phone area code.
     * 
     * @return The area code as a string
     */
    @Override
    public String toString() {
        return code;
    }
}