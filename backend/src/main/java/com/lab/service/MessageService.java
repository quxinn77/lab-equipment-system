package com.lab.service;

import com.lab.common.PageResult;
import com.lab.entity.Message;

public interface MessageService {

    /** 发送站内信 */
    void send(Long userId, String title, String content, String type);

    /** 我的消息分页 */
    PageResult<Message> myMessages(Integer isRead, int pageNum, int pageSize);

    /** 未读数量 */
    long unreadCount();

    /** 标记已读 */
    void markRead(Long id);

    /** 全部已读 */
    void readAll();
}
