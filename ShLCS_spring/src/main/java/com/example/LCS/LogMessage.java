package com.example.LCS;

import java.util.Date;
import java.util.List;

import com.example.LCS.Database.Logging;

public class LogMessage {
	/**
	 * Dạng HbH - EtE - Result Code - data
	 * @param loggingList
	 * @param data
	 * @param HopByHopId
	 * @param EndToEndId
	 * @param resultCode
	 */
    static void addLogging(List<Logging> loggingList, String data, Long HopByHopId, Long EndToEndId, long resultCode) {
        Logging logs = new Logging();
        Date date = new Date();
		logs.setTime(date.toString());
		logs.setData("Hop by Hop Identifier: "+HopByHopId+". End to End Identifier: "+EndToEndId+". Result Code = "+resultCode+": "+data);
		loggingList.add(logs);
	}

	/**
	 * Dạng HbH - EtE - data
	 * @param loggingList
	 * @param data
	 * @param HopByHopId
	 * @param EndToEndId
	 */
	static void addLogging(List<Logging> loggingList, String data, Long HopByHopId, Long EndToEndId) {
        Logging logs = new Logging();
        Date date = new Date();
		logs.setTime(date.toString());
		logs.setData("Hop by Hop Identifier: "+HopByHopId+". End to End Identifier: "+EndToEndId+". "+data);
		loggingList.add(logs);
	}

	/**
	 * Log data thủ công
	 * @param loggingList
	 * @param data
	 */
	static void addLogging(List<Logging> loggingList, String data) {
        Logging logs = new Logging();
        Date date = new Date();
		logs.setTime(date.toString());
		logs.setData(data);
		loggingList.add(logs);
	}

    static void addLogRequest(List<Logging> loggingList, Long HopByHopId, Long EndToEndId, String MSISDN) {
        Logging logs = new Logging();
        Date date = new Date();
		logs.setTime(date.toString());
		logs.setData("Hop by Hop Identifier: "+HopByHopId+". End to End Identifier: "+EndToEndId+". Thực hiện gửi bản tin Request. MSISDN = " + MSISDN);
		loggingList.add(logs);
	}

	/**
	 * Log Result Code của bản tin answer nhận được
	 * @param loggingList
	 * @param resultCode
	 * @param HopByHopId
	 * @param EndToEndId
	 * @return
	 */
	static boolean logResultCode(List<Logging> loggingList, long resultCode, Long HopByHopId, Long EndToEndId) {
		boolean result = false;
		switch ((int) resultCode) {
			case 2001:
				LogMessage.addLogging(loggingList, "DIAMETER_SUCCESS", HopByHopId, EndToEndId, resultCode);
				result = true;
				break;
			case 4100:
				LogMessage.addLogging(loggingList, "DIAMETER_USER_DATA_NOT_AVAILABLE", HopByHopId, EndToEndId, resultCode);
				break;
			case 4101:
				LogMessage.addLogging(loggingList, "DIAMETER_PRIOR_UPDATE_IN_PROGRESS", HopByHopId, EndToEndId, resultCode);
				break;
			case 4201:
				LogMessage.addLogging(loggingList, "The location of the targeted user is not known at this time to satisfy the requested operation!", HopByHopId, EndToEndId, resultCode);
				break;
			case 5001:
				LogMessage.addLogging(loggingList, "DIAMETER_AVP_UNSUPPORTED", HopByHopId, EndToEndId, resultCode);
				break;
			case 5002:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_IDENTITIES_DONT_MATCH", HopByHopId, EndToEndId, resultCode);
				break;	
			case 5004:
				LogMessage.addLogging(loggingList, "Có vấn đề với HSS!", HopByHopId, EndToEndId, resultCode);
				break;
			case 5005:
				LogMessage.addLogging(loggingList, "Có vấn đề với HSS!", HopByHopId, EndToEndId, resultCode);
				break;
			case 5006:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_SUBS_DATA_ABSENT", HopByHopId, EndToEndId, resultCode);
				break;
			case 5007:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_NO_SUBSCRIPTION_TO_DATA", HopByHopId, EndToEndId, resultCode);
				break;
			case 5008:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_TOO_MUCH_DATA", HopByHopId, EndToEndId, resultCode);
				break;
			case 5011:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_FEATURE_UNSUPPORTED", HopByHopId, EndToEndId, resultCode);
				break;
			case 5100:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_NOT_RECOGNIZED", HopByHopId, EndToEndId, resultCode);
				break;
			case 5101:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_OPERATION_NOT_ALLOWED", HopByHopId, EndToEndId, resultCode);
				break;
			case 5102:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_READ", HopByHopId, EndToEndId, resultCode);
				break;
			case 5103:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_MODIFIED", HopByHopId, EndToEndId, resultCode);
				break;
			case 5104:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_USER_DATA_CANNOT_BE_NOTIFIED", HopByHopId, EndToEndId, resultCode);
				break;
			case 5105:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_TRANSPARENT_DATA_OUT_OF_SYNC", HopByHopId, EndToEndId, resultCode);
				break;
			case 5108:
				LogMessage.addLogging(loggingList, "DIAMETER_ERROR_DSAI_NOT_AVAILABLE", HopByHopId, EndToEndId, resultCode);
				break;
			case 5490:
				LogMessage.addLogging(loggingList, "The requesting GMLC's network is not authorized to request UE location information!", HopByHopId, EndToEndId, resultCode);
				break;
		}
		return result;
	}
}
