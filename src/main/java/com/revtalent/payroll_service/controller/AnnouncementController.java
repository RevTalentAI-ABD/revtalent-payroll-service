package com.revtalent.payroll_service.controller;

import com.revtalent.payroll_service.model.Announcement;
import com.revtalent.payroll_service.repository.AnnouncementRepository;
import com.revtalent.payroll_service.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")

public class AnnouncementController {

    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private NotificationService notificationService;

    // GET ALL
    @GetMapping
    public List<Announcement> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Announcement announcement) {
        try {
            announcement.setCreatedAt(
                    java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            Announcement saved = repository.save(announcement);

            // 📣 Notify all managers about this announcement
            String notifMessage = "📢 New Announcement: " + saved.getTitle() + " — " + saved.getMessage();
            notificationService.broadcastAnnouncementToAllManagers(notifMessage);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
