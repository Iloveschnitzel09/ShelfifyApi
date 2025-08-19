package com.lager.lagerappapi.scheduler;

import com.lager.lagerappapi.model.Produkte;
import com.lager.lagerappapi.repository.ProduktRepository;
import com.lager.lagerappapi.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ExpirationCheckScheduler {

    @Autowired
    private ProduktRepository produktRepo;

    @Autowired
    private EmailService emailService;

    // Jeden Montag um 7:00 Uhr
    @Scheduled(cron = "0 0 7 * * MON")
    public void checkExpiringProducts() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        
        // Bereits abgelaufene Produkte
        List<Produkte> expiredProducts = produktRepo.findByAblaufdatumBefore(today);
        
        // Produkte die in den nächsten 7 Tagen ablaufen
        List<Produkte> expiringProducts = produktRepo.findByAblaufdatumBetween(today, sevenDaysFromNow);
        
        if (!expiredProducts.isEmpty() || !expiringProducts.isEmpty()) {
            emailService.sendExpirationAlert(expiredProducts, expiringProducts);
        }
    }
} 