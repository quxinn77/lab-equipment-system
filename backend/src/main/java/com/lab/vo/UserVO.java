package com.lab.vo;

import com.lab.entity.SysUser;
import lombok.Data;

import java.util.Date;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String college;
    private String phone;
    private String email;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Integer status;
    private Integer overdueCount;
    private Date createTime;

    public static UserVO from(SysUser user, String roleCode, String roleName) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setCollege(user.getCollege());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRoleId(user.getRoleId());
        vo.setRoleCode(roleCode);
        vo.setRoleName(roleName);
        vo.setStatus(user.getStatus());
        vo.setOverdueCount(user.getOverdueCount());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
