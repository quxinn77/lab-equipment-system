package com.lab.mapper;

import com.lab.vo.MonthCountVO;
import com.lab.vo.NameCountVO;
import com.lab.vo.NameValueVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface StatsMapper {

    @Select("SELECT status AS name, COUNT(*) AS value FROM device WHERE deleted = 0 GROUP BY status")
    List<NameValueVO> countDeviceByStatus();

    @Select("SELECT l.name AS name, COUNT(d.id) AS value FROM lab l " +
            "LEFT JOIN device d ON d.lab_id = l.id AND d.deleted = 0 " +
            "GROUP BY l.id, l.name ORDER BY l.id")
    List<NameValueVO> countDeviceByLab();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS count FROM borrow_record " +
            "WHERE create_time >= #{start} GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month")
    List<MonthCountVO> countBorrowMonthly(@Param("start") Date start);

    @Select("SELECT d.name AS name, COUNT(*) AS count FROM borrow_record b " +
            "JOIN device d ON b.device_id = d.id " +
            "GROUP BY b.device_id, d.name ORDER BY count DESC LIMIT #{limit}")
    List<NameCountVO> topDevices(@Param("limit") int limit);

    @Select("SELECT u.real_name AS name, COUNT(*) AS count FROM borrow_record b " +
            "JOIN sys_user u ON b.user_id = u.id " +
            "GROUP BY b.user_id, u.real_name ORDER BY count DESC LIMIT #{limit}")
    List<NameCountVO> userRank(@Param("limit") int limit);

    @Select("SELECT status AS name, COUNT(*) AS value FROM repair_record GROUP BY status")
    List<NameValueVO> countRepairByStatus();

    @Select("SELECT d.name AS name, COUNT(*) AS count FROM repair_record r " +
            "JOIN device d ON r.device_id = d.id " +
            "GROUP BY r.device_id, d.name ORDER BY count DESC LIMIT #{limit}")
    List<NameCountVO> topFaultDevices(@Param("limit") int limit);
}
