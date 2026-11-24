package com.tradebridge.validation.country;

import java.util.Locale;
import java.util.Set;

/**
 * ISO 3166-1 country code validation (alpha-2 and alpha-3).
 *
 * <p>Alpha-2 codes are sourced from {@link Locale#getISOCountries()}.
 * Alpha-3 codes are embedded (ISO 3166-1:2024) because the JDK does
 * not expose them directly. Kept small and stable — updated only when
 * ISO publishes changes (rare, roughly once per year).
 *
 * <p>Whether the country is <b>enabled</b> for onboarding is a Country
 * Service concern (spec §4.4).
 */
public final class CountryCodeValidator {

    private static final Set<String> ALPHA2 = Set.of(Locale.getISOCountries());

    private static final Set<String> ALPHA3 = Set.of(
            "ABW","AFG","AGO","AIA","ALA","ALB","AND","ARE","ARG","ARM",
            "ASM","ATA","ATF","ATG","AUS","AUT","AZE","BDI","BEL","BEN",
            "BES","BFA","BGD","BGR","BHR","BHS","BIH","BLM","BLR","BLZ",
            "BMU","BOL","BRA","BRB","BRN","BTN","BVT","BWA","CAF","CAN",
            "CCK","CHE","CHL","CHN","CIV","CMR","COD","COG","COK","COL",
            "COM","CPV","CRI","CUB","CUW","CXR","CYM","CYP","CZE","DEU",
            "DJI","DMA","DNK","DOM","DZA","ECU","EGY","ERI","ESH","ESP",
            "EST","ETH","FIN","FJI","FLK","FRA","FRO","FSM","GAB","GBR",
            "GEO","GGY","GHA","GIB","GIN","GLP","GMB","GNB","GNQ","GRC",
            "GRD","GRL","GTM","GUF","GUM","GUY","HKG","HMD","HND","HRV",
            "HTI","HUN","IDN","IMN","IND","IOT","IRL","IRN","IRQ","ISL",
            "ISR","ITA","JAM","JEY","JOR","JPN","KAZ","KEN","KGZ","KHM",
            "KIR","KNA","KOR","KWT","LAO","LBN","LBR","LBY","LCA","LIE",
            "LKA","LSO","LTU","LUX","LVA","MAC","MAF","MAR","MCO","MDA",
            "MDG","MDV","MEX","MHL","MKD","MLI","MLT","MMR","MNE","MNG",
            "MNP","MOZ","MRT","MSR","MTQ","MUS","MWI","MYS","MYT","NAM",
            "NCL","NER","NFK","NGA","NIC","NIU","NLD","NOR","NPL","NRU",
            "NZL","OMN","PAK","PAN","PCN","PER","PHL","PLW","PNG","POL",
            "PRI","PRK","PRT","PRY","PSE","PYF","QAT","REU","ROU","RUS",
            "RWA","SAU","SDN","SEN","SGP","SGS","SHN","SJM","SLB","SLE",
            "SLV","SMR","SOM","SPM","SRB","SSD","STP","SUR","SVK","SVN",
            "SWE","SWZ","SXM","SYC","SYR","TCA","TCD","TGO","THA","TJK",
            "TKL","TKM","TLS","TON","TTO","TUN","TUR","TUV","TWN","TZA",
            "UGA","UKR","UMI","URY","USA","UZB","VAT","VCT","VEN","VGB",
            "VIR","VNM","VUT","WLF","WSM","YEM","ZAF","ZMB","ZWE"
    );

    private CountryCodeValidator() { }

    public static boolean isValidAlpha2(String code) {
        if (code == null) return false;
        String upper = code.trim().toUpperCase(Locale.ROOT);
        return upper.length() == 2 && ALPHA2.contains(upper);
    }

    public static boolean isValidAlpha3(String code) {
        if (code == null) return false;
        String upper = code.trim().toUpperCase(Locale.ROOT);
        return upper.length() == 3 && ALPHA3.contains(upper);
    }

    public static boolean isValid(String code) {
        return isValidAlpha2(code) || isValidAlpha3(code);
    }

    public static String normalize(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
