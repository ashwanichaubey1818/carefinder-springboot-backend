package com.carefinder.backend.config;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalRepository;
import com.carefinder.backend.insurance.InsuranceProvider;
import com.carefinder.backend.insurance.InsuranceProviderRepository;
import com.carefinder.backend.user.Role;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<String> INSURERS = List.of(
            "Star Health",
            "Niva Bupa",
            "HDFC ERGO",
            "ICICI Lombard",
            "Care Health",
            "Aditya Birla Health",
            "Bajaj Allianz",
            "Tata AIG"
    );

    private static final List<List<String>> SPECIALTY_GROUPS = List.of(
            List.of("Cardiology", "Neurology", "Emergency Care"),
            List.of("Oncology", "Orthopaedics", "Critical Care"),
            List.of("Paediatrics", "Obstetrics", "General Surgery"),
            List.of("Nephrology", "Gastroenterology", "Internal Medicine"),
            List.of("Cardiac Surgery", "Pulmonology", "Diagnostics")
    );

    private static final Map<String, Coordinates> COORDINATES = Map.ofEntries(
            Map.entry("New Delhi", new Coordinates(28.6139, 77.2090)),
            Map.entry("Mumbai", new Coordinates(19.0760, 72.8777)),
            Map.entry("Bengaluru", new Coordinates(12.9716, 77.5946)),
            Map.entry("Chennai", new Coordinates(13.0827, 80.2707)),
            Map.entry("Hyderabad", new Coordinates(17.3850, 78.4867)),
            Map.entry("Kolkata", new Coordinates(22.5726, 88.3639)),
            Map.entry("Bhubaneswar", new Coordinates(20.2961, 85.8245)),
            Map.entry("Pune", new Coordinates(18.5204, 73.8567)),
            Map.entry("Ahmedabad", new Coordinates(23.0225, 72.5714)),
            Map.entry("Jaipur", new Coordinates(26.9124, 75.7873)),
            Map.entry("Lucknow", new Coordinates(26.8467, 80.9462)),
            Map.entry("Kochi", new Coordinates(9.9312, 76.2673)),
            Map.entry("Chandigarh", new Coordinates(30.7333, 76.7794)),
            Map.entry("Mohali", new Coordinates(30.7046, 76.7179)),
            Map.entry("Patna", new Coordinates(25.5941, 85.1376)),
            Map.entry("Bhopal", new Coordinates(23.2599, 77.4126)),
            Map.entry("Indore", new Coordinates(22.7196, 75.8577)),
            Map.entry("Nagpur", new Coordinates(21.1458, 79.0882)),
            Map.entry("Guwahati", new Coordinates(26.1445, 91.7362)),
            Map.entry("Rishikesh", new Coordinates(30.0869, 78.2676)),
            Map.entry("Dehradun", new Coordinates(30.3165, 78.0322)),
            Map.entry("Ranchi", new Coordinates(23.3441, 85.3096)),
            Map.entry("Raipur", new Coordinates(21.2514, 81.6296)),
            Map.entry("Jammu", new Coordinates(32.7266, 74.8570)),
            Map.entry("Katra", new Coordinates(32.9915, 74.9318)),
            Map.entry("Srinagar", new Coordinates(34.0837, 74.7973)),
            Map.entry("Shimla", new Coordinates(31.1048, 77.1734)),
            Map.entry("Bilaspur", new Coordinates(31.3380, 76.7565)),
            Map.entry("Panaji", new Coordinates(15.4909, 73.8278)),
            Map.entry("Visakhapatnam", new Coordinates(17.6868, 83.2185)),
            Map.entry("Vijayawada", new Coordinates(16.5062, 80.6480)),
            Map.entry("Coimbatore", new Coordinates(11.0168, 76.9558)),
            Map.entry("Shillong", new Coordinates(25.5788, 91.8933)),
            Map.entry("Imphal", new Coordinates(24.8170, 93.9368)),
            Map.entry("Agartala", new Coordinates(23.8315, 91.2868))
    );

    private final AppProperties properties;
    private final InsuranceProviderRepository insuranceRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DataInitializer(
            AppProperties properties,
            InsuranceProviderRepository insuranceRepository,
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.properties = properties;
        this.insuranceRepository = insuranceRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        validateProductionCredentials();
        if (properties.seedData()) {
            seedInsurers();
            seedHospitals();
        }
        if (properties.bootstrapAdmin()) {
            seedAccounts();
        }
    }

    private void validateProductionCredentials() {
        if (!environment.acceptsProfiles(Profiles.of("prod")) || !properties.bootstrapAdmin()) {
            return;
        }
        String adminPassword = properties.adminPassword().toLowerCase(Locale.ROOT);
        String staffPassword = properties.staffPassword().toLowerCase(Locale.ROOT);
        if (adminPassword.contains("changeme") || staffPassword.contains("changeme")) {
            throw new IllegalStateException(
                    "Unique ADMIN_PASSWORD and STAFF_PASSWORD values are required when bootstrapping prod."
            );
        }
    }

    private void seedInsurers() {
        if (insuranceRepository.count() > 0) {
            return;
        }
        List<InsuranceProvider> providers = INSURERS.stream()
                .map(name -> new InsuranceProvider(
                        name,
                        slug(name),
                        "Cashless participation must be verified for the member's exact policy and treatment."
                ))
                .toList();
        insuranceRepository.saveAll(providers);
        log.info("Seeded {} insurance providers", providers.size());
    }

    private void seedHospitals() {
        if (hospitalRepository.count() > 0) {
            return;
        }
        Map<String, InsuranceProvider> providers = new LinkedHashMap<>();
        insuranceRepository.findAll().forEach(provider -> providers.put(provider.getName(), provider));
        List<Hospital> hospitals = new ArrayList<>();
        List<String> lines = readSeedLines();

        for (int index = 0; index < lines.size(); index++) {
            int frontendId = index + 1;
            String[] values = lines.get(index).split("\\|", -1);
            String name = values[0].trim();
            String city = values[1].trim();
            String state = values[2].trim();
            Coordinates base = COORDINATES.getOrDefault(city, new Coordinates(20.5937, 78.9629));
            double offset = ((frontendId % 5) - 2) * 0.012;

            Hospital hospital = new Hospital(name, slug(name), city, state);
            hospital.setLatitude(round4(base.latitude() + offset));
            hospital.setLongitude(round4(base.longitude() - offset / 2));
            hospital.setRating(Math.round((4 + ((frontendId * 7) % 9) / 10.0) * 10.0) / 10.0);
            hospital.setReviews(320 + ((frontendId * 137) % 2500));
            hospital.setEmergency(frontendId % 7 != 0);
            hospital.setOpen24x7(frontendId % 5 != 0);
            hospital.setType(frontendId % 10 == 0
                    ? "Teaching & research hospital"
                    : "Multi-speciality hospital");
            hospital.setBeds(150 + ((frontendId * 37) % 650));
            hospital.setAccreditation(frontendId % 4 == 0 ? "NABH & JCI" : "NABH");
            hospital.setAddress(city + ", " + state + ", India");
            hospital.setPhone("+91 1800 000 " + String.format("%04d", frontendId));
            hospital.setDescription(
                    "CareFinder directory profile for " + name
                            + ". Confirm appointments, clinical services and cashless eligibility directly."
            );
            hospital.getSpecialties().addAll(SPECIALTY_GROUPS.get(index % SPECIALTY_GROUPS.size()));
            int insuranceCount = 3 + (frontendId % 3);
            for (int insuranceIndex = 0; insuranceIndex < insuranceCount; insuranceIndex++) {
                String insurerName = INSURERS.get((frontendId + insuranceIndex * 2) % INSURERS.size());
                hospital.getInsuranceProviders().add(providers.get(insurerName));
            }
            hospitals.add(hospital);
        }
        hospitalRepository.saveAll(hospitals);
        log.info("Seeded {} hospitals from the Angular directory", hospitals.size());
    }

    private void seedAccounts() {
        String adminEmail = properties.adminEmail().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(adminEmail).orElseGet(() -> userRepository.save(
                new UserAccount(
                        "CareFinder Administrator",
                        adminEmail,
                        "+91 90000 00000",
                        passwordEncoder.encode(properties.adminPassword()),
                        Role.ADMIN
                )
        ));

        String staffEmail = properties.staffEmail().trim().toLowerCase(Locale.ROOT);
        UserAccount staff = userRepository.findByEmailIgnoreCase(staffEmail).orElseGet(() -> userRepository.save(
                new UserAccount(
                        "Demo Hospital Staff",
                        staffEmail,
                        "+91 90000 00001",
                        passwordEncoder.encode(properties.staffPassword()),
                        Role.HOSPITAL_STAFF
                )
        ));
        hospitalRepository.findById(1L).ifPresent(hospital -> {
            if (hospital.getManager() == null) {
                hospital.setManager(staff);
            }
        });
    }

    private List<String> readSeedLines() {
        ClassPathResource resource = new ClassPathResource("db/seed/hospitals.csv");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            return reader.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read hospital seed data.", exception);
        }
    }

    private String slug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private record Coordinates(double latitude, double longitude) {
    }
}
