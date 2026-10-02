package com.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.entity.Message;
import com.lab.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping
    public Result<PageResult<Message>> my(@RequestParam(required = false) Integer isRead,
                                          @RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(messageService.myMessages(isRead, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.ok();
    }
}
