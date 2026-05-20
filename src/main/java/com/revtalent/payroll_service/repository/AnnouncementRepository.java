package com.revtalent.payroll_service.repository;

import com.revtalent.payroll_service.model.Announcement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

}
