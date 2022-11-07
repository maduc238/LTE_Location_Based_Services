package com.example.LCS;

import java.util.Date;
import java.util.List;

public class LogMessage {
    static void addLogging(List<Logging> loggingList, String data, Long HopByHopId, Long EndToEndId, long resultCode) {
        Logging logs = new Logging();
        Date date = new Date();
		logs.setTime(date.toString());
		logs.setData("Hop by Hop Identifier: "+HopByHopId+". End to End Identifier: "+EndToEndId+". Result Code = "+resultCode+": "+data);
		loggingList.add(logs);
	}

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

}
