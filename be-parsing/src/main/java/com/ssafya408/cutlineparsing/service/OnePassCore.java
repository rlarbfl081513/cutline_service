package com.ssafya408.cutlineparsing.service;

import java.util.List;

public interface OnePassCore {
    List<MonthOutput> run(String kakaoExportText, String meDisplayName, String friendDisplayName);
}