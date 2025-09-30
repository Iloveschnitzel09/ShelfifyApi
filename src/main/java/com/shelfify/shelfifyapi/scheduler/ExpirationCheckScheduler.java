package com.shelfify.shelfifyapi.scheduler;

import com.shelfify.shelfifyapi.model.Products;
import com.shelfify.shelfifyapi.repository.ProduktRepository;
import com.shelfify.shelfifyapi.repository.UserRepository;
import com.shelfify.shelfifyapi.service.EmailService;
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

    @Autowired
    private UserRepository userRepo;

    // Jeden Montag um 7:00 Uhr
    @Scheduled(cron = "0 53 23 * * TUE")
    public void checkExpiringProducts() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);

        userRepo.findAll().stream()
            .filter(user -> user.isVerified() && user.isNotify())
            .forEach(user -> {
                String datagroup = user.getDatagroup();
                String email = user.getEmail();

                List<Products> expiredProducts = produktRepo.findByAblaufdatumBeforeAndDatagroup(today, datagroup);

                List<Products> expiringProducts = produktRepo.findByAblaufdatumBetweenAndDatagroup(today, sevenDaysFromNow, datagroup);

                if (!expiredProducts.isEmpty() || !expiringProducts.isEmpty()) {
                    emailService.sendExpirationAlert(expiredProducts, expiringProducts, email);
                }
        });
    }
} 