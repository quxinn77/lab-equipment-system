package com.lab.service;

import com.lab.vo.MonthCountVO;
import com.lab.vo.NameCountVO;
import com.lab.vo.NameValueVO;
import com.lab.vo.OverviewVO;

import java.util.List;
import java.util.Map;

public interface StatsService {

    OverviewVO overview();

    List<NameValueVO> deviceByStatus();

    List<NameValueVO> deviceByLab();

    List<MonthCountVO> borrowMonthly(int months);

    List<NameCountVO> topDevices(int limit);

    Map<String, Object> overdueStats();

    Map<String, Object> repairSummary();

    List<NameCountVO> userRank(int limit);
}
