/*
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

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * Phone Area Code enumeration containing all valid North American phone area codes
 * converted from COBOL VALID-PHONE-AREA-CODE 88-level condition for customer data validation.
 * 
 * This enum maintains exact compatibility with NANPA (North America Numbering Plan Administrator)
 * standards as specified in the original COBOL lookup logic from CSLKPCDY copybook.
 * 
 * Source: North America Numbering Plan Administrator
 * Reference: https://nationalnanpa.com/nanp1/npa_report.csv
 * 
 * Supports:
 * - Jakarta Bean Validation for Spring Boot REST API endpoints
 * - React Hook Form validation for customer data forms
 * - All 300+ valid area codes from original COBOL VALUES clause
 * - Preserves exact validation behavior from mainframe implementation
 */
public enum PhoneAreaCode {
    
    // Geographic Area Codes (Regular Service Areas)
    AREA_201("201", "New Jersey - Newark/Jersey City"),
    AREA_202("202", "District of Columbia - Washington DC"),
    AREA_203("203", "Connecticut - Bridgeport/New Haven"),
    AREA_204("204", "Manitoba - Winnipeg"),
    AREA_205("205", "Alabama - Birmingham/Tuscaloosa"),
    AREA_206("206", "Washington - Seattle"),
    AREA_207("207", "Maine - Statewide"),
    AREA_208("208", "Idaho - Statewide"),
    AREA_209("209", "California - Stockton/Modesto"),
    AREA_210("210", "Texas - San Antonio"),
    AREA_212("212", "New York - Manhattan"),
    AREA_213("213", "California - Los Angeles Downtown"),
    AREA_214("214", "Texas - Dallas"),
    AREA_215("215", "Pennsylvania - Philadelphia"),
    AREA_216("216", "Ohio - Cleveland"),
    AREA_217("217", "Illinois - Springfield/Champaign"),
    AREA_218("218", "Minnesota - Duluth"),
    AREA_219("219", "Indiana - Gary/Hammond"),
    AREA_220("220", "Ohio - Newark/Zanesville"),
    AREA_223("223", "Pennsylvania - Eastern Pennsylvania"),
    AREA_224("224", "Illinois - Evanston/Waukegan"),
    AREA_225("225", "Louisiana - Baton Rouge"),
    AREA_226("226", "Ontario - London/Windsor"),
    AREA_228("228", "Mississippi - Gulfport/Biloxi"),
    AREA_229("229", "Georgia - Albany/Valdosta"),
    AREA_231("231", "Michigan - Traverse City/Muskegon"),
    AREA_234("234", "Ohio - Akron/Youngstown"),
    AREA_236("236", "British Columbia - Vancouver"),
    AREA_239("239", "Florida - Fort Myers/Naples"),
    AREA_240("240", "Maryland - Frederick/Rockville"),
    AREA_242("242", "Bahamas - Nassau"),
    AREA_246("246", "Barbados - Statewide"),
    AREA_248("248", "Michigan - Troy/Pontiac"),
    AREA_249("249", "Ontario - Barrie/Collingwood"),
    AREA_250("250", "British Columbia - Victoria"),
    AREA_251("251", "Alabama - Mobile"),
    AREA_252("252", "North Carolina - Greenville/Rocky Mount"),
    AREA_253("253", "Washington - Tacoma"),
    AREA_254("254", "Texas - Killeen/Waco"),
    AREA_256("256", "Alabama - Huntsville/Florence"),
    AREA_260("260", "Indiana - Fort Wayne"),
    AREA_262("262", "Wisconsin - Kenosha/Racine"),
    AREA_264("264", "Anguilla - Statewide"),
    AREA_267("267", "Pennsylvania - Philadelphia"),
    AREA_268("268", "Antigua and Barbuda - Statewide"),
    AREA_269("269", "Michigan - Kalamazoo/Battle Creek"),
    AREA_270("270", "Kentucky - Bowling Green/Paducah"),
    AREA_272("272", "Pennsylvania - Scranton/Wilkes-Barre"),
    AREA_276("276", "Virginia - Martinsville/Bristol"),
    AREA_279("279", "California - Sacramento/Fairfield"),
    AREA_281("281", "Texas - Houston"),
    AREA_284("284", "British Virgin Islands - Statewide"),
    AREA_289("289", "Ontario - Hamilton/St. Catharines"),
    AREA_301("301", "Maryland - Bethesda/Hagerstown"),
    AREA_302("302", "Delaware - Statewide"),
    AREA_303("303", "Colorado - Denver/Boulder"),
    AREA_304("304", "West Virginia - Statewide"),
    AREA_305("305", "Florida - Miami"),
    AREA_306("306", "Saskatchewan - Statewide"),
    AREA_307("307", "Wyoming - Statewide"),
    AREA_308("308", "Nebraska - North Platte/Grand Island"),
    AREA_309("309", "Illinois - Peoria/Rock Island"),
    AREA_310("310", "California - Beverly Hills/Torrance"),
    AREA_312("312", "Illinois - Chicago Downtown"),
    AREA_313("313", "Michigan - Detroit"),
    AREA_314("314", "Missouri - St. Louis"),
    AREA_315("315", "New York - Syracuse/Utica"),
    AREA_316("316", "Kansas - Wichita"),
    AREA_317("317", "Indiana - Indianapolis"),
    AREA_318("318", "Louisiana - Shreveport/Monroe"),
    AREA_319("319", "Iowa - Cedar Rapids/Dubuque"),
    AREA_320("320", "Minnesota - St. Cloud"),
    AREA_321("321", "Florida - Melbourne/Cocoa Beach"),
    AREA_323("323", "California - Los Angeles"),
    AREA_325("325", "Texas - Abilene/San Angelo"),
    AREA_326("326", "Ohio - Ashtabula"),
    AREA_330("330", "Ohio - Akron/Youngstown"),
    AREA_331("331", "Illinois - Aurora/Elgin"),
    AREA_332("332", "New York - New York City"),
    AREA_334("334", "Alabama - Montgomery/Dothan"),
    AREA_336("336", "North Carolina - Greensboro/Winston-Salem"),
    AREA_337("337", "Louisiana - Lafayette/Lake Charles"),
    AREA_339("339", "Massachusetts - Quincy/Revere"),
    AREA_340("340", "U.S. Virgin Islands - St. Thomas/St. Croix"),
    AREA_341("341", "California - Oakland/Fremont"),
    AREA_343("343", "Ontario - Ottawa"),
    AREA_345("345", "Cayman Islands - Statewide"),
    AREA_346("346", "Texas - Houston"),
    AREA_347("347", "New York - Brooklyn/Queens"),
    AREA_351("351", "Massachusetts - Lowell/Lawrence"),
    AREA_352("352", "Florida - Gainesville/Ocala"),
    AREA_360("360", "Washington - Olympia/Bellingham"),
    AREA_361("361", "Texas - Corpus Christi"),
    AREA_364("364", "Kentucky - Bowling Green"),
    AREA_365("365", "Ontario - Hamilton"),
    AREA_367("367", "Quebec - Montreal"),
    AREA_368("368", "Alberta - Edmonton"),
    AREA_380("380", "Ohio - Columbus"),
    AREA_385("385", "Utah - Salt Lake City"),
    AREA_386("386", "Florida - Daytona Beach/Gainesville"),
    AREA_401("401", "Rhode Island - Statewide"),
    AREA_402("402", "Nebraska - Omaha/Lincoln"),
    AREA_403("403", "Alberta - Calgary"),
    AREA_404("404", "Georgia - Atlanta"),
    AREA_405("405", "Oklahoma - Oklahoma City"),
    AREA_406("406", "Montana - Statewide"),
    AREA_407("407", "Florida - Orlando"),
    AREA_408("408", "California - San Jose"),
    AREA_409("409", "Texas - Beaumont/Galveston"),
    AREA_410("410", "Maryland - Baltimore"),
    AREA_412("412", "Pennsylvania - Pittsburgh"),
    AREA_413("413", "Massachusetts - Springfield/Pittsfield"),
    AREA_414("414", "Wisconsin - Milwaukee"),
    AREA_415("415", "California - San Francisco"),
    AREA_416("416", "Ontario - Toronto"),
    AREA_417("417", "Missouri - Springfield/Joplin"),
    AREA_418("418", "Quebec - Quebec City"),
    AREA_419("419", "Ohio - Toledo/Lima"),
    AREA_423("423", "Tennessee - Chattanooga"),
    AREA_424("424", "California - Torrance/Redondo Beach"),
    AREA_425("425", "Washington - Bellevue/Everett"),
    AREA_430("430", "Texas - Northeast Texas"),
    AREA_431("431", "Manitoba - Winnipeg"),
    AREA_432("432", "Texas - Midland/Odessa"),
    AREA_434("434", "Virginia - Lynchburg"),
    AREA_435("435", "Utah - St. George/Logan"),
    AREA_437("437", "Ontario - Toronto"),
    AREA_438("438", "Quebec - Montreal"),
    AREA_440("440", "Ohio - Parma/Lorain"),
    AREA_441("441", "Bermuda - Statewide"),
    AREA_442("442", "California - Oceanside/El Centro"),
    AREA_443("443", "Maryland - Baltimore"),
    AREA_445("445", "Pennsylvania - Philadelphia"),
    AREA_447("447", "Illinois - Normal/Rantoul"),
    AREA_448("448", "Florida - Jacksonville"),
    AREA_450("450", "Quebec - Laval/Saint-Jerome"),
    AREA_458("458", "Oregon - Eugene/Corvallis"),
    AREA_463("463", "Indiana - Indianapolis"),
    AREA_464("464", "Illinois - Chicago"),
    AREA_469("469", "Texas - Dallas"),
    AREA_470("470", "Georgia - Atlanta"),
    AREA_473("473", "Grenada - Statewide"),
    AREA_474("474", "Saskatchewan - Statewide"),
    AREA_475("475", "Connecticut - Bridgeport"),
    AREA_478("478", "Georgia - Macon"),
    AREA_479("479", "Arkansas - Fort Smith/Fayetteville"),
    AREA_480("480", "Arizona - Mesa/Scottsdale"),
    AREA_484("484", "Pennsylvania - Allentown/Reading"),
    AREA_501("501", "Arkansas - Little Rock"),
    AREA_502("502", "Kentucky - Louisville"),
    AREA_503("503", "Oregon - Portland/Salem"),
    AREA_504("504", "Louisiana - New Orleans"),
    AREA_505("505", "New Mexico - Albuquerque/Santa Fe"),
    AREA_506("506", "New Brunswick - Statewide"),
    AREA_507("507", "Minnesota - Rochester/Mankato"),
    AREA_508("508", "Massachusetts - Worcester/Cape Cod"),
    AREA_509("509", "Washington - Spokane/Yakima"),
    AREA_510("510", "California - Oakland/Fremont"),
    AREA_512("512", "Texas - Austin"),
    AREA_513("513", "Ohio - Cincinnati"),
    AREA_514("514", "Quebec - Montreal"),
    AREA_515("515", "Iowa - Des Moines"),
    AREA_516("516", "New York - Hempstead/Levittown"),
    AREA_517("517", "Michigan - Lansing/Jackson"),
    AREA_518("518", "New York - Albany/Schenectady"),
    AREA_519("519", "Ontario - London/Windsor"),
    AREA_520("520", "Arizona - Tucson"),
    AREA_530("530", "California - Redding/Chico"),
    AREA_531("531", "Nebraska - Omaha"),
    AREA_534("534", "Wisconsin - Eau Claire"),
    AREA_539("539", "Oklahoma - Tulsa"),
    AREA_540("540", "Virginia - Roanoke/Fredericksburg"),
    AREA_541("541", "Oregon - Eugene/Medford"),
    AREA_548("548", "Ontario - Kitchener/Waterloo"),
    AREA_551("551", "New Jersey - Jersey City/Hoboken"),
    AREA_559("559", "California - Fresno/Visalia"),
    AREA_561("561", "Florida - West Palm Beach/Boca Raton"),
    AREA_562("562", "California - Long Beach"),
    AREA_563("563", "Iowa - Davenport/Dubuque"),
    AREA_564("564", "Washington - Olympia"),
    AREA_567("567", "Ohio - Toledo"),
    AREA_570("570", "Pennsylvania - Scranton/Wilkes-Barre"),
    AREA_571("571", "Virginia - Arlington/Alexandria"),
    AREA_572("572", "Oklahoma - Tulsa"),
    AREA_573("573", "Missouri - Columbia/Cape Girardeau"),
    AREA_574("574", "Indiana - South Bend/Elkhart"),
    AREA_575("575", "New Mexico - Las Cruces/Roswell"),
    AREA_579("579", "Quebec - Sherbrooke"),
    AREA_580("580", "Oklahoma - Lawton/Enid"),
    AREA_581("581", "Quebec - Quebec City"),
    AREA_582("582", "Pennsylvania - Altoona"),
    AREA_585("585", "New York - Rochester"),
    AREA_586("586", "Michigan - Warren/Sterling Heights"),
    AREA_587("587", "Alberta - Calgary/Edmonton"),
    AREA_601("601", "Mississippi - Jackson"),
    AREA_602("602", "Arizona - Phoenix"),
    AREA_603("603", "New Hampshire - Statewide"),
    AREA_604("604", "British Columbia - Vancouver"),
    AREA_605("605", "South Dakota - Statewide"),
    AREA_606("606", "Kentucky - Ashland/Middlesboro"),
    AREA_607("607", "New York - Binghamton/Elmira"),
    AREA_608("608", "Wisconsin - Madison"),
    AREA_609("609", "New Jersey - Trenton/Atlantic City"),
    AREA_610("610", "Pennsylvania - Allentown/Reading"),
    AREA_612("612", "Minnesota - Minneapolis"),
    AREA_613("613", "Ontario - Ottawa"),
    AREA_614("614", "Ohio - Columbus"),
    AREA_615("615", "Tennessee - Nashville"),
    AREA_616("616", "Michigan - Grand Rapids/Kalamazoo"),
    AREA_617("617", "Massachusetts - Boston"),
    AREA_618("618", "Illinois - Carbondale/Cairo"),
    AREA_619("619", "California - San Diego"),
    AREA_620("620", "Kansas - Dodge City/Garden City"),
    AREA_623("623", "Arizona - Glendale/Peoria"),
    AREA_626("626", "California - Pasadena/Pomona"),
    AREA_628("628", "California - San Francisco"),
    AREA_629("629", "Tennessee - Nashville"),
    AREA_630("630", "Illinois - Aurora/Naperville"),
    AREA_631("631", "New York - Suffolk County"),
    AREA_636("636", "Missouri - O'Fallon/St. Charles"),
    AREA_639("639", "Saskatchewan - Statewide"),
    AREA_640("640", "California - San Jose"),
    AREA_641("641", "Iowa - Mason City/Ottumwa"),
    AREA_646("646", "New York - Manhattan"),
    AREA_647("647", "Ontario - Toronto"),
    AREA_649("649", "Turks and Caicos Islands - Statewide"),
    AREA_650("650", "California - San Mateo/Palo Alto"),
    AREA_651("651", "Minnesota - St. Paul"),
    AREA_656("656", "Florida - St. Petersburg"),
    AREA_657("657", "California - Anaheim/Huntington Beach"),
    AREA_658("658", "Jamaica - Kingston"),
    AREA_659("659", "Alabama - Birmingham"),
    AREA_660("660", "Missouri - Sedalia/Marshall"),
    AREA_661("661", "California - Bakersfield/Lancaster"),
    AREA_662("662", "Mississippi - Tupelo/Greenville"),
    AREA_664("664", "Montserrat - Statewide"),
    AREA_667("667", "Maryland - Baltimore"),
    AREA_669("669", "California - San Jose"),
    AREA_670("670", "Northern Mariana Islands - Saipan"),
    AREA_671("671", "Guam - Statewide"),
    AREA_672("672", "British Columbia - Vancouver"),
    AREA_678("678", "Georgia - Atlanta"),
    AREA_680("680", "New York - Syracuse"),
    AREA_681("681", "West Virginia - Charleston"),
    AREA_682("682", "Texas - Fort Worth"),
    AREA_683("683", "Texas - Plano/Richardson"),
    AREA_684("684", "American Samoa - Statewide"),
    AREA_689("689", "Florida - Orlando"),
    AREA_701("701", "North Dakota - Statewide"),
    AREA_702("702", "Nevada - Las Vegas"),
    AREA_703("703", "Virginia - Arlington/Alexandria"),
    AREA_704("704", "North Carolina - Charlotte"),
    AREA_705("705", "Ontario - Sault Ste. Marie/North Bay"),
    AREA_706("706", "Georgia - Columbus/Augusta"),
    AREA_707("707", "California - Santa Rosa/Napa"),
    AREA_708("708", "Illinois - Cicero/Oak Park"),
    AREA_709("709", "Newfoundland and Labrador - Statewide"),
    AREA_712("712", "Iowa - Sioux City"),
    AREA_713("713", "Texas - Houston"),
    AREA_714("714", "California - Anaheim/Huntington Beach"),
    AREA_715("715", "Wisconsin - Eau Claire/Wausau"),
    AREA_716("716", "New York - Buffalo/Niagara Falls"),
    AREA_717("717", "Pennsylvania - Harrisburg/Lancaster"),
    AREA_718("718", "New York - Brooklyn/Queens/Staten Island"),
    AREA_719("719", "Colorado - Colorado Springs/Pueblo"),
    AREA_720("720", "Colorado - Denver"),
    AREA_721("721", "Sint Maarten - Statewide"),
    AREA_724("724", "Pennsylvania - Washington/Uniontown"),
    AREA_725("725", "Nevada - Las Vegas"),
    AREA_726("726", "Texas - San Antonio"),
    AREA_727("727", "Florida - St. Petersburg/Clearwater"),
    AREA_731("731", "Tennessee - Jackson"),
    AREA_732("732", "New Jersey - New Brunswick/Toms River"),
    AREA_734("734", "Michigan - Ann Arbor/Livonia"),
    AREA_737("737", "Texas - Austin"),
    AREA_740("740", "Ohio - Zanesville/Athens"),
    AREA_742("742", "Ontario - Toronto"),
    AREA_743("743", "North Carolina - Greensboro"),
    AREA_747("747", "California - Burbank/Glendale"),
    AREA_753("753", "Ontario - Thunder Bay"),
    AREA_754("754", "Florida - Fort Lauderdale"),
    AREA_757("757", "Virginia - Norfolk/Virginia Beach"),
    AREA_758("758", "Saint Lucia - Statewide"),
    AREA_760("760", "California - Oceanside/El Centro"),
    AREA_762("762", "Georgia - Augusta"),
    AREA_763("763", "Minnesota - Brooklyn Park/Plymouth"),
    AREA_765("765", "Indiana - Muncie/Lafayette"),
    AREA_767("767", "Dominica - Statewide"),
    AREA_769("769", "Mississippi - Jackson"),
    AREA_770("770", "Georgia - Atlanta Suburbs"),
    AREA_771("771", "Washington - Seattle"),
    AREA_772("772", "Florida - Port St. Lucie/Vero Beach"),
    AREA_773("773", "Illinois - Chicago"),
    AREA_774("774", "Massachusetts - Worcester"),
    AREA_775("775", "Nevada - Reno/Carson City"),
    AREA_778("778", "British Columbia - Vancouver"),
    AREA_779("779", "Illinois - Rockford"),
    AREA_780("780", "Alberta - Edmonton"),
    AREA_781("781", "Massachusetts - Lynn/Waltham"),
    AREA_782("782", "Nova Scotia - Halifax"),
    AREA_784("784", "Saint Vincent and the Grenadines - Statewide"),
    AREA_785("785", "Kansas - Topeka/Salina"),
    AREA_786("786", "Florida - Miami"),
    AREA_787("787", "Puerto Rico - San Juan/Bayamon"),
    AREA_801("801", "Utah - Salt Lake City"),
    AREA_802("802", "Vermont - Statewide"),
    AREA_803("803", "South Carolina - Columbia"),
    AREA_804("804", "Virginia - Richmond/Petersburg"),
    AREA_805("805", "California - Ventura/Santa Barbara"),
    AREA_806("806", "Texas - Lubbock/Amarillo"),
    AREA_807("807", "Ontario - Thunder Bay/Kenora"),
    AREA_808("808", "Hawaii - Statewide"),
    AREA_809("809", "Dominican Republic - Santo Domingo"),
    AREA_810("810", "Michigan - Flint/Pontiac"),
    AREA_812("812", "Indiana - Evansville/Bloomington"),
    AREA_813("813", "Florida - Tampa"),
    AREA_814("814", "Pennsylvania - Erie/Altoona"),
    AREA_815("815", "Illinois - Rockford/Kankakee"),
    AREA_816("816", "Missouri - Kansas City"),
    AREA_817("817", "Texas - Fort Worth/Arlington"),
    AREA_818("818", "California - Burbank/Van Nuys"),
    AREA_819("819", "Quebec - Sherbrooke/Trois-Rivieres"),
    AREA_820("820", "California - San Francisco"),
    AREA_825("825", "Alberta - Calgary"),
    AREA_826("826", "Virginia - Martinsville"),
    AREA_828("828", "North Carolina - Asheville"),
    AREA_829("829", "Dominican Republic - Santiago"),
    AREA_830("830", "Texas - Fredericksburg/Uvalde"),
    AREA_831("831", "California - Salinas/Monterey"),
    AREA_832("832", "Texas - Houston"),
    AREA_838("838", "New York - Albany"),
    AREA_839("839", "South Carolina - Hilton Head"),
    AREA_840("840", "California - San Francisco Bay Area"),
    AREA_843("843", "South Carolina - Charleston"),
    AREA_845("845", "New York - Poughkeepsie/Newburgh"),
    AREA_847("847", "Illinois - Elgin/Evanston"),
    AREA_848("848", "New Jersey - New Brunswick"),
    AREA_849("849", "Dominican Republic - Santiago"),
    AREA_850("850", "Florida - Tallahassee/Pensacola"),
    AREA_854("854", "South Carolina - Charleston"),
    AREA_856("856", "New Jersey - Camden"),
    AREA_857("857", "Massachusetts - Boston"),
    AREA_858("858", "California - San Diego"),
    AREA_859("859", "Kentucky - Lexington/Covington"),
    AREA_860("860", "Connecticut - Hartford/New Britain"),
    AREA_862("862", "New Jersey - Newark"),
    AREA_863("863", "Florida - Lakeland/Bartow"),
    AREA_864("864", "South Carolina - Greenville/Spartanburg"),
    AREA_865("865", "Tennessee - Knoxville"),
    AREA_867("867", "Yukon/Northwest Territories - Whitehorse"),
    AREA_868("868", "Trinidad and Tobago - Port of Spain"),
    AREA_869("869", "Saint Kitts and Nevis - Basseterre"),
    AREA_870("870", "Arkansas - Jonesboro/Pine Bluff"),
    AREA_872("872", "Illinois - Chicago"),
    AREA_873("873", "Quebec - Sherbrooke"),
    AREA_876("876", "Jamaica - Kingston"),
    AREA_878("878", "Pennsylvania - Pittsburgh"),
    AREA_901("901", "Tennessee - Memphis"),
    AREA_902("902", "Nova Scotia/Prince Edward Island - Halifax"),
    AREA_903("903", "Texas - Tyler/Longview"),
    AREA_904("904", "Florida - Jacksonville"),
    AREA_905("905", "Ontario - Hamilton/Mississauga"),
    AREA_906("906", "Michigan - Upper Peninsula"),
    AREA_907("907", "Alaska - Statewide"),
    AREA_908("908", "New Jersey - Elizabeth/Somerville"),
    AREA_909("909", "California - San Bernardino/Riverside"),
    AREA_910("910", "North Carolina - Fayetteville/Wilmington"),
    AREA_912("912", "Georgia - Savannah"),
    AREA_913("913", "Kansas - Kansas City/Overland Park"),
    AREA_914("914", "New York - White Plains/Yonkers"),
    AREA_915("915", "Texas - El Paso"),
    AREA_916("916", "California - Sacramento"),
    AREA_917("917", "New York - New York City"),
    AREA_918("918", "Oklahoma - Tulsa"),
    AREA_919("919", "North Carolina - Raleigh/Durham"),
    AREA_920("920", "Wisconsin - Green Bay/Appleton"),
    AREA_925("925", "California - Concord/Antioch"),
    AREA_928("928", "Arizona - Flagstaff/Yuma"),
    AREA_929("929", "New York - Queens/Brooklyn"),
    AREA_930("930", "Indiana - New Albany"),
    AREA_931("931", "Tennessee - Cookeville/Columbia"),
    AREA_934("934", "New York - Westchester County"),
    AREA_936("936", "Texas - Huntsville/Conroe"),
    AREA_937("937", "Ohio - Dayton/Springfield"),
    AREA_938("938", "Alabama - Huntsville"),
    AREA_939("939", "Puerto Rico - Caguas/Arecibo"),
    AREA_940("940", "Texas - Wichita Falls/Denton"),
    AREA_941("941", "Florida - Sarasota/Bradenton"),
    AREA_943("943", "Texas - Waco"),
    AREA_945("945", "Texas - Dallas"),
    AREA_947("947", "Michigan - Troy"),
    AREA_948("948", "Arizona - Phoenix"),
    AREA_949("949", "California - Irvine/Mission Viejo"),
    AREA_951("951", "California - Riverside/Moreno Valley"),
    AREA_952("952", "Minnesota - Bloomington/Plymouth"),
    AREA_954("954", "Florida - Fort Lauderdale"),
    AREA_956("956", "Texas - Laredo/Brownsville"),
    AREA_959("959", "Connecticut - Hartford"),
    AREA_970("970", "Colorado - Fort Collins/Grand Junction"),
    AREA_971("971", "Oregon - Portland"),
    AREA_972("972", "Texas - Dallas"),
    AREA_973("973", "New Jersey - Newark/Paterson"),
    AREA_978("978", "Massachusetts - Lowell/Lawrence"),
    AREA_979("979", "Texas - College Station/Bay City"),
    AREA_980("980", "North Carolina - Charlotte"),
    AREA_983("983", "Colorado - Denver"),
    AREA_984("984", "North Carolina - Raleigh"),
    AREA_985("985", "Louisiana - Hammond/Houma"),
    AREA_986("986", "Idaho - Boise"),
    AREA_989("989", "Michigan - Saginaw/Midland"),
    
    // Special Service Area Codes and Easily Recognizable Codes
    AREA_200("200", "Reserved - Not in Public Use"),
    AREA_211("211", "Community Information/Social Services"),
    AREA_222("222", "Reserved - Not in Public Use"),
    AREA_233("233", "Reserved - Not in Public Use"),
    AREA_244("244", "Reserved - Not in Public Use"),
    AREA_255("255", "Reserved - Not in Public Use"),
    AREA_266("266", "Reserved - Not in Public Use"),
    AREA_277("277", "Reserved - Not in Public Use"),
    AREA_288("288", "Reserved - Not in Public Use"),
    AREA_299("299", "Reserved - Not in Public Use"),
    AREA_300("300", "Reserved - Not in Public Use"),
    AREA_311("311", "Municipal Government Services"),
    AREA_322("322", "Reserved - Not in Public Use"),
    AREA_333("333", "Reserved - Not in Public Use"),
    AREA_344("344", "Reserved - Not in Public Use"),
    AREA_355("355", "Reserved - Not in Public Use"),
    AREA_366("366", "Reserved - Not in Public Use"),
    AREA_377("377", "Reserved - Not in Public Use"),
    AREA_388("388", "Reserved - Not in Public Use"),
    AREA_399("399", "Reserved - Not in Public Use"),
    AREA_400("400", "Reserved - Not in Public Use"),
    AREA_411("411", "Directory Information Services"),
    AREA_422("422", "Reserved - Not in Public Use"),
    AREA_433("433", "Reserved - Not in Public Use"),
    AREA_444("444", "Reserved - Not in Public Use"),
    AREA_455("455", "Reserved - Not in Public Use"),
    AREA_466("466", "Reserved - Not in Public Use"),
    AREA_477("477", "Reserved - Not in Public Use"),
    AREA_488("488", "Reserved - Not in Public Use"),
    AREA_499("499", "Reserved - Not in Public Use"),
    AREA_500("500", "Reserved - Not in Public Use"),
    AREA_511("511", "Traffic/Travel Information Services"),
    AREA_522("522", "Reserved - Not in Public Use"),
    AREA_533("533", "Reserved - Not in Public Use"),
    AREA_544("544", "Reserved - Not in Public Use"),
    AREA_555("555", "Information/Test Numbers"),
    AREA_566("566", "Reserved - Not in Public Use"),
    AREA_577("577", "Reserved - Not in Public Use"),
    AREA_588("588", "Reserved - Not in Public Use"),
    AREA_599("599", "Reserved - Not in Public Use"),
    AREA_600("600", "Reserved - Not in Public Use"),
    AREA_611("611", "Repair Service"),
    AREA_622("622", "Reserved - Not in Public Use"),
    AREA_633("633", "Reserved - Not in Public Use"),
    AREA_644("644", "Reserved - Not in Public Use"),
    AREA_655("655", "Reserved - Not in Public Use"),
    AREA_666("666", "Reserved - Not in Public Use"),
    AREA_677("677", "Reserved - Not in Public Use"),
    AREA_688("688", "Reserved - Not in Public Use"),
    AREA_699("699", "Reserved - Not in Public Use"),
    AREA_700("700", "Reserved - Not in Public Use"),
    AREA_711("711", "Telecommunications Relay Service"),
    AREA_722("722", "Reserved - Not in Public Use"),
    AREA_733("733", "Reserved - Not in Public Use"),
    AREA_744("744", "Reserved - Not in Public Use"),
    AREA_755("755", "Reserved - Not in Public Use"),
    AREA_766("766", "Reserved - Not in Public Use"),
    AREA_777("777", "Reserved - Not in Public Use"),
    AREA_788("788", "Reserved - Not in Public Use"),
    AREA_799("799", "Reserved - Not in Public Use"),
    AREA_800("800", "Toll-Free Service"),
    AREA_811("811", "Underground Utility Location"),
    AREA_822("822", "Reserved - Not in Public Use"),
    AREA_833("833", "Toll-Free Service"),
    AREA_844("844", "Toll-Free Service"),
    AREA_855("855", "Toll-Free Service"),
    AREA_866("866", "Toll-Free Service"),
    AREA_877("877", "Toll-Free Service"),
    AREA_888("888", "Toll-Free Service"),
    AREA_899("899", "Reserved - Not in Public Use"),
    AREA_900("900", "Premium Rate Service"),
    AREA_911("911", "Emergency Services"),
    AREA_922("922", "Reserved - Not in Public Use"),
    AREA_933("933", "Reserved - Not in Public Use"),
    AREA_944("944", "Reserved - Not in Public Use"),
    AREA_955("955", "Reserved - Not in Public Use"),
    AREA_966("966", "Reserved - Not in Public Use"),
    AREA_977("977", "Reserved - Not in Public Use"),
    AREA_988("988", "Suicide & Crisis Lifeline"),
    AREA_999("999", "Reserved - Not in Public Use");

    private final String code;
    private final String description;

    /**
     * Constructor for PhoneAreaCode enum constant.
     * 
     * @param code The 3-digit area code string
     * @param description Human-readable description of the area code service area
     */
    PhoneAreaCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the 3-digit area code as a string.
     * Preserves exact format from original COBOL VALUES clause.
     * 
     * @return The area code string (e.g., "201", "555", "800")
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the human-readable description for this area code.
     * Includes geographic region or service type information.
     * 
     * @return Description string for the area code
     */
    public String getDescription() {
        return description;
    }

    /**
     * Validates if a given area code string is valid according to NANPA standards.
     * Replicates the exact validation behavior of the original COBOL 88-level condition.
     * 
     * @param areaCode The area code string to validate (can be null)
     * @return true if the area code exists in the valid area codes list, false otherwise
     */
    public static boolean isValid(String areaCode) {
        if (areaCode == null || areaCode.trim().isEmpty()) {
            return false;
        }
        
        // Normalize input - trim and pad with leading zeros if needed
        String normalizedCode = areaCode.trim();
        if (normalizedCode.length() == 1) {
            normalizedCode = "00" + normalizedCode;
        } else if (normalizedCode.length() == 2) {
            normalizedCode = "0" + normalizedCode;
        }
        
        // Check if the normalized code matches any valid area code
        for (PhoneAreaCode phoneAreaCode : values()) {
            if (phoneAreaCode.code.equals(normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to find a PhoneAreaCode enum constant for the given area code string.
     * Provides null-safe processing for area code lookup operations.
     * 
     * @param areaCode The area code string to look up (can be null)
     * @return Optional containing the matching PhoneAreaCode, or empty if not found
     */
    public static Optional<PhoneAreaCode> fromCode(String areaCode) {
        if (areaCode == null || areaCode.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Normalize input - trim and pad with leading zeros if needed
        String normalizedCode = areaCode.trim();
        if (normalizedCode.length() == 1) {
            normalizedCode = "00" + normalizedCode;
        } else if (normalizedCode.length() == 2) {
            normalizedCode = "0" + normalizedCode;
        }
        
        // Find matching area code
        for (PhoneAreaCode phoneAreaCode : values()) {
            if (phoneAreaCode.code.equals(normalizedCode)) {
                return Optional.of(phoneAreaCode);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all valid PhoneAreaCode enum constants.
     * Maintains compatibility with standard Java enum values() method.
     * Used by Jakarta Bean Validation and React Hook Form validation.
     * 
     * @return Array of all PhoneAreaCode enum constants
     */
    public static PhoneAreaCode[] values() {
        return PhoneAreaCode.class.getEnumConstants();
    }

    /**
     * String representation of the area code for debugging and logging.
     * 
     * @return String in format "PhoneAreaCode{code='XXX', description='...'}"
     */
    @Override
    public String toString() {
        return String.format("PhoneAreaCode{code='%s', description='%s'}", code, description);
    }
}