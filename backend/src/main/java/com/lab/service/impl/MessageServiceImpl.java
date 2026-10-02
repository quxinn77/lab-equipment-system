package com.lab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.BusinessException;
import com.lab.common.PageResult;
import com.lab.entity.Message;
import com.lab.interceptor.UserContext;
import com.lab.mapper.MessageMapper;
import com.lab.service.MessageService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private MessageMapper messageMapper;

    @Override
    public void send(Long userId, String title, String content, String type) {
        if (userId == null) {
            return;
        }
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type == null ? "SYSTEM" : type);
        message.setIsRead(0);
        messageMapper.insert(message);
    }

    @Override
    public PageResult<Message> myMessages(Integer isRead, int pageNum, int pageSize) {
        Long userId = UserContext.userId();
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        wrapper.orderByDesc(Message::getId);
        Page<Message> result = messageMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public long unreadCount() {
        Long userId = UserContext.userId();
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    public void markRead(Long id) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        Long userId = UserContext.userId();
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该消息");
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    @Override
    public void readAll() {
        Long userId = UserContext.userId();
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }
}
