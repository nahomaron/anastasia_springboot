package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.seeder.DataSeeder;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserSeeder {
    private static final Logger logger = LoggerFactory.getLogger(UserSeeder.class);  // ✅ Use SLF4J logger
    private final UserRepository userRepository;

    public List<UserEntity> seedUsers() {
        if (userRepository.count() == 0) {
            Faker faker = new Faker();
            List<UserEntity> users = new ArrayList<>();

            for (int i = 0; i <= 100; i++) {
                String password = "{noop}Password123!";

                UserEntity user = UserEntity.builder()
                        .fullName(faker.name().fullName())
                        .email(faker.internet().emailAddress())
                        .password(password)
                        .userType(UserType.GUEST)
                        .build();

                users.add(user);
            }

            List<UserEntity> savedUsers = userRepository.saveAll(users);
            logger.info("Seeded {} fake users", savedUsers.size());
            return savedUsers;
        }

        return Collections.emptyList();
    }

}
