package com.carddemo.common.enums;

import java.util.Optional;

/**
 * North American phone area code enumeration converted from COBOL VALID-PHONE-AREA-CODE
 * 88-level condition for customer data validation.
 * 
 * This enum contains all valid North American area codes from the North America Numbering
 * Plan Administrator (NANPA) as specified in the original COBOL CSLKPCDY.cpy lookup table.
 * 
 * The implementation preserves exact COBOL validation behavior while supporting:
 * - Jakarta Bean Validation for Spring Boot REST API endpoints
 * - React Hook Form validation for customer information forms
 * - Customer data integrity validation in microservices architecture
 * 
 * Reference: https://nationalnanpa.com/nanp1/npa_report.csv
 * Original COBOL: app/cpy/CSLKPCDY.cpy VALID-PHONE-AREA-CODE 88-level condition
 */
public enum PhoneAreaCode {
    
    // Real North American area codes from NANPA (lines 30-439 in COBOL source)
    AREA_201("201", "New Jersey (Northern)"),
    AREA_202("202", "District of Columbia"),
    AREA_203("203", "Connecticut (Southwest)"),
    AREA_204("204", "Manitoba"),
    AREA_205("205", "Alabama (Central)"),
    AREA_206("206", "Washington (Seattle)"),
    AREA_207("207", "Maine"),
    AREA_208("208", "Idaho"),
    AREA_209("209", "California (Central Valley)"),
    AREA_210("210", "Texas (San Antonio)"),
    AREA_212("212", "New York (Manhattan)"),
    AREA_213("213", "California (Los Angeles)"),
    AREA_214("214", "Texas (Dallas)"),
    AREA_215("215", "Pennsylvania (Philadelphia)"),
    AREA_216("216", "Ohio (Cleveland)"),
    AREA_217("217", "Illinois (Central)"),
    AREA_218("218", "Minnesota (Northern)"),
    AREA_219("219", "Indiana (Northwest)"),
    AREA_220("220", "Ohio (Northern)"),
    AREA_223("223", "Pennsylvania (Southeast)"),
    AREA_224("224", "Illinois (Chicago suburbs)"),
    AREA_225("225", "Louisiana (Baton Rouge)"),
    AREA_226("226", "Ontario (Southwest)"),
    AREA_228("228", "Mississippi (South)"),
    AREA_229("229", "Georgia (Southwest)"),
    AREA_231("231", "Michigan (Northwest)"),
    AREA_234("234", "Ohio (Northeast)"),
    AREA_236("236", "British Columbia (Vancouver)"),
    AREA_239("239", "Florida (Southwest)"),
    AREA_240("240", "Maryland (Suburban)"),
    AREA_242("242", "Bahamas"),
    AREA_246("246", "Barbados"),
    AREA_248("248", "Michigan (Oakland County)"),
    AREA_249("249", "Ontario (Northern)"),
    AREA_250("250", "British Columbia (Central)"),
    AREA_251("251", "Alabama (South)"),
    AREA_252("252", "North Carolina (East)"),
    AREA_253("253", "Washington (Tacoma)"),
    AREA_254("254", "Texas (Central)"),
    AREA_256("256", "Alabama (North)"),
    AREA_260("260", "Indiana (Northeast)"),
    AREA_262("262", "Wisconsin (Southeast)"),
    AREA_264("264", "Anguilla"),
    AREA_267("267", "Pennsylvania (Philadelphia)"),
    AREA_268("268", "Antigua and Barbuda"),
    AREA_269("269", "Michigan (Southwest)"),
    AREA_270("270", "Kentucky (West)"),
    AREA_272("272", "Pennsylvania (Northeast)"),
    AREA_276("276", "Virginia (Southwest)"),
    AREA_279("279", "California (Sacramento)"),
    AREA_281("281", "Texas (Houston)"),
    AREA_284("284", "British Virgin Islands"),
    AREA_289("289", "Ontario (Toronto)"),
    AREA_301("301", "Maryland (Suburban)"),
    AREA_302("302", "Delaware"),
    AREA_303("303", "Colorado (Denver)"),
    AREA_304("304", "West Virginia"),
    AREA_305("305", "Florida (Miami)"),
    AREA_306("306", "Saskatchewan"),
    AREA_307("307", "Wyoming"),
    AREA_308("308", "Nebraska (West)"),
    AREA_309("309", "Illinois (West)"),
    AREA_310("310", "California (Los Angeles)"),
    AREA_312("312", "Illinois (Chicago)"),
    AREA_313("313", "Michigan (Detroit)"),
    AREA_314("314", "Missouri (St. Louis)"),
    AREA_315("315", "New York (Central)"),
    AREA_316("316", "Kansas (South)"),
    AREA_317("317", "Indiana (Central)"),
    AREA_318("318", "Louisiana (Northwest)"),
    AREA_319("319", "Iowa (East)"),
    AREA_320("320", "Minnesota (Central)"),
    AREA_321("321", "Florida (Central)"),
    AREA_323("323", "California (Los Angeles)"),
    AREA_325("325", "Texas (West)"),
    AREA_326("326", "Ohio (Southern)"),
    AREA_330("330", "Ohio (Northeast)"),
    AREA_331("331", "Illinois (Chicago suburbs)"),
    AREA_332("332", "New York (Manhattan)"),
    AREA_334("334", "Alabama (Central)"),
    AREA_336("336", "North Carolina (North)"),
    AREA_337("337", "Louisiana (Southwest)"),
    AREA_339("339", "Massachusetts (Boston)"),
    AREA_340("340", "US Virgin Islands"),
    AREA_341("341", "California (Bay Area)"),
    AREA_343("343", "Ontario (Ottawa)"),
    AREA_345("345", "Cayman Islands"),
    AREA_346("346", "Texas (Houston)"),
    AREA_347("347", "New York (Brooklyn)"),
    AREA_351("351", "Massachusetts (Northeast)"),
    AREA_352("352", "Florida (North Central)"),
    AREA_360("360", "Washington (Southwest)"),
    AREA_361("361", "Texas (Coastal)"),
    AREA_364("364", "Kentucky (North)"),
    AREA_365("365", "Ontario (Toronto)"),
    AREA_367("367", "Quebec (Montreal)"),
    AREA_368("368", "Alberta (Edmonton)"),
    AREA_380("380", "Ohio (Columbus)"),
    AREA_385("385", "Utah (Salt Lake City)"),
    AREA_386("386", "Florida (North)"),
    AREA_401("401", "Rhode Island"),
    AREA_402("402", "Nebraska (East)"),
    AREA_403("403", "Alberta (Calgary)"),
    AREA_404("404", "Georgia (Atlanta)"),
    AREA_405("405", "Oklahoma (Central)"),
    AREA_406("406", "Montana"),
    AREA_407("407", "Florida (Central)"),
    AREA_408("408", "California (San Jose)"),
    AREA_409("409", "Texas (Southeast)"),
    AREA_410("410", "Maryland (Eastern)"),
    AREA_412("412", "Pennsylvania (Pittsburgh)"),
    AREA_413("413", "Massachusetts (West)"),
    AREA_414("414", "Wisconsin (Milwaukee)"),
    AREA_415("415", "California (San Francisco)"),
    AREA_416("416", "Ontario (Toronto)"),
    AREA_417("417", "Missouri (Southwest)"),
    AREA_418("418", "Quebec (Quebec City)"),
    AREA_419("419", "Ohio (Northwest)"),
    AREA_423("423", "Tennessee (East)"),
    AREA_424("424", "California (Los Angeles)"),
    AREA_425("425", "Washington (Seattle suburbs)"),
    AREA_430("430", "Texas (Northeast)"),
    AREA_431("431", "Manitoba (Winnipeg)"),
    AREA_432("432", "Texas (West)"),
    AREA_434("434", "Virginia (South)"),
    AREA_435("435", "Utah (Southern)"),
    AREA_437("437", "Ontario (Toronto)"),
    AREA_438("438", "Quebec (Montreal)"),
    AREA_440("440", "Ohio (Northeast)"),
    AREA_441("441", "Bermuda"),
    AREA_442("442", "California (San Diego)"),
    AREA_443("443", "Maryland (Baltimore)"),
    AREA_445("445", "Pennsylvania (Philadelphia)"),
    AREA_447("447", "Illinois (Central)"),
    AREA_448("448", "Florida (Southwest)"),
    AREA_450("450", "Quebec (Montreal suburbs)"),
    AREA_458("458", "Oregon (Eugene)"),
    AREA_463("463", "Indiana (Indianapolis)"),
    AREA_464("464", "Illinois (Chicago suburbs)"),
    AREA_469("469", "Texas (Dallas)"),
    AREA_470("470", "Georgia (Atlanta)"),
    AREA_473("473", "Grenada"),
    AREA_474("474", "Saskatchewan (Regina)"),
    AREA_475("475", "Connecticut (New Haven)"),
    AREA_478("478", "Georgia (Central)"),
    AREA_479("479", "Arkansas (Northwest)"),
    AREA_480("480", "Arizona (East)"),
    AREA_484("484", "Pennsylvania (Eastern)"),
    AREA_501("501", "Arkansas (Central)"),
    AREA_502("502", "Kentucky (North)"),
    AREA_503("503", "Oregon (Portland)"),
    AREA_504("504", "Louisiana (New Orleans)"),
    AREA_505("505", "New Mexico (North)"),
    AREA_506("506", "New Brunswick"),
    AREA_507("507", "Minnesota (South)"),
    AREA_508("508", "Massachusetts (South)"),
    AREA_509("509", "Washington (East)"),
    AREA_510("510", "California (Bay Area)"),
    AREA_512("512", "Texas (Austin)"),
    AREA_513("513", "Ohio (Southwest)"),
    AREA_514("514", "Quebec (Montreal)"),
    AREA_515("515", "Iowa (Des Moines)"),
    AREA_516("516", "New York (Long Island)"),
    AREA_517("517", "Michigan (South)"),
    AREA_518("518", "New York (Northeast)"),
    AREA_519("519", "Ontario (Southwest)"),
    AREA_520("520", "Arizona (South)"),
    AREA_530("530", "California (Northeast)"),
    AREA_531("531", "Nebraska (Omaha)"),
    AREA_534("534", "Wisconsin (North)"),
    AREA_539("539", "Oklahoma (Tulsa)"),
    AREA_540("540", "Virginia (Northwest)"),
    AREA_541("541", "Oregon (Central)"),
    AREA_548("548", "Ontario (Toronto)"),
    AREA_551("551", "New Jersey (Northern)"),
    AREA_559("559", "California (Central Valley)"),
    AREA_561("561", "Florida (Southeast)"),
    AREA_562("562", "California (Los Angeles)"),
    AREA_563("563", "Iowa (East)"),
    AREA_564("564", "Washington (Seattle)"),
    AREA_567("567", "Ohio (Northwest)"),
    AREA_570("570", "Pennsylvania (Northeast)"),
    AREA_571("571", "Virginia (Northern)"),
    AREA_572("572", "Oklahoma (Tulsa)"),
    AREA_573("573", "Missouri (Southeast)"),
    AREA_574("574", "Indiana (North)"),
    AREA_575("575", "New Mexico (South)"),
    AREA_579("579", "Quebec (Montreal)"),
    AREA_580("580", "Oklahoma (South)"),
    AREA_581("581", "Quebec (Quebec City)"),
    AREA_582("582", "Pennsylvania (Pittsburgh)"),
    AREA_585("585", "New York (Rochester)"),
    AREA_586("586", "Michigan (Detroit suburbs)"),
    AREA_587("587", "Alberta (Calgary)"),
    AREA_601("601", "Mississippi (Central)"),
    AREA_602("602", "Arizona (Phoenix)"),
    AREA_603("603", "New Hampshire"),
    AREA_604("604", "British Columbia (Vancouver)"),
    AREA_605("605", "South Dakota"),
    AREA_606("606", "Kentucky (East)"),
    AREA_607("607", "New York (South)"),
    AREA_608("608", "Wisconsin (Southwest)"),
    AREA_609("609", "New Jersey (Central)"),
    AREA_610("610", "Pennsylvania (Southeast)"),
    AREA_612("612", "Minnesota (Minneapolis)"),
    AREA_613("613", "Ontario (Ottawa)"),
    AREA_614("614", "Ohio (Columbus)"),
    AREA_615("615", "Tennessee (Nashville)"),
    AREA_616("616", "Michigan (West)"),
    AREA_617("617", "Massachusetts (Boston)"),
    AREA_618("618", "Illinois (South)"),
    AREA_619("619", "California (San Diego)"),
    AREA_620("620", "Kansas (South)"),
    AREA_623("623", "Arizona (Phoenix)"),
    AREA_626("626", "California (Los Angeles)"),
    AREA_628("628", "California (San Francisco)"),
    AREA_629("629", "Tennessee (Nashville)"),
    AREA_630("630", "Illinois (Chicago suburbs)"),
    AREA_631("631", "New York (Long Island)"),
    AREA_636("636", "Missouri (St. Louis)"),
    AREA_639("639", "Saskatchewan (Saskatoon)"),
    AREA_640("640", "New Jersey (Northern)"),
    AREA_641("641", "Iowa (Central)"),
    AREA_646("646", "New York (Manhattan)"),
    AREA_647("647", "Ontario (Toronto)"),
    AREA_649("649", "Turks and Caicos"),
    AREA_650("650", "California (San Francisco)"),
    AREA_651("651", "Minnesota (St. Paul)"),
    AREA_656("656", "Florida (St. Petersburg)"),
    AREA_657("657", "California (Orange County)"),
    AREA_658("658", "Jamaica"),
    AREA_659("659", "Alabama (Birmingham)"),
    AREA_660("660", "Missouri (North)"),
    AREA_661("661", "California (Central Valley)"),
    AREA_662("662", "Mississippi (North)"),
    AREA_664("664", "Montserrat"),
    AREA_667("667", "Maryland (Baltimore)"),
    AREA_669("669", "California (San Jose)"),
    AREA_670("670", "Northern Mariana Islands"),
    AREA_671("671", "Guam"),
    AREA_672("672", "British Columbia (Vancouver)"),
    AREA_678("678", "Georgia (Atlanta)"),
    AREA_680("680", "New York (Syracuse)"),
    AREA_681("681", "West Virginia (Charleston)"),
    AREA_682("682", "Texas (Fort Worth)"),
    AREA_683("683", "Texas (Plano)"),
    AREA_684("684", "American Samoa"),
    AREA_689("689", "Florida (Orlando)"),
    AREA_701("701", "North Dakota"),
    AREA_702("702", "Nevada (Las Vegas)"),
    AREA_703("703", "Virginia (Northern)"),
    AREA_704("704", "North Carolina (Charlotte)"),
    AREA_705("705", "Ontario (Northern)"),
    AREA_706("706", "Georgia (Augusta)"),
    AREA_707("707", "California (North Bay)"),
    AREA_708("708", "Illinois (Chicago suburbs)"),
    AREA_709("709", "Newfoundland and Labrador"),
    AREA_712("712", "Iowa (West)"),
    AREA_713("713", "Texas (Houston)"),
    AREA_714("714", "California (Orange County)"),
    AREA_715("715", "Wisconsin (North)"),
    AREA_716("716", "New York (Buffalo)"),
    AREA_717("717", "Pennsylvania (South)"),
    AREA_718("718", "New York (Brooklyn)"),
    AREA_719("719", "Colorado (Colorado Springs)"),
    AREA_720("720", "Colorado (Denver)"),
    AREA_721("721", "Sint Maarten"),
    AREA_724("724", "Pennsylvania (Southwest)"),
    AREA_725("725", "Nevada (Las Vegas)"),
    AREA_726("726", "Texas (San Antonio)"),
    AREA_727("727", "Florida (St. Petersburg)"),
    AREA_731("731", "Tennessee (West)"),
    AREA_732("732", "New Jersey (Central)"),
    AREA_734("734", "Michigan (Ann Arbor)"),
    AREA_737("737", "Texas (Austin)"),
    AREA_740("740", "Ohio (Southeast)"),
    AREA_742("742", "Ontario (Toronto)"),
    AREA_743("743", "North Carolina (Greensboro)"),
    AREA_747("747", "California (Los Angeles)"),
    AREA_753("753", "Ontario (Kingston)"),
    AREA_754("754", "Florida (Fort Lauderdale)"),
    AREA_757("757", "Virginia (Southeast)"),
    AREA_758("758", "Saint Lucia"),
    AREA_760("760", "California (San Diego)"),
    AREA_762("762", "Georgia (Columbus)"),
    AREA_763("763", "Minnesota (Minneapolis)"),
    AREA_765("765", "Indiana (West)"),
    AREA_767("767", "Dominica"),
    AREA_769("769", "Mississippi (Jackson)"),
    AREA_770("770", "Georgia (Atlanta)"),
    AREA_771("771", "Washington (Spokane)"),
    AREA_772("772", "Florida (Southeast)"),
    AREA_773("773", "Illinois (Chicago)"),
    AREA_774("774", "Massachusetts (Worcester)"),
    AREA_775("775", "Nevada (Reno)"),
    AREA_778("778", "British Columbia (Vancouver)"),
    AREA_779("779", "Illinois (Rockford)"),
    AREA_780("780", "Alberta (Edmonton)"),
    AREA_781("781", "Massachusetts (Boston)"),
    AREA_782("782", "Nova Scotia (Halifax)"),
    AREA_784("784", "Saint Vincent and the Grenadines"),
    AREA_785("785", "Kansas (North)"),
    AREA_786("786", "Florida (Miami)"),
    AREA_787("787", "Puerto Rico"),
    AREA_801("801", "Utah (Salt Lake City)"),
    AREA_802("802", "Vermont"),
    AREA_803("803", "South Carolina (Columbia)"),
    AREA_804("804", "Virginia (Richmond)"),
    AREA_805("805", "California (Central Coast)"),
    AREA_806("806", "Texas (Lubbock)"),
    AREA_807("807", "Ontario (Thunder Bay)"),
    AREA_808("808", "Hawaii"),
    AREA_809("809", "Dominican Republic"),
    AREA_810("810", "Michigan (Flint)"),
    AREA_812("812", "Indiana (South)"),
    AREA_813("813", "Florida (Tampa)"),
    AREA_814("814", "Pennsylvania (Northwest)"),
    AREA_815("815", "Illinois (Rockford)"),
    AREA_816("816", "Missouri (Kansas City)"),
    AREA_817("817", "Texas (Fort Worth)"),
    AREA_818("818", "California (Los Angeles)"),
    AREA_819("819", "Quebec (Sherbrooke)"),
    AREA_820("820", "California (San Francisco)"),
    AREA_825("825", "Alberta (Calgary)"),
    AREA_826("826", "Virginia (Petersburg)"),
    AREA_828("828", "North Carolina (West)"),
    AREA_829("829", "Dominican Republic"),
    AREA_830("830", "Texas (South)"),
    AREA_831("831", "California (Central Coast)"),
    AREA_832("832", "Texas (Houston)"),
    AREA_838("838", "New York (Albany)"),
    AREA_839("839", "South Carolina (Beaufort)"),
    AREA_840("840", "California (Bay Area)"),
    AREA_843("843", "South Carolina (Charleston)"),
    AREA_845("845", "New York (Hudson Valley)"),
    AREA_847("847", "Illinois (Chicago suburbs)"),
    AREA_848("848", "New Jersey (Central)"),
    AREA_849("849", "Dominican Republic"),
    AREA_850("850", "Florida (Panhandle)"),
    AREA_854("854", "South Carolina (Charleston)"),
    AREA_856("856", "New Jersey (South)"),
    AREA_857("857", "Massachusetts (Boston)"),
    AREA_858("858", "California (San Diego)"),
    AREA_859("859", "Kentucky (Lexington)"),
    AREA_860("860", "Connecticut (Hartford)"),
    AREA_862("862", "New Jersey (Newark)"),
    AREA_863("863", "Florida (Central)"),
    AREA_864("864", "South Carolina (Greenville)"),
    AREA_865("865", "Tennessee (Knoxville)"),
    AREA_867("867", "Yukon, Northwest Territories, Nunavut"),
    AREA_868("868", "Trinidad and Tobago"),
    AREA_869("869", "Saint Kitts and Nevis"),
    AREA_870("870", "Arkansas (East)"),
    AREA_872("872", "Illinois (Chicago)"),
    AREA_873("873", "Quebec (Sherbrooke)"),
    AREA_876("876", "Jamaica"),
    AREA_878("878", "Pennsylvania (Pittsburgh)"),
    AREA_901("901", "Tennessee (Memphis)"),
    AREA_902("902", "Nova Scotia, Prince Edward Island"),
    AREA_903("903", "Texas (Northeast)"),
    AREA_904("904", "Florida (Jacksonville)"),
    AREA_905("905", "Ontario (Toronto)"),
    AREA_906("906", "Michigan (Upper Peninsula)"),
    AREA_907("907", "Alaska"),
    AREA_908("908", "New Jersey (Central)"),
    AREA_909("909", "California (Inland Empire)"),
    AREA_910("910", "North Carolina (Southeast)"),
    AREA_912("912", "Georgia (Savannah)"),
    AREA_913("913", "Kansas (Kansas City)"),
    AREA_914("914", "New York (Westchester)"),
    AREA_915("915", "Texas (El Paso)"),
    AREA_916("916", "California (Sacramento)"),
    AREA_917("917", "New York (New York City)"),
    AREA_918("918", "Oklahoma (Tulsa)"),
    AREA_919("919", "North Carolina (Raleigh)"),
    AREA_920("920", "Wisconsin (Green Bay)"),
    AREA_925("925", "California (Bay Area)"),
    AREA_928("928", "Arizona (Northern)"),
    AREA_929("929", "New York (Queens)"),
    AREA_930("930", "Indiana (Evansville)"),
    AREA_931("931", "Tennessee (Middle)"),
    AREA_934("934", "New York (Long Island)"),
    AREA_936("936", "Texas (Huntsville)"),
    AREA_937("937", "Ohio (Dayton)"),
    AREA_938("938", "Alabama (Huntsville)"),
    AREA_939("939", "Puerto Rico"),
    AREA_940("940", "Texas (North)"),
    AREA_941("941", "Florida (Sarasota)"),
    AREA_943("943", "Texas (Rio Grande Valley)"),
    AREA_945("945", "Texas (Dallas)"),
    AREA_947("947", "Michigan (Troy)"),
    AREA_948("948", "Arizona (Phoenix)"),
    AREA_949("949", "California (Orange County)"),
    AREA_951("951", "California (Riverside)"),
    AREA_952("952", "Minnesota (Minneapolis)"),
    AREA_954("954", "Florida (Fort Lauderdale)"),
    AREA_956("956", "Texas (Laredo)"),
    AREA_959("959", "Connecticut (Hartford)"),
    AREA_970("970", "Colorado (Northern)"),
    AREA_971("971", "Oregon (Portland)"),
    AREA_972("972", "Texas (Dallas)"),
    AREA_973("973", "New Jersey (Northern)"),
    AREA_978("978", "Massachusetts (Northeast)"),
    AREA_979("979", "Texas (College Station)"),
    AREA_980("980", "North Carolina (Charlotte)"),
    AREA_983("983", "Colorado (Denver)"),
    AREA_984("984", "North Carolina (Raleigh)"),
    AREA_985("985", "Louisiana (Southeast)"),
    AREA_986("986", "Idaho (Boise)"),
    AREA_989("989", "Michigan (Central)"),
    
    // Test/Easily Recognizable area codes (lines 441-520 in COBOL source)
    // These are used for testing and validation purposes
    AREA_200("200", "Test Area Code"),
    AREA_211("211", "Test Area Code"),
    AREA_222("222", "Test Area Code"),
    AREA_233("233", "Test Area Code"),
    AREA_244("244", "Test Area Code"),
    AREA_255("255", "Test Area Code"),
    AREA_266("266", "Test Area Code"),
    AREA_277("277", "Test Area Code"),
    AREA_288("288", "Test Area Code"),
    AREA_299("299", "Test Area Code"),
    AREA_300("300", "Test Area Code"),
    AREA_311("311", "Test Area Code"),
    AREA_322("322", "Test Area Code"),
    AREA_333("333", "Test Area Code"),
    AREA_344("344", "Test Area Code"),
    AREA_355("355", "Test Area Code"),
    AREA_366("366", "Test Area Code"),
    AREA_377("377", "Test Area Code"),
    AREA_388("388", "Test Area Code"),
    AREA_399("399", "Test Area Code"),
    AREA_400("400", "Test Area Code"),
    AREA_411("411", "Test Area Code"),
    AREA_422("422", "Test Area Code"),
    AREA_433("433", "Test Area Code"),
    AREA_444("444", "Test Area Code"),
    AREA_455("455", "Test Area Code"),
    AREA_466("466", "Test Area Code"),
    AREA_477("477", "Test Area Code"),
    AREA_488("488", "Test Area Code"),
    AREA_499("499", "Test Area Code"),
    AREA_500("500", "Test Area Code"),
    AREA_511("511", "Test Area Code"),
    AREA_522("522", "Test Area Code"),
    AREA_533("533", "Test Area Code"),
    AREA_544("544", "Test Area Code"),
    AREA_555("555", "Test Area Code"),
    AREA_566("566", "Test Area Code"),
    AREA_577("577", "Test Area Code"),
    AREA_588("588", "Test Area Code"),
    AREA_599("599", "Test Area Code"),
    AREA_600("600", "Test Area Code"),
    AREA_611("611", "Test Area Code"),
    AREA_622("622", "Test Area Code"),
    AREA_633("633", "Test Area Code"),
    AREA_644("644", "Test Area Code"),
    AREA_655("655", "Test Area Code"),
    AREA_666("666", "Test Area Code"),
    AREA_677("677", "Test Area Code"),
    AREA_688("688", "Test Area Code"),
    AREA_699("699", "Test Area Code"),
    AREA_700("700", "Test Area Code"),
    AREA_711("711", "Test Area Code"),
    AREA_722("722", "Test Area Code"),
    AREA_733("733", "Test Area Code"),
    AREA_744("744", "Test Area Code"),
    AREA_755("755", "Test Area Code"),
    AREA_766("766", "Test Area Code"),
    AREA_777("777", "Test Area Code"),
    AREA_788("788", "Test Area Code"),
    AREA_799("799", "Test Area Code"),
    AREA_800("800", "Test Area Code"),
    AREA_811("811", "Test Area Code"),
    AREA_822("822", "Test Area Code"),
    AREA_833("833", "Test Area Code"),
    AREA_844("844", "Test Area Code"),
    AREA_855("855", "Test Area Code"),
    AREA_866("866", "Test Area Code"),
    AREA_877("877", "Test Area Code"),
    AREA_888("888", "Test Area Code"),
    AREA_899("899", "Test Area Code"),
    AREA_900("900", "Test Area Code"),
    AREA_911("911", "Test Area Code"),
    AREA_922("922", "Test Area Code"),
    AREA_933("933", "Test Area Code"),
    AREA_944("944", "Test Area Code"),
    AREA_955("955", "Test Area Code"),
    AREA_966("966", "Test Area Code"),
    AREA_977("977", "Test Area Code"),
    AREA_988("988", "Test Area Code"),
    AREA_999("999", "Test Area Code");
    
    private final String code;
    private final String description;
    
    /**
     * Constructor for PhoneAreaCode enum values.
     * 
     * @param code The 3-digit area code string matching COBOL PIC XXX format
     * @param description Human-readable description of the area code location
     */
    PhoneAreaCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Returns the 3-digit area code string exactly as specified in the original
     * COBOL VALID-PHONE-AREA-CODE VALUES clause.
     * 
     * @return The area code string (e.g., "201", "555", "999")
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns the human-readable description of the area code location.
     * 
     * @return The description string (e.g., "New Jersey (Northern)", "Test Area Code")
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Validates if the provided area code string is valid according to the
     * North America Numbering Plan Administrator (NANPA) standards as preserved
     * from the original COBOL VALID-PHONE-AREA-CODE 88-level condition.
     * 
     * This method provides the exact same validation behavior as the original
     * COBOL "IF VALID-PHONE-AREA-CODE" condition.
     * 
     * @param areaCode The area code string to validate (3 digits)
     * @return true if the area code is valid, false otherwise
     */
    public static boolean isValid(String areaCode) {
        if (areaCode == null || areaCode.length() != 3) {
            return false;
        }
        
        // Check if the area code exists in our enum values
        for (PhoneAreaCode phoneAreaCode : values()) {
            if (phoneAreaCode.code.equals(areaCode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Attempts to parse the provided area code string and return the corresponding
     * PhoneAreaCode enum value. This method supports null-safe processing and
     * robust error handling for customer phone number validation.
     * 
     * @param areaCode The area code string to parse (3 digits)
     * @return Optional containing the PhoneAreaCode enum value if valid, empty otherwise
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
     * Returns all valid PhoneAreaCode enum values. This method provides access
     * to the complete set of valid area codes for use in validation frameworks,
     * dropdown lists, and data validation routines.
     * 
     * @return Array of all PhoneAreaCode enum values
     */
    public static PhoneAreaCode[] values() {
        return PhoneAreaCode.values();
    }
    
    /**
     * Returns the string representation of this PhoneAreaCode, which is the
     * 3-digit area code matching the original COBOL format.
     * 
     * @return The area code string
     */
    @Override
    public String toString() {
        return code;
    }
}