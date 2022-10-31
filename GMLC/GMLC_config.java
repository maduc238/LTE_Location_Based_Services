package org.example.client;

import static org.junit.Assert.assertFalse;

public class Dataset {
    private static long VENDOR_ID = 10415;
	private static String GMLC_Address = "127.0.0.10";
	private static String IMSI = "452041234567813";
	private static String MSISDN = "840987654321";
	private static String GMLC_number = "1";
	private static String IMEI = "123456789012345";
	private static String PLMN_ID = "45204";

    public static long getVENDOR_ID() {
        return VENDOR_ID;
    }

    public static void setGMLC_Address(String gMLC_Address) {
        GMLC_Address = gMLC_Address;
    }
    public static String getGMLC_Address() {
        return GMLC_Address;
    }

    public static void setIMSI(String iMSI) {
        IMSI = iMSI;
    }
    public static String getIMSI() {
        return IMSI;
    }

    public static void setMSISDN(String mSISDN) {
        MSISDN = mSISDN;
    }
    public static String getMSISDN() {
        return MSISDN;
    }

    public static void setGMLC_number(String gMLC_number) {
        GMLC_number = gMLC_number;
    }
    public static String getGMLC_number() {
        return GMLC_number;
    }

    public static void setIMEI(String iMEI) {
        IMEI = iMEI;
    }
    public static String getIMEI() {
        return IMEI;
    }

    public static void setPLMN_ID(String pLMN_ID) {
        PLMN_ID = pLMN_ID;
    }
    public static String getPLMN_ID() {
        return PLMN_ID;
    }

    private static int SLG_LOCATION_TYPE = 0;
    /**
     * This Information Element shall contain the type of location
     * measurement requested, such as current location, initial location, last
     * known location
     * <p> {@code 0} CURRENT_LOCATION <p>
     * <p> {@code 1} CURRENT_OR_LAST_KNOW_LOCATION <p>
     * <p> {@code 2} INITIAL_LOCATION <p>
     * <p> {@code 3} ACTIVATE_DEFERRED_LOCATION <p>
     * <p> {@code 4} CANCEL_DEFERRED_LOCATION <p>
     * <p> {@code 5} NOTIFICATION_VERIFICATION_ONLY <p>
     */
    public static void setSLG_LOCATION_TYPE(int sLG_LOCATION_TYPE) {
        SLG_LOCATION_TYPE = sLG_LOCATION_TYPE;
    }
    public static int getSLG_LOCATION_TYPE() {
        return SLG_LOCATION_TYPE;
    }

    private static String LCS_NAME_STRING = "https://aovl.com.vn";
    public static void setLCS_NAME_STRING(String lCS_NAME_STRING) {
        LCS_NAME_STRING = lCS_NAME_STRING;
    }
    public static String getLCS_NAME_STRING() {
        return LCS_NAME_STRING;
    }

    private static String LCS_REQUESTOR_ID_STRING = "https://aothatday.com";
    public static void setLCS_REQUESTOR_ID_STRING(String lCS_REQUESTOR_ID_STRING) {
        LCS_REQUESTOR_ID_STRING = lCS_REQUESTOR_ID_STRING;
    }
    public static String getLCS_REQUESTOR_ID_STRING() {
        return LCS_REQUESTOR_ID_STRING;
    }

    private static int LCS_CLIENT_TYPE = 1;
    /**
     * Loại dịch vụ mà LCS Client yêu cầu
     * <p> {@code 0} EMERGENCY_SERVICES <p>
     * <p> {@code 1} VALUE_ADDED_SERVICES <p>
     * <p> {@code 2} PLMN_OPERATOR_SERVICES <p>
     * <p> {@code 3} LAWFUL_INTERCEPT_SERVICES <p>
     * @param lCS_CLIENT_TYPE
     */
    public static void setLCS_CLIENT_TYPE(int lCS_CLIENT_TYPE) {
        LCS_CLIENT_TYPE = lCS_CLIENT_TYPE;
    }
    public static int getLCS_CLIENT_TYPE() {
        return LCS_CLIENT_TYPE;
    }

    public static String magicConvert(String a) {
        String cd = "";
        int len = a.length();
        if (a.length() % 2 != 0) {
            a = a + "f";
            len ++;
        }
        for(int i=0; i < len/2;i++){
            int temp = Integer.decode("0x"+a.charAt(2*i+1)+a.charAt(2*i));
            cd += Character.toString((char)temp);
        }
        return cd;
    }

}
